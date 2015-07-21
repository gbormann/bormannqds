/*
    Copyright 2014, 2015 Guy Bormann

    This file is part of auxdata-access.

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

package com.bormannqds.lib.auxdataaccess.referencedata;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDate;
import java.util.List;
import java.util.StringJoiner;

/**
 * Expands generic symbols to FQ symbols using roll schedules and a symbol parser.
 */
public class FqSymbols<SymbolParser extends ExpiringInstrumentSymbolParser> {
    public FqSymbols(final RollScheduleResourceInterface rollScheduleResource, SymbolParser symbolParser) {
        this.rollScheduleResource = rollScheduleResource;
        this.symbolParser = symbolParser;
    }

    /**
     * Expands a given symbol to a Fully Qualified symbol. If symbol is already FQ, it returns the symbol itself! So,
     * if you don't want this behaviour, only pass generic or naked base symbols. The same applies to basket legs!
     *
     * @param date
     * @param symbol
     * @return
     * @throws MissingRollScheduleException
     * @throws UnrecognisedSymbolException
     */
    public String expandToFqSymbolOn(LocalDate date, String symbol) throws MissingRollScheduleException, UnrecognisedSymbolException {
        if (symbolParser.isFqSymbol(symbol))
            return symbol;
        List<ExpiringInstrumentSymbolComponents> legsSymbolComponents = symbolParser.parseExceptFqSymbols(symbol);
        if (legsSymbolComponents.size() > 1) { // composed symbol, i.e. basket symbol such as synthetic spread symbol
            final StringJoiner symbolJoiner = new StringJoiner("-");
            for (ExpiringInstrumentSymbolComponents symbolComponents: legsSymbolComponents) {
                if (symbolComponents instanceof GenericSymbolComponents) symbolJoiner.add(treatGenericCase(date, (GenericSymbolComponents) symbolComponents));
                else if (symbolComponents instanceof IdentitySymbolComponents) symbolJoiner.add(((IdentitySymbolComponents) symbolComponents).getSymbol());
            }
            return symbolJoiner.toString();
        }
        else { // simple symbol, i.e. outright, exchange spread or basket leg
            ExpiringInstrumentSymbolComponents symbolComponents = legsSymbolComponents.get(0);
            if (symbolComponents instanceof GenericSymbolComponents) return treatGenericCase(date, (GenericSymbolComponents) symbolComponents);
            else if (symbolComponents instanceof IdentitySymbolComponents) return ((IdentitySymbolComponents) symbolComponents).getSymbol();
        }

        return null;
    }

//private:
    private String treatGenericCase(LocalDate date, GenericSymbolComponents symbolComponents) throws MissingRollScheduleException {
        final String baseSymbol = symbolComponents.getBaseSymbol();
        short rank = symbolComponents.getRank();
        return rollScheduleResource.getRollSchedule(baseSymbol).getNthFrontMonthSymbolOn(date, rank);
    }

    //private:
    private final RollScheduleResourceInterface rollScheduleResource;
    private final SymbolParser symbolParser;
}
