package com.jmw.sda.view;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseListener;
import java.util.Collection;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import com.jmw.sda.model.EmailList;
import com.jmw.sda.view.widgets.ScrollingTextArea;

public class ListMailPanel extends JPanel {
	private static final long serialVersionUID = -8137740724373864984L;
	protected MailTable mailTable;
	protected JButton refreshButton;
	protected JButton readButton;
	protected JButton newButton;
	protected JButton deleteButton;
	protected JComboBox<String> hostBox;
	protected JLabel lblMail;
	protected JScrollPane mailScrollPane;
	protected ScrollingTextArea infoTextArea;
	public ListMailPanel(String name) {
		super();
		this.setName(name);
		setLayout(null);
		this.addComponentListener(new ResizeListener());	
		this.refreshButton = new JButton("Refresh");
		this.refreshButton.setBounds(10, 11, 76, 23);
		add(this.refreshButton);
		this.readButton = new JButton("Read");
		this.readButton.setBounds(96, 11, 64, 23);
		add(this.readButton);
		
		this.newButton = new JButton("New");
		this.newButton.setBounds(170, 11, 64, 23);
		add(this.newButton);
		this.deleteButton = new JButton("Delete");
		this.deleteButton.setBounds(244, 11, 69, 23);
		add(this.deleteButton);
		this.hostBox = new JComboBox<>();
		this.hostBox.setBounds(323, 12, 325, 20);
		add(this.hostBox);
		this.infoTextArea = new ScrollingTextArea();
		this.infoTextArea.setBounds(10, 45, 638, 75);
		this.infoTextArea.setEnabled(false);
		add(this.infoTextArea);
		this.mailTable = new MailTable();
		this.mailTable.setColumnSelectionAllowed(false);
		this.mailScrollPane = new JScrollPane(this.mailTable);
		this.mailScrollPane.setBounds(10, 132, 638, 252);
		add(this.mailScrollPane);
	}
	
	public int getSelectedRow(){
		return this.mailTable.getSelectedRow();
	}
	public void addRefreshListener(ActionListener listener){
		this.refreshButton.addActionListener(listener);
	}
	public void removeRefreshListener(ActionListener listener){
		this.refreshButton.removeActionListener(listener);
	}
	public void addHostChangeListener(ItemListener listener){
		this.hostBox.addItemListener(listener);
	}
	public void removeHostChangeListener(ItemListener listener){
		this.hostBox.removeItemListener(listener);
	}
	public void addCreateListener(ActionListener listener){
		this.newButton.addActionListener(listener);
	}
	public void removeCreateListener(ActionListener listener){
		this.newButton.removeActionListener(listener);
	}
	public void addReadListener(ActionListener listener){
		this.readButton.addActionListener(listener);
	}
	public void removeReadListener(ActionListener listener){
		this.readButton.removeActionListener(listener);
	}
	public void addDeleteListener(ActionListener listener){
		this.deleteButton.addActionListener(listener);
	}
	public void removeDeleteListener(ActionListener listener){
		this.deleteButton.removeActionListener(listener);
	}
	@Override
	public synchronized void addMouseListener(MouseListener listener){
		this.mailTable.addMouseListener(listener);
	}
	@Override
	public synchronized void removeMouseListener(MouseListener listener){
		this.mailTable.removeMouseListener(listener);
	}
	
	public void setInfo(String info){
		this.infoTextArea.setText(info);
	}
	
	public void setModel(EmailList dataModel){
		this.mailTable.setModel(dataModel);
	}
	public void addAllHosts(Collection<String> hosts){
		String selected = (String)this.hostBox.getSelectedItem();
		this.hostBox.removeAllItems();
		for(String host : hosts){
			this.hostBox.addItem(host);
		}
		if (selected != null && hosts.contains(selected)){
			this.hostBox.setSelectedItem(selected);
		}
	}
	public void addHost(String host){
		this.hostBox.addItem(host);
	}
	public void setCurrentHost(String host){
		this.hostBox.setSelectedItem(host);
	}
	public String getCurrentHost(){
		return (String)this.hostBox.getSelectedItem();
	}
	
	class MailTable extends JTable{
		private static final long serialVersionUID = 30889488340673109L;
		protected EmailList emailModel;
		public MailTable(){
			this.setAutoCreateRowSorter(true);
		}

		public void setModel(EmailList model){
			super.setModel(model);
			this.emailModel = model;
			//chooose time column for sorting
			this.getRowSorter().toggleSortOrder(EmailList.getTimeColumn());
			//change time column from ascending to descending
			this.getRowSorter().toggleSortOrder(EmailList.getTimeColumn());
		}
		@Override
	    public Component prepareRenderer(TableCellRenderer renderer, int row, int column){
            Component comp = super.prepareRenderer(renderer, row, column);
			try{
				comp.setFont(Styling.UNREAD_EMAIL);
				if(!this.emailModel.isRowSignatureValid(row)){
					comp.setForeground(Styling.FAILED_SIGNATURE_LIST_FOREGROUND);
					comp.setBackground(Styling.FAILED_SIGNATURE_LIST_BACKGROUND);
				}
			}catch(Exception e){
				comp.setForeground(Styling.EXCEPTION_SIGNATURE_LIST_FOREGROUND);
				comp.setBackground(Styling.EXCEPTION_SIGNATURE_LIST_BACKGROUND);
			}
            return comp;
        }		
	}
	
    class ResizeListener extends ComponentAdapter{
		@Override
		public void componentResized(ComponentEvent evt) {
			Dimension dimension = evt.getComponent().getSize();
			int width = (int)dimension.getWidth();
			int height = (int)dimension.getHeight();
			ListMailPanel.this.mailScrollPane.setSize(width - Styling.BORDER_WIDTH*2, height - (ListMailPanel.this.mailScrollPane.getY() + Styling.BORDER_WIDTH));
			ListMailPanel.this.mailTable.setSize(width - Styling.BORDER_WIDTH*2, height - (ListMailPanel.this.mailScrollPane.getY() + Styling.BORDER_WIDTH));
			ListMailPanel.this.infoTextArea.setSize(width - Styling.BORDER_WIDTH*2, ListMailPanel.this.infoTextArea.getHeight());
			ListMailPanel.this.hostBox.setSize(width - (ListMailPanel.this.hostBox.getX()+Styling.BORDER_WIDTH), ListMailPanel.this.hostBox.getHeight());
		}
    }

}
