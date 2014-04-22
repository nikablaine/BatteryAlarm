package de.kraflapps.apps.batteryalarm;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.widget.Toast;

public class AlarmService extends Service {

	private Alarm alarm;
	private Context appContext;
	
	
	@Override
	public void onCreate()
    {
        super.onCreate(); 
        appContext = getApplicationContext();
        alarm = new Alarm(appContext);
        alarm.setAlarm(appContext);
    }

	@Override
    public void onDestroy() {
        
		alarm.cancelAlarm(appContext);
        Toast.makeText(this, "Service stopped", Toast.LENGTH_SHORT).show();
    }
       
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	
}
