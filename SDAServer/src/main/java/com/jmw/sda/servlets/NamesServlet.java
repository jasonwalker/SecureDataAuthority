package com.jmw.sda.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.jmw.sda.Constants.Urls;
import com.jmw.sda.dbProviders.CurrentProvider;
import com.jmw.sda.dbProviders.DBException;
import com.jmw.sda.dbProviders.IDatabase;
@WebServlet(Urls.names)
public class NamesServlet extends HttpServlet {
	private static final long serialVersionUID = 11602024694464569L;
    public NamesServlet() {
        super();
    }

    private final static String join(String[] strings){
    	StringBuilder sb = new StringBuilder();
    	for (String s : strings){
    		sb.append(s);
    		sb.append(" ");
    	}
    	return sb.toString();
    }
    
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try(PrintWriter writer = response.getWriter();){
			response.setStatus(HttpServletResponse.SC_OK);
			response.setContentType("text/plain");
			IDatabase database = CurrentProvider.get();
			String[] users = database.listUsers();
			writer.write(join(users));
		}catch(DBException e){
			throw new ServletException(e);
		}
	}
}
