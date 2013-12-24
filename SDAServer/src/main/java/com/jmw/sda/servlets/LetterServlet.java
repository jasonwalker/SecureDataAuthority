package com.jmw.sda.servlets;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.jmw.sda.Constants.UrlParams;
import com.jmw.sda.Utils.ServletUtils;
import com.jmw.sda.crypto.AbstractCrypto;
import com.jmw.sda.crypto.FailedCryptException;
import com.jmw.sda.crypto.IPublicKey;
import com.jmw.sda.crypto.JavaInstallationMissingComponentsException;
import com.jmw.sda.crypto.Utils;
import com.jmw.sda.dbProviders.CurrentProvider;
import com.jmw.sda.dbProviders.DBException;
import com.jmw.sda.dbProviders.IDatabase;
import com.jmw.sda.dbProviders.Mailbox;
import com.jmw.sda.transport.ServerDataMover;
import com.jmw.sda.transport.TransportUtils;
import com.jmw.sda.transport.objects.ServerMailMetaData;
import com.jmw.sda.transport.objects.TotalListItem;


@WebServlet({"/letter", "/letter/*"})
public class LetterServlet extends HttpServlet {
	private static final long serialVersionUID = -2606126609616453364L;

	protected static AbstractCrypto crypto = AbstractCrypto.getCrypto();
	protected IDatabase database = CurrentProvider.get();
	public LetterServlet() {
        super();
    }
	
	protected void listAvailableLetters(String mailboxName, HttpServletResponse response) throws IOException, DBException{
		try{
			TotalListItem[] mailInfo = this.database.getList(mailboxName);

			if (mailInfo == null){
				mailInfo = new TotalListItem[0];
			}
			response.setStatus(HttpServletResponse.SC_OK);
			try(OutputStream os = response.getOutputStream();){
				response.setContentType("text/plain");
				ServerDataMover.sendMailList(mailInfo, os);
			}
		}catch(JavaInstallationMissingComponentsException | IOException e){
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Utils.ppStackTrace(e));
		}
	}
	
	/**
	 * If only mailbox name is provided, returns list of available letters
	 * If letter id also provided, returns encrypted letter data
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String[] pathInfo = ServletUtils.getPathInfo(request);
		if (pathInfo == null){
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Must provide a mailbox name");
			return;
		}
		String mailboxName = pathInfo[1];
		if (ServletUtils.stringIsMalformed(mailboxName)) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Malformed mailbox name");
			return;
		}
		try{
			listAvailableLetters(mailboxName, response);
		}catch(DBException e){
			throw new ServletException(e);
		}
	}
	/**
	 * Call this PUT to send encrypted data to server
	 */
	@Override
	protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try(InputStream is = request.getInputStream();){
			ServerMailMetaData serverData = ServerDataMover.getServerData(is);
			for (String toBox : serverData.getToMailbox()){
				if (ServletUtils.stringIsMalformed(toBox)) {
					response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Malformed destination mailbox name");
					return;
				}
			}
			try{
				Mailbox fromMailbox = this.database.getMailbox(serverData.getFromMailbox());
				if (!serverData.verify(crypto.stringToPublicKey(fromMailbox.getPublicKey()))){
					response.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE, "Signature does not match public key, message not saved.");
					return;
				}
				int counter = 0;
				while (ServerDataMover.hasMore(is)){
					byte[] listData = ServerDataMover.getListData(is);
					if (counter >= serverData.getToMailbox().length){
						response.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE, "Client sent too much data for named recipients.");
						return;
					}
					this.database.putMail(serverData.getToMailbox()[counter++], serverData.getTimestamp(), request.getRemoteAddr(), listData);
				}
				response.setStatus(HttpServletResponse.SC_CREATED);

			}catch(DBException e){
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
			}
		}catch(FailedCryptException | JavaInstallationMissingComponentsException e){
			log(Utils.ppStackTrace(e));
			throw new ServletException(e);
		}
	}
	/**
	 * This is for delete.  could not use delete because needs a signature parameter
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
		String[] pathInfo = ServletUtils.getPathInfo(request);
		if (pathInfo == null){
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Must provide a mailbox name");
			return;
		}
		if (pathInfo.length < 3){
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Must provide a letter id");
			return;			
		}
		String mailboxName = pathInfo[1];
		if (ServletUtils.stringIsMalformed(mailboxName)) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Malformed mailbox name");
			return;
		}
		String letterId = pathInfo[2];
		if (ServletUtils.stringIsMalformed(mailboxName)) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Malformed letter id");
			return;
		}
		String signature = request.getParameter(UrlParams.signature);
		try {
			Mailbox mailbox = this.database.getMailbox(mailboxName);
			IPublicKey signerKey = crypto.stringToPublicKey(mailbox.getPublicKey());
			if (crypto.verify(signerKey, signature, TransportUtils.getStringForActionMailboxNameAndId(UrlParams.delete, mailboxName, letterId))){
				this.database.deleteMail(mailboxName, letterId);
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
