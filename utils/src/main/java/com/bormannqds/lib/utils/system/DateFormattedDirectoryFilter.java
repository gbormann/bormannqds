/*
    Copyright 2014, 2015 Guy Bormann

    This file is part of utils.

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
package com.bormannqds.lib.utils.system;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bormannqds.lib.utils.chrono.CalendarUtils;

public class DateFormattedDirectoryFilter implements DirectoryStream.Filter<Path> {
	public DateFormattedDirectoryFilter(final Date cutoffDate) {
		this.cutoffDate = cutoffDate;
	}

	@Override
	public boolean accept(final Path entry) throws IOException {
		try {
			if (Files.isDirectory(entry)) {
				Date dateFromPathname = dateFormat.parse(entry.getFileName().toString());
				if (dateFromPathname.after(cutoffDate)) {
					return true;
				}
			}
		}
		catch (ParseException pe) {
			LOGGER.warn("Base directory pollution? " + entry.toString(), pe);
		}
		return false;
	}

	// -------- Private ----------

	private static final Logger LOGGER = LogManager.getLogger(DateFormattedDirectoryFilter.class);

	private final DateFormat dateFormat = new SimpleDateFormat(CalendarUtils.DATE_FORMAT);
	private final Date cutoffDate;
}
