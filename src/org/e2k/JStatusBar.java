// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.

// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

// Rivet Copyright (C) 2011 Ian Wraith
// This program comes with ABSOLUTELY NO WARRANTY

package org.e2k;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import javax.swing.border.Border;

public class JStatusBar extends JPanel {
	
	public static final long serialVersionUID=1;
	private JLabel logMode=new JLabel();
	private JLabel statusLabel=new JLabel();
	private JLabel modeLabel=new JLabel();
	private JProgressBar volumeBar=new JProgressBar(0,100);
	private Border loweredbevel=BorderFactory.createLoweredBevelBorder();
	private Rivet TtheApp;
	
	public JStatusBar() {
		logMode.setHorizontalAlignment(SwingConstants.LEFT);
		logMode.updateUI();
		logMode.setBorder(loweredbevel);
		statusLabel.setHorizontalAlignment(SwingConstants.LEFT);
		statusLabel.setBorder(loweredbevel);
		statusLabel.updateUI();
		modeLabel.setHorizontalAlignment(SwingConstants.LEFT);
		modeLabel.setBorder(loweredbevel);
		modeLabel.updateUI();
		// Give the volume progress bar a border //
		volumeBar.setBorder(loweredbevel);
		// Ensure the elements of the status bar are displayed from the left
		this.setLayout(new FlowLayout(FlowLayout.LEFT));
		this.add(volumeBar,BorderLayout.CENTER);
		this.add(modeLabel,BorderLayout.CENTER);
		this.add(logMode,BorderLayout.CENTER);
		this.add(statusLabel,BorderLayout.CENTER);
	}
	
	// Sets the logging label text
	public void setLoggingStatus(String text) {
		logMode.setText(text);
	}

	// Sets the status label
	public void setStatusLabel (String st)	{
		statusLabel.setText(st);
	}
	
	// Set the volume bar display
	public void setVolumeBar(int val) {
		volumeBar.setValue(val);
		volumeBar.repaint();
	}
	
	public void setModeLabel (String label)	{
		modeLabel.setText(label);
	}
	

	public void setApp (Rivet theApp)	{
		TtheApp=theApp;
	}

	// This class listens for button events
	class ButtonListener implements ActionListener {
		  ButtonListener() {
		  }

		  public void actionPerformed(ActionEvent e) {
			
		  }
		}

}