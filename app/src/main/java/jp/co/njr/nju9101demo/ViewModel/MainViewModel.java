package jp.co.njr.nju9101demo;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.view.View;

import java.util.Map;

import gueei.binding.Command;
import gueei.binding.observables.BooleanObservable;
import gueei.binding.observables.StringObservable;

public class MainViewModel
{
    private static final int REQUEST_ENABLE_BT = 6;

    private KonashiManager mKonashiManager;
    private Context mContext;
    private Handler mGuiThreadHandler;

    private MainModel mMainModel;
    private MqttConfigure mMqttConfigure;

    public StringObservable temperature = new StringObservable();
    public StringObservable sensorData = new StringObservable();

    public Command startBleScan = new Command() {
        @Override
        public void Invoke(View view, Object... args) {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter != null) {
                if (adapter.isEnabled()) {
                    mMainModel.startBleScan();
                }
                else {
                    Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    ((Activity)MainViewModel.this.mContext).startActivityForResult(intent, REQUEST_ENABLE_BT);
                }
            }
        }
    };

    public Command stopBleScan = new Command() {
        @Override
        public void Invoke(View view, Object... args) {
            mMainModel.stopContinuousReadData();
            mMainModel.stopBleScan();
        }
    };

    public StringObservable readDataButtonText = new StringObservable();
    public BooleanObservable readDataButtonChecked = new BooleanObservable();
    public BooleanObservable isBleSupported = new BooleanObservable();
    public BooleanObservable isBleDeviceEnabled = new BooleanObservable();
    public Command readData = new Command() {
        @Override
        public void Invoke(View view, Object... args) {
            mMainModel.readData();
        }
    };

    public Command readDataButtonClick = new Command() {
        @Override
        public void Invoke(View view, Object... args) {
            if (mMainModel.isContinuousRead()) {
                mMainModel.stopContinuousReadData();
            }
            else {
                setEnabledAsync(readDataButtonChecked, false);
                mMainModel.readData();
            }
        }
    };

    public Command readDataButtonLongClick = new Command() {
        @Override
        public void Invoke(View view, Object... args) {
            mMainModel.startContinuousReadData(5);
        }
    };

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == BluetoothAdapter.ACTION_STATE_CHANGED) {
                switch (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                    case BluetoothAdapter.STATE_ON:
                        break;
                    case BluetoothAdapter.STATE_OFF:
                        break;
                }
            }
        }
    };

    public MainViewModel(Context context) {
        mContext = context;
        mGuiThreadHandler = new Handler();
        setEnabledAsync(isBleSupported, isBleSupported());
        setTextAsync(readDataButtonText, "Read Data");
    }

    public void initialize(MqttConfigure mqttConfigure) {
        mMqttConfigure = mqttConfigure;
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        mContext.registerReceiver(mBroadcastReceiver, filter);
        initializeModel();
    }

    private void initializeModel() {
        mMainModel = new MainModel(mContext, mMqttConfigure);
        mMainModel.onDataRead = new Func<Map<String, Double>>() {
            @Override
            public void execute(Map<String, Double> readData) {
                Double temp = readData.get("temperature");
                Double data = readData.get("sensorData");
                setTextAsync(MainViewModel.this.temperature, String.format("%.2f", temp));
                setTextAsync(MainViewModel.this.sensorData, String.format("%.0f", data));
            }
        };
        mMainModel.onBleDeviceEnableChanged = new Func<Boolean>() {
            @Override
            public void execute(Boolean enable) {
                setEnabledAsync(isBleDeviceEnabled, enable);
            }
        };
        mMainModel.onContinuousReadStateChanged = new Func<Boolean>() {
            @Override
            public void execute(Boolean enable) {
                if (enable) {
                    setTextAsync(readDataButtonText, mContext.getString(R.string.button_read_data_stop));
                }
                else {
                    setTextAsync(readDataButtonText, mContext.getString(R.string.button_read_data));
                }
                setEnabledAsync(readDataButtonChecked, enable);
            }
        };
    }

    public void deinitialize() {
        mContext.unregisterReceiver(mBroadcastReceiver);
        deinitializeModel();
    }

    public void deinitializeModel() {
        mMainModel.stopContinuousReadData();
        mMainModel.disconnectBle();
        mMainModel.disconnectMqtt();
    }

    private void setTextAsync(final StringObservable stringObservable, final String text){
        mGuiThreadHandler.post(new Runnable(){
            @Override
            public void run() {
                stringObservable.set(text);
            }
        });
    }

    private void setTextAsync(final StringObservable stringObservable, final String text, final Action callback){
        mGuiThreadHandler.post(new Runnable(){
            @Override
            public void run() {
                stringObservable.set(text);
                callback.execute();
            }
        });
    }

    private void setEnabledAsync(final BooleanObservable booleanObservable, final Boolean enabled){
        mGuiThreadHandler.post(new Runnable(){
            @Override
            public void run() {
                booleanObservable.set(enabled);
            }
        });
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
            }
        }
    }

    public boolean isBleSupported() {
        PackageManager pm = mContext.getPackageManager();
        return pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) ||
               pm.hasSystemFeature("jp.gr.java_conf.ble_profile.api.gatt");
    }
}
