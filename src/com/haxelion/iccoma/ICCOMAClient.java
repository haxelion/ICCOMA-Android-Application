package com.haxelion.iccoma;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.security.MessageDigest;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;

import android.net.http.AndroidHttpClient;
import android.util.Log;

public class ICCOMAClient extends Thread {

	private AndroidHttpClient client;
	private int status;
	private String address, key;
	private static char[] MAP = {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};
	private int ordered_cups;
	private int order_passed = 0;
	
	public ICCOMAClient(String address, String key) {
		client = AndroidHttpClient.newInstance("ICCOMA Android Client 1.0");
		this.address = address;
		this.key = key;
		status = -1; //offline
		ordered_cups = -1;
	}
	
	public int getStatus() {
		return status;
	}
	
	public int getOrderStatus() {
		return order_passed;
	}
	
	public void changeAddress(String address) {
		this.address = address;
	}
	
	public void changeKey(String key) {
		this.key = key;
	}
	
	public void order(int cups) {
		ordered_cups = cups;
		order_passed = 0;
	}
	
	private void updateStatus() {
		try {
			HttpResponse response = client.execute(new HttpGet("http://" + address + "/status"));
			if(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
	            response.getEntity().writeTo(out);
	            out.close();
	            String s = out.toString();
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
				Log.e("ICCOMAClient", "Http error: " + response.getStatusLine().getStatusCode());
			}
		}
		catch (Exception e) {
			Log.e("ICCOMAClient", "Error: " + e.getMessage());
			status = -1;
		}
	}
	
	private void orderBrewing() {
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
			HttpResponse response = client.execute(new HttpGet("http://" + address + "/brew?cups=" + Integer.toString(ordered_cups)));
			if(response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				Log.e("ICCOMAClient", "Http error: " + Integer.toString(response.getStatusLine().getStatusCode()));
				return;
			}
            response.getEntity().writeTo(out);
            out.close();
            String nonce = out.toString();
            out.reset();
            sha256.update(key.getBytes("US-ASCII"));
            sha256.update(nonce.getBytes("US-ASCII"));
            sha256.update("cups=".getBytes("US-ASCII"));
            byte[] hash = sha256.digest(Integer.toString(ordered_cups).getBytes("US-ASCII"));
            char hmac[] = new char[64];
            for(int i = 0; i<hash.length; i++) {
            	hmac[2*i] = MAP[(hash[i]>>4)&0xF];
            	hmac[2*i+1] = MAP[hash[i]&0xF];
            }
            response = client.execute(new HttpGet("http://" + address + "/validate?hmac=" + new String(hmac)));
            if(response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				Log.e("ICCOMAClient", "Http error: " + Integer.toString(response.getStatusLine().getStatusCode()));
				return;
			}
            response.getEntity().writeTo(out);
            out.close();
            String s = out.toString();
            if(s.contains("ok")) 
            	order_passed = 1; 
            else {
            	Log.e("ICCOMAClient", "Order didn't pass");
            	order_passed = -1;
            }
            	
            ordered_cups = -1;
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
				if(ordered_cups != -1)
					orderBrewing();
			}
			try {
				for(int i = 0; i<50 && ordered_cups == -1; i++)
					sleep(100);
			}
			catch(Exception e) {
				
			}
				
		}
	}

}
