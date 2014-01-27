package com.haxelion.iccoma;

import java.util.Timer;
import java.util.TimerTask;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.app.NotificationCompat;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class MainActivity extends Activity implements OnSeekBarChangeListener, OnClickListener {
	
	private SeekBar cups_slider;
	private TextView cups_text, status_text;
	private Button order_button, configure_button, reset_button, open_button, close_button;
	private ICCOMAClient client;
	private Timer statusDaemonTimer;
	private int status = -1;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		SharedPreferences settings = getSharedPreferences("settings", 0);
		client = new ICCOMAClient(settings.getString("address", ""), settings.getString("password", ""));
		cups_slider = (SeekBar)findViewById(R.id.cups_slider);
		cups_text = (TextView)findViewById(R.id.cups_text);
		status_text = (TextView)findViewById(R.id.status_text);
		order_button = (Button)findViewById(R.id.order_button);
		reset_button = (Button)findViewById(R.id.reset_button);
		open_button = (Button)findViewById(R.id.open_button);
		close_button = (Button)findViewById(R.id.close_button);
		configure_button = (Button)findViewById(R.id.configure_button);
		cups_slider.setOnSeekBarChangeListener(this);
		order_button.setOnClickListener(this);
		configure_button.setOnClickListener(this);
		reset_button.setOnClickListener(this);
		open_button.setOnClickListener(this);
		close_button.setOnClickListener(this);
		statusDaemonTimer = new Timer(true);
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
		if(v.getId() == order_button.getId())
			client.orderCoffee(cups_slider.getProgress()+1);
		else if(v.getId() == configure_button.getId()) {
			Intent intent = new Intent(this, SettingsActivity.class);
			startActivityForResult(intent, 0);
		}
		else if(v.getId() == open_button.getId()) {
			client.sendCommand(ICCOMAClient.CMD_OPEN_TRAY);
		}
		else if(v.getId() == close_button.getId()) {
			client.sendCommand(ICCOMAClient.CMD_CLOSE_TRAY);
		}
		else if(v.getId() == reset_button.getId()) {
			client.sendCommand(ICCOMAClient.CMD_RESET);
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(resultCode == Activity.RESULT_OK) {
			client.changeAddress(data.getStringExtra("address"));
			client.changePassword(data.getStringExtra("password"));
		}
	}
	
	private void showAlert(String msg) {
		new AlertDialog.Builder(this).setMessage(msg).setNeutralButton("OK", null).show();
	}

	private TimerTask statusDaemon = new TimerTask() {
		
		@Override
		public void run() {
			if(status != client.getStatus()) {
				status = client.getStatus();
				statusHandler.obtainMessage(1).sendToTarget();
				if(status == 2) {
					NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext());
					builder.setSmallIcon(R.drawable.ic_launcher);
					builder.setContentTitle("Coffee is ready!");
					builder.setDefaults(Notification.DEFAULT_ALL);
					builder.setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0, new Intent(getApplicationContext(), MainActivity.class), 0));
					((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(0, builder.build());
				}
			} 
			if(client.getOrderStatus() != 0)
				alertHandler.obtainMessage(1).sendToTarget();
			
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
				status_text.setText("Executing");
				status_text.setTextColor(getResources().getColor(R.color.orange));
			}
	    	else if(status == 3) {
				status_text.setText("Ready");
				status_text.setTextColor(getResources().getColor(R.color.green));
			}
	    }
	};
	
	private Handler alertHandler = new Handler() {
		public void handleMessage(Message msg) {
			if(client.getOrderStatus() == 1)
				showAlert("Request succesful.");
			else if(client.getOrderStatus() == -1)
				showAlert("Request failed.");
			client.acknowledgeOrderStatus();
		}
	};
}
