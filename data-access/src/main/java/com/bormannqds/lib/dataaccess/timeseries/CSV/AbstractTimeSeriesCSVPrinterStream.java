/*
    Copyright 2014, 2015 Guy Bormann

    This file is part of data-access.

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
package com.bormannqds.lib.dataaccess.timeseries.CSV;

import com.bormannqds.lib.utils.chrono.CalendarUtils;
import com.bormannqds.lib.dataaccess.resources.ResourceIOException;
import com.bormannqds.lib.utils.system.FileSystemUtils;
import com.bormannqds.lib.dataaccess.timeseries.AbstractTimeSeriesOutputResource;
import com.bormannqds.lib.dataaccess.timeseries.TimeSeriesSample;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public abstract class AbstractTimeSeriesCSVPrinterStream<Sample extends TimeSeriesSample>
	extends AbstractTimeSeriesOutputResource<URL, Sample, Sample> {

	// -------- Protected ----------

	protected AbstractTimeSeriesCSVPrinterStream(final CSVFormat csvFormat) {
		super();
		this.csvFormat = csvFormat;
	}

	protected AbstractTimeSeriesCSVPrinterStream(final URL locator, final CSVFormat csvFormat) {
		super(locator);
		this.csvFormat = csvFormat;
	}

	@Override
	protected void putRecord(final Sample sample) throws ResourceIOException {
		putRecord(printer, sample);
	}

	/**
	 *  Opens or reopens the resource for the date at locator base.
	 *  
	 *  When a resource reader is already in the reader cache, it will be closed first (effectively resetting the stream).
	 */
	@Override
	protected void openImpl() throws ResourceIOException {
		try {
			printer = new CSVPrinter(FileSystemUtils.createBufferedWriter(getLocator()), csvFormat);
			setDirty();
		}
		catch (URISyntaxException|IOException me) {
			LOGGER.error("Exception caught whilst trying to open a CSV stream resource at " + getLocator(), me);
			throw new ResourceIOException("Exception caught whilst trying to open a CSV stream resource at " + getLocator(), me);
		}
	}

	@Override
	protected void commitImpl() throws ResourceIOException {
		try {
			printer.flush();
		}
		catch (IOException ioe) {
			LOGGER.error("Exception caught whilst trying to flush to a CSV stream resource at " + getLocator(), ioe);
			throw new ResourceIOException("Exception caught whilst trying to flush to a CSV stream resource at " + getLocator(), ioe);
		}
	}

	@Override
	protected void closeImpl() throws ResourceIOException {
		if (printer != null) {
			try {
				printer.close();
			}
			catch (IOException ioe) {
				// Since this is used as a read-only resource, convert exceptions into a silent warnings for now.
				// If it has to do with file system problems, attempts hereafter to reopen will throw an exception anyway.
				LOGGER.warn("Exception caught whilst attempting to close a CSV stream resource at " + getLocator(), ioe);
				throw new ResourceIOException("Exception caught whilst attempting to close a CSV stream resource at " + getLocator(), ioe);
			}
			finally {
				printer = null;
			}
		}
	}

	/*
	 * Parsing exceptions are turned into sentinel values in order to let the client handle bad data without the need to propagate exception handling
	 */
	protected String formatTimestampField(final Date timestamp) {
		DateFormat dateFormat = new SimpleDateFormat(CalendarUtils.TIMESTAMP_FORMAT);
		return dateFormat.format(timestamp);
	}

/*
	protected String formatDoubleField(double doubleValue) {
		return Double.toString(doubleValue);
	}

	protected String formatIntField(int intValue) {
		return Integer.toString(intValue);
	}
*/

	/*
	 * Note that CSVPrinter turns field values directly into a line string of field separator-delimited value string representations
	 * so that createRecord can be an identity function on Sample.
	 */
	@Override
	protected Sample createRecord(final Sample sample) {
		return sample;
	}

	protected abstract void putRecord(final CSVPrinter printer, final Sample ptaItem) throws ResourceIOException;

	// -------- Private ----------

	private static final Logger LOGGER = LogManager.getLogger(AbstractTimeSeriesCSVPrinterStream.class);

	private final CSVFormat csvFormat;
	private CSVPrinter printer;
}