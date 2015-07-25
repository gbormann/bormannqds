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

import com.bormannqds.lib.dataaccess.resources.ResourceNotOpenException;

public interface ReferenceDataResourceInterface
{
    /**
     * Get generic instrument via internal base symbol.
     * @param baseSymbol - base symbol from internal symbology
     * @return - generic Instrument
	 * @throws MissingInstrumentException
	 * @throws UnsupportedInstrumentTypeException
     */
	Instrument getInstrument(final String baseSymbol) throws MissingInstrumentException, UnsupportedInstrumentTypeException;

	/**
	 * Get future instrument by internal symbol.
	 * 
	 * @param symbol - symbol from internal symbology
	 * @return - Future implementation of Instrument
	 * @throws ResourceNotOpenException
	 * @throws UnrecognisedSymbolException
	 * @throws NotAFutureSymbolException
	 * @throws MissingInstrumentException
	 */
	Future getFuture(final String symbol) throws ResourceNotOpenException, UnrecognisedSymbolException, NotAFutureSymbolException, MissingInstrumentException;

	/**
	 * Get spread instrument by internal symbol.
	 *
	 * @param symbol - symbol from internal symbology
	 * @return - Spread implementation of Instrument
	 * @throws ResourceNotOpenException
	 * @throws UnrecognisedSymbolException
	 * @throws NotAnXchSpreadSymbolException
	 * @throws MissingInstrumentException
	 */
	Spread getSpread(final String symbol) throws ResourceNotOpenException, UnrecognisedSymbolException, NotAnXchSpreadSymbolException, MissingInstrumentException;
}
