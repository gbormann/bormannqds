/*
    Copyright 2014, 2015 Guy Bormann

    This file is part of bricks.

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

package com.bormannqds.lib.bricks.gui;

import com.bormannqds.lib.bricks.gateway.AppStatusInterface;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;

@SuppressWarnings("serial")
public abstract class AbstractShowOpenFileDialogAction extends AbstractAction {
	public void actionPerformed(ActionEvent ae) {
		if (ae.getActionCommand().equals(getValue(ACTION_COMMAND_KEY))) {
			final JFileChooser fileChooser = getCustomisedFileChooser();
			File selectedFile = null;
			int chooserResult = fileChooser.showOpenDialog(getDialogParent());

			switch(chooserResult) {
			case JFileChooser.APPROVE_OPTION:
				selectedFile = fileChooser.getSelectedFile();
				break;
			case JFileChooser.ERROR_OPTION:
				LOGGER.error(errorMsg);
				if (appStatusInterface != null) appStatusInterface.setStatus(errorMsg);
				// TODO Display an error message
			case JFileChooser.CANCEL_OPTION:
				// Do nothing...
				break;
			}

			if (selectedFile != null) {
				LOGGER.info(successMsg + selectedFile.getAbsolutePath());
				if (appStatusInterface != null) appStatusInterface.setStatus(successMsg + selectedFile.getAbsolutePath());
				storeFile(selectedFile);
			}
		}
	}

	// -------- Protected ---------

	protected AbstractShowOpenFileDialogAction(final String whatKind, final AppStatusInterface appStatusInterface) {
		this.appStatusInterface = appStatusInterface;
		this.errorMsg = "An error occurred whilst the user tried to select a " + whatKind + '!';
		this.successMsg = "Selected " + whatKind + ": ";
	}

	abstract protected JFileChooser getCustomisedFileChooser();
	abstract protected Container getDialogParent();
	abstract protected void storeFile(final File file);

	// -------- Private ---------

	private static final Logger LOGGER = LogManager.getLogger(AbstractShowOpenFileDialogAction.class);

	private final AppStatusInterface appStatusInterface;
	private final String errorMsg;
	private final String successMsg;
}
