package com.jmw.sda.servlets;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;

import com.jmw.sda.Constants.UrlParams;
import com.jmw.sda.Utils.ServletUtils;
import com.jmw.sda.crypto.AbstractCrypto;
import com.jmw.sda.crypto.FailedCryptException;
import com.jmw.sda.crypto.IPublicKey;
import com.jmw.sda.crypto.JavaInstallationMissingComponentsException;
import com.jmw.sda.crypto.Utils;
import com.jmw.sda.dbProviders.CurrentProvider;
import com.jmw.sda.dbProviders.DBException;
import com.jmw.sda.dbProviders.IDAndOutputStream;
import com.jmw.sda.dbProviders.IDatabase;
import com.jmw.sda.dbProviders.Mailbox;
import com.jmw.sda.transport.ServerDataMover;
import com.jmw.sda.transport.TransportUtils;

@WebServlet({"/attachment", "/attachment/*"})
public class AttachmentServlet extends HttpServlet{
	private static final long serialVersionUID = -788530977133922038L;
	protected IDatabase database;
	protected static AbstractCrypto crypto = AbstractCrypto.getCrypto();
	public AttachmentServlet() {
		this.database = CurrentProvider.get();
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try{
			String[] pathInfo = ServletUtils.getPathInfo(request);
			if (pathInfo == null){
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Must provide an attachment id");
				return;
			}
			String attachmentId = pathInfo[1];
			if (ServletUtils.stringIsMalformed(attachmentId)) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Malformed attachment id");
				return;
			}
			response.setStatus(HttpServletResponse.SC_OK);
			try(InputStream is = this.database.getAttachment(attachmentId);
				OutputStream os = response.getOutputStream();){
				ServerDataMover.sendAttachment(is, os);
			}
		}catch(DBException e){
			throw new ServletException("Server error: attachment could not be retrieved");
		}
	}
	
	
	
	@Override
	protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
		try{
			List<String> idList = new ArrayList<>();
			try(InputStream is = request.getInputStream();){
				if (ServerDataMover.hasAttachments(is)){
					while(ServerDataMover.hasMore(is)){
						try(IDAndOutputStream os = this.database.putAttachment();){
							idList.add(os.getId());
							ServerDataMover.getAttachment(is, os.getOs());
						}
					}
				}
			}
			response.setStatus(HttpServletResponse.SC_CREATED);
			try(OutputStream os  = response.getOutputStream();){
				if (idList.size() != 0){
					os.write(StringUtils.join(idList, " ").getBytes(Utils.ENCODING));
				}
			}
		}catch (DBException e){
			throw new ServletException(e);
		}
	}
	/**
	 * This is for delete, can't use delete HTTP call because we need to send signature
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
		String[] pathInfo = ServletUtils.getPathInfo(request);
		if (pathInfo == null || pathInfo.length < 2){
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Must provide an mailbox name");
			return;
		}
		if (pathInfo.length < 3){
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Must provide an attachment id");
			return;				
		}
		String mailboxName = pathInfo[1];
		String attachmentId = pathInfo[2];
		if (ServletUtils.stringIsMalformed(mailboxName)) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Malformed mailbox name");
			return;
		}
		if (ServletUtils.stringIsMalformed(attachmentId)) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Malformed attachment id");
			return;
		}
		String action = request.getParameter(UrlParams.action);
		String signature = request.getParameter(UrlParams.signature);
		String fromMailbox = request.getParameter(UrlParams.mailboxName);
		if (fromMailbox == null || fromMailbox.length() == 0){
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Must supply \"From Mailbox\" as POST parameter");
			return;
		}
		if (ServletUtils.stringIsMalformed(fromMailbox)){
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Malformed \"From Mailbox\"");
			return;			
		}
		try {
			Mailbox mailbox = this.database.getMailbox(fromMailbox);
			IPublicKey signerKey = crypto.stringToPublicKey(mailbox.getPublicKey());
			if (crypto.verify(signerKey, signature, TransportUtils.getStringForActionMailboxNameAndId(action, mailboxName, attachmentId))){
				if (action.equalsIgnoreCase(UrlParams.delete)){
					this.database.deleteAttachment(mailboxName, attachmentId);
					response.setStatus(HttpServletResponse.SC_OK);
				}else if (action.equalsIgnoreCase(UrlParams.addRecipient)){
					this.database.addMailboxToAttachment(mailboxName, attachmentId);
				}else{
					response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Incorrect action specified: " + action);
					return;
				}
			}else{
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Supplied signature failed to verify");
				return;
			}
		} catch (JavaInstallationMissingComponentsException
				| FailedCryptException | DBException e) {
			throw new ServletException(e);
		}			
		response.setStatus(HttpServletResponse.SC_OK);
	}
}
