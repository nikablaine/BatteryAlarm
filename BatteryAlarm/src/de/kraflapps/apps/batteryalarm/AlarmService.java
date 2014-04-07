package de.kraflapps.apps.batteryalarm;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.widget.Toast;

public class AlarmService extends Service {

	private final Alarm alarm = new Alarm();
	private Context context;
	
	
	@Override
	public void onCreate()
    {
        super.onCreate(); 
        context = getApplicationContext();
        alarm.setAlarm(context);
    }

	@Override
    public void onDestroy() {
        
		alarm.cancelAlarm(context);
        Toast.makeText(this, "Service stopped", Toast.LENGTH_SHORT).show();
    }
       
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	
}
