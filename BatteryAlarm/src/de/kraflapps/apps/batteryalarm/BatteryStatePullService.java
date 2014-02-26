package de.kraflapps.apps.batteryalarm;


import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Calendar;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.URLName;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.sun.mail.smtp.SMTPTransport;
import com.sun.mail.util.BASE64EncoderStream;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.preference.PreferenceManager;

public class BatteryStatePullService extends IntentService {
	
	Session session = null;
	String authToken = null;
	String recepient = null;
	String sender = null;
	int alarmValue = 0;
	

	public BatteryStatePullService() {
		super("BatteryStatePullService");
	}
	
	private Date getCurTime() {
		Calendar cal = Calendar.getInstance();
		Date curTime = (Date) cal.getTime();
		return curTime;
	}
	
	private Session createSessionObject() {
		Properties props = new Properties();
	    props.put("mail.smtp.starttls.enable", "true");
	    props.put("mail.smtp.starttls.required", "true");
	    props.put("mail.smtp.sasl.enable", "true");
	    props.put("mail.smtp.sasl.mechanisms", "XOAUTH2");
	    //props.put("mail.smtp.auth","true");
	    
	    Session session = Session.getInstance(props);
	    session.setDebug(true);
	    return session;
	}
	
	public SMTPTransport connectToSmtp(String userEmail, String oauthToken, Session session) throws Exception {
	    final URLName unusedUrlName = null;
	    SMTPTransport transport = new SMTPTransport(session, unusedUrlName);
	    final String emptyPassword = null;
	    final String host = "smtp.gmail.com";
	    final int port = 587;
	    
	    transport.connect(host, port, userEmail, emptyPassword);

	    byte[] response = String.format("user=%s\1auth=Bearer %s\1\1", userEmail, oauthToken).getBytes();
	    response = BASE64EncoderStream.encode(response);

	    transport.issueCommand("AUTH XOAUTH2 " + new String(response), 235);

	    return transport;
	}
	
	private void sendMail(String email, String subject, String messageBody) {
	 
	    try {
	        Message message = createMessage(email, subject, messageBody, session);
	        new SendMailTask().execute(message);
	    } catch (AddressException e) {
	        e.printStackTrace();
	    } catch (MessagingException e) {
	        e.printStackTrace();
	    } catch (UnsupportedEncodingException e) {
	        e.printStackTrace();
	    }
	}
	
	private Message createMessage(String email, String subject, String messageBody, Session session) throws MessagingException, UnsupportedEncodingException {
	    Message message = new MimeMessage(session);
	    message.setFrom(new InternetAddress("super@alarm.com", "Special Alarm"));
	    message.addRecipient(Message.RecipientType.TO, new InternetAddress(email, email));
	    message.setSubject(subject);
	    message.setText(messageBody);
	    return message;
	}
	
	private class SendMailTask extends AsyncTask<Message, Void, Void> {
	    //private ProgressDialog progressDialog;
	 
	    @Override
	    protected void onPreExecute() {
	        super.onPreExecute();
	       // progressDialog = ProgressDialog.show(MainActivity.this, "Please wait", "Sending mail", true, false);
	    }
	 
	    @Override
	    protected void onPostExecute(Void aVoid) {
	        super.onPostExecute(aVoid);
	        //progressDialog.dismiss();
	    }
	 
	    @Override
	    protected Void doInBackground(Message... messages) {
	        try {
	        	SMTPTransport transport = connectToSmtp(sender, authToken, session);
	            transport.sendMessage(messages[0], messages[0].getAllRecipients());
	        } catch (MessagingException e) {
	            e.printStackTrace();
	        } catch (Exception e) {
				e.printStackTrace();
			}
	        return null;
	    }
	}
	
	

	@Override
	protected void onHandleIntent(Intent intent) {
		

		
		final String filename = "battstats.txt";
		
		final int updatePeriod = 5000;
		
	    recepient = intent.getExtras().getString("Recepient");
		authToken = intent.getExtras().getString("AuthToken");
		sender = intent.getExtras().getString("Sender");
		alarmValue = Integer.parseInt(intent.getExtras().getString("AlarmValue"));
		
		session = createSessionObject();
		
		
		Timer mTimer = new Timer();
		TimerTask batteryTask = new TimerTask() {
			
			@Override
			public void run() {
				IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
				final Intent batteryStatus = getApplicationContext().registerReceiver(null, ifilter);
				
				int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
				int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

				float batteryPct = (level / (float)scale) * 100;
				
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
				boolean workingFlag = prefs.getBoolean("serviceShouldWork", true);
				
				FileOutputStream outputStream;
				
				try {
					  getApplicationContext();
					  
					  Date curTime = getCurTime();
					  String dateString = curTime.toString();
					  String string = dateString + ": " + batteryPct + "%\n";  
			          
					  outputStream = openFileOutput(filename, Context.MODE_APPEND);
					  
					  outputStream.write(string.getBytes());
					  outputStream.close();
					  
					  if (batteryPct <= alarmValue && workingFlag){
						  sendMail(recepient, "Battery Alarm Mail", string);
						  
						  SharedPreferences.Editor editor = prefs.edit();
						  editor.putBoolean("serviceShouldWork", false);
						  editor.commit();
						  
					  }
					  
					  if (batteryPct > alarmValue && !workingFlag){
						  
						  SharedPreferences.Editor editor = prefs.edit();
						  editor.putBoolean("serviceShouldWork", true);
						  editor.commit();
						  
					  }
					  
					} catch (Exception e) {
					  e.printStackTrace();
					}
				
				
			}
		};		
		
		mTimer.schedule(batteryTask, getCurTime(), updatePeriod);

	}

}
