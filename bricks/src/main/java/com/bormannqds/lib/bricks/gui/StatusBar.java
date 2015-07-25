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

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import javax.swing.border.BevelBorder;
import java.awt.*;

@SuppressWarnings("serial")
public class StatusBar extends JPanel {
	public StatusBar() {
		super();
		initialise();
	}

	public AppStatusInterface getAppStatusBean() {
		return appStatusBean;
	}

	// -------- Private ----------

	private class AppStatusBean implements AppStatusInterface {
		public AppStatusBean(final String initialStatus) {
			setStatus(initialStatus);
		}

		@Override
		public String getStatus() {
			return statusTextField.getText();
		}

		@Override
		public void setStatus(final String status) {
			statusTextField.setText(status);
		}
	}

	private void initialise() {
		GroupLayout groupLayout = new GroupLayout(this);
		setLayout(groupLayout);

		setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		setPreferredSize(new Dimension(570, 40));
		setMinimumSize(new Dimension(570, 40));
		setMaximumSize(new Dimension(32767, 40));

		statusTextField.setAlignmentX(Component.LEFT_ALIGNMENT);
		statusTextField.setHorizontalAlignment(SwingConstants.LEFT);
		statusTextField.setToolTipText("Showing current status or result of latest operation");
		statusTextField.setEditable(false);
		statusTextField.setColumns(40);
		{
			JLabel lblStatusLine = new JLabel("Status:");
			lblStatusLine.setAlignmentX(Component.CENTER_ALIGNMENT);
			lblStatusLine.setHorizontalAlignment(SwingConstants.CENTER);

			groupLayout.setHorizontalGroup(
				groupLayout.createParallelGroup(Alignment.LEADING)
					.addGroup(groupLayout.createSequentialGroup()
						.addGap(5)
						.addComponent(lblStatusLine)
						.addGap(5)
						.addComponent(statusTextField, GroupLayout.PREFERRED_SIZE, 500, 32767)
						.addGap(5))
			);
			groupLayout.setVerticalGroup(
				groupLayout.createParallelGroup(Alignment.LEADING)
					.addGroup(groupLayout.createSequentialGroup()
						.addGap(10)
						.addComponent(lblStatusLine))
					.addGroup(groupLayout.createSequentialGroup()
						.addGap(5)
						.addComponent(statusTextField, GroupLayout.PREFERRED_SIZE, 25, 32767)
						.addGap(5))
			);
		}
	}

	private final JTextField statusTextField = new JTextField();
	private final AppStatusInterface appStatusBean = new AppStatusBean("Initialising..."); 
}
