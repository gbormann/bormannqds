package com.bormannqds.mds.lib.configuration;

import com.bormannqds.lib.dataaccess.resources.WritableXmlConfigurationResource;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by bormanng on 25/06/15.
 */
public class ConfigurationResource extends WritableXmlConfigurationResource
{
//public:
	public ConfigurationResource() {
		super();
	}
	
    public ConfigurationResource(final URL locator) {
    	super(locator);
    }

	/* to illustrate string conversions
	public String getDefaultCsvRecordSeparator() {
		return fileConfig.getString(RECSEP_KEY, "\r\n").replaceAll("\\\\r", "\r").replaceAll("\\\\n", "\n");
	}

	public void setDefaultCsvRecordSeparator(final String newValue) {
		fileConfig.setProperty(RECSEP_KEY, newValue.replaceAll("\r", Matcher.quoteReplacement("\\r")).replaceAll("\n", Matcher.quoteReplacement("\\n")));
		setDirty();
	}
	*/

	public URL getRefDataResourceLocator() {
		return getUrl(REFDATA_KEY, REFDATA_MSG);
	}

	public URL getMulticastConfigResourceLocator() {
		return getUrl(MCCONFIG_KEY, MCCONFIG_MSG);
	}

	public URL getOutrightsRollSchedulesResourceLocator() {
		return getUrl(OUTRIGHTS_ROLLSCHEDS_KEY, ROLLSCHED_MSG);
	}

	public URL getSpreadsRollSchedulesResourceLocator() {
		return getUrl(SPREADS_ROLLSCHEDS_KEY, ROLLSCHED_MSG);
	}

	public Map<String, List<String>> getInterestList() {
        if (interestListMap == null) {
            interestListMap = new HashMap<>();
            final List<Object> pgObjs = fileConfig.getList(IL_PG_KEY);
            final StringBuilder keyBuilder = new StringBuilder();
            int pgCnt = 0;
            for (Object pgObj: pgObjs) {
                final String productGroup = (String)pgObj;
                final List<String> baseSymbols = new ArrayList<>();
                keyBuilder.append(IL_BTKR_KEY_PREFIX).append(pgCnt).append(IL_BTKR_KEY_SUFFIX);
                fileConfig.getList(keyBuilder.toString()).forEach(baseSymObj -> baseSymbols.add((String) baseSymObj));
                interestListMap.put(productGroup, baseSymbols);
                keyBuilder.setLength(0);
                ++pgCnt;
            }
        }
        return interestListMap;
	}

//private:
//	private static final Logger LOGGER = LogManager.getLogger(ConfigurationResource.class);
	/*
	private static final String RECSEP_KEY = "settings.csv.defaultRecordSeparator";
	private static final String QUOTEFILTERING_KEY = "settings.md.defaultQuoteFiltering";
	*/
	private static final String REFDATA_KEY = "env.files.referenceDataUrl";
	private static final String REFDATA_MSG = "Malformed URL to reference data file: "; // msgs like these can go into a msg catalogue
	private static final String MCCONFIG_KEY = "env.files.multicastConfigUrl";
	private static final String MCCONFIG_MSG = "Malformed URL to multicast config file: "; // msgs like these can go into a msg catalogue
	private static final String OUTRIGHTS_ROLLSCHEDS_KEY = "env.files.outrightsRollSchedulesUrl";
	private static final String SPREADS_ROLLSCHEDS_KEY = "env.files.spreadsRollSchedulesUrl";
	private static final String ROLLSCHED_MSG = "Malformed URL to roll schedule file: "; // msgs like these can go into a msg catalogue
	private static final String IL_PG_KEY = "settings.interestLists.interestList[@group]";
    private static final String IL_BTKR_KEY_PREFIX = "settings.interestLists.interestList(";
    private static final String IL_BTKR_KEY_SUFFIX = ").baseTicker[@name]";

    private Map<String, List<String>> interestListMap = null;
}
