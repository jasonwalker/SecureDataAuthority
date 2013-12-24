package com.jmw.sda;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import com.jmw.sda.controller.AdminController;
import com.jmw.sda.controller.CreateMailboxController;
import com.jmw.sda.controller.ListMailController;
import com.jmw.sda.crypto.FailedCryptException;
import com.jmw.sda.crypto.IPrivateKey;
import com.jmw.sda.crypto.JavaInstallationMissingComponentsException;
import com.jmw.sda.model.EmailList;
import com.jmw.sda.model.MailboxCache;
import com.jmw.sda.model.SecretKeyHolder;
import com.jmw.sda.utils.Configuration;
import com.jmw.sda.view.AdminPanel;
import com.jmw.sda.view.CreateMailboxPanel;
import com.jmw.sda.view.ListMailPanel;
import com.jmw.sda.view.MainFrame;

public class Main {
	protected static ListMailController listMailController;
	protected static CreateMailboxController createMailboxController;
	protected static AdminController adminController;
    public static void main(String[] args) throws JavaInstallationMissingComponentsException, FailedCryptException {
        try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException
				| IllegalAccessException | UnsupportedLookAndFeelException e) {
			//ignore, just go to default swing look
		}
    	EmailList emailList = new EmailList();
    	final ListMailPanel listMailPanel = new ListMailPanel("Mailbox");
    	listMailController = new ListMailController(listMailPanel, emailList);
    	Configuration.addConfigurationChangeListener(listMailController);
    	final CreateMailboxPanel createMailboxPanel = new CreateMailboxPanel("CreateMailbox");
    	createMailboxController = new CreateMailboxController(createMailboxPanel, new SecretKeyHolder());
    	AdminPanel adminPanel = new AdminPanel("Admin");
    	adminController = new AdminController(adminPanel);
    	adminController.addHostDeleteListener(listMailController);
        javax.swing.SwingUtilities.invokeLater(new StartUI(Configuration.getMyPrivateKey(), Configuration.getMyMailboxName(), listMailPanel, createMailboxPanel, adminPanel));
        MailboxCache.loadLocalMailboxes();
    }
    static class StartUI implements Runnable{
    	protected final IPrivateKey privateKey;
    	protected final String mailboxName;
    	protected final ListMailPanel listMailPanel;
    	protected final CreateMailboxPanel createMailboxPanel;
    	protected final AdminPanel adminPanel;
    	public StartUI(IPrivateKey privateKey, String mailboxName, final ListMailPanel listMailPanel, final CreateMailboxPanel createMailboxPanel, final AdminPanel adminPanel){
    		this.privateKey = privateKey;
    		this.mailboxName = mailboxName;
    		this.listMailPanel = listMailPanel;
    		this.createMailboxPanel = createMailboxPanel;
    		this.adminPanel = adminPanel;
    	}
		@Override
		public void run() {
            MainFrame mainView = new MainFrame(this.mailboxName, this.listMailPanel, this.createMailboxPanel, this.adminPanel);
            createMailboxController.addMailboxNameChangeListener(mainView.getMailboxNameChangeListener());
            listMailController.addMailboxNameChangeListener(mainView.getMailboxNameChangeListener());
            createMailboxController.addConfigurationChangeListener(listMailController);
            
            if (this.privateKey == null){
            	mainView.setSelectedTab(this.createMailboxPanel);
            }
		}
    }
}
