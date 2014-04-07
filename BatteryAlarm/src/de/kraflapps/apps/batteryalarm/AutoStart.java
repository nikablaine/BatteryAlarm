package de.kraflapps.apps.batteryalarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class AutoStart extends BroadcastReceiver {

	Alarm alarm = new Alarm();

	@Override
	public void onReceive(Context context, Intent intent) {

		if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
			
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
	        if (prefs.getBoolean("serviceIsOn", false)) {
	        	Intent intentForAlarmService = new Intent(context, AlarmService.class);
				context.startService(intentForAlarmService);
				alarm.setAlarm(context);
	        }
		}
	}

}
