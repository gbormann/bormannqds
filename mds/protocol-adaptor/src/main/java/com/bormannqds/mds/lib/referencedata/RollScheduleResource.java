package com.bormannqds.mds.lib.referencedata;

import com.bormannqds.lib.dataaccess.referencedata.MissingRollScheduleException;
import com.bormannqds.lib.dataaccess.referencedata.RollScheduleResourceInterface;
import com.bormannqds.lib.dataaccess.resources.XmlResource;
import nu.xom.Node;
import nu.xom.Nodes;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URL;
import java.time.LocalDate;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by User on 23/03/2015.
 * TODO Implement controlled formatting LocalDateTime parsing!
 */
public class RollScheduleResource extends XmlResource implements RollScheduleResourceInterface
{
    public RollScheduleResource() {
        super(RSC_NAME);
	}
	
    public RollScheduleResource(final URL locator) {
    	super(RSC_NAME, locator);
    }

    /**
     * To allow quick check whether a roll schedule is defined for a given base ticker without triggering an exception.
     *
     * @param baseTicker
     * @return
     */
    public boolean hasRollSchedule(final String baseTicker) {
        return rollScheduleCache.get(baseTicker) != null || getSecurityNodes(baseTicker).size() > 0;
    }

    public RollSchedule getRollSchedule(final String baseTicker) throws MissingRollScheduleException {
        RollSchedule rollSchedule = rollScheduleCache.get(baseTicker);
        if (rollSchedule == null) {
            Nodes secNodes = getSecurityNodes(baseTicker);
            if (secNodes.size() == 0) {
                LOGGER.error(baseTicker + ": no roll schedule data found!");
                throw new MissingRollScheduleException(baseTicker + ": no roll schedule data found!");
            }
            // We should only get one! If not, the roll schedule data XML is fishy!
            if (secNodes.size() > 1) {
                LOGGER.warn(baseTicker + ": multiple roll schedules found! Taking the first one (in textual order)...");
            }

            // If we crash with an NPE or format error in the last stretch, it's because the XML is not valid. If only we had a DTD!
            final Nodes rollNodes = secNodes.get(0).query("activecontract"); // take the first matching security
            if (rollNodes.size() == 0) {
                LOGGER.error(baseTicker + ": no roll dates found!");
                throw new MissingRollScheduleException(baseTicker + ": no roll dates found!");
            }

            rollSchedule = new RollSchedule(baseTicker);
            for (int i = 0; i < rollNodes.size(); ++i) {
                final Node rollNode = rollNodes.get(i);
                final LocalDate rollDate = LocalDate.parse(rollNode.query("rolldate").get(0).getValue());
                final String expiryCode = rollNode.query("contract").get(0).getValue();
                rollSchedule.add(rollDate, expiryCode);
            }
            rollScheduleCache.put(baseTicker, rollSchedule);
        }

        return rollSchedule;
    }

//private:
    private Nodes getSecurityNodes(final String baseTicker) {
        final StringBuilder xpathBuilder = new StringBuilder("/securities/security[baseTicker='");
        xpathBuilder.append(baseTicker);
        xpathBuilder.append("']");

        return document.query(xpathBuilder.toString());
    }

    private static final Logger LOGGER = LogManager.getLogger(ReferenceDataResource.class);
    private static final String RSC_NAME = "Roll Schedules";

    private Map<String, RollSchedule> rollScheduleCache = new TreeMap<>();
}
