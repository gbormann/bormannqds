package com.bormannqds.mds.lib.protocoladaptor;

import com.bormannqds.mds.lib.configuration.MulticastConfigResource;
import com.bormannqds.mds.lib.gateway.ApplicationContext;
import com.bormannqds.mds.lib.protobufmessages.MarketData.MarketDataMessage;
import com.bormannqds.mds.lib.protobufmessages.MarketData.TobQuote;
import com.bormannqds.mds.lib.protobufmessages.MarketData.Trade;
import com.bormannqds.mds.lib.protocoladaptor.utils.ChronoUtils;
import com.bormannqds.mds.lib.protocoladaptor.utils.LegacyConverter;

import com.bormannqds.mds.lib.referencedata.InternalSymbolParser;
import com.bormannqds.mds.lib.referencedata.ReferenceDataResource;
import com.bormannqds.lib.dataaccess.referencedata.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zeromq.ZFrame;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Poller;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMsg;

import java.time.LocalDate;
import java.util.*;

/**
 * input:	ZContext, control port, data port, MC config
 * 
 * maps:
 * 		active symbol -> multiplier
 * 		active symbol -> message statistics
 * set:	active symbols(front month, second front month) (=> topics when {L|T}-coded)
 * 
 * creations:
 *		(i)   REP socket
 *		(ii)  SUB sockets
 *		(iii) PUB socket
 *
 *	connections:
 *		1. REP socket -> bind control port
 *		2. PUB socket -> bind data port
 *		3. For each SUB socket: connect MC address on START cmd
 *
 *	conversations:
 * S1:
 *  ADD <base symbol> on REP :
 *      if (instrument(<base symbol>) exists):
 *	        if (instrument(<base symbol>) suppported type):
 *		        if (roll schedule(<base symbol>) exists):
 *		            generate active symbols(base symbol) -> topic set
 *		            active symbols -> new msg stats record
 *		            active symbols -> multiplier
 *  		        reply DONE -> REP
 *	    	        restart S1
 *		        else:
 *		            reply NO CONFIG -> REP
 *		            restart S1
 *		    else:
 *		        reply UNSUPPORTED -> REP
 *		        restart S1
 *		else:
 *		    reply UNRECOGNISED -> REP
 *		    restart S1
 *	START on REP:
 *	    connect Trade SUB socket -> MC Trade address
 *		connect ToB Quote SUB socket -> MC Book address
 *		For each active symbol:
 *		    subscribe to topic=T<active symbol> on Trade SUB socket
 *			subscribe to topic=L<active symbol> on ToB Quote SUB socket
 *		READY -> REP
 *
 * S2:
 *  Poll loop {CTRL socket, SUB sockets}:
 *      Ctrl msg: "parse command and act accordingly"
 *          ...
 *		Trade msg: transform to Protobuf with Trade payload
 *			send Protobuf -> PUB socket
 *		ToB Quote msg: transform to Protobuf with ToB Quote payload
 *			send Protobuf -> PUB socket
 *
 * NOTE: This class needs today's date to determine front mont and second month symbols. In order to work properly any
 * application using this class needs to restart after midnight or provide a way to re-evaluate today's date.
 *
 * Created by bormanng on 24/06/15.
 */
public class DataPump implements Runnable {
    public static final String ADD_REQ_CMD = "ADD";
    public static final String START_CMD = "START";
    public static final String SUSPEND_CMD = "SUSPEND";
    public static final String RESUME_CMD = "RESUME";
    public static final String STOP_CMD = "STOP";
    public static final String DONE_REPLY = "DONE"; // to ADD_REQ_CMD, SUSPEND_CMD, RESUME_CMD, STOP_CMD
    public static final String UNRECOGNISED_REPLY = "UNRECOGNISED"; // to ADD_REQ_CMD
    public static final String NO_CONFIG_REPLY = "NO CONFIG"; // to ADD_REQ_CMD
    public static final String UNSUPPORTED_REPLY = "UNSUPPORTED"; // to ADD_REQ_CMD
    public static final String READY_REPLY = "READY"; // to START_CMD

    //public:
    public DataPump(final Connection ctrlConn, final ComboConnection mcConn, final Connection dataConn) {
        this.ctrlConn = ctrlConn;
        this.mcConn = mcConn;
        this.dataConn = dataConn;
    }

    public Connection getCtrlConnection() {
        return ctrlConn;
    }

    public Connection getDataConnection() {
        return dataConn;
    }

    public void start(final String name) {
        if (me == null) { // avoid accidental thread rabbits
            me = new Thread(this, name);
            me.start();
            try {
                Thread.sleep(5); // allow the thread to settle in before allowing clients to connect
            }
            catch (InterruptedException ie) {
                LOGGER.warn("Settle-in sleep interrupted...", ie);
            }
        }
    }

    public void stop() {
        if (me != null) { // avoid cascading interrupts
            Thread moriturus = me;
            me = null;
            if (moriturus.isAlive()) {
                try {
                    Thread.sleep(5); // allow the thread to wind down before cutting clients off
                }
                catch (InterruptedException ie) {
                    LOGGER.warn("Wind-down sleep interrupted...", ie);
                }
                moriturus.interrupt();
            }
        }
    }

    /**
     * First inefficient implementation of listening to multicast data by subscribing to all and dropping updates on
     * instruments not on the interest list. Explicit subscriptions based on active contracts would eliminate updates
     * on the pub side! OTOH, topic wildcard implementation is primitive and company topic structuring is poor.
     *
     * TODO Design smarter topic structure
     *      such as {L|T}/<base symbol>/maturity, e.g. L/CL/1507 (outrights) and T/RB-HO/1509-1512 (spreads)
     *      allowing generic subscription to all crack spread maturities as follows: L/RB-HO/*
     *
     * TODO Figure out why topics still have prefixes despite different addresses.
     */
    public void run() {
        // let's be ready asap on the server connection before the thundering horde arrives
    	Socket ctrlSocket = Connection.createSocket(ZMQ.REP); // Control connection
        ctrlSocket.bind(ctrlConn.getAddress());

        Socket outSocket = Connection.createSocket(ZMQ.PUB); // Egress connection
        outSocket.bind(dataConn.getAddress());

        if (processControlMsgsUntilStartCmd(ctrlSocket)) { // we were interrupted, time to leave
            fsmState = PumpFsmState.S2_STARTED;
            if (processMsgsUntilStopCmd(ctrlSocket, outSocket)) {
                ctrlSocket.close();
            }
        }

        outSocket.close();
        fsmState = PumpFsmState.HALT;
    }

    //private:
    private interface MdsFrameFsm<ConcreteMdsFrameFsm extends MdsFrameFsm> {
        ConcreteMdsFrameFsm nextState();
    }

    private enum PumpFsmState {
        START, RECEIVING_ADD_REQS, S2_STARTED, RECEIVING_CMD_OR_MD, SUSPENDED, STOPPED, HALT
    }

    private enum TradeFrameFsm implements MdsFrameFsm<TradeFrameFsm> {
        SYMBOL { public TradeFrameFsm nextState() { return PRICE; } },
        PRICE { public TradeFrameFsm nextState() { return SIZE; } },
        SIZE { public TradeFrameFsm nextState() { return SEQ_NO; } },
        SEQ_NO { public TradeFrameFsm nextState() { return ICE_LEG_SYSTEMPRICED; } },
        ICE_LEG_SYSTEMPRICED { public TradeFrameFsm nextState() { return HALT; } }, // for ICE trade updates: isSystemImpliedLeg flag
        HALT { public TradeFrameFsm nextState() { return HALT; } }
    }

    private enum TobQuoteFrameFsm implements MdsFrameFsm<TobQuoteFrameFsm> {
        SYMBOL { public TobQuoteFrameFsm nextState() { return ASK; } },
        ASK { public TobQuoteFrameFsm nextState() { return ASK_SIZE; } },
        ASK_SIZE { public TobQuoteFrameFsm nextState() { return BID; } },
        BID { public TobQuoteFrameFsm nextState() { return BID_SIZE; } },
        BID_SIZE { public TobQuoteFrameFsm nextState() { return SEQ_NO; } },
        SEQ_NO { public TobQuoteFrameFsm nextState() { return HALT; } },
        HALT { public TobQuoteFrameFsm nextState() { return HALT; } }
    }

    private static class SourceMessageStatistics {
        //public:
        public void incDropCount() {
            ++dropCount;
        }

        public void incSysPricedLegCount() {
            ++sysPricedLegCount;
        }

        // feed messages dropped for various reasons, including business reasons (such as filtering implied legs).
        public int getDropCount() {
            return dropCount;
        }

        public int getSysPricedLegCount() {
            return sysPricedLegCount;
        }

        //private:
        private int dropCount = 0;
        private int sysPricedLegCount = 0;
    }

    private boolean processControlMsgsUntilStartCmd(final Socket ctrlSocket) {
        Thread thisThread = Thread.currentThread();

        // S1
        // --
        ReferenceDataResource refDataResource = ApplicationContext.getInstance().getReferenceDataResource();
        FqSymbols oSymbolGen = new FqSymbols<>(ApplicationContext.getInstance().getOutRightsRollSchedulesResource(), InternalSymbolParser.getInstance());
        FqSymbols sSymbolGen = new FqSymbols<>(ApplicationContext.getInstance().getSpreadsRollSchedulesResource(), InternalSymbolParser.getInstance());
        MulticastConfigResource mcConfigResource = ApplicationContext.getInstance().getMulticastConfigResource();
        LocalDate today = ApplicationContext.getInstance().today();
        fsmState = PumpFsmState.RECEIVING_ADD_REQS;
        while (me == thisThread && !Thread.currentThread().isInterrupted()) {
            final ZMsg cmdMsg;
            cmdMsg = ZMsg.recvMsg(ctrlSocket);
            if (cmdMsg == null) {
                LOGGER.warn("Interrupted at ZMsg.recvMsg()...");
                break;
            }
            final String cmd = cmdMsg.popString();
            LOGGER.debug("cmd: "+cmd);
            switch (cmd) {
                case ADD_REQ_CMD:
                    final String reqSymbol = cmdMsg.popString();
                    final ZMsg replyMsg = new ZMsg();
                    String baseSymbol = reqSymbol; // ref data and multicast config require naked base symbols for outrights and exchange spreads
                    try {
                        final List<ExpiringInstrumentSymbolComponents> symbolComponents = InternalSymbolParser.getInstance().parse(reqSymbol);
                        if (symbolComponents.size() == 1) {
                            baseSymbol = symbolComponents.get(0).getBaseSymbol();
                        }
                        final Instrument instrument = refDataResource.getInstrument(baseSymbol);
                        FqSymbols symbolGenToUse = null;
                        switch (instrument.getProductType()) {
                            case Future:
                                symbolGenToUse = oSymbolGen;
                                break;
                            case Spread:
                                symbolGenToUse = sSymbolGen;
                                break;
                        }
                        if (symbolGenToUse == null) {
                            throw new UnsupportedInstrumentTypeException(baseSymbol + ": instrument of unsupported type " + instrument.getProductType());
                        }
                        final String fqSymbol = symbolGenToUse.expandToFqSymbolOn(today, reqSymbol);
                        if (fqSymbol == null) {
                            LOGGER.error(reqSymbol + ": front month for today is not covered by roll schedule. Please extend!");
                            replyMsg.add(NO_CONFIG_REPLY);
                            replyMsg.send(ctrlSocket);
                        }
                        else {
                            double multiplier = mcConfigResource.getMulticastConfig(baseSymbol).getMultiplier();
                            activeSymbols.add(fqSymbol);
                            msgStats.put(fqSymbol, new SourceMessageStatistics());
                            multiplierCache.put(fqSymbol, multiplier);
                            replyMsg.add(DONE_REPLY);
                            replyMsg.send(ctrlSocket);
                        }
                    }
                    catch (MissingInstrumentException mie) {
                        LOGGER.warn(baseSymbol + ": reference data not found.", mie);
                        replyMsg.add(UNRECOGNISED_REPLY);
                        replyMsg.send(ctrlSocket);
                    }
                    catch (UnsupportedInstrumentTypeException uie) {
                        LOGGER.warn(baseSymbol + ": instrument of unsupported type.", uie);
                        replyMsg.add(UNSUPPORTED_REPLY);
                        replyMsg.send(ctrlSocket);
                    }
                    catch (MissingRollScheduleException mrse) {
                        LOGGER.warn(reqSymbol + ": roll schedule not found.", mrse);
                        replyMsg.add(NO_CONFIG_REPLY);
                        replyMsg.send(ctrlSocket);
                    }
                    catch (UnrecognisedSymbolException e) {
                        e.printStackTrace();
                    }
                    break;
                case START_CMD:
                    cmdMsg.destroy();
                    return true;
                default:
                    LOGGER.warn(cmd + ": unsupported command received in S1.");
                    break;
            }
            cmdMsg.destroy();
        }
        return false; // we were interrupted!
    }

    private void generateTopics() {
        for (final String activeSymbol: activeSymbols) { // no topics if there are no active symbols defined!
            final StringBuilder topicBuilder = new StringBuilder();
            topicBuilder.append(activeSymbol).append('|'); // later, on a single stream, we need both anyway (and we'd just eliminate one of the two topics collections)
            tradeTopics.add(topicBuilder.toString());
            topicBuilder.setLength(0);
            topicBuilder.append(activeSymbol).append('|');
            quoteTopics.add(topicBuilder.toString());
            topicBuilder.setLength(0);
        }
        LOGGER.debug("Topics generated :");
        tradeTopics.forEach(LOGGER::debug);
        quoteTopics.forEach(LOGGER::debug);
    }

    private void subscribe(final Socket quotesSocket, final Socket tradesSocket) {
        // no subscriptions if there are no topics defined!
        tradeTopics.forEach(t -> tradesSocket.subscribe(t.getBytes()));
        quoteTopics.forEach(t -> quotesSocket.subscribe(t.getBytes()));
    }

    private void unsubscribe(final Socket quotesSocket, final Socket tradesSocket) {
        // no unsubscriptions if there are no topics defined!
        tradeTopics.forEach(t -> tradesSocket.unsubscribe(t.getBytes()));
        quoteTopics.forEach(t -> quotesSocket.unsubscribe(t.getBytes()));
    }

    private String getSymbol(final String topic) {
        return topic.substring(0, topic.length() - 2); // pinch off |L or |T
    }

    private double getPrice(final String symbol, final ZFrame frame) {
        return multiplierCache.get(symbol) * LegacyConverter.netDecimalBytes2Double(frame.getData());
    }

    private int getSize(final ZFrame frame) {
        //int size = LegacyConverter.netIntBytes2Int(frame.getData());
        int sanerSizeQM = LegacyConverter.sanerNetAbsIntBytes2Int(frame.getData());
        //LOGGER.debug(size + "(orig) vs " + sanerSizeQM + "(sane?)");
        return sanerSizeQM;
    }

    private boolean processIceTradeFlag(final String symbol, final ZFrame frame) {
        if (LegacyConverter.sanerNetAbsIntBytes2Int(frame.getData()) > 0) {
            msgStats.get(symbol).incSysPricedLegCount();
            return true;
        }

        return false;
    }

    /** NOTES:
     *  1. Prize field
     *  o Should always work since the multiplier is read from the multicast config so missing values
     *    would be flagged there.
     *  o We convert price first to double and then scale by multiplier because it's faster. Scaling
     *    first would give more false precision were it not that multiplier is read as a double from the
     *    multicast config file.
     */
    private MarketDataMessage transformTradeMsg(final String symbol, final long gwTs, final ZMsg zMsg) {
        // zMsg is a multi-frame, not a serialised blob
        MarketDataMessage.Builder marketDataMsgBuilder = MarketDataMessage.newBuilder();
        Trade.Builder tradeBuilder = Trade.newBuilder();
        marketDataMsgBuilder.setGatewayTimestamp(gwTs);

        TradeFrameFsm frameState = TradeFrameFsm.SYMBOL;
        marketDataMsgBuilder.setSymbol(symbol);
        frameState = frameState.nextState();
        try {
            for (ZFrame frame: zMsg) {
                switch (frameState) {
                    case PRICE: tradeBuilder.setPrice(getPrice(symbol, frame)); frameState = frameState.nextState(); break;
                    case SIZE: tradeBuilder.setSize(getSize(frame)); frameState = frameState.nextState(); break;
                    case SEQ_NO: marketDataMsgBuilder.setSeqNo(LegacyConverter.sanerNetAbsIntBytes2Int(frame.getData())); frameState = frameState.nextState(); break;
                    case ICE_LEG_SYSTEMPRICED: processIceTradeFlag(symbol, frame); frameState = frameState.nextState(); break;
                    case HALT: LOGGER.warn("Superfluous frame received on trade message: " + Arrays.toString(frame.getData())); frameState = frameState.nextState(); break; // might have to add encoding for logging
                }
            }
        }
        catch (Exception e) {
            LOGGER.catching(e);
        }
        if (!(frameState == TradeFrameFsm.HALT || frameState == TradeFrameFsm.ICE_LEG_SYSTEMPRICED)) {
            LOGGER.error("Parsing problem. Next expected frame : " + frameState.name());
            return null;
        }
        marketDataMsgBuilder.setTrade(tradeBuilder);
        marketDataMsgBuilder.setTimestamp(ChronoUtils.estimateHpNowInUs());
        return marketDataMsgBuilder.build();
    }

    /** NOTES:
     * Ask/Bid Price
     *  o Should always work since the multiplier is read from the multicast config so missing values
     *    would be flagged there.
     *  o We convert price first to double and then scale by multiplier because it's faster. Scaling
     *    first would give more false precision were it not that multiplier is read as a double from the
     *    multicast config file...not to mention that ICE Decimals are derived from doubles...
     */
    private MarketDataMessage transformTobQuoteMsg(final String symbol, final long gwTs, final ZMsg zMsg) { // zMsg is a multi-frame, not a serialised blob
        MarketDataMessage.Builder marketDataMsgBuilder = MarketDataMessage.newBuilder();
        TobQuote.Builder tobQuoteBuilder = TobQuote.newBuilder();
        marketDataMsgBuilder.setGatewayTimestamp(gwTs);

        TobQuoteFrameFsm frameState = TobQuoteFrameFsm.SYMBOL;
        marketDataMsgBuilder.setSymbol(symbol);
        frameState = frameState.nextState();
        try {
            for (ZFrame frame: zMsg) {
                switch (frameState) {
                    case ASK: tobQuoteBuilder.setAsk(getPrice(symbol, frame)); frameState = frameState.nextState(); break;
                    case ASK_SIZE: tobQuoteBuilder.setAskSize(getSize(frame)); frameState = frameState.nextState(); break;
                    case BID: tobQuoteBuilder.setBid(getPrice(symbol, frame)); frameState = frameState.nextState(); break;
                    case BID_SIZE: tobQuoteBuilder.setBidSize(getSize(frame)); frameState = frameState.nextState(); break;
                    case SEQ_NO: marketDataMsgBuilder.setSeqNo(LegacyConverter.sanerNetAbsIntBytes2Int(frame.getData())); frameState = frameState.nextState(); break;
                    case HALT: LOGGER.warn("Superfluous frame received on quote message: " + Arrays.toString(frame.getData())); frameState = frameState.nextState(); break; // might have to add encoding for logging
                }
            }
        }
        catch (Exception e) {
            LOGGER.catching(e);
        }
        if (frameState != TobQuoteFrameFsm.HALT) {
            LOGGER.error("Parsing problem. Next expected frame : " + frameState.name());
            return null;
        }
        marketDataMsgBuilder.setTobQuote(tobQuoteBuilder);
        marketDataMsgBuilder.setTimestamp(ChronoUtils.estimateHpNowInUs());
        return marketDataMsgBuilder.build();
    }

    private SourceMessageStatistics retrieveMsgStats(String symbol) {
        SourceMessageStatistics srcMsgStats = msgStats.get(symbol);
        if (srcMsgStats == null) {
            srcMsgStats = new SourceMessageStatistics();
            msgStats.put(symbol, srcMsgStats);
        }

        return srcMsgStats;
    }

    private boolean processMsgsUntilStopCmd(Socket ctrlSocket, Socket outSocket) {
        Thread thisThread = Thread.currentThread();

        // S2
        // --
        Socket quotesSocket = Connection.createSocket(ZMQ.SUB); // Ingress connection
        Socket tradesSocket = Connection.createSocket(ZMQ.SUB); // Ingress connection

        // client connections
        quotesSocket.connect(mcConn.getQuotesAddress());
        tradesSocket.connect(mcConn.getTradesAddress());

        Poller inPoller = new Poller(3);
        inPoller.register(ctrlSocket, Poller.POLLIN); // ndx: 0
        inPoller.register(tradesSocket, Poller.POLLIN); // ndx: 1
        inPoller.register(quotesSocket, Poller.POLLIN); // ndx: 2

        // subscribe MC topics and signal readiness
        generateTopics();
        subscribe(quotesSocket, tradesSocket);

        ZMsg replyMsg = new ZMsg();
        replyMsg.add(READY_REPLY);
        replyMsg.send(ctrlSocket);

        // start main poll loop
        fsmState = PumpFsmState.RECEIVING_CMD_OR_MD;
        LOGGER.info("Publication started...");
        MarketDataMessage mdPayload;
        while (me == thisThread && !Thread.currentThread().isInterrupted()) {
            int nrRaised = inPoller.poll();
            if (nrRaised == 0) { // spurious wake-up, interrupted???
                LOGGER.warn("Spurious wake-up: interrupted at Poller.poll()???");
                continue; // not sure we were interrupted so retest poll loop condition
            }
            if (inPoller.pollin(0)) { // Control calling
                final ZMsg cmdMsg = ZMsg.recvMsg(ctrlSocket);
                if (cmdMsg == null) {
                    LOGGER.warn("Interrupted at ZMsg.recvMsg()...");
                    break;
                }
                final String cmd = cmdMsg.popString();
                LOGGER.debug("cmd: "+cmd);
                switch (cmd) {
                    case SUSPEND_CMD:
                        if (fsmState != PumpFsmState.SUSPENDED) {
                            unsubscribe(quotesSocket, tradesSocket);
                            fsmState = PumpFsmState.SUSPENDED;
                        }
                        replyMsg = new ZMsg();
                        replyMsg.add(DONE_REPLY);
                        replyMsg.send(ctrlSocket);
                        break;
                    case RESUME_CMD:
                        if (fsmState != PumpFsmState.RECEIVING_CMD_OR_MD) {
                            subscribe(quotesSocket, tradesSocket);
                            fsmState = PumpFsmState.RECEIVING_CMD_OR_MD;
                        }
                        replyMsg = new ZMsg();
                        replyMsg.add(READY_REPLY);
                        replyMsg.send(ctrlSocket);
                        break;
                    case STOP_CMD:
                        if (fsmState != PumpFsmState.SUSPENDED) {
                            unsubscribe(quotesSocket, tradesSocket);
                        }
                        tradesSocket.close();
                        quotesSocket.close();
                        replyMsg = new ZMsg();
                        replyMsg.add(DONE_REPLY);
                        replyMsg.send(ctrlSocket);
                        fsmState = PumpFsmState.STOPPED;
                        return true;
                    default:
                        LOGGER.warn(cmd + ": unsupported command received in S2.");
                        break;
                }
                cmdMsg.destroy();
            }
            if (inPoller.pollin(1)) { // Trades calling; if we got paused, the pollin() should reset the poller
                if (fsmState == PumpFsmState.RECEIVING_CMD_OR_MD) {
                    final ZMsg tradeMsg = ZMsg.recvMsg(tradesSocket); // drain socket
                    if (tradeMsg == null) {
                        LOGGER.warn("Interrupted at ZMsg.recvMsg()...");
                        break;
                    }
                    final String symbol = getSymbol(tradeMsg.popString());
                    mdPayload = transformTradeMsg(symbol, ChronoUtils.estimateHpNowInUs(), tradeMsg);
                    tradeMsg.destroy();
                    if (mdPayload != null) {
                        ZMsg mdMsg = new ZMsg();
                        mdMsg.add(new ZFrame(mdPayload.getSymbol()));
                        mdMsg.add(new ZFrame(mdPayload.toByteArray()));
                        mdMsg.send(outSocket);
                    }
                    else {
                        LOGGER.warn("Trade update dropped for " + symbol);
                        SourceMessageStatistics srcMsgStats = retrieveMsgStats(symbol);
                        srcMsgStats.incDropCount();
                    }
                }
                // else: silently drop msg whilst SUSPENDED
            }
            if (inPoller.pollin(2)) { // Quotes calling; if we got paused, the pollin() should reset the poller
                if (fsmState == PumpFsmState.RECEIVING_CMD_OR_MD) {
                    final ZMsg quoteMsg = ZMsg.recvMsg(quotesSocket); // drain socket
                    if (quoteMsg == null) {
                        LOGGER.warn("Interrupted at ZMsg.recvMsg()...");
                        break;
                    }
                    final String symbol = getSymbol(quoteMsg.popString());
                    mdPayload = transformTobQuoteMsg(symbol, ChronoUtils.estimateHpNowInUs(), quoteMsg);
                    quoteMsg.destroy();
                    if (mdPayload != null) {
                        ZMsg mdMsg = new ZMsg();
                        mdMsg.add(new ZFrame(mdPayload.getSymbol()));
                        mdMsg.add(new ZFrame(mdPayload.toByteArray()));
                        mdMsg.send(outSocket);
                    }
                    else {
                        LOGGER.warn("Quote update dropped for " + symbol);
                        SourceMessageStatistics srcMsgStats = retrieveMsgStats(symbol);
                        srcMsgStats.incDropCount();
                    }
                }
                // else: silently drop msg whilst SUSPENDED
            }
        }
        return false;
    }

    private static final Logger LOGGER = LogManager.getLogger(DataPump.class);

    private final Connection ctrlConn;
    private final ComboConnection mcConn;
    private final Connection dataConn;
    private final Set<String> activeSymbols = new HashSet<>();
    private final Set<String> tradeTopics = new HashSet<>();
    private final Set<String> quoteTopics = new HashSet<>();
    private final Map<String, Double> multiplierCache = new HashMap<>(); // Ugly! By distributing unscaled prices, every client needs to scale.
    private final Map<String, SourceMessageStatistics> msgStats = new HashMap<>();

    private PumpFsmState fsmState = PumpFsmState.START;
    private volatile Thread me;
}
