package de.kraflapps.apps.batteryalarm;

import java.io.IOException;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.auth.UserRecoverableAuthException;

public class GetTokenTask extends AsyncTask<String, Void, String> {

	public final static String SCOPE = "oauth2:https://mail.google.com";
	private String textInToast;
	public AsyncTokenGet delegate;
	public Context context;
	
	public GetTokenTask(final Context cntxt){
		super();
		setContext(cntxt);
	}
	
	
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		delegate.asyncTaskGetTokenStarted();
	}




	@Override
	protected String doInBackground(final String... params) {
		
		String token = null;

		try {
			token = GoogleAuthUtil.getToken(context, params[0], SCOPE);
			GoogleAuthUtil.invalidateToken(context, token);
			token = GoogleAuthUtil.getToken(context, params[0], SCOPE);
		} catch (GooglePlayServicesAvailabilityException playEx) {
			Log.e(MainActivity.TAG, "GPlayException: " + Log.getStackTraceString(playEx));
			setTextInToast("Google Play is unavailable. Please try again later.");
		} catch (UserRecoverableAuthException userAuthEx) {
			// Start the user recoverable action using the intent returned by getIntent()
			Log.i(MainActivity.TAG, "UserRecoverableException: " + Log.getStackTraceString(userAuthEx));
			delegate.asyncTaskGetTokenAuth(userAuthEx);
		} catch (IOException transientEx) {
			// network or server error, the call is expected to succeed if
			// you try again later.
			// Don't attempt to call again immediately - the request is
			// likely to
			// fail, you'll hit quotas or back-off.
			Log.e(MainActivity.TAG, "IOException: " + Log.getStackTraceString(transientEx));
			setTextInToast("Network or server error. Please try again later.");
		} catch (GoogleAuthException authEx) {
			// Failure. The call is not expected to ever succeed so it
			// should not be
			// retried.
			Log.e(MainActivity.TAG, "AuthException: " + Log.getStackTraceString(authEx));
			setTextInToast(textInToast = "Authorization problem.");
		} 
		return token;
		
	}

	@Override
	protected void onPostExecute(final String result) {
		super.onPostExecute(result);
		delegate.asyncTaskGetTokenFinished(result, textInToast);
		//startService(new Intent(getApplicationContext(), AlarmService.class));

	}
	
	public AsyncTokenGet getDelegate() {
		return delegate;
	}

	public void setDelegate(final AsyncTokenGet delegate) {
		this.delegate = delegate;
	}


	public String getTextInToast() {
		return textInToast;
	}


	public void setTextInToast(final String textInToast) {
		this.textInToast = textInToast;
	}


	public Context getContext() {
		return context;
	}


	public void setContext(final Context context) {
		this.context = context;
	}

	
}
