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

/**
 * This behaves like a two-entry ring buffer of time series samples.
 * 
 * An actual ring buffer would be more than required.
 * @author guy
 *
 */
public class Bracket<Sample extends TimeSeriesSample> {
	public Bracket (final Sample earlier, final Sample later) {
		this.earlier = earlier;
		this.later = later;
	}

	public Bracket<Sample> pushNewSample(final Sample sample) {
		earlier = later;
		later = sample;
		return this;
	}

	public Sample getEarlier() {
		return earlier;
	}

	public Sample getLater() {
		return later;
	}

	/**
	 * Test whether a given timestamp is inside this reference bracket.
	 * 
	 * @param ts - the timestamp to be tested against this reference bracket
	 * @return <code>true</code> if given 
	 */
	public boolean isInsideBracket(final Date ts) {
		 // where (earlier == null && later == null) assumed always false
		return (earlier == null || earlier.getTimestamp().compareTo(ts) < 0)
				&& (later == null || ts.compareTo(later.getTimestamp()) <= 0);
	}

	@Override
	public String toString() {
		return "Bracket [earlier=" + earlier + ", later=" + later + "]";
	}

	// -------- Private ----------


	private Sample earlier;
	private Sample later;
}
