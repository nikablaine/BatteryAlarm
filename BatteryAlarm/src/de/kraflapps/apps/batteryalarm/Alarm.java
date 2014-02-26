package de.kraflapps.apps.batteryalarm;

import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.URLName;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.sun.mail.smtp.SMTPTransport;
import com.sun.mail.util.BASE64EncoderStream;

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
import android.widget.Toast;

public class Alarm extends BroadcastReceiver {
	
	boolean workingFlag = true;
	String recepient = null;
	String authToken = null;
	String sender = null;
	int	alarmValue = 0;
		
	Session	session;


	@Override
	public void onReceive(Context context, Intent intent) {
		PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "");
        wl.acquire();

        // Put here YOUR code.
        Toast.makeText(context, "Alarm !!!!!!!!!!", Toast.LENGTH_LONG).show(); // For example
        
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		final Intent batteryStatus = context.registerReceiver(null, ifilter);
		
		int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
		int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

		float batteryPct = (level / (float)scale) * 100;
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		workingFlag = prefs.getBoolean("serviceShouldWork", true);
		recepient = prefs.getString("Recepient", null);
		authToken = prefs.getString("AuthToken", null);
		sender = prefs.getString("Sender", null);
		alarmValue = Integer.parseInt(prefs.getString("AlarmValue", "0"));
			
		session = createSessionObject();
		
		try {
			  
			  if (batteryPct <= alarmValue && workingFlag){
				  sendMail(recepient, "Battery Alarm Mail", "Current battery status is " + batteryPct);
				  
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
		
		
	

        wl.release();
	}
	
	public void SetAlarm(Context context)
    {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, Alarm.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
        am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 1000 * 30, pi); // Millisec * Second * Minute
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor prefsEditor = prefs.edit();
        prefsEditor.putBoolean("serviceIsOn", true);
        prefsEditor.commit();
        
    }

    public void CancelAlarm(Context context)
    {
        Intent intent = new Intent(context, Alarm.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(sender);
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor prefsEditor = prefs.edit();
        prefsEditor.putBoolean("serviceIsOn", false);
        prefsEditor.commit();
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

}
