package com.pcl.util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SHAUtil {

	public static String getEncriptStr(String password) {
		MessageDigest sha;
		try {
			sha = MessageDigest.getInstance("SHA-256");
			sha.update(password.getBytes());  
			byte[] pbytes = sha.digest();
			return new String(pbytes,"UTF-8");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}  
		return password;
	}
	
	public static void main(String[] args) {
		
		System.out.println( getEncriptStr("pcl123456"));
		
		
		
	}
	
}
