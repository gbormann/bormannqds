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

import java.util.Locale;

/**
 * helper class to check the operating system this Java VM runs in
 *
 * please keep the notes below as a pseudo-license
 *
 * http://stackoverflow.com/questions/228477/how-do-i-programmatically-determine-operating-system-in-java
 * compare to http://svn.terracotta.org/svn/tc/dso/tags/2.6.4/code/base/common/src/com/tc/util/runtime/Os.java
 * http://www.docjar.com/html/api/org/apache/commons/lang/SystemUtils.java.html
 */
public final class OsCheck {
  /**
   * types of Operating Systems
   */
  public enum OSType {
    WINDOWS, MACOS, LINUX, OTHER
  };

  /**
   * detect the operating system from the os.name System property and cache
   * the result
   * 
   * @returns - the operating system detected
   */
  public static OSType getOperatingSystemType() {
    return OS_TYPE;
  }

  static {
    final String osName = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);

    if ((osName.indexOf("mac") >= 0) || (osName.indexOf("darwin") >= 0)) {
      OS_TYPE = OSType.MACOS;
    }
    else if (osName.indexOf("win") >= 0) {
      OS_TYPE = OSType.WINDOWS;
    }
    else if (osName.indexOf("nux") >= 0) {
      OS_TYPE = OSType.LINUX;
    }
    else {
      OS_TYPE = OSType.OTHER;
    }
  }

  // cached result of OS detection
  private static OSType OS_TYPE;
}
