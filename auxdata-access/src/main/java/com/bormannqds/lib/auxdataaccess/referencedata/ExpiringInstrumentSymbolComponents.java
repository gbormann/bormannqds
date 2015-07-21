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

import java.util.List;

/**
 * Created by guy on 20/07/15.
 */
public class ExpiringInstrumentSymbolComponents {
    public enum BaseSymbolDecorator {
        PREFIX, INFIX, POSTFIX
    }

    public ExpiringInstrumentSymbolComponents(final String baseSymbol, final List<String> decorators) {
        this.baseSymbol = baseSymbol;
        this.decorators = decorators;
    }

    public String getBaseSymbol() {
        return baseSymbol;
    }

    public List<String> getDecorators() { // to store other symbol-encoded contract properties such as option strikes and optionality (i.e. C/P)
        return decorators;
    }

    // ------ Private ------

    private final String baseSymbol;
    private final List<String> decorators;
}
