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

import com.bormannqds.lib.dataaccess.resources.ResourceIOException;
import com.bormannqds.lib.dataaccess.timeseries.AbstractTimeSeriesInputResource;
import com.bormannqds.lib.dataaccess.timeseries.Filter;
import com.bormannqds.lib.dataaccess.timeseries.TimeSeriesSample;

import com.bormannqds.lib.utils.chrono.CalendarUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

public abstract class AbstractTimeSeriesCSVParserStream<Sample extends TimeSeriesSample>
	extends AbstractTimeSeriesInputResource<URL, CSVRecord, Sample> {

	public Map<String, Integer> getHeaderMap() {
		if (parser == null) {
			return null;
		}
		return parser.getHeaderMap();
	}

	// -------- Protected ----------

	protected AbstractTimeSeriesCSVParserStream(final CSVFormat csvFormat) {
		super();
		this.csvFormat = csvFormat;
	}

	protected AbstractTimeSeriesCSVParserStream(final URL locator, final CSVFormat csvFormat) {
		super(locator);
		this.csvFormat = csvFormat;
	}

	/**
	 *  Opens or reopens the resource for the date at locator base.
	 *  
	 *  When a resource reader is already in the reader cache, it will be closed first (effectively resetting the stream).
	 */
	@Override
	protected void openImpl() throws ResourceIOException {
		try {
			super.openImpl();
			parser = CSVParser.parse(getLocator(), Charset.defaultCharset(), csvFormat);
		}
		catch (IOException ioe) {
			LOGGER.error("Exception caught whilst trying to open a CSV stream resource at " + getLocator(), ioe);
			throw new ResourceIOException("Exception caught whilst trying to open a CSV stream resource at " + getLocator(), ioe);
		}
	
		/* Whilst there is a heavy reliance on field names, there is tolerance in field order per stream.
		 * The following initialisation of field indices avoids string key map lookups for every quote creation. */
		tsFieldNdx = parser.getHeaderMap().get(TIMESTAMP_FIELDNAME);
		initialiseSpecificFieldIndices();
		recordIterator = parser.iterator();
	}

	@Override
	protected void closeImpl() throws ResourceIOException {
		if (parser != null && !parser.isClosed()) {
			try {
				parser.close();
			}
			catch (IOException ioe) {
				// Since this is used as a read-only resource, convert exceptions into a silent warnings for now.
				// If it has to do with file system problems, attempts hereafter to reopen will throw an exception anyway.
				LOGGER.warn("Exception caught whilst attempting to close a CSV stream resource at " + getLocator(), ioe);
				throw new ResourceIOException("Exception caught whilst attempting to close a CSV stream resource at " + getLocator(), ioe);
			}
			finally {
				parser = null;
			}
		}
	}

	protected Filter<CSVRecord> getRecordFilter() {
		return DEFAULT_RECORD_FILTER;
	}

	@Override
	protected CSVRecord nextRecord() {
		CSVRecord record = null;
		while (!getRecordFilter().accept(record) && recordIterator.hasNext()) {
			record = recordIterator.next();
		}
		if (record != null) {
			setLatestTimestamp(parseTimestampField(record));
		}
		return record;
	}

	/*
	 * Parsing exceptions are turned into sentinel values in order to let the client handle bad data without the need to propagate exception handling
	 */
	protected Date parseTimestampField(final CSVRecord record) {
		// strip timezone info if present and normalise the remainder of the timestamp field (see below)
		final String fieldValue = normaliseTimestampField(record.get(tsFieldNdx).split("\\+")[0]);
		final DateFormat dateFormat = new SimpleDateFormat(CalendarUtils.TIMESTAMP_FORMAT);
		Date ts = null;
		try {
			ts = dateFormat.parse(fieldValue);
		}
		catch (ParseException pe) {
			LOGGER.error(buildParseErrorMsg(getLocator(), "ts", fieldValue), pe);
		}

		return ts;
	}

	protected double parseDoubleField(final String fieldName, final String fieldValue) {
		double field = Double.NaN;
		try {
			field = Double.parseDouble(fieldValue);
		}
		catch (java.lang.NumberFormatException nfe) {
			LOGGER.error(buildParseErrorMsg(getLocator(), fieldName, fieldValue), nfe);
		}

		return field;
	}

	protected long parseLongField(final String fieldName, final String fieldValue) {
		long field = 0;
		try {
			field = Long.parseLong(fieldValue);
		}
		catch (NumberFormatException nfe) {
			LOGGER.error(buildParseErrorMsg(getLocator(), fieldName, fieldValue), nfe);
		}

		return field;
	}

	protected int parseIntField(final String fieldName, final String fieldValue) {
		int field = 0;
		try {
			field = Integer.parseInt(fieldValue);
		}
		catch (NumberFormatException nfe) {
			LOGGER.error(buildParseErrorMsg(getLocator(), fieldName, fieldValue), nfe);
		}

		return field;
	}

	protected abstract void initialiseSpecificFieldIndices();

	// -------- Private ----------

	// Pad millis field with zeros to enforce length 3
	private String normaliseTimestampField(final String rawTimestamp) {
		final String[] timestampParts = rawTimestamp.split("\\.");
		final StringBuilder zerosPadder = new StringBuilder(rawTimestamp);
		if (timestampParts.length == 1) {
			zerosPadder.append(".000");
			return zerosPadder.toString();
		}
		switch (timestampParts[1].length()) {
		case 1:
			zerosPadder.append("00");
			break;
		case 2:
			zerosPadder.append('0');
			break;
		case 3: // conformant millis field => nothing to do
		default: // something's wrong but we don't know what; let it crash and burn downstream...
			break;
		}
		return zerosPadder.toString();
	}

	private static String buildParseErrorMsg(final URL locator, final String fieldName, final String fieldValue) {
		StringBuilder errMsgBuilder = new StringBuilder("Could not parse field ");
		errMsgBuilder.append(fieldName).append('=').append(fieldValue).append(" of a record in the resource at ").append(locator);
		return  errMsgBuilder.toString();
	}

	private static final Logger LOGGER = LogManager.getLogger(AbstractTimeSeriesCSVParserStream.class);
	private static final String TIMESTAMP_FIELDNAME = "ts";
	private static final NullRecordFilter DEFAULT_RECORD_FILTER = new NullRecordFilter();

	private int tsFieldNdx;
	private CSVFormat csvFormat;
	private CSVParser parser;
	private Iterator<CSVRecord> recordIterator;
}