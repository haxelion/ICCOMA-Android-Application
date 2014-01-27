/*
 * This file is part of ICCOMA android application.
 * 
 * ICCOMA android application is free software: you can redistribute it and/or modify            
 * it under the terms of the GNU General Public License as published by               
 * the Free Software Foundation, either version 3 of the License, or                  
 * (at your option) any later version.                                                
 *                                                                                    
 * ICCOMA android application is distributed in the hope that it will be useful,                 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                     
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the                       
 * GNU General Public License for more details.                                       
 *                                                                                    
 * You should have received a copy of the GNU General Public License                  
 * along with this program. If not, see <http://www.gnu.org/licenses/>.               
 *                                                                                    
 * Copyright 2014 Charles Hubain <haxelion@gmail.com> 
 */

package com.haxelion.iccoma;

import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;

import android.util.Log;

public class ICCOMAClient extends Thread {

	private int status;
	private String address, password;
	private static char[] MAP = {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};
	private int cups, cmd, order_passed;
	public static int CMD_OPEN_TRAY = 1;
	public static int CMD_CLOSE_TRAY = 2;
	public static int CMD_RESET = 3;
	
	public ICCOMAClient(String address, String password) {
		super();
		this.address = address;
		this.password = password;
		status = -1; // offline
		cups = 0;
		cmd = 0;
		order_passed = 0;
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
		this.cups = cups;
		order_passed = 0;
	}
	
	public void sendCommand(int cmd) {
		this.cmd = cmd;
		order_passed = 0;
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
	            else if(s.contains("executing"))
	            	status = 2;
	            else if (s.contains("ready"))
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
	
	private void authenticatedRequest(String ressource, String param) {
		try {
			byte response[] = new byte[128];
			MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
			URL url = new URL("http://" + address + ressource + "?" + param);
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
            byte[] hash = sha256.digest(param.getBytes("US-ASCII"));
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
				if(cmd != 0) {
					authenticatedRequest("/command", "cmd="+Integer.toString(cmd));
					cmd = 0;
				}
				if(cups != 0) {
					authenticatedRequest("/brew", "cups="+Integer.toString(cups));
					cups = 0;
				}
				
			}
			try {
				for(int i = 0; i<100 && cups == 0; i++)
					sleep(100);
			}
			catch(Exception e) {
				
			}
				
		}
	}

}
