package de.kraflapps.apps.batteryalarm;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.widget.Toast;

public class AlarmService extends Service {

	Alarm alarm = new Alarm();
	
	
	@Override
	public void onCreate()
    {
        super.onCreate();       
        alarm.SetAlarm(getApplicationContext());
        Toast.makeText(this, "Service started", Toast.LENGTH_SHORT).show();
    }

	@Override
    public void onDestroy() {
        
		alarm.CancelAlarm(getApplicationContext());
        Toast.makeText(this, "Service stopped", Toast.LENGTH_SHORT).show();
    }
       
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

}
