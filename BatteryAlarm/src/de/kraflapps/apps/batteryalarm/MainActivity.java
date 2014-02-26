package de.kraflapps.apps.batteryalarm;

import java.io.IOException;
import java.util.ArrayList;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.sun.mail.imap.protocol.FLAGS;

import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.Contacts;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends Activity {

	ProgressBar prgrBar = null;
	EditText fieldPercentageAlarm;
	AutoCompleteTextView fieldSendTo;

	String authToken = "";
	Spinner spnrAccnt = null;

	String textSender = null;
	String textRecepient = null;
	String textPercentageAlarm = null;

	ToggleButton tglBtnStartService;
	
	ArrayList<CustomContact> contactList;
	ArrayAdapter<CustomContact> contactListAdapter;

	private int REQUEST_AUTHORIZATION = 11;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		tglBtnStartService = (ToggleButton) findViewById(R.id.toggleButtonStart);
		fieldPercentageAlarm = (EditText) findViewById(R.id.fieldPercentageAlarm);
		fieldSendTo = (AutoCompleteTextView) findViewById(R.id.fieldSendTo);

		spnrAccnt = (Spinner) findViewById(R.id.spinnerAccounts);

		prgrBar = (ProgressBar) findViewById(R.id.progressBar);
		prgrBar.setVisibility(View.GONE);
		
		contactList = new ArrayList<CustomContact>();

		ContentResolver cr = getContentResolver();

		Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI, null,
				null, null, null);
		if (cur.getCount() > 0) {
			while (cur.moveToNext()) {
				String id = cur.getString(cur
						.getColumnIndex(ContactsContract.Contacts._ID));
				String name = cur
						.getString(cur
								.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
				
				Cursor emailCur = cr.query(
						ContactsContract.CommonDataKinds.Email.CONTENT_URI, null,
						ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
						new String[] { id }, null);
				while (emailCur.moveToNext()) {
					// This would allow you get several email addresses
					// if the email addresses were stored in an array
					String email = emailCur
							.getString(emailCur
									.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA));
					int emailTypeNum = emailCur
							.getInt(emailCur
									.getColumnIndex(ContactsContract.CommonDataKinds.Email.TYPE));
					String emailType = getEmailType(emailTypeNum);

					
					CustomContact cstCnt = new CustomContact(name, email, emailType);
					contactList.add(cstCnt);
				}
				emailCur.close();
			}
			}
		

		
		CustomContact[] contactsArray = new CustomContact[contactList.size()];
		contactsArray = contactList.toArray(contactsArray);
		
		contactListAdapter = new CustomAdapter(this, contactList);

		fieldSendTo.setAdapter(contactListAdapter);

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(
				getApplicationContext(),
				android.R.layout.simple_spinner_dropdown_item,
				getAccountNames());

		spnrAccnt.setAdapter(adapter);
		
		setTglBtnServiceSetCheck(tglBtnStartService);

		tglBtnStartService.setOnCheckedChangeListener(tglBtnServiceListener);

	}

	@Override
	protected void onResume() {
		super.onResume();
		// setTglBtnServiceSetCheck(tglBtnStartService);
	}
	
	private String getEmailType(int tp) {
	
		switch (tp) {
		case ContactsContract.CommonDataKinds.Email.TYPE_HOME: return getString(R.string.mail_type_home);
		case ContactsContract.CommonDataKinds.Email.TYPE_MOBILE: return getString(R.string.mail_type_mobile);
		case ContactsContract.CommonDataKinds.Email.TYPE_OTHER: return getString(R.string.mail_type_other);
		case ContactsContract.CommonDataKinds.Email.TYPE_WORK: return getString(R.string.mail_type_work);
		}
		
		return "";
	}

	private void setTglBtnServiceSetCheck(ToggleButton tglBtn) {
		tglBtn.setChecked(isAlarmServiceRunning());
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
		AccountManager mAccountManager = AccountManager.get(this);
		Account[] accounts = mAccountManager
				.getAccountsByType(GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE);
		String[] names = new String[accounts.length];
		for (int i = 0; i < names.length; i++) {
			names[i] = accounts[i].name;
		}
		return names;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	private class GetTokenTask extends AsyncTask<String, Void, String> {

		String textInToast = null;
		String scope = "oauth2:https://mail.google.com";

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			prgrBar.setVisibility(View.VISIBLE);
		}

		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			prgrBar.setVisibility(View.GONE);
			authToken = result;

			if (textInToast != null) {
				Toast.makeText(getApplicationContext(), textInToast,
						Toast.LENGTH_SHORT).show();
			}

			SharedPreferences preferences = PreferenceManager
					.getDefaultSharedPreferences(getApplicationContext());
			SharedPreferences.Editor editor = preferences.edit();
			editor.putString("Recepient", textRecepient);
			editor.putString("Sender", textSender);
			editor.putString("AlarmValue", textPercentageAlarm);
			editor.putString("AuthToken", authToken);
			editor.putBoolean("serviceShouldWork", true);
			editor.commit();

			startService(new Intent(getApplicationContext(), AlarmService.class));

		}

		@Override
		protected String doInBackground(String... params) {
			String token = null;
			String newToken = null;

			try {
				token = GoogleAuthUtil.getToken(getApplicationContext(),
						params[0], scope);

				GoogleAuthUtil.invalidateToken(getApplicationContext(), token);
				textInToast = "Server auth error, please try again.";
				newToken = GoogleAuthUtil.getToken(getApplicationContext(),
						params[0], scope);
				return newToken;
			} catch (GooglePlayServicesAvailabilityException playEx) {
				playEx.getMessage();
				playEx.printStackTrace();
				return null;
			} catch (UserRecoverableAuthException userAuthEx) {
				// Start the user recoverable action using the intent returned
				// by
				// getIntent()
				userAuthEx.printStackTrace();
				startActivityForResult(userAuthEx.getIntent(),
						REQUEST_AUTHORIZATION);
				return null;
			} catch (IOException transientEx) {
				// network or server error, the call is expected to succeed if
				// you try again later.
				// Don't attempt to call again immediately - the request is
				// likely to
				// fail, you'll hit quotas or back-off.
				transientEx.printStackTrace();
				textInToast = "Service unavailable";
				return null;
			} catch (GoogleAuthException authEx) {
				// Failure. The call is not expected to ever succeed so it
				// should not be
				// retried.
				authEx.printStackTrace();
				textInToast = "Google service unavailable";
				return null;
			} catch (Exception e) {
				e.printStackTrace();
			}
			return newToken;
		}

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == REQUEST_AUTHORIZATION) {
			if (resultCode == Activity.RESULT_OK) {
				data.getExtras();
				new GetTokenTask().execute();
			}
		}
	}

	private OnCheckedChangeListener tglBtnServiceListener = new OnCheckedChangeListener() {

		@Override
		public void onCheckedChanged(CompoundButton buttonView,
				boolean isChecked) {
			if (isChecked) {
				textSender = spnrAccnt.getSelectedItem().toString();
				textRecepient = fieldSendTo.getText().toString();
				textPercentageAlarm = fieldPercentageAlarm.getText().toString();

				new GetTokenTask().execute(textSender);
			} else {
				Intent intent = new Intent(getApplicationContext(),
						AlarmService.class);
				startService(intent);
				stopService(intent);
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

}
