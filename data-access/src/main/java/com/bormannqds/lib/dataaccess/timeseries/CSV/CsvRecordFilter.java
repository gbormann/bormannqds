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

import com.bormannqds.lib.dataaccess.timeseries.Filter;
import org.apache.commons.csv.CSVRecord;

/**
 * This filter is a means to eliminate records based on unparsed data.
 * 
 * Currently envisioned application is filtering L1BOOK data for price or volume transitions (mainly to reduce redundancy in quote data).
 * On first look, applying the filtering on the parsed data would be more performant (i.e. integer and double checks vs string comparison).
 * However, parsing data already requires a full scan of the value string, so comparing unparsed data is good enough for certain types of filtering.

 * @author guy
 *
 */
public abstract class CsvRecordFilter implements Filter<CSVRecord> {

	// ------- Protected ---------

	protected CSVRecord prevRecord = null;
}
