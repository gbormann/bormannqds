
package com.bormannqds.mds.lib.protocoladaptor;

import com.bormannqds.mds.lib.configuration.MulticastConfig;
import com.bormannqds.mds.lib.configuration.MulticastConfigResource;
import com.bormannqds.mds.lib.gateway.ApplicationContext;
import com.bormannqds.mds.lib.referencedata.InternalSymbolParser;
import com.bormannqds.lib.dataaccess.referencedata.ExpiringInstrumentSymbolComponents;
import com.bormannqds.lib.dataaccess.referencedata.UnrecognisedSymbolException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMsg;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *	input: MulticastConfig, ZContext
 *	maps:
 *	    base symbol -> external MC addresses (includes Book Address, aka ToB Quote Address)
 *	    external Book Address -> internal datapump address
 *
 *	creations:
 *	    (i)   set of DPs
 *	    (ii)  REP socket
 *	    (iii) REQ sockets
 *
 *	connections:
 *	    1. REP socket -> bind TB control port
 *	    2. For each DP: REQ socket -> connect DP control port
 *
 *	conversations:
 *	A. Receive cmd on REP
 * S1:
 *	ADDRESS <base symbol>:
 *	    check <base symbol> against MC map keys
 *	    if present:
 *	        ADD <base symbol> -> DP control port
 *	        receive DONE
 *	            reply <DP(base symbol) data port> -> REP
 *	        receive UNRECOGNISED
 *	            fwd(REP)
 *	        receive UNSUPPORTED
 *	            fwd(REP)
 *	        receive NO CONFIG
 *	            fwd(REP)
 *      else:
 *          reply NO CONFIG -> REP
 *      restart S1
 *  START:
 *      For each DP:
 *          START -> DP control port
 *          receive READY
 *      reply READY -> REP
 *      tau
 *
 * S2:
 *  PAUSE:
 *      if (suspended)
 *          restart S2
 *      For each DP:
 *          send PAUSE -> DP control port
 *          receive DONE on DP control port
 *      reply DONE -> REP
 *      restart S2
 *  RESUME:
 *      if (resumed)
 *          restart S2
 *      For each DP:
 *          send RESUME -> DP control port
 *          receive READY on DP control port
 *      reply READY -> REP
 *      restart S2
 *  STOP:
 *      For each DP:
 *          send STOP -> DP control port
 *          receive DONE on DP control port
 *      reply DONE -> REP
 *      tau
 *  HALT:
 *
 * NOTE: Current implementation of the DP activation stage at the end of run() assumes uniqueness
 * of ToB Quote Address -> datapump control socket!
 */
public class TranslatingBridge implements Runnable {
//public:
    public static final String ADDRESS_REQ_CMD = "ADDRESS";
    public static final String START_CMD = DataPump.START_CMD;
    public static final String SUSPEND_CMD = DataPump.SUSPEND_CMD;
    public static final String RESUME_CMD = DataPump.RESUME_CMD;
    public static final String STOP_CMD = DataPump.STOP_CMD;
    public static final String DONE_REPLY = DataPump.DONE_REPLY; // to ADDRESS_REQ_CMD, SUSPEND_CMD, RESUME_CMD, STOP_CMD
    public static final String READY_REPLY = DataPump.READY_REPLY; // to START_CMD
    public static final String REPLY_SYNC_SEQ = "AAA";
    public static final String UNRECOGNISED_REPLY = DataPump.UNRECOGNISED_REPLY; // to ADDRESS_REQ_CMD
    public static final String NO_CONFIG_REPLY = DataPump.NO_CONFIG_REPLY; // to ADDRESS_REQ_CMD
    public static final String UNSUPPORTED_REPLY = DataPump.UNSUPPORTED_REPLY; // to ADDRESS_REQ_CMD

    public Connection getCtrlConnection() {
        return ctrlConn;
    }

    public void start(final String name) {
        if (me == null) { // avoid accidental thread rabbits
            me = new Thread(this, name);
            me.start();
            try {
                Thread.sleep(100); // allow the thread to settle in before allowing clients to connect
            }
            catch (InterruptedException ie) {
                LOGGER.warn("Settle-in sleep interrupted...", ie);
            }
        }
    }

    public void stop() {
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

    public void run() {
        // let's be ready asap on the server connection before the thundering horde arrives
        final Socket ctrlSocket = Connection.createSocket(ZMQ.REP); // Control connection
        ctrlSocket.bind(ctrlConn.getAddress());

        // sockets are thread-specific, so we need to pass them around!
        final Map<String, Socket> dpCtrlSockets = setUpDataPumps();

        // S1
        // --
        fsmState = FsmState.RECEIVING_ADDRESS_REQS;
        if (processControlMsgsUntilStartCmd(ctrlSocket, dpCtrlSockets)) { // if we were interrupted, time to leave
            // S2
            // --
            fsmState = FsmState.S2_STARTED;
            if (processCtrlMsgsUntilStopCmd(ctrlSocket, dpCtrlSockets)) {
                ctrlSocket.close();
                dpCtrlSockets.values().forEach(Socket::close);
            }
        }

        tearDownDataPumps();
        fsmState = FsmState.HALT;
    }

//private:
    private enum FsmState {
        START, RECEIVING_ADDRESS_REQS, S2_STARTED, SUSPENDED, STOPPED, HALT
    }

    /**
     * Amongst other things, maps distributor ToB quote addresses to internal datapump addresses.
     *
     * So, with calls to MulticastConfig's getMulticastConfig in the startup control loop, this establishes the
     * following link chain: base symbol -> ToB Quote distributor address -> internal datapump data port
     */
    private Map<String, Socket> setUpDataPumps() {
        final MulticastConfigResource mcConfigResource = ApplicationContext.getInstance().getMulticastConfigResource();
        final Map<String, Socket> dpCtrlSockets = new HashMap<>();
        int dpCnt = 1;
        for (final ComboConnection mcConn: mcConfigResource.getConnectionSet()) {
            final String pumpId = "Pump" + dpCnt;
            LOGGER.info(mcConn.getQuotesAddress() + " mapped to " + pumpId);
            final Connection dpCtrlConn = new Connection("inproc://ctrlPort_" + pumpId);
            final Connection dpDataConn = new Connection("inproc://dataPort_" + pumpId);
            DataPump dataPump = new DataPump(dpCtrlConn, mcConn, dpDataConn);
            dataPumps.put(mcConn.getQuotesAddress(), dataPump);
            dataPump.start(pumpId); // threads will be waiting for control messages in their message loops
            final Socket dpCtrlSocket = Connection.createSocket(ZMQ.REQ);
            dpCtrlSockets.put(dpCtrlConn.getAddress(), dpCtrlSocket);
            dpCtrlSocket.connect(dpCtrlConn.getAddress());
            ++dpCnt;
        }
        return dpCtrlSockets;
    }

    /**
     *
     */
    private void tearDownDataPumps() {
        dataPumps.values().forEach(DataPump::stop);
    }

    private boolean processControlMsgsUntilStartCmd(final Socket ctrlSocket, final Map<String, Socket> dpCtrlSockets) {
        final Thread thisThread = Thread.currentThread();

        final MulticastConfigResource mcConfigResource = ApplicationContext.getInstance().getMulticastConfigResource();
        while (me == thisThread && !Thread.currentThread().isInterrupted()) {
            final ZMsg cmdMsg = ZMsg.recvMsg(ctrlSocket);
            if (cmdMsg == null) {
                LOGGER.warn("Interrupted at ZMsg.recvMsg()...");
                break;
            }
            final  String cmd = cmdMsg.popString();
            LOGGER.debug("cmd: "+cmd);
            switch (cmd) {
                case ADDRESS_REQ_CMD:
                    final String reqSymbol = cmdMsg.popString();
                    MulticastConfig mcConfig = null;
                    try {
                        final List<ExpiringInstrumentSymbolComponents> symbolComponents = InternalSymbolParser.getInstance().parse(reqSymbol);
                        String baseSymbol = reqSymbol;
                        if (symbolComponents.size() == 1) {
                            baseSymbol = symbolComponents.get(0).getBaseSymbol();
                        }
                        mcConfig = mcConfigResource.getMulticastConfig(baseSymbol);
                    } catch (UnrecognisedSymbolException use) {
                        LOGGER.warn(reqSymbol + " doesn't seem to be an internal symbol");
                    }
                    final ZMsg replyMsg = new ZMsg();
                    if (mcConfig != null) {
                        final String tobQuoteAddress = mcConfig.getConnection().getQuotesAddress();
                        final DataPump dataPump = dataPumps.get(tobQuoteAddress);
                        final Socket dpCtrlSocket = dpCtrlSockets.get(dataPump.getCtrlConnection().getAddress());
                        final ZMsg dpReqMsg = new ZMsg();
                        dpReqMsg.add(DataPump.ADD_REQ_CMD);
                        dpReqMsg.add(reqSymbol);
                        dpReqMsg.send(dpCtrlSocket);

                        final ZMsg dpResponseMsg = ZMsg.recvMsg(dpCtrlSocket);
                        if (dpResponseMsg == null) {
                            LOGGER.warn("Interrupted at ZMsg.recvMsg()...");
                            break;
                        }
                        final String response = dpResponseMsg.popString();
                        if (response.equals(DataPump.DONE_REPLY)) {
                            replyMsg.add(REPLY_SYNC_SEQ);
                            replyMsg.add(dataPump.getDataConnection().getAddress());
                        } else {
                            replyMsg.add(response);
                        }
                        replyMsg.send(ctrlSocket);
                        dpResponseMsg.destroy();
                    } else {
                        replyMsg.add(NO_CONFIG_REPLY);
                        replyMsg.send(ctrlSocket);
                    }
                    break;
                case START_CMD: // S1 end state
                    cmdMsg.destroy();
                    return controlDataPumpsAndReply(DataPump.START_CMD, DataPump.READY_REPLY, dpCtrlSockets, READY_REPLY, ctrlSocket);
                default:
                    LOGGER.warn(cmd + ": unsupported command received in S1.");
                    break;
            }
            cmdMsg.destroy();
        }

        return false; // we were interrupted!
    }

    private boolean processCtrlMsgsUntilStopCmd(final Socket ctrlSocket, final Map<String, Socket> dpCtrlSockets) {
        final Thread thisThread = Thread.currentThread();

        while (me == thisThread && !Thread.currentThread().isInterrupted()) {
            final ZMsg cmdMsg = ZMsg.recvMsg(ctrlSocket);
            if (cmdMsg == null) {
                LOGGER.warn("Interrupted at ZMsg.recvMsg()...");
                break;
            }
            final String cmd = cmdMsg.popString();
            LOGGER.debug("cmd: "+cmd);
            switch (cmd) {
                case SUSPEND_CMD:
                    if (fsmState == FsmState.S2_STARTED && controlDataPumpsAndReply(DataPump.SUSPEND_CMD, DataPump.DONE_REPLY, dpCtrlSockets, DONE_REPLY, ctrlSocket)) {
                        fsmState = FsmState.SUSPENDED;
                    }
                    break; // interrupted? => will fall through while
                case RESUME_CMD:
                    if (fsmState == FsmState.SUSPENDED && controlDataPumpsAndReply(DataPump.RESUME_CMD, DataPump.READY_REPLY, dpCtrlSockets, READY_REPLY, ctrlSocket)) {
                        fsmState = FsmState.S2_STARTED;
                    }
                    break; // interrupted? => will fall through while
                case STOP_CMD:
                    cmdMsg.destroy();
                    fsmState = FsmState.STOPPED;
                    return controlDataPumpsAndReply(DataPump.STOP_CMD, DataPump.DONE_REPLY, dpCtrlSockets, DONE_REPLY, ctrlSocket);
                default:
                    LOGGER.warn(cmd + ": unsupported command received in S2.");
                    break;
            }
            cmdMsg.destroy();
        }
        return false;
    }

    /**
     * control datapumps
     */
    private static boolean controlDataPumps(final String cmd, final String expect, final Map<String, Socket> dpCtrlSockets) {
        final StringBuilder logMsgBuilder = new StringBuilder();
        for (final Map.Entry<String, Socket> dpCtrlSocketEntry: dpCtrlSockets.entrySet()) {
            final ZMsg reqMsg = new ZMsg();
            reqMsg.add(cmd);
            reqMsg.send(dpCtrlSocketEntry.getValue());
            final ZMsg dpResponseMsg = ZMsg.recvMsg(dpCtrlSocketEntry.getValue());
            if (dpResponseMsg == null) {
                LOGGER.warn("Interrupted at ZMsg.recvMsg()...");
                return false;
            }
            final String dpResponse = dpResponseMsg.popString();
            if (!dpResponse.equals(expect)) {
                logMsgBuilder.append(dpResponse)
                        .append(": datapump ")
                        .append(dpCtrlSocketEntry.getKey())
                        .append(" is mumbling");
                LOGGER.warn(logMsgBuilder.toString());
                // since this potentially a STOP command, we'll have to be tolerant for a mumbling pump; Trust but Verify, so watch the logs
            }
            dpResponseMsg.destroy();
            LOGGER.debug("response: " + dpResponse);
            logMsgBuilder.setLength(0);
        }
        return true;
    }

    /**
     * control datapumps and signal readiness to client
     */
    private static boolean controlDataPumpsAndReply(final String cmd, final String expect, final Map<String, Socket> dpCtrlSockets, final String reply, final Socket ctrlSocket) {
        if (controlDataPumps(cmd, expect, dpCtrlSockets)) {
            final ZMsg replyMsg = new ZMsg();
            replyMsg.add(reply);
            replyMsg.send(ctrlSocket);
            return true;
        }
        return false;
    }

    private static final Logger LOGGER = LogManager.getLogger(TranslatingBridge.class);
    private static final String CTRL_ADDRESS = "inproc://CtrlBridge";

    private final Connection ctrlConn = new Connection(CTRL_ADDRESS);
    private final Map<String, DataPump> dataPumps = new HashMap<>();

    private FsmState fsmState = FsmState.START;
    private volatile Thread me;
}
