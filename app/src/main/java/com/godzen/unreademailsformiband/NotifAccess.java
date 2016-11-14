package com.godzen.unreademailsformiband;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.godzen.unreademailsformiband.helper.GmailContract;


public class NotifAccess extends NotificationListenerService
{
    private String TAG = this.getClass().getSimpleName();


    @Override
    public IBinder onBind(Intent intent)
    {
        return super.onBind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public boolean bindService(Intent service, ServiceConnection conn, int flags)
    {
        return super.bindService(service, conn, flags);
    }

    @Override
    public void unbindService(ServiceConnection conn) {
        super.unbindService(conn);
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        Log.d(TAG, "Started notification service.");
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }



    @Override
    public void onNotificationPosted(StatusBarNotification sbn)
    {
        if (sbn.getPackageName().equals(GmailContract.PACKAGE) && BluetoothAdapter.getDefaultAdapter().isEnabled())
        {
            Intent intent = new Intent(Constants.INTENT_NOTIFY_GMAIL);
            sendBroadcast(intent);
        }
    }


    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        //super.onNotificationRemoved(sbn);
    }

}



