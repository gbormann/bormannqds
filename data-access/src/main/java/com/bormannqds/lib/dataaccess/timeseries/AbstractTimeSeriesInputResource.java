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

import com.bormannqds.lib.dataaccess.resources.AbstractResourceWrapper;
import com.bormannqds.lib.dataaccess.resources.ResourceIOException;

import java.util.Calendar;
import java.util.Date;

public abstract class AbstractTimeSeriesInputResource<Locator, SrcRecord, Sample extends TimeSeriesSample>
	extends AbstractResourceWrapper<Locator>
	implements TimeSeriesInputResource<Sample> {

	@Override
	public Date getLatestTimestamp() {
		return latestTimestamp;
	}

	@Override
	public Bracket<Sample> propagateBracket(final Bracket<Sample> prevBracket) {
		return propagateBracket(prevBracket, null);
	}

	@Override
	public Bracket<Sample> propagateBracket(final Bracket<Sample> prevBracket, final Date ts) {
		if (latestRecord == null) { // the stream is dry already, so just return current
			return prevBracket;
		}
		SrcRecord curRecord = nextRecord();
		Sample newSample = null;
		if (ts != null) { // 'fast forward' to target
			SrcRecord prevRecord = latestRecord;
			for (; curRecord != null && getLatestTimestamp().compareTo(ts) < 0; curRecord = nextRecord()) {
				prevRecord = curRecord;
			}
			prevBracket.pushNewSample(createSample(prevRecord));
		}
		if (curRecord != null) { // the stream hasn't dried up before we found the upper bound infimum
			newSample = createSample(curRecord);
			latestRecord = curRecord;
		}

		return prevBracket.pushNewSample(newSample);
	}

	/*
	 * 0) <> => null
	 * 1) Q1(ts <= t) => [null, Q1]
	 * 2) Q1(t < ts) => [Q1, null]
	 * 3) Q1(ts <= t1), Q2(t2)  => [null, Q1]
	 * 4) Q1(t1 < ts), Q2(t1 < t2 < ts) => [Q2, null]
	 * 5) Q1(t1 < ts), Q2(t1 < ts <= t2) => [Q2, null]
	 */
	@Override
	public Bracket<Sample> getBracket(final Date ts) {
		SrcRecord prevRecord = latestRecord;
		SrcRecord curRecord = nextRecord();
		for (; curRecord != null && getLatestTimestamp().compareTo(ts) < 0; curRecord = nextRecord()) {
			prevRecord = curRecord;
		}
		if (curRecord == null) { // the stream dried up before we found the upper bound infimum on ts
			if (latestRecord == null) { // it even dried up on the first time through!
				latestRecord = prevRecord;
			}
			return new Bracket<Sample>(createSample(latestRecord), null);
		}
		latestRecord = curRecord;
		// getLatestTimestamp().compareTo(ts) >= 0:
		if (prevRecord == null) { // the first bracket
			return new Bracket<Sample>(null, createSample(curRecord));
		}

		return new Bracket<Sample>(createSample(prevRecord), createSample(curRecord));
	}

	// -------- Protected ----------

	protected AbstractTimeSeriesInputResource() {
	}

	protected AbstractTimeSeriesInputResource(final Locator locator) {
		super(locator);
	}

	protected void openImpl() throws ResourceIOException {
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(0);
		latestTimestamp = cal.getTime();
		latestRecord = null;
	}

	protected void setLatestTimestamp(final Date timestamp) {
		latestTimestamp = timestamp;
	}
	protected abstract Sample createSample(final SrcRecord record);
	protected abstract SrcRecord nextRecord();

	// -------- Private ----------

	private Date latestTimestamp;
	private SrcRecord latestRecord;
}
