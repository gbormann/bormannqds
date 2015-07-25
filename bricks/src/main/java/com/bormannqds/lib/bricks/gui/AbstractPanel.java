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

import java.awt.LayoutManager;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

@SuppressWarnings("serial")
abstract public class AbstractPanel extends JPanel {

	/**
	 * Create the panel.
	 */
	public AbstractPanel() {
		super();
	}

	/**
	 * Create the panel with a BoxLayout.
	 */
	public AbstractPanel(final int boxLayoutStyle) {
		super();
		setLayout(new BoxLayout(this, boxLayoutStyle));
	}

	/**
	 * Create the panel with a custom layout manager.
	 */
	public AbstractPanel(final LayoutManager layoutManager) {
		super(layoutManager); //PREFERRED! <- avoids creating an unused FlowLayout object
	}

}
