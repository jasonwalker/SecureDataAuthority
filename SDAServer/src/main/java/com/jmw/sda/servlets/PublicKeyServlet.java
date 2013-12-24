package com.jmw.sda.servlets;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.jmw.sda.Utils.Configuration;

/**
 * Servlet implementation class PublicKeyServlet
 */
@WebServlet("/publickey")
public class PublicKeyServlet extends HttpServlet {
	private static final long serialVersionUID = -3750914881430354266L;
	private static String publicKey;
    public PublicKeyServlet() {
        super();

    }
    @Override
    public void init(){
    	try {
			Configuration config = new Configuration(new File("."));
			publicKey = config.getPublicKey();   
		} catch (IOException e) {
			e.printStackTrace();
		}
    }

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType("text/plain");
		try(PrintWriter writer = response.getWriter();){
			writer.write(publicKey);
		}
	}

}
