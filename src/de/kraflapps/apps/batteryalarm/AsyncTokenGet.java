package de.kraflapps.apps.batteryalarm;

import com.google.android.gms.auth.UserRecoverableAuthException;

public interface AsyncTokenGet {
	
	void asyncTaskGetTokenStarted();
	void asyncTaskGetTokenAuth(UserRecoverableAuthException exc);
	void asyncTaskGetTokenFinished(String result, String text);

}
