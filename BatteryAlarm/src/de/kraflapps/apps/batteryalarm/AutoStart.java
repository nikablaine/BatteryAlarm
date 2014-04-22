package de.kraflapps.apps.batteryalarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class AutoStart extends BroadcastReceiver {

	Alarm alarm;

	@Override
	public void onReceive(Context context, Intent intent) {

		if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
			
			alarm = new Alarm(context);
			
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
	        if (prefs.getBoolean(Alarm.SERVICE_ON, true)) {
	        	Intent intentForAlarmService = new Intent(context, AlarmService.class);
				context.startService(intentForAlarmService);
				alarm.setAlarm(context);
	        }
		}
	}

}
