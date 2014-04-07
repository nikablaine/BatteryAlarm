package de.kraflapps.apps.batteryalarm;

import java.util.ArrayList;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.MultiAutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;

public class MainActivity extends Activity implements AsyncResult {

	public final static int MAX_PERCENTAGE_VALUE = 100;
	public final static int MIN_PERCENTAGE_VALUE = 0;
	public final static int CUR_PERCENTAGE_VALUE = 30;
	
	public final static int REQUEST_AUTHORIZATION = 11;
	public final static int REQUEST_PERCENTAGE_CHANGE = 22;
	
	public final static String CONTACT_LIST = "de.kraflapps.apps.batteryalarm.contact_list";
	
	public final static String TAG = "Battery-Alarm-App";
	
	private static final String DIALOG_NUMBER = "number";
	
	public Context mContext = this;
	
	private SharedPreferences mPreferences;
	
	ContactListLoader contactLoaderTask = new ContactListLoader(this);
	
	
	ProgressBar prgrBar = null;
	EditText fieldPercentageAlarm;
	MultiAutoCompleteTextView fieldSendTo;
	

	String authToken;
	Spinner spnrAccnt = null;

	String textSender = null;
	String textRecipient = null;
	String textPercentageAlarm = null;

	Switch swServiceState;
	
	TextView tvPercentage = null;
	TextView tvSetPercentage = null;
	TextView tvCustomMessages = null;
	private TextView tvConnected;
	
	View batteryLevelView;
	
	public int alarmBatteryLevel;
	
	ArrayList<CustomContact> contactList;
	ArrayAdapter<CustomContact> contactListAdapter;

	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		mPreferences = PreferenceManager
				.getDefaultSharedPreferences(mContext);

		swServiceState = (Switch) findViewById(R.id.swServiceState);
		fieldSendTo = (MultiAutoCompleteTextView) findViewById(R.id.fieldSendTo);
		batteryLevelView = (View) findViewById(R.id.linLayoutBatteryLevel);
		tvSetPercentage = (TextView) findViewById(R.id.tvSetPercentage);
		tvCustomMessages = (TextView) findViewById(R.id.tvCustomMessages);
		tvConnected = (TextView) findViewById(R.id.tvConnected);
		
		

		spnrAccnt = (Spinner) findViewById(R.id.spinnerAccounts);
		spnrAccnt.setOnItemSelectedListener(onAccountChangeListener);
		
		prgrBar = (ProgressBar) findViewById(R.id.progressBar);
		prgrBar.setVisibility(View.GONE);
		
		setAlarmBatteryLevel(mPreferences.getInt(Alarm.ALARM_VALUE, CUR_PERCENTAGE_VALUE));
		
		setAuthToken(mPreferences.getString(Alarm.AUTH_TOKEN, null));
		
		setTextRecipient(mPreferences.getString(Alarm.RECIPIENT, ""));
		
		setTextSender(mPreferences.getString(Alarm.SENDER_ACCOUNT, null));
		
		setTVBatteryLevel();
		
		setTVConnectAccount();
		
		setSPNRAccnt();
		
		setFieldSendTo();
		
		setSWServiceState();
		
		if (mPreferences.getBoolean(Alarm.SERVICE_ON, false)) {
			fieldSendTo.setText(getTextRecipient());
			fieldSendTo.setEnabled(false);
			Intent intent = new Intent(mContext, AlarmService.class);
			startService(intent);
		} else {
			if (savedInstanceState == null){
				//make field disabled until contact list is loaded
				fieldSendTo.setEnabled(false);
				//load contact list
				executeContactListLoaderTask();
			} else {
				contactList = (ArrayList<CustomContact>) savedInstanceState.getSerializable(CONTACT_LIST);
				if (contactList == null){
					executeContactListLoaderTask();
				} else {
					setContactListAdapter();
				}
			}
		}
		
		batteryLevelView.setOnClickListener(btrLvlPickerListener);
		
		
		
		//setTglBtnServiceSetCheck(tglBtnStartService);

		swServiceState.setOnCheckedChangeListener(swServiceStateListener);

	}


	@Override
	protected void onPause() {
		super.onPause();
		putPrefs();
	}
	
	@Override
	protected void onSaveInstanceState (Bundle outState) {
		super.onSaveInstanceState(outState);
		
		//persistent information
		putPrefs();
		
		//non-persistent information
		if (contactList != null) {
			outState.putSerializable(CONTACT_LIST, contactList);
		}
	}
	
	


	private boolean isAlarmServiceRunning() {
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager
				.getRunningServices(Integer.MAX_VALUE)) {
			if (AlarmService.class.getName().equals(
					service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	private String[] getAccountNames() {
		final AccountManager mAccountManager = AccountManager.get(this);
		final Account[] accounts = mAccountManager
				.getAccountsByType(GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE);
		String[] names = new String[accounts.length];
		for (int i = 0; i < names.length; i++) {
			names[i] = accounts[i].name;
		}
		return names;
	}

	/*@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}*/

	

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == REQUEST_AUTHORIZATION) {
			if (resultCode == Activity.RESULT_OK) {
				data.getExtras();
				GetTokenTask getTokenTask = new GetTokenTask(this);
				getTokenTask.setDelegate(this);
				getTokenTask.execute();
			}
		}
		
		if (requestCode == REQUEST_PERCENTAGE_CHANGE) {
			Bundle extras = data.getExtras();
			setAlarmBatteryLevel(extras.getInt(NumberPickerFragment.CUR_NUMBER));
			setTVBatteryLevel();
		}
	}

	private OnCheckedChangeListener swServiceStateListener = new OnCheckedChangeListener() {

		@Override
		public void onCheckedChanged(CompoundButton buttonView,
				boolean isChecked) {
			
			SharedPreferences preferences = PreferenceManager
					.getDefaultSharedPreferences(mContext);
			
			if (isChecked) {
				//check if token is activated
				if (getAuthToken() == null){
					Toast.makeText(mContext, getResources().getString(R.string.string_connect_your_account), Toast.LENGTH_SHORT).show();
					buttonView.setChecked(false);
					return;
				}
				
				//check if there is at least one recipient
				if (getTextRecipient() == null){
					Toast.makeText(mContext, getResources().getString(R.string.string_select_recipient), Toast.LENGTH_SHORT).show();
					buttonView.setChecked(false);
					return;
				}
				
				//if everything is initialized, put shared preferences and start the service
				
				fieldSendTo.setEnabled(false);
				spnrAccnt.setEnabled(false);
				setTextRecipient(fieldSendTo.getText().toString());
				
				
				SharedPreferences.Editor editor = preferences.edit();
				editor.putString(Alarm.RECIPIENT, getTextRecipient());
				editor.putString(Alarm.SENDER_ACCOUNT, getTextSender());
				editor.putInt(Alarm.ALARM_VALUE, getAlarmBatteryLevel());
				editor.putString(Alarm.AUTH_TOKEN, getAuthToken());
				editor.putBoolean(Alarm.WORKING_FLAG, true);
				editor.commit();
				
				Intent intent = new Intent(getApplicationContext(),	AlarmService.class);
				startService(intent);
				Toast.makeText(mContext, "Service started", Toast.LENGTH_SHORT).show();
			} else {
				
				//check if service was started
				boolean isOn = preferences.getBoolean(Alarm.SERVICE_ON, false);
				
				if (isOn) {
					Intent intent = new Intent(getApplicationContext(),
							AlarmService.class);
					startService(intent);
					stopService(intent);
					fieldSendTo.setEnabled(true);
					spnrAccnt.setEnabled(true);
				}
				
				SharedPreferences.Editor editor = mPreferences.edit();
				editor.putBoolean(Alarm.WORKING_FLAG, true);
				editor.commit();
				
				if (contactList == null) {
					executeContactListLoaderTask();
				}
				
			}

		}
	};

	private OnClickListener cntPickerListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			Intent intent = new Intent(Intent.ACTION_PICK,
					ContactsContract.Contacts.CONTENT_URI);
			startActivityForResult(intent, 100);
		}
	};
	
	private OnClickListener onConnectListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			setTextSender(spnrAccnt.getSelectedItem().toString());
			GetTokenTask getTokenTask = new GetTokenTask(mContext);
			getTokenTask.setDelegate(MainActivity.this);
			getTokenTask.execute(getTextSender());
			
		}
	};
	
	private OnItemSelectedListener onAccountChangeListener = new OnItemSelectedListener() {

		@Override
		public void onItemSelected(AdapterView<?> parent, View view,
				int position, long id) {
			
			if (!spnrAccnt.getSelectedItem().toString().equals(textSender)) {
				setTextSender(spnrAccnt.getSelectedItem().toString());
				GoogleAuthUtil.invalidateToken(mContext, authToken);
				setAuthToken(null);
				putPrefs();
				setTVConnectAccount();
			}
			
		}

		@Override
		public void onNothingSelected(AdapterView<?> parent) {
			// TODO Auto-generated method stub
			
		}
	};
	
	private void setSPNRAccnt() {
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(
				mContext,
				R.layout.spinner, R.id.accountName,
				getAccountNames());

		int spinnerPosition = adapter.getPosition(getTextSender());
		
		spnrAccnt.setAdapter(adapter);
		spnrAccnt.setSelection(spinnerPosition);
		
		//TODO If account exists no more
		
		
	}
	
	private void setFieldSendTo() {
		fieldSendTo.setText(getTextRecipient());
	}
	
	private void setTVBatteryLevel() {
		tvSetPercentage.setText("Currently is set to " + getAlarmBatteryLevel() + "%");
	}
	
	private void setTVConnectAccount() {
		
		if (getAuthToken() == null) {
			//initialize "connect button"
			tvConnected.setText(R.string.string_connect_underlined);
			tvConnected.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
			tvConnected.setOnClickListener(onConnectListener);
		} else {
			tvConnected.setText(R.string.string_connected);
			tvConnected.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
		}
		
	}
	
	private void setSWServiceState() {
		
		boolean isOn = mPreferences.getBoolean(Alarm.SERVICE_ON, false);
		swServiceState.setChecked(isOn);
	
	}
	
	private void setContactListAdapter() {
		contactListAdapter = new CustomAdapter(this, contactList);
		fieldSendTo.setEnabled(true);
		fieldSendTo.setAdapter(contactListAdapter);	
		fieldSendTo.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
	}

	
	private OnClickListener btrLvlPickerListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			
			FragmentManager fm = getFragmentManager();
            NumberPickerFragment dialog = NumberPickerFragment
            		.newInstance(getAlarmBatteryLevel(), MAX_PERCENTAGE_VALUE, MIN_PERCENTAGE_VALUE);
            dialog.show(fm, DIALOG_NUMBER);
			
			
			
			
			
			/*RelativeLayout linearLayout = new RelativeLayout(mContext);
			final NumberPicker numPicker = new NumberPicker(getApplicationContext());
			numPicker.setMaxValue(MAX_PERCENTAGE_VALUE);
			numPicker.setMinValue(MIN_PERCENTAGE_VALUE);
			numPicker.setValue(CUR_PERCENTAGE_VALUE);
			
	        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(50, 50);
	        RelativeLayout.LayoutParams numPicerParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
	        numPicerParams.addRule(RelativeLayout.CENTER_HORIZONTAL);

	        linearLayout.setLayoutParams(params);
	        linearLayout.addView(numPicker, numPicerParams);

	        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
	        alertDialogBuilder.setTitle("Set percentage");
	        alertDialogBuilder.setView(linearLayout);
	        alertDialogBuilder
	                .setCancelable(false)
	                .setPositiveButton("Ok",
	                        new DialogInterface.OnClickListener() {
	                            public void onClick(DialogInterface dialog,
	                                                int id) {
	                                Log.i("","New Quantity Value : "+ numPicker.getValue());
	                                alarmBatteryLevel = numPicker.getValue();
	                                //tvPercentage.setText("Currently is set to " + alarmBatteryLevel + "%");

	                            }
	                        })
	                .setNegativeButton("Cancel",
	                        new DialogInterface.OnClickListener() {
	                            public void onClick(DialogInterface dialog,
	                                                int id) {
	                                dialog.cancel();
	                            }
	                        });
	        AlertDialog alertDialog = alertDialogBuilder.create();
	        alertDialog.show();		*/	
		}
	};
	
	private void executeContactListLoaderTask() {
		prgrBar.setVisibility(View.VISIBLE);
		tvCustomMessages.setText("Loading contact list...");
		contactLoaderTask.delegate = this;
		contactLoaderTask.execute();
	}
	
	private void putPrefs() {
		SharedPreferences.Editor editor = mPreferences.edit();
		editor.putString(Alarm.AUTH_TOKEN, getAuthToken());
		editor.putString(Alarm.SENDER_ACCOUNT, getTextSender());
		editor.putString(Alarm.RECIPIENT, getTextRecipient());
		editor.commit();
	}





	@Override
	public void asyncTaskContactLoaderFinished(ArrayList<CustomContact> cntList) {
		//contact list stopped loading
		
		contactList = cntList;
		prgrBar.setVisibility(View.INVISIBLE);
		tvCustomMessages.setText("");
		
		CustomContact[] contactsArray = new CustomContact[contactList.size()];
		contactsArray = contactList.toArray(contactsArray);
		
		setContactListAdapter();
	}

	@Override
	public void asyncTaskGetTokenStarted() {
		prgrBar.setVisibility(View.VISIBLE);
		tvCustomMessages.setText("Authorizating account...");
	}

	@Override
	public void asyncTaskGetTokenFinished(String result, String text) {
		prgrBar.setVisibility(View.INVISIBLE);
		tvCustomMessages.setVisibility(View.INVISIBLE);
		setAuthToken(result);
		
		setTVConnectAccount();

		if (text != null) {
			Toast.makeText(getApplicationContext(), text,
					Toast.LENGTH_SHORT).show();
		}

		/*SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());
		SharedPreferences.Editor editor = preferences.edit();
		editor.putString("Recepient", textRecepient);
		editor.putString("Sender", textSender);
		editor.putInt("AlarmValue", alarmBatteryLevel);
		editor.putString("AuthToken", authToken);
		editor.putBoolean("serviceShouldWork", true);
		editor.commit();*/
		
	}

	@Override
	public void asyncTaskGetTokenAuth(UserRecoverableAuthException exc) {
		startActivityForResult(exc.getIntent(), REQUEST_AUTHORIZATION);
	}
	
	@Override
	public void numPickerFragmentAlarmValueChosen(int value) {
		SharedPreferences.Editor editor = mPreferences.edit();
		editor.putInt(Alarm.ALARM_VALUE, value);
		editor.commit();
		setAlarmBatteryLevel(value);
		setTVBatteryLevel();
	}

	public int getAlarmBatteryLevel() {
		return alarmBatteryLevel;
	}

	public void setAlarmBatteryLevel(int alarmBatteryLevel) {
		this.alarmBatteryLevel = alarmBatteryLevel;
	}

	public String getTextSender() {
		return textSender;
	}

	public void setTextSender(String textSender) {
		this.textSender = textSender;
	}

	public String getTextRecipient() {
		return textRecipient;
	}

	public void setTextRecipient(String textRecipient) {
		this.textRecipient = textRecipient;
	}

	public String getAuthToken() {
		return authToken;
	}

	public void setAuthToken(String authToken) {
		this.authToken = authToken;
	}
	
	
	
	

}
