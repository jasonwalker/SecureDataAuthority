package com.jmw.sda.Utils;

import javax.servlet.http.HttpServletRequest;

public class ServletUtils {

	public static final boolean stringIsMalformed(String val){
		if (val.length() > 128){
			return true;
		}
		if (val.indexOf('\\') != -1){
			return true;
		}
		if (val.indexOf('/') != -1){
			return true;
		}
		return false;
	}
	public static final String[] getPathInfo(HttpServletRequest request){
		String path = request.getPathInfo();
		if (path == null){
			return null;
		}
		String[] vals = path.split("/");
		if (vals.length < 2){
			return null;
		}
		return vals;
	}
	
}
