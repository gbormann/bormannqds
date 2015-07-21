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

import nu.xom.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URL;

public class XmlResource extends AbstractResourceWrapper<URL> implements ResourceLocatorBean<URL>, ResourceWrapper {
//public:
	public XmlResource(final String contentType) {
		super();
        this.contentType = contentType + " XML document!";
    }

	public XmlResource(final String contentType, final URL locator) {
		super(locator);
        this.contentType = contentType + " XML document!";
    }

	//protected:
	@Override
	protected void openImpl() throws ResourceIOException {
		if (!isOpen()) {
			try {
				document = builder.build(getLocator().openStream());
			}
			catch (ParsingException pe) {
				LOGGER.error("A parsing error occurred whilst reading the " + contentType, pe);
				throw new ResourceIOException("A parsing error occurred whilst reading the " + contentType, pe);
			}
			catch (IOException ioe) {
				LOGGER.error("An I/O error occurred whilst reading the " + contentType, ioe);
				throw new ResourceIOException("An I/O error occurred whilst reading the " + contentType, ioe);
			}
		}
	}

	@Override
	protected void closeImpl() {
		document = null; // put the document with the thrash for GC
	}

    protected final Builder builder = new Builder();
    protected Document document = null;

//private:
	private static final Logger LOGGER = LogManager.getLogger(XmlResource.class);
    private final String contentType;
}
