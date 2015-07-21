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
 * This abstract class implements the generic part of a read/write resource wrapper.
 * 
 * @author guy
 *
 * @param <Locator>
 */
public abstract class AbstractWritableResourceWrapper<Locator>
        extends AbstractResourceWrapper<Locator>
		implements ResourceLocatorBean<Locator>, WritableResourceWrapper {
	@Override
	public void setDirty() {
		isDirty = true;
	}

    @Override
	public boolean isDirty() {
		return isDirty;
	}

    @Override
	public void commit() throws ResourceIOException {
		if (isDirty()) {
			commitImpl();
			isDirty = false;
		}
	}

	@Override
	public void close() throws ResourceIOException {
		if (isOpen()) {
			commit();
			doClose();
		}
	}

	// -------- Protected ---------

	protected AbstractWritableResourceWrapper() {
	}

	protected AbstractWritableResourceWrapper(final Locator locator) {
		super(locator);
	}

	protected abstract void commitImpl() throws ResourceIOException;

	// -------- Private ----------

	private boolean isDirty = false;
}
