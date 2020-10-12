package com.bormannqds.mds.lib.configuration;

import com.bormannqds.mds.lib.protocoladaptor.ComboConnection;

import com.bormannqds.lib.dataaccess.resources.ResourceNotOpenException;
import com.bormannqds.lib.dataaccess.resources.XmlResource;
import nu.xom.Node;
import nu.xom.Nodes;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URL;
import java.util.*;

/**
 * Created by User on 24/03/2015.
 */
public class MulticastConfigResource extends XmlResource
{
//public:
	public MulticastConfigResource() {
        super(RSC_NAME);
	}
	
	public MulticastConfigResource(final URL locator) {
		super(RSC_NAME, locator);
    }

    /**
     * Maps a base ticker to a multicast config if found on resource.
     *
     * @param baseTicker
     * @return multicastConfig for baseTicker or <code>null</code> if not found
     */
    public MulticastConfig getMulticastConfig(String baseTicker) {
        if (!isOpen()) {
            LOGGER.error("BUG: Use of resource before it's opened!");
            throw new ResourceNotOpenException();
        }

        MulticastConfig multicastConfig = mcConfigs.get(baseTicker);
        if (multicastConfig == null) { // not in the multicast config cache (by base ticker)
            StringBuilder xpathBuilder = new StringBuilder("/multicastconfig/product[baseTicker='");
            xpathBuilder.append(baseTicker)
                    .append("']");
            Nodes results = document.query(xpathBuilder.toString());
            if (results.size() == 0) {
                LOGGER.warn(baseTicker + ": no multicast configuration found!");
                return null;
            }
            if (results.size() > 1) {
                LOGGER.warn(baseTicker + ": multiple multicast configuration entries found!");
            }
            Node productNode = results.get(0); // We should only get one! If not, the multicast XML is fishy.

            // If we crash with an NPE or format error in the last stretch, it's because the XML is not valid. If only we had a DTD!
            double multiplier = Double.parseDouble(productNode.query("multiplier").get(0).getValue());
            String bookAddr = productNode.query("bookAddress").get(0).getValue();
            String tradeAddr = productNode.query("tradeAddress").get(0).getValue();
            multicastConfig = new MulticastConfig(baseTicker, multiplier, new ComboConnection(bookAddr, tradeAddr));
            LOGGER.debug("Adding multicast configuration for " + baseTicker + " to cache...");
            mcConfigs.put(baseTicker, multicastConfig);
        }

        return multicastConfig;
    }

    /**
     *
     * @return set of multicast connections over the set of all internal base tickers
     */
    public Set<ComboConnection> getConnectionSet() {
        if (!isOpen()) {
            LOGGER.error("BUG: Use of resource before it's opened!");
            throw new ResourceNotOpenException();
        }

        if (mcConns.isEmpty()) {
            Nodes results = document.query("/multicastconfig/product");
            if (results.size() == 0) {
                LOGGER.warn("Multicast configuration has no entries!");
                return null;
            }

            for (int i = 0; i < results.size(); ++i) {
                Node productNode = results.get(i);
                // If we crash with an NPE or format error in the last stretch, it's because the XML is not valid. If only we had a DTD!
                String bookAddr = productNode.query("bookAddress").get(0).getValue();
                if (!mcConns.contains(bookAddr)) {
                    String tradeAddr = productNode.query("tradeAddress").get(0).getValue();
                    mcConns.add(new ComboConnection(bookAddr, tradeAddr));
                }
            }
        }

        return mcConns;
    }

//private:
	private static final Logger LOGGER = LogManager.getLogger(MulticastConfigResource.class);
    private static final String RSC_NAME = "Multicast Config";

    private final Map<String, MulticastConfig> mcConfigs = new HashMap<String, MulticastConfig>();
    private final Set<ComboConnection> mcConns = new HashSet<ComboConnection>();
}
