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

package com.bormannqds.lib.auxdataaccess.resources;

import java.io.IOException;

@SuppressWarnings("serial")
public class ResourceIOException extends IOException {
	public ResourceIOException() {
		super();
	}

	public ResourceIOException(final String msg) {
		super(msg);
	}

	public ResourceIOException(Throwable cause) {
		super(cause);
	}

	public ResourceIOException(final String msg, Throwable cause) {
		super(msg, cause);
	}
}
