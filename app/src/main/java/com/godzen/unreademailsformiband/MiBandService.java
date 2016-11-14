package com.godzen.unreademailsformiband;


import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;


import com.godzen.unreademailsformiband.helper.GmailHelper;
import com.godzen.unreademailsformiband.helper.SettingsZen;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class MiBandService extends Service{

    private static final String TAG = MiBandService.class.getSimpleName();

    private IBinder mBinder = new MyBinder();

    private boolean isConnected;
    private BluetoothDevice mBluetoothMi;
    private BluetoothGatt mGatt;
    private Handler handler;


    private BluetoothGattCharacteristic charact_2A46;
    private BluetoothGattCharacteristic charact_2A06;
    private CountDownLatch mWait;





    public MiBandService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.INTENT_NOTIFY_GMAIL);
        registerReceiver(receiver, intentFilter);

        handler = new Handler(Looper.getMainLooper());
        isConnected=false;
        connect();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");

        unregisterReceiver(receiver);

        handler=null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class MyBinder extends Binder {
        public MiBandService getService() {
            return MiBandService.this;
        }
    }

    public void connectAndStart()
    {
        if(!isConnected)
            connect();
    }

    private void connect() {

        if(!BluetoothAdapter.getDefaultAdapter().isEnabled())
            return;

        isConnected = false;

        String mDeviceAddress = SettingsZen.getInstance().getMiBandMAC(getApplicationContext());
        if (mDeviceAddress == null || mDeviceAddress.isEmpty()) {
            return;
        }

        final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        try {
            mBluetoothMi = mBluetoothAdapter.getRemoteDevice(mDeviceAddress);
        } catch (Exception e) {
            mBluetoothMi = null;
            e.printStackTrace();
        }
        if (mBluetoothMi != null) {
            if (!connectGatt()) {
                isConnected = false;
                Log.w(TAG, "Can't connect to Mi Band");
            }
        }

    }


    public boolean connectGatt()
    {
        if(mBluetoothMi==null)
            return false;

        closeGatt();//older gatt??
        mGatt = mBluetoothMi.connectGatt(getApplicationContext(), false, callback);//false
        if (mGatt == null)
            return false;

        return mGatt.connect();
    }


    BluetoothGattCallback callback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, int status, int newState)
        {
            super.onConnectionStateChange(gatt, status, newState);
            Log.d(TAG, "onConnectionStateChange status="+status+" newState="+newState);
            if (newState == BluetoothProfile.STATE_CONNECTED)
            {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        gatt.discoverServices();
                    }
                });
            }
            else if (newState == BluetoothProfile.STATE_DISCONNECTED)
            {
                //Util.sendBroadcast(getApplicationContext(), Constants.INTENT_MISCALE_DISCONNECTED);

                closeGatt();
            }
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, int status)
        {
            super.onServicesDiscovered(gatt, status);
            Log.d(TAG, "onServicesDiscovered status="+status);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                charact_2A06 = gatt.getService(Constants.UUID_SERVICE_1802).getCharacteristic(Constants.UUID_CHARACTERISTIC_2A06);

                isConnected = true;
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic)
        {
            super.onCharacteristicChanged(gatt, characteristic);
            Log.d(TAG, "onCharacteristicChanged "+characteristic.getUuid()+" "+ Arrays.toString(characteristic.getValue()));
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            Log.d(TAG, "onCharacteristicWrite "+characteristic.getUuid()+" "+ Arrays.toString(characteristic.getValue()));
            if(mWait!=null && mWait.getCount()>0)
                mWait.countDown();
        }
    };





    public void closeGatt()
    {
        if (mGatt == null)
            return;
        try {
            mGatt.disconnect();
            mGatt.close();
        }
        catch (Exception ignored){}
        mGatt = null;
    }



    public void notifyGmail(int email)
    {
        if(!isConnected)
        {
            connect();
        }


        try {

            mWait = new CountDownLatch(1);


            mWait = new CountDownLatch(1);

            charact_2A06.setValue(new byte[]{-3, 2, (byte) (email & 0xFF), (byte) ((email >> 8) & 0xFF)});
            mGatt.writeCharacteristic(charact_2A06);

            try {
                mWait.await(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
            }

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }


    }


    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action.equals(Constants.INTENT_NOTIFY_GMAIL))
            {
                int unreadEmail = GmailHelper.getInstance().getUnreadEmails(getApplicationContext());
                notifyGmail(unreadEmail);
            }
        }
    };



    public boolean isConnected() {
        return isConnected;
    }

}
