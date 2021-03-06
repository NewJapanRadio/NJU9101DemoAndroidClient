package jp.co.njr.nju9101demo;

import android.content.Context;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledFuture;

public class MainModel {
    private static final int REQUEST_ENABLE_BT = 6;

    private Context mContext;
    private KonashiManager mKonashiManager;
    private NJU9101Model mNJU9101Model;
    private MqttManager mMqttManager;
    private ScheduledExecutorService mScheduler;
    private ScheduledFuture<?> mFuture;
    private Boolean mIsContinuousRead;

    public Func<Map<String, Double>> onDataRead;
    public Func<Boolean> onBleDeviceEnableChanged;
    public Func<Boolean> onContinuousReadStateChanged;

    public MainModel(Context context, MqttConfigure mqttConfigure) {
        this.mContext = context;

        mKonashiManager = new KonashiManager(context);
        mKonashiManager.onConnectListener = new Action() {
            @Override
            public void execute() {
                if (onBleDeviceEnableChanged != null) {
                    onBleDeviceEnableChanged.execute(true);
                }
            }
        };
        mKonashiManager.onDisconnectListener = new Action() {
            @Override
            public void execute() {
                if (onBleDeviceEnableChanged != null) {
                    onBleDeviceEnableChanged.execute(false);
                }
            }
        };
        mNJU9101Model = new NJU9101Model(mKonashiManager);
        if (onBleDeviceEnableChanged != null) {
            onBleDeviceEnableChanged.execute(false);
        }

        mMqttManager = new MqttManager(mContext, mqttConfigure);
        mMqttManager.connect();

        mScheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public void startBleScan() {
        if (mKonashiManager != null) {
            mKonashiManager.startLeScan();
        }
    }

    public void stopBleScan() {
        if (mKonashiManager != null) {
            mKonashiManager.stopLeScan();
            mKonashiManager.disconnect();
        }
    }

    private Map readDataMap = new HashMap<String, Double>();

    public void startContinuousReadData(int interval) {
        mIsContinuousRead = true;
        if (onContinuousReadStateChanged != null) {
            onContinuousReadStateChanged.execute(true);
        }
        mFuture = mScheduler.scheduleAtFixedRate(
                new Runnable() {
                    public void run() {
                        MainModel.this.readData();
                    }
                },
                0, interval, TimeUnit.SECONDS
                );
    }

    public void stopContinuousReadData() {
        if (mFuture != null) {
            mFuture.cancel(true);
        }
        mIsContinuousRead = false;
        if (onContinuousReadStateChanged != null) {
            onContinuousReadStateChanged.execute(false);
        }
    }

    public void readData() {
        // Read Temperature
        mNJU9101Model.onTemperatureReadListener = new Func<Double>() {
            @Override
            public void execute(Double temperature) {
                readDataMap.put("temperature", temperature);
                // Read Sensor Data
                mNJU9101Model.onSensorDataReadListener = new Func<byte[]>() {
                    @Override
                    public void execute(byte[] data) {
                        short rawData = (short)(((data[0] & 0xFF) << 8) + (data[1] & 0xFF));
                        readDataMap.put("sensorData", (double)rawData);
                        MainModel.this.publish();
                        if (onDataRead != null) {
                            onDataRead.execute(readDataMap);
                        }
                    }
                };
                mNJU9101Model.readSensorData();
            }
        };
        mNJU9101Model.readTemperature();
    }

    private void publish() {
        Date now = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss 'GMT'Z", Locale.US);
        Map map = new HashMap<String, Object>();
        map.put("temp", readDataMap.get("temperature"));
        map.put("data", readDataMap.get("sensorData"));
        map.put("time", sdf.format(now));

        JSONObject jsonObject = new JSONObject(map);

        mMqttManager.publish("NewJapanRadio/NJU9101", jsonObject.toString());
    }

    public void disconnectBle() {
        if (mKonashiManager != null) {
            mKonashiManager.disconnect();
        }
    }

    public void disconnectMqtt() {
        if (mMqttManager != null) {
            mMqttManager.release();
        }
    }

    public Boolean isContinuousRead() {
        if (mIsContinuousRead != null) {
            return mIsContinuousRead;
        }
        else {
            return false;
        }
    }
}
