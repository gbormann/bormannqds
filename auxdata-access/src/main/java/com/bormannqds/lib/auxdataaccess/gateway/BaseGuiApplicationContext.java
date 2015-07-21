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

package com.bormannqds.lib.auxdataaccess.gateway;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;

/**
 * Application context singleton interface for Swing applications
 * @author bormanng
 *
 */
public class BaseGuiApplicationContext {

	// ------- Public  -----------
	
    public Path getApplicationWorkingDirectory() {
		return appWorkingDirectory;
	}

	public AppStatusInterface getAppStatusBean() {
		return appStatusBean;
	}

	public void setAppStatusBean(AppStatusInterface appStatusBean) {
		this.appStatusBean = appStatusBean;
	}

    // -------- Protected ----------

    protected BaseGuiApplicationContext(final Path appWorkingDirectory) {
        this.appWorkingDirectory = appWorkingDirectory;
    }

	// -------- Private ----------

    private static final Logger LOGGER = LogManager.getLogger(BaseGuiApplicationContext.class);
	private static BaseGuiApplicationContext instance;

	private final Path appWorkingDirectory;

	private AppStatusInterface appStatusBean = null;
}
