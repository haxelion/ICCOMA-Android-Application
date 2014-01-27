package com.haxelion.iccoma;

import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;

import android.util.Log;

public class ICCOMAClient extends Thread {

	private int status;
	private String address, password;
	private static char[] MAP = {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};
	private int order;
	private int order_passed = 0;
	
	public ICCOMAClient(String address, String password) {
		this.address = address;
		this.password = password;
		status = -1; // offline
		order = -1;
	}
	
	public int getStatus() {
		return status;
	}
	
	public int getOrderStatus() {
		return order_passed;
	}
	
	public void acknowledgeOrderStatus() {
		order_passed = 0;
	}
	
	public void changeAddress(String address) {
		this.address = address;
	}
	
	public void changePassword(String password) {
		this.password = password;
	}
	
	public void orderCoffee(int cups) {
		order = cups;
		order_passed = 0;
	}
	
	public void orderReset() {
		order = -2;
		order_passed = 0;
	}
	
	private void resetICCOMA() {
		try {
			URL url = new URL("http://" + address + "/reset");
			HttpURLConnection request = (HttpURLConnection) url.openConnection();
			if(request.getResponseCode() == HttpURLConnection.HTTP_OK) {
				byte response[] = new byte[128];
				request.getInputStream().read(response);
				String s = new String(response);
				request.disconnect();
	            if(s.contains("ok"))
	            	order_passed = 1;
	            else {
	            	Log.e("ICCOMAClient", "Unrecognized status: " + s);
	            	order_passed = -1;
	            }
	            order = -1;
			}
			else
			{
				status = -1;
				Log.e("ICCOMAClient", "Http error: " + Integer.toString(request.getResponseCode()));
			}
		}
		catch (Exception e) {
			Log.e("ICCOMAClient", "Error: " + e.getMessage());
			status = -1;
		}
	}
	
	private void updateStatus() {
		try {
			URL url = new URL("http://" + address + "/status");
			HttpURLConnection request = (HttpURLConnection) url.openConnection();
			if(request.getResponseCode() == HttpURLConnection.HTTP_OK) {
				byte response[] = new byte[128];
				request.getInputStream().read(response);
				String s = new String(response);
				request.disconnect();
	            if(s.contains("standby"))
	            	status = 0;
	            else if (s.contains("brewing"))
	            	status = 1;
	            else if(s.contains("ready"))
	            	status = 2;
	            else if (s.contains("error"))
	            	status = 3;
	            else 
	            	Log.e("ICCOMAClient", "Unrecognized status: " + s);
			}
			else
			{
				status = -1;
				Log.e("ICCOMAClient", "Http error: " + Integer.toString(request.getResponseCode()));
			}
		}
		catch (Exception e) {
			Log.e("ICCOMAClient", "Error: " + e.getMessage());
			status = -1;
		}
	}
	
	private void orderBrewing() {
		try {
			byte response[] = new byte[128];
			MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
			URL url = new URL("http://" + address + "/brew?cups=" + Integer.toString(order));
			HttpURLConnection request = (HttpURLConnection) url.openConnection();
			if(request.getResponseCode() != HttpURLConnection.HTTP_OK) {
				Log.e("ICCOMAClient", "Http error: " + Integer.toString(request.getResponseCode()));
				return;
			}
			request.getInputStream().read(response);
			String nonce = new String(response, 0, 8);
			request.disconnect();
            sha256.update(password.getBytes("US-ASCII"));
            sha256.update(nonce.getBytes("US-ASCII"));
            sha256.update("cups=".getBytes("US-ASCII"));
            byte[] hash = sha256.digest(Integer.toString(order).getBytes("US-ASCII"));
            char hmac[] = new char[64];
            for(int i = 0; i<hash.length; i++) {
            	hmac[2*i] = MAP[(hash[i]>>4)&0xF];
            	hmac[2*i+1] = MAP[hash[i]&0xF];
            }
            url = new URL("http://" + address + "/validate?hmac=" + new String(hmac));
            request = (HttpURLConnection) url.openConnection();
            if(request.getResponseCode() != HttpURLConnection.HTTP_OK) {
				Log.e("ICCOMAClient", "Http error: " + Integer.toString(request.getResponseCode()));
				return;
			}
            request.getInputStream().read(response);
			String s = new String(response);
			request.disconnect();
            if(s.contains("ok")) 
            	order_passed = 1; 
            else {
            	Log.e("ICCOMAClient", "Order didn't pass");
            	order_passed = -1;
            }
            	
            order = -1;
		}
		catch (Exception e) {
			Log.e("ICCOMAClient", e.getMessage());
			e.printStackTrace();
		}
	}
	
	@Override
	public void run() {
		while(true) {
			if(address != "") {
				updateStatus();
				if(order == -2)
					resetICCOMA();
				else if(order != -1)
					orderBrewing();
				
			}
			try {
				for(int i = 0; i<10 && order == -1; i++)
					sleep(100);
			}
			catch(Exception e) {
				
			}
				
		}
	}

}
