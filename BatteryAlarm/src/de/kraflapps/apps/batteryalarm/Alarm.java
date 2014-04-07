package de.kraflapps.apps.batteryalarm;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.URLName;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.auth.UserRecoverableAuthException;
import com.sun.mail.smtp.SMTPTransport;
import com.sun.mail.util.BASE64EncoderStream;

public class Alarm extends BroadcastReceiver implements AsyncResult {

	public static final String WORKING_FLAG = "de.kraflapps.apps.batteryalarm.working_flag";
	public static final String SERVICE_ON = "de.kraflapps.apps.batteryalarm.service_on";
	public static final String AUTH_TOKEN = "de.kraflapps.apps.batteryalarm.auth_token";
	public static final String ALARM_VALUE = "de.kraflapps.apps.batteryalarm.alarm_value";
	public static final String SENDER_ACCOUNT = "de.kraflapps.apps.batteryalarm.sender_account";
	public static final String RECIPIENT = "de.kraflapps.apps.batteryalarm.recipient";

	private boolean workingFlag;
	private String recipient;
	private String[] recipientArr;
	private String authToken;
	private String sender;
	private int alarmValue;

	private Session session;

	private Context appContext;

	private boolean repeat;

	@Override
	public void onReceive(Context context, Intent intent) {

		android.os.Debug.waitForDebugger();
		final PowerManager powerManager = (PowerManager) context
				.getSystemService(Context.POWER_SERVICE);
		PowerManager.WakeLock wakelock = powerManager.newWakeLock(
				PowerManager.PARTIAL_WAKE_LOCK, "");
		wakelock.acquire();

		// here goes the operation that has to be done by service
		Toast.makeText(context, "Alarm !!!!!!!!!!", Toast.LENGTH_LONG).show();

		final IntentFilter ifilter = new IntentFilter(
				Intent.ACTION_BATTERY_CHANGED);
		final Intent batteryStatus = context.registerReceiver(null, ifilter);

		int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
		int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

		float batteryPct = (level / (float) scale) * 100;

		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		workingFlag = prefs.getBoolean(WORKING_FLAG, true);
		recipient = prefs.getString(RECIPIENT, null);
		recipientArr = (recipient == null ? null : recipient.trim().split(","));
		authToken = prefs.getString(AUTH_TOKEN, null);
		sender = prefs.getString(SENDER_ACCOUNT, null);
		alarmValue = prefs.getInt(ALARM_VALUE, 0);

		session = createSessionObject();

		if (batteryPct <= alarmValue && workingFlag) {
			sendMail(recipientArr, "Battery Alarm Mail",
					"Current battery status is " + batteryPct);

		}

		if (batteryPct > alarmValue && !workingFlag) {

			SharedPreferences.Editor editor = prefs.edit();
			editor.putBoolean(WORKING_FLAG, true);
			editor.commit();

		}

		wakelock.release();

	}

	public void setAlarm(Context context) {

		setAppContext(context);
		AlarmManager am = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		Intent i = new Intent(context, Alarm.class);
		PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
		am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(),
				1000 * 30, pi); // Millisec * Second * Minute

		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		SharedPreferences.Editor prefsEditor = prefs.edit();
		prefsEditor.putBoolean(SERVICE_ON, true);
		prefsEditor.commit();

	}

	public void cancelAlarm(Context context) {
		Intent intent = new Intent(context, Alarm.class);
		PendingIntent sender = PendingIntent
				.getBroadcast(context, 0, intent, 0);
		AlarmManager alarmManager = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		alarmManager.cancel(sender);

		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		SharedPreferences.Editor prefsEditor = prefs.edit();
		prefsEditor.putBoolean(SERVICE_ON, false);
		prefsEditor.commit();
	}

	private Session createSessionObject() {
		Properties props = new Properties();
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.starttls.required", "true");
		props.put("mail.smtp.sasl.enable", "true");
		props.put("mail.smtp.sasl.mechanisms", "XOAUTH2");
		// props.put("mail.smtp.auth","true");

		Session session = Session.getInstance(props);
		session.setDebug(true);
		return session;
	}

	public SMTPTransport connectToSmtp(String userEmail, String oauthToken,
			Session session) throws Exception {
		final URLName unusedUrlName = null;
		SMTPTransport transport = new SMTPTransport(session, unusedUrlName);
		final String emptyPassword = null;
		final String host = "smtp.gmail.com";
		final int port = 587;

		transport.connect(host, port, userEmail, emptyPassword);

		byte[] response = String.format("user=%s\1auth=Bearer %s\1\1",
				userEmail, oauthToken).getBytes();
		response = BASE64EncoderStream.encode(response);

		transport.issueCommand("AUTH XOAUTH2 " + new String(response), 235);

		return transport;
	}

	private boolean sendMail(String[] email, String subject, String messageBody) {

		boolean flag = true;
		for (int i = 0; i < email.length; i++) {
			try {
				Message message = createMessage(email[i], subject, messageBody,
						session);
				new SendMailTask().execute(message);
				Log.i(MainActivity.TAG, "Creating message with recipient = "
						+ email[i]);
				flag = true;
			} catch (AddressException e) {
				// TODO: process exception
				e.printStackTrace();
				flag = false;
			} catch (MessagingException e) {
				e.printStackTrace();
				GetTokenTask getTokenTask = new GetTokenTask(getAppContext());
				getTokenTask.setDelegate(this);
				getTokenTask.execute(getSender());
				flag = false;
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
				flag = false;
			}
		}
		return flag;
	}

	private Message createMessage(String email, String subject,
			String messageBody, Session session) throws MessagingException,
			UnsupportedEncodingException {
		Message message = new MimeMessage(session);
		message.setFrom(new InternetAddress("super@alarm.com", "Special Alarm"));
		message.addRecipient(Message.RecipientType.TO, new InternetAddress(
				email, email));
		message.setSubject(subject);
		message.setText(messageBody);
		return message;
	}

	private class SendMailTask extends AsyncTask<Message, Void, Void> {
		// private ProgressDialog progressDialog;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			// progressDialog = ProgressDialog.show(MainActivity.this,
			// "Please wait", "Sending mail", true, false);
		}

		@Override
		protected void onPostExecute(Void aVoid) {
			super.onPostExecute(aVoid);

			if (!repeat) {
				SharedPreferences.Editor editor = PreferenceManager
						.getDefaultSharedPreferences(appContext).edit();
				editor.putBoolean(WORKING_FLAG, false);
				editor.commit();
			}

		}

		@Override
		protected Void doInBackground(Message... messages) {

			try {
				SMTPTransport transport = connectToSmtp(sender, authToken,
						session);
				transport.sendMessage(messages[0],
						messages[0].getAllRecipients());
			} catch (MessagingException e) {
				e.printStackTrace();
				GetTokenTask getTokenTask = new GetTokenTask(getAppContext());
				getTokenTask.setDelegate(Alarm.this);
				getTokenTask.execute(getSender());
				repeat = true;
			} catch (Exception e) {
				e.printStackTrace();
				repeat = false;
			}
			return null;
		}
	}

	public boolean isWorkingFlag() {
		return workingFlag;
	}

	public void setWorkingFlag(boolean workingFlag) {
		this.workingFlag = workingFlag;
	}

	public String getRecepient() {
		return recipient;
	}

	public void setRecepient(String recepient) {
		this.recipient = recepient;
	}

	public String getAuthToken() {
		return authToken;
	}

	public void setAuthToken(String authToken) {
		this.authToken = authToken;
	}

	public String getSender() {
		return sender;
	}

	public void setSender(String sender) {
		this.sender = sender;
	}

	public int getAlarmValue() {
		return alarmValue;
	}

	public void setAlarmValue(int alarmValue) {
		this.alarmValue = alarmValue;
	}

	public Session getSession() {
		return session;
	}

	public void setSession(Session session) {
		this.session = session;
	}

	public Context getAppContext() {
		return appContext;
	}

	public void setAppContext(Context appContext) {
		this.appContext = appContext;
	}

	@Override
	public void asyncTaskContactLoaderFinished(ArrayList<CustomContact> result) {
		// TODO Auto-generated method stub

	}

	@Override
	public void asyncTaskGetTokenStarted() {
		// TODO Auto-generated method stub

	}

	@Override
	public void asyncTaskGetTokenAuth(UserRecoverableAuthException exc) {
		// TODO Auto-generated method stub

	}

	@Override
	public void asyncTaskGetTokenFinished(String result, String text) {
		SharedPreferences.Editor editor = PreferenceManager
				.getDefaultSharedPreferences(appContext).edit();
		editor.putString(AUTH_TOKEN, result);
		editor.commit();

	}

	@Override
	public void numPickerFragmentAlarmValueChosen(int value) {
		// TODO Auto-generated method stub

	}

}
