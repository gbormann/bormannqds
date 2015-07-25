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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Date;

public final class FileSystemUtils {

	public static boolean isExistingReadableDirectory(final Path path) {
		return Files.exists(path) && Files.isDirectory(path) && Files.isReadable(path); 
	}

	public static DirectoryStream<Path> getDateNameFilteredSubdirDirectoryStream(final Path basePath, final Date cutoffDate) throws IOException {
		return Files.newDirectoryStream(basePath, new DateFormattedDirectoryFilter(cutoffDate));
	}

	public static DirectoryStream<Path> getSubdirFilteredDirectoryStream(Path basePath) throws IOException {
		return Files.newDirectoryStream(basePath, new SubDirectoryFilter());
	}

	public static void createRWDirectory(Path requestedPath) throws IOException {
		if (Files.notExists(requestedPath)) {
			switch (OsCheck.getOperatingSystemType()) {
			case WINDOWS:
			case OTHER:
				Files.createDirectory(requestedPath);
				break;
			case LINUX:
			case MACOS:
				Files.createDirectory(requestedPath, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwxr-x---")));
				break;
			}
		}
		else if (!Files.isDirectory(requestedPath)){
			LOGGER.fatal("Existing " + requestedPath + " is not a directory!");
			throw new IOException("Existing " + requestedPath + " is not a directory!");
		}
		else if (!(Files.isReadable(requestedPath)
				&& Files.isWritable(requestedPath))) {
			LOGGER.fatal(requestedPath + " is not read/writeable!");				
			throw new IOException(requestedPath + " is not read/writeable!");
		}
	}

	public static File toFile(final URL fileLocator) throws URISyntaxException {
		Path filePath = Paths.get(fileLocator.toURI());
		return filePath.toFile();
	}

	public static BufferedReader createBufferedReader(final Path filePath) throws IOException {
		if (Files.exists(filePath)) {
			return Files.newBufferedReader(filePath, StandardCharsets.UTF_8);
		}
		return null;
	}

	public static BufferedWriter createBufferedWriter(final URL fileLocator) throws IOException, URISyntaxException {
		Path filePath = Paths.get(fileLocator.toURI());
		if (Files.notExists(filePath)) {
			Path parentOrderPath = null;
			switch (OsCheck.getOperatingSystemType()) {
			case WINDOWS:
			case OTHER:
				parentOrderPath = Files.createFile(filePath);
				break;
			case LINUX:
			case MACOS:
				parentOrderPath = Files.createFile(filePath , PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-r-----")));
				break;
			}
			if (parentOrderPath == null) {
				LOGGER.fatal("Could not create a writer to " + filePath + " for reason unknown!");
				throw new IOException("Could not create a writer to " + filePath + " for reason unknown!");
			}
			return Files.newBufferedWriter(parentOrderPath, StandardCharsets.UTF_8, StandardOpenOption.WRITE);
		}
		return Files.newBufferedWriter(filePath, StandardCharsets.UTF_8, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
	}

	// -------- Private ----------

	private static final Logger LOGGER = LogManager.getLogger(FileSystemUtils.class);
}
