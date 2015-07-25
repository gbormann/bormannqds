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

package com.bormannqds.lib.dataaccess.referencedata;

import java.util.Set;

public class Spread extends Instrument {
	public Spread(final Set<String> internalBaseSymbols, final Currency ccy, final double minPriceVariance, final double tickValue) {
		super(internalBaseSymbols, InstrumentType.Spread, ccy);
		this.minPriceVar = minPriceVariance;
		this.tickValue = tickValue;
	}

	public double getMinPriceVariance() {
		return minPriceVar;
	}

	public double getTickValue() {
		return tickValue;
	}

	@Override
	public String toString() {
		return "Spread [" + super.toString() + ", minPriceVar=" + minPriceVar + ", tickValue="
				+ tickValue + "]";
	}

	// -------- Private ----------

	private final double minPriceVar;
	private final double tickValue;
}
