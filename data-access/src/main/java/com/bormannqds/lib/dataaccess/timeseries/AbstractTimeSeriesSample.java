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
package com.bormannqds.lib.dataaccess.timeseries;

import com.bormannqds.lib.utils.chrono.CalendarUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public abstract class AbstractTimeSeriesSample implements TimeSeriesSample {

	public Date getTimestamp() {
		return timestamp;
	}

	@Override
	public String toString() {
		DateFormat tsFormat = new SimpleDateFormat(CalendarUtils.TIMESTAMP_FORMAT);
		final StringBuilder stringBuilder = new StringBuilder("AbstractTimeSeriesSample [timestamp=");
		stringBuilder.append(tsFormat.format(timestamp))
			.append(']');

		return stringBuilder.toString();
	}

	// -------- Protected ----------

	protected AbstractTimeSeriesSample(final Date timestamp) {
		this.timestamp = timestamp;
	}

	// -------- Private ----------

	private final Date timestamp;
}
