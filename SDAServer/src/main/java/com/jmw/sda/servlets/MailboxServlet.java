package com.jmw.sda.servlets;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.jmw.sda.Constants.UrlParams;
import com.jmw.sda.Utils.Configuration;
import com.jmw.sda.Utils.ServletUtils;
import com.jmw.sda.crypto.AbstractCrypto;
import com.jmw.sda.crypto.FailedCryptException;
import com.jmw.sda.crypto.IPrivateKey;
import com.jmw.sda.crypto.IPublicKey;
import com.jmw.sda.crypto.JavaInstallationMissingComponentsException;
import com.jmw.sda.crypto.Utils;
import com.jmw.sda.dbProviders.CurrentProvider;
import com.jmw.sda.dbProviders.DBException;
import com.jmw.sda.dbProviders.IDatabase;
import com.jmw.sda.dbProviders.Mailbox;
import com.jmw.sda.transport.ServerDataMover;

@WebServlet("/mailbox/*")
public class MailboxServlet extends HttpServlet {
	private static final long serialVersionUID = -5513339145835245212L;
	private static IPrivateKey privateKey;
	protected static AbstractCrypto crypto = AbstractCrypto.getCrypto();

    public MailboxServlet() {
        super();
        
    }
    
    @Override
    public void init(){
    	try {
    		Configuration config = new Configuration(new File("."));
			privateKey = crypto.stringToPrivateKey(config.getPrivateKey());
		} catch (IOException | JavaInstallationMissingComponentsException | FailedCryptException e) {
			e.printStackTrace();
		}
    }

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String path = request.getPathInfo();
		if (path == null || path.length() < 1){
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Must provide a mailbox name");
			return;
		}
		String mailboxName = path.substring(1);
		if (ServletUtils.stringIsMalformed(mailboxName)) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Malformed mailbox name: " + mailboxName);
			return;
		}
		try{
			IDatabase database = CurrentProvider.get();
			Mailbox mailbox = database.getMailbox(mailboxName);
			if (mailbox == null){
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Mailbox: " + mailboxName + " does not exist.");
				return;
			}
			response.setStatus(HttpServletResponse.SC_OK);
			response.setContentType("text/plain");
			ServerDataMover.sendMailbox(mailbox.getPublicKey(), mailbox.getServerSignature(), response.getOutputStream());
		}catch(DBException | JavaInstallationMissingComponentsException e){
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}

	/** Call call to this will create a mailbox or delete a mailbox
	 * 
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String mailboxName = request.getParameter(UrlParams.mailboxName);
		String publicKey = request.getParameter(UrlParams.publicKey);
		String action = request.getParameter(UrlParams.action);
		String signature = request.getParameter(UrlParams.signature);
		if (action == null){
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Must provide an action parameter");
			return;
		}
		if (!action.equals(UrlParams.create) && !action.equals(UrlParams.delete)){
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, String.format("Provided action must be '%s' or '%s'", UrlParams.create, UrlParams.delete));
			return;			
		}
		if (action.equals(UrlParams.create)){
			createMailbox(response, mailboxName, publicKey);
		}else if (action.equals(UrlParams.delete)){
			deleteMailbox(response, mailboxName, signature);
		}
	}
	
	protected static void createMailbox(HttpServletResponse response, String encryptedMailboxName, String publicKey) throws IOException{
		if (encryptedMailboxName == null || publicKey == null){
			response.setContentType("text/plain");
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Create must provide a mailbox name and public key");
			return;
		}
		try{
			String decryptedMailboxName = crypto.decrypt(privateKey, encryptedMailboxName);
			if (ServletUtils.stringIsMalformed(decryptedMailboxName)) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Malformed mailbox name: " + decryptedMailboxName);
				return;
			}		
			IDatabase database = CurrentProvider.get();
			String signature = crypto.sign(privateKey, decryptedMailboxName, publicKey);
			database.setMailbox(new Mailbox(decryptedMailboxName, publicKey, signature));
			response.setStatus(HttpServletResponse.SC_CREATED);
		}catch(DBException | JavaInstallationMissingComponentsException e){
			response.setContentType("text/plain");
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
		} catch (FailedCryptException e) {
			System.out.println(Utils.ppStackTrace(e));
			response.setContentType("text/plain");
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Please verify that server's public key is correct");
		}		
	}
	
	protected static void deleteMailbox(HttpServletResponse response, String mailboxName, String signature) throws IOException, ServletException{
		if (mailboxName == null || signature == null){
			response.setContentType("text/plain");
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Delete must provide a mailbox name and signature");
			return;
		}		
		IDatabase database = CurrentProvider.get();
		try {
			Mailbox mailbox = database.getMailbox(mailboxName);
			IPublicKey signerKey = crypto.stringToPublicKey(mailbox.getPublicKey());
			if (crypto.verify(signerKey, signature, mailbox.getPublicKey(), mailboxName)){
				database.deleteMailbox(mailboxName);
				response.setStatus(HttpServletResponse.SC_OK);
			}else{
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Supplied signature failed to verify");
			}
		} catch (JavaInstallationMissingComponentsException
				| FailedCryptException | DBException e) {
			throw new ServletException(e);
		}
	}


}
