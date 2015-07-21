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

/**
 * This abstract class implements the generic part of a read-only resource wrapper.
 * 
 * @author guy
 *
 * @param <Locator>
 */
public abstract class AbstractResourceWrapper<Locator>
	extends AbstractResourceLocatorBean<Locator>
	implements ResourceLocatorBean<Locator>,
				ResourceWrapper {
	@Override
	public void open() throws ResourceIOException {
		if (!isOpen()) {
			openImpl();
			isOpen = true;
		}
	}

    @Override
	public boolean isOpen() {
		return isOpen;
	}

    @Override
	public void close() throws ResourceIOException {
		if (isOpen()) {
			doClose();
		}
	}

	// -------- Protected ---------

	protected AbstractResourceWrapper() {
	}

	protected AbstractResourceWrapper(final Locator locator) {
		super(locator);
	}

	protected void doClose() throws ResourceIOException {
		closeImpl();
		isOpen = false;
	}

	@Override
	protected void reset() throws ResourceIOException {
		close();
	}

	protected abstract void openImpl() throws ResourceIOException ;
	protected abstract void closeImpl() throws ResourceIOException;

	// -------- Private ----------

	private boolean isOpen = false;
}
