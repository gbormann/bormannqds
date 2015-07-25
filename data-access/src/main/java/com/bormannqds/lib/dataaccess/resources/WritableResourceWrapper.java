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

package com.bormannqds.lib.dataaccess.resources;


/**
 * Wrapper for application read/write resource
 * 
 * @author guy
 *
 */
public interface WritableResourceWrapper extends ResourceWrapper {

	/** 
	 * Set commit state to dirty for the resource at the current locator.
	 * 
	 * NOTE: make sure to call this method when making changes to the resource representation otherwise these changes might not be persisted to the resource on closing.
	 */
	public void setDirty();

	/**
	 * Check commit state of the resource at the current locator.
	 * @return <code>true</code> if there are uncommitted changes - boolean
	 */
	boolean isDirty();

	/**
	 * Close the resource at the current locator.
	 */
	void commit() throws ResourceIOException;
}
