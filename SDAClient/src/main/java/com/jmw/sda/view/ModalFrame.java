package com.jmw.sda.view;

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

public class ModalFrame extends JDialog {
	private static final long serialVersionUID = 6495179099153997737L;
	protected JPanel panel;
	public ModalFrame(JFrame parent, String title, JPanel panel, int width, int height) throws HeadlessException {
		super(parent, true);
		JTabbedPane pane = new JTabbedPane();
		this.getContentPane().add(pane);
		this.panel = panel;
		this.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
		pane.addTab(title, panel);
		this.addComponentListener(new ResizeListener());
        pack();
        setSize(width, height);
        setVisible(true);
	}

    class ResizeListener extends ComponentAdapter{
		@Override
		public void componentResized(ComponentEvent evt) {
			Dimension dimension = evt.getComponent().getSize();
			dimension.setSize(dimension.getWidth()-10, dimension.getHeight());
			ModalFrame.this.panel.setSize(dimension);
		}
    }
}
