package com.jmw.sda.servlets;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.jmw.sda.Constants.Urls;
import com.jmw.sda.crypto.Utils;

@WebServlet(Urls.client)
public class DownloadClientServlet extends HttpServlet {  
	private static final long serialVersionUID = -7398374848055181483L;
	
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)  throws ServletException, IOException {  
		try(OutputStream os = response.getOutputStream();
		    InputStream is = Utils.inputStreamForFile(new File(getServletContext().getRealPath("WEB-INF/static/SDAClient.jar")));){
			response.setContentType("APPLICATION/OCTET-STREAM");
			response.setHeader("Content-Disposition","attachment; filename=\"SDAClient.jar\"");
			Utils.writeFromInputToOutputStream(is, os);
		}
	}

}  