package com.haxelion.iccoma;

import java.util.Timer;
import java.util.TimerTask;

import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView.OnEditorActionListener;
import android.widget.TextView;

public class MainActivity extends Activity implements OnSeekBarChangeListener, OnClickListener, OnEditorActionListener {
	
	private SeekBar cups_slider;
	private TextView cups_text, status_text;
	private Button order_button;
	private EditText address_field, password_field;
	private ICCOMAClient client;
	private Timer statusDaemonTimer;
	private int status = -1;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		client = new ICCOMAClient("", "");
		cups_slider = (SeekBar)findViewById(R.id.cups_slider);
		cups_text = (TextView)findViewById(R.id.cups_text);
		status_text = (TextView)findViewById(R.id.status_text);
		order_button = (Button)findViewById(R.id.order_button);
		address_field = (EditText)findViewById(R.id.address_field);
		password_field = (EditText)findViewById(R.id.password_field);
		cups_slider.setOnSeekBarChangeListener(this);
		order_button.setOnClickListener(this);
		address_field.setOnEditorActionListener(this);
		password_field.setOnEditorActionListener(this);
		statusDaemonTimer = new Timer();
		statusDaemonTimer.schedule(statusDaemon, 0,500);
		client.start();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		cups_text.setText(Integer.toString(progress+1));
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onClick(View v) {
		client.order(cups_slider.getProgress()+1);
	}

	@Override
	public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		if(actionId == EditorInfo.IME_ACTION_DONE) {
			if(v.getId() == password_field.getId()) {
				client.changeKey(v.getText().toString());
				return true;
			}
			else if(v.getId() == address_field.getId()) {
				client.changeAddress(v.getText().toString());
				return true;
			}
		}
		return false;
	}
	
	private TimerTask statusDaemon = new TimerTask() {
		
		@Override
		public void run() {
			if(status != client.getStatus()) {
				status = client.getStatus();
				statusHandler.obtainMessage(1).sendToTarget();
			} 
			
		}
	};
	
	private Handler statusHandler = new Handler() {
	    public void handleMessage(Message msg) {
	    	if(status == -1) {
				status_text.setText("Offline");
				status_text.setTextColor(getResources().getColor(R.color.red));
			}
	    	else if(status == 0) {
				status_text.setText("Standby");
				status_text.setTextColor(getResources().getColor(R.color.blue));
			}
	    	else if(status == 1) {
				status_text.setText("Brewing");
				status_text.setTextColor(getResources().getColor(R.color.orange));
			}
	    	else if(status == 2) {
				status_text.setText("Ready");
				status_text.setTextColor(getResources().getColor(R.color.green));
			}
	    	else if(status == 3) {
				status_text.setText("Error");
				status_text.setTextColor(getResources().getColor(R.color.red));
			}
	    }
	};
}
