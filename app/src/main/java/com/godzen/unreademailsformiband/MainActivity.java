package com.godzen.unreademailsformiband;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ButtonBarLayout;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.godzen.unreademailsformiband.helper.GmailHelper;
import com.godzen.unreademailsformiband.helper.SettingsZen;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int REQUEST_PERMISSION = 11;



    private MiBandService mBoundService;

    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBoundService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MiBandService.MyBinder myBinder = (MiBandService.MyBinder) service;
            mBoundService = myBinder.getService();
        }
    };




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        if(sharedPreferences.getString("listGmail", "").isEmpty())
        {
            String[] accounts = GmailHelper.getInstance().getAllAccountNames(getApplicationContext());
            if(accounts!=null && accounts.length>0)
            {
                String firstAccount =  accounts[0];
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("listGmail", firstAccount);
                editor.apply();

                //Toast.makeText(this, "Using default account "+firstAccount, Toast.LENGTH_LONG).show();
            }
        }

        if(sharedPreferences.getString("mibandMAC", "").isEmpty()) {
            String pairedMiBand = getPairedMiBandMAC();
            if (!pairedMiBand.equals("")) {
                SettingsZen.getInstance().setMiBandMAC(getApplicationContext(), pairedMiBand);
            }
        }


        if(sharedPreferences.getString("mibandMAC", "").isEmpty()) {
            Toast.makeText(this, "Please set your Mi Band MAC address", Toast.LENGTH_LONG).show();

            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
        }
        else {
            connectService();
        }


        final Button buttonService = (Button) findViewById(R.id.buttonService);
        buttonService.setText("Stop");
        buttonService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mBoundService == null) {
                    connectService();
                    buttonService.setText("Stop");

                } else {
                    disconnectService(true);
                    buttonService.setText("Start");
                }
            }
        });


        final Button buttonTest = (Button) findViewById(R.id.buttonTest);
        buttonTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mBoundService==null || !mBoundService.isConnected())
                {
                    Toast.makeText(MainActivity.this, "Not connected", Toast.LENGTH_LONG).show();
                }
                else {
                    mBoundService.notifyGmail(10);
                }
            }
        });

    }

    private String getPairedMiBandMAC() {

        String result = "";

        if(BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            final Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

            for (BluetoothDevice pairedDevice : pairedDevices) {
                if (pairedDevice != null && pairedDevice.getAddress() != null && pairedDevice.getName() != null && pairedDevice.getName().toLowerCase().contains("mi")) {
                    result = pairedDevice.getAddress();
                }
            }
        }

        return result;
    }

    private void connectService()
    {
        if (mBoundService==null) {
            Intent intent = new Intent(getApplicationContext(), MiBandService.class);
            bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
            startService(intent);
        }
        else{
            mBoundService.connectAndStart();
        }
    }

    private void disconnectService(boolean stop)
    {
        if (mBoundService!=null) {
            try {
                unbindService(mServiceConnection);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if(stop)
                mBoundService.stopSelf();
        }
        mBoundService = null;
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();

        disconnectService(false);
    }

    private boolean isNotificationAccessEnabled()
    {
        String pkgName = getPackageName();
        final String flat = Settings.Secure.getString(getContentResolver(),"enabled_notification_listeners");
        if(!TextUtils.isEmpty(flat))
        {
            final String[] names = flat.split(":");
            for (String name : names) {
                final ComponentName cn = ComponentName.unflattenFromString(name);
                if (cn != null) {
                    if (TextUtils.equals(pkgName, cn.getPackageName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_mainactivity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_settings:
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                break;
        }
        return true;
    }

    private void showEnableNotificationAccessDialog() {
        new AlertDialog.Builder(this)
            .setMessage("Please enable notification access")
            .setTitle("Action required")
            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
                    dialog.dismiss();
                }
            })
            .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                }
            })
            .create().show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode==REQUEST_PERMISSION)
        {
            //checkAllPermissions();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkAllPermissions();
    }

    private boolean checkAllPermissions() {

        if(!isNotificationAccessEnabled()) {
            showEnableNotificationAccessDialog();
            return false;
        }


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED)
        {
            Toast.makeText(this, "Please grant app permission to access accounts", Toast.LENGTH_LONG).show();

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.GET_ACCOUNTS}, REQUEST_PERMISSION);

            return false;
        }



        if (ActivityCompat.checkSelfPermission(this, "com.google.android.gm.permission.READ_CONTENT_PROVIDER") != PackageManager.PERMISSION_GRANTED)
        {
            Toast.makeText(this, "Please grant app permission to read emails", Toast.LENGTH_LONG).show();

            ActivityCompat.requestPermissions(this, new String[]{"com.google.android.gm.permission.READ_CONTENT_PROVIDER"}, REQUEST_PERMISSION);

            return false;
        }


        return true;
    }
}
