package jp.co.njr.nju9101demo;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

import gueei.binding.v30.app.BindingActivityV30;

public class NJU9101Demo extends BindingActivityV30
{
    private MainViewModel mMainViewModel;
    private MenuViewModel mMenuViewModel;
    private WakeLock mWakeLock;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        mMainViewModel = new MainViewModel(this);
        mMenuViewModel = new MenuViewModel(this);
        inflateAndBind(R.xml.mainactivity_metadata, mMainViewModel);
        bindOptionsMenu(mMenuViewModel);
        if (!mMainViewModel.isBleSupported()) {
            new AlertDialog.Builder(this)
                .setTitle("Warning")
                .setMessage("Your device does not support BLE")
                .setPositiveButton("OK", null)
                .show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        MqttConfigure mqttConfigure = new MqttConfigure();
        mqttConfigure.host = sharedPreferences.getString("mqtt_host", "");
        mqttConfigure.port = sharedPreferences.getString("mqtt_port", "");
        mqttConfigure.user = sharedPreferences.getString("mqtt_user", "");
        mqttConfigure.password = sharedPreferences.getString("mqtt_password", "");
        if (mMainViewModel.isBleSupported()) {
            mMainViewModel.initialize(mqttConfigure);
        }
        PowerManager pm = (PowerManager)getSystemService(POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, getString(R.string.app_name));
        mWakeLock.acquire();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mMainViewModel.isBleSupported()) {
            mMainViewModel.deinitialize();
        }
        mWakeLock.release();
    }
}
