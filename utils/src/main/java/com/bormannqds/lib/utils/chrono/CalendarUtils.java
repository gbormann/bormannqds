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
package com.bormannqds.lib.utils.chrono;

import java.util.Calendar;
import java.util.Date;

public final class CalendarUtils {
	public static final String DATE_FORMAT = "yyyy-MM-dd";
	public static final String KDB_DATE_FORMAT = "yyyy.MM.dd";
	public static final String TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS"; // 2014-10-27 07:02:55.422

	public static Date toStartOfDay(final Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.set(Calendar.MILLISECOND, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.HOUR, 0);
		return calendar.getTime();
	}

	public static Date nrWeeksEarlier(final Date refDate, int nrWeeks) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(refDate);
		calendar.add(Calendar.WEEK_OF_YEAR, -Math.abs(nrWeeks));
		return calendar.getTime();
	}
}
