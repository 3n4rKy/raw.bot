package com.piclienta.main;

import java.net.InetAddress;
import java.util.Timer;
import java.util.TimerTask;

import com.piclienta.R;
import com.piclienta.audio.Recorder;
import com.piclienta.settings.GetIpWorker;
import com.piclienta.settings.ReceiveIp;
import com.piclienta.settings.Settings;
import com.util.PacketSender;

import android.app.Activity;
import android.app.Dialog;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnCheckedChangeListener {

	private static final int REQUEST_CODE = 10;
	ImageButton btnForward;
	ImageButton btnBackward;
	ImageButton btnLeft;
	ImageButton btnRight;
	ImageButton btnForwardRight;
	ImageButton btnForwardLeft;
	ImageButton btnBackwardRight;
	ImageButton btnBackwardLeft;
	ImageButton btnStrafeLeft;
	ImageButton btnStrafeRight;
	TextView textDrive;
	TextView powerLevelText;
	SeekBar powerLevel;
	private Switch btnSwitch;
	PacketSender ps = new PacketSender();
	final Context context = this;
	public String ip = "192.168.1.201";
	private InetAddress ipAddress = null;
	public boolean send;
	public Timer timer;
	Thread receiveIp = null;
	int progress = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.activity_main);

		btnForward = (ImageButton) findViewById(R.id.fwd);
		btnForwardRight = (ImageButton) findViewById(R.id.fwdrgt);
		btnForwardLeft = (ImageButton) findViewById(R.id.fwdlft);
		btnBackward = (ImageButton) findViewById(R.id.bwd);
		btnBackwardRight = (ImageButton) findViewById(R.id.bwdrgt);
		btnBackwardLeft = (ImageButton) findViewById(R.id.bwdlft);
		btnStrafeLeft = (ImageButton) findViewById(R.id.strafelft);
		btnStrafeRight = (ImageButton) findViewById(R.id.strafergt);

		btnLeft = (ImageButton) findViewById(R.id.lft);
		btnRight = (ImageButton) findViewById(R.id.rgt);
		textDrive = (TextView) findViewById(R.id.textView1);
		btnSwitch = (Switch) findViewById(R.id.switch1);
		btnSwitch.setOnCheckedChangeListener(this);

		powerLevel = (SeekBar) findViewById(R.id.powerLevel);
		powerLevelText = (TextView) findViewById(R.id.powerLevelText);
		powerLevel.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			

			@Override
			public void onProgressChanged(SeekBar seekBar, int progresValue, boolean fromUser) {
				progress = progresValue;
				powerLevelText.setText("Engine Power Level: " + progresValue + " %");
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				powerLevelText.setText("Engine Power Level: " + progress + " %");
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				powerLevelText.setText("Engine Power Level: " + progress + " %");
			}
		});

		getIp();
		startTimer();

		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction().add(R.id.container, new PlaceholderFragment()).commit();
		}
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

		if (isChecked) {
			send = true;
			Toast.makeText(this, "Transmission enabled", Toast.LENGTH_LONG).show();
		} else if (!isChecked) {
			send = false;
			Toast.makeText(this, "Transmission disabled", Toast.LENGTH_LONG).show();
		}

	}

	public void startTimer() {
		final Handler handler = new Handler();
		Timer timer = new Timer();
		TimerTask task = new TimerTask() {

			@Override
			public void run() {
				handler.post(new Runnable() {
					public void run() {
						MyAsyncWorker maw = new MyAsyncWorker(MainActivity.this);
						maw.execute("0");
					}
				});
			}
		};
		timer.schedule(task, 0, 100);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.options, menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		if (item.getItemId() == R.id.about) {

			Toast.makeText(this, "\u00a9 Benjamin Biedermann", Toast.LENGTH_LONG).show();
			return true;
		} else if (item.getItemId() == R.id.setIp) {
			Bundle myBundle = new Bundle();
			myBundle.putString("ip", ip);
			Intent in = new Intent(this, Settings.class);
			in.putExtras(myBundle);
			startActivityForResult(in, REQUEST_CODE);

			final Dialog dialog = new Dialog(context);
			dialog.setContentView(R.layout.activity_settings);

			return true;
		} else if (item.getItemId() == R.id.recorder) {
			Intent in = new Intent(this, Recorder.class);
			Bundle myBundle = new Bundle();
			myBundle.putString("ip", ip);
			in.putExtra("ip", ip);
			startActivityForResult(in, REQUEST_CODE);
			final Dialog dialog = new Dialog(context);
			dialog.setContentView(R.layout.activity_recorder);
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK && requestCode == REQUEST_CODE) {
			if (data.hasExtra("ip")) {
				ip = data.getExtras().getString("ip");
				Toast.makeText(this, "IP " + ip + " saved", Toast.LENGTH_SHORT).show();
			}
		}
	}

	public InetAddress getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(InetAddress ipAddress) {
		this.ipAddress = ipAddress;
		ip = this.ipAddress.toString();
		showIp();
	}

	public void getIp() {
		timer = new Timer();
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				GetIpWorker giw = new GetIpWorker();
				giw.execute("0");
			}
		};
		timer.schedule(task, 0, 1000);

		this.receiveIp = new Thread(new ReceiveIp(MainActivity.this));
		this.receiveIp.start();
	}

	public void showIp() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(context, "Pr0boter Server found: " + ip, Toast.LENGTH_LONG).show();
			}
		});
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {
		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container, false);
			return rootView;
		}
	}
}
