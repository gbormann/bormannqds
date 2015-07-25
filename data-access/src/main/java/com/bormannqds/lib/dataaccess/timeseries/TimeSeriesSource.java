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

import java.util.Date;

public interface TimeSeriesSource<Sample extends TimeSeriesSample> {
	/**
	 * Get the timestamp of the latest sample (accessible as getLater() on the latest bracket).
	 * 
	 * @return timestamp
	 */
	Date getLatestTimestamp();

	/**
	 * Propagate the given sample bracket (essentially a ring buffer of 2) to the next sample pair.
	 * 
	 * @param bracket - the current bracket
	 * @return the next bracket
	 */
	Bracket<Sample> propagateBracket(final Bracket<Sample> bracket);

	/**
	 * Propagate the given sample bracket until it brackets the given timestamp.
	 * 
	 * @param bracket - the current bracket
	 * @param ts - the target timestamp
	 * @return the next bracket
	 */
	Bracket<Sample> propagateBracket(final Bracket<Sample> bracket, final Date ts);

	/**
	 * Get the sample pair that brackets the given timestamp later than getLatestTimestamp(), 
	 * and such that bracket.getLater().getTimestamp() >= ts (this allows for application latency).
	 * 
	 * 
	 * NOTE: if needed, time ordering can be improved using microsecond-resolution fields.
	 * 
	 * @param ts - the target timestamp to bracket with samples, at or later than getLatestTimestamp()
	 * @return the new bracket
	 */
	Bracket<Sample> getBracket(final Date ts);
}
