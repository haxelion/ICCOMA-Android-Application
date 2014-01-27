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

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class SettingsActivity extends Activity implements OnClickListener{
	
	private EditText address_field, password_field;
	private Button save_button, cancel_button;
	private SharedPreferences settings;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);
		settings = getSharedPreferences("settings", 0);
		address_field = (EditText)findViewById(R.id.address_field);
		password_field = (EditText)findViewById(R.id.password_field);
		save_button = (Button)findViewById(R.id.save_button);
		cancel_button = (Button)findViewById(R.id.cancel_button);
		address_field.setText(settings.getString("address", ""));
		password_field.setText(settings.getString("password", ""));
		save_button.setOnClickListener(this);
		cancel_button.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		if(v.getId() == save_button.getId()) {
			SharedPreferences.Editor edit = settings.edit();
			edit.putString("address", address_field.getText().toString());
			edit.putString("password", password_field.getText().toString());
			edit.commit();
			Intent result = new Intent();
			result.putExtra("address", address_field.getText().toString());
			result.putExtra("password", password_field.getText().toString());
			setResult(RESULT_OK, result);
			finish();
		}
		else if(v.getId() == cancel_button.getId()) {
			setResult(RESULT_CANCELED);
			finish();
		}
	}
}
