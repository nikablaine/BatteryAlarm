package de.kraflapps.apps.batteryalarm;

import java.util.ArrayList;

import com.google.android.gms.auth.UserRecoverableAuthException;

public interface AsyncResult {
	
	void asyncTaskContactLoaderFinished(ArrayList<CustomContact> result);
	void numPickerFragmentAlarmValueChosen(int value);

}
