package com.jmw.sda.servlets;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.jmw.sda.Constants.Urls;
import com.jmw.sda.Utils.Configuration;
import com.jmw.sda.crypto.AbstractCrypto;
import com.jmw.sda.crypto.FailedCryptException;
import com.jmw.sda.crypto.IPrivateKey;
import com.jmw.sda.crypto.JavaInstallationMissingComponentsException;
import com.jmw.sda.crypto.VersionString;

@WebServlet(Urls.id)
public class IdentificationServlet extends HttpServlet {
	private static final long serialVersionUID = 1515717177128054080L;
	private String id;

    public IdentificationServlet() {
        super();
		try {
			Configuration config = new Configuration(new File("."));
	    	IPrivateKey privateKey = AbstractCrypto.getCrypto().stringToPrivateKey(config.getPrivateKey());
	    	VersionString vs = new VersionString(privateKey);
	    	this.id = vs.getVersion();
		} catch (IOException | JavaInstallationMissingComponentsException | FailedCryptException e) {
			this.id = "version generation failed";
		}
    }

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType("text/plain");
		try(PrintWriter writer = response.getWriter();){
			writer.write(this.id);
		}
	}
}
