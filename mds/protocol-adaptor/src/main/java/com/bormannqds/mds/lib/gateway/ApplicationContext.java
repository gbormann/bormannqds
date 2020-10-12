package com.bormannqds.mds.lib.gateway;

import com.bormannqds.mds.lib.configuration.ConfigurationResource;
import com.bormannqds.mds.lib.configuration.MulticastConfigResource;
import com.bormannqds.mds.lib.referencedata.ReferenceDataResource;
import com.bormannqds.mds.lib.referencedata.RollScheduleResource;

import com.bormannqds.lib.bricks.gateway.AppStatusInterface;
import com.bormannqds.lib.bricks.gateway.BaseGuiApplicationContext;
import com.bormannqds.lib.dataaccess.resources.ResourceIOException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zeromq.ZContext;

import java.nio.file.Path;
import java.time.LocalDate;

/**
 * Application context singleton
 * @author bormanng
 *
 */
public class ApplicationContext extends BaseGuiApplicationContext {

	// ------- Public  -----------
	
	public static void createInstance(final Path appWorkingDirectory,
										final ConfigurationResource configResource,
										final ReferenceDataResource refDataResource,
										final RollScheduleResource outrightsRollSchedResource,
									  	final RollScheduleResource spreadsRollSchedResource,
										final MulticastConfigResource mcConfigResource) {
		instance = new ApplicationContext(appWorkingDirectory,
											configResource,
											refDataResource,
											outrightsRollSchedResource,
											spreadsRollSchedResource,
											mcConfigResource);
	}

	public static ApplicationContext getInstance() {
		if (instance == null) {
			LOGGER.fatal("BUG: use of ApplicationContext instance before it is initialised!");
			throw new NullPointerException("BUG: use of ApplicationContext instance before it is initialised!");
		}
	
		return instance;
	}

    public LocalDate today() {
        return today;
    }

    public void renewToday() {
        today = LocalDate.now();
    }

    public ZContext getZmqContext() {
        return zmqContext;
    }

	public ConfigurationResource getConfigurationResource() {
		return configResource;
	}

	public ReferenceDataResource getReferenceDataResource() {
		return refDataResource;
	}

	public RollScheduleResource getOutRightsRollSchedulesResource() {
		return outrightsRollSchedResource;
	}

	public RollScheduleResource getSpreadsRollSchedulesResource() {
		return spreadsRollSchedResource;
	}

	public MulticastConfigResource getMulticastConfigResource() {
		return mcConfigResource;
	}

	public void dispose() throws ResourceIOException, InterruptedException {
		configResource.close();
		refDataResource.close();
		outrightsRollSchedResource.close();
        spreadsRollSchedResource.close();
		mcConfigResource.close();
        Thread reaperReaperThread = new Thread(
                () -> { ApplicationContext.getInstance().getZmqContext().destroy(); },
                "zmq reaper reaper");
        reaperReaperThread.start();
        reaperReaperThread.join(1000); // give it a second, otherwise nuke the app
	}

	// -------- Private ----------

	private ApplicationContext(final Path appWorkingDirectory,
							   final ConfigurationResource configResource,
							   final ReferenceDataResource refDataresource,
							   final RollScheduleResource outrightsRollSchedResource,
							   final RollScheduleResource spreadsRollSchedResource,
							   final MulticastConfigResource mcConfigResource) {
		super(appWorkingDirectory);
        this.configResource = configResource;
		this.refDataResource = refDataresource;
		this.outrightsRollSchedResource = outrightsRollSchedResource;
		this.spreadsRollSchedResource = spreadsRollSchedResource;
		this.mcConfigResource = mcConfigResource;
        renewToday();
    }

    private static final Logger LOGGER = LogManager.getLogger(ApplicationContext.class);
	private static ApplicationContext instance;

    private final ZContext zmqContext = new ZContext(1);
	private final ConfigurationResource configResource;
	private final ReferenceDataResource refDataResource;
	private final RollScheduleResource outrightsRollSchedResource;
	private final RollScheduleResource spreadsRollSchedResource;
	private final MulticastConfigResource mcConfigResource;

	private AppStatusInterface appStatusBean = null;
    private volatile LocalDate today;
}
