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

//for symbology map:
//import java.util.Map;

import java.util.Set;

public abstract class Instrument implements InstrumentInterface {
	@Override
	public Set<String> getInternalBaseSymbols() {
		return baseSymbols;
	}

	/*
	String getSymbol(final Symbology symbology) {
		return symbols.get(symbology); // TODO Implement proper symbol-not-found signalling...
	}
	*/

	@Override
	public InstrumentType getProductType() {
		return type;
	}

	@Override
	public Currency getCurrency() {
		return ccy;
	}

	@Override
	public String toString() {
		return "Instrument [baseSymbols=" + baseSymbols + ", type=" + type
				+ ", ccy=" + ccy + "]";
	}

	// -------- Protected ----------

	protected Instrument(final Set<String> baseSymbols, final InstrumentType type, final Currency ccy) {
		this.baseSymbols = baseSymbols;
		this.type = type;
		this.ccy = ccy;
	}

	// -------- Private ----------

	private final Set<String> baseSymbols;
	private final InstrumentType type;
	private final Currency ccy;
	//private final Map<Symbology, String> symbols;
}
