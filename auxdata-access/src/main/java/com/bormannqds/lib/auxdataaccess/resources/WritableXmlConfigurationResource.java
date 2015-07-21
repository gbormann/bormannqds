/*
    Copyright 2014, 2015 Guy Bormann

    This file is part of auxdata-access.

    Foobar is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Foobar is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.bormannqds.lib.auxdataaccess.resources;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.FileConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by bormanng on 25/06/15.
 */
public class WritableXmlConfigurationResource
        extends AbstractWritableResourceWrapper<URL>
		implements ResourceLocatorBean<URL>, WritableResourceWrapper {
//public:
	public WritableXmlConfigurationResource() {
	}
	
    public WritableXmlConfigurationResource(final URL locator) throws IOException {
    	super(locator);
    }

//protected:
	@Override
	protected void openImpl() throws ResourceIOException {
		try {
			XMLConfiguration tmpXmlConfig = new XMLConfiguration(getLocator());
//				tmpXmlConfig.setValidating(true); // NOTE: we'd need a DTD
			tmpXmlConfig.setAutoSave(true); // TODO: Revisit if configuration gets more GUI support!!
			tmpXmlConfig.setDelimiterParsingDisabled(true);
			tmpXmlConfig.setAttributeSplittingDisabled(true);

			fileConfig = tmpXmlConfig;
		}
		catch (ConfigurationException ce) {
			LOGGER.error("Exception caught whilst trying to load the configuration from the resource at the locator: " + getLocator(), ce);
			throw new ResourceIOException("Exception caught whilst trying to load the configuration from the resource at the locator: " + getLocator(), ce);
		}
	}

	@Override
	protected void commitImpl() throws ResourceIOException {
		try {
			if (!fileConfig.isAutoSave()) {
				fileConfig.save();
			}
		}
		catch (ConfigurationException ce) {
			LOGGER.error("Exception caught whilst trying to write the configuration to the resource at the locator: " + getLocator(), ce);
			throw new ResourceIOException("Exception caught whilst trying to write the configuration to the resource at the locator: " + getLocator(), ce);
		}
	}

	@Override
	protected void closeImpl() {
		fileConfig = null; // put the document with the thrash for GC			
	}

	protected URL getUrl(final String key, final String errorMsg) {
		String fileUrlString = fileConfig.getString(key, null);

		if (fileUrlString != null) {
			try {
				return new URL(fileUrlString);
			}
			catch(MalformedURLException mue) {
				LOGGER.error(errorMsg + fileUrlString, mue);
			}
		}

		return null;		
	}

    protected FileConfiguration fileConfig;

//private:
	private static final Logger LOGGER = LogManager.getLogger(WritableXmlConfigurationResource.class);
}
