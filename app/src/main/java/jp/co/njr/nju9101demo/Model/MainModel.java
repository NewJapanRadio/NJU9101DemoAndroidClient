package jp.co.njr.nju9101demo;

import android.content.Context;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class MainModel {
    private static final int REQUEST_ENABLE_BT = 6;

    private Context mContext;
    private KonashiManager mKonashiManager;
    private NJU9101Model mNJU9101Model;
    private MqttManager mMqttManager;
    private Timer mReadDataTimer;
    private TimerTask mReadDataTimerTask;

    public Func<Map<String, Double>> onDataRead;
    public Func<Boolean> onBleDeviceEnableChanged;

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

        mReadDataTimer = new Timer();
        mReadDataTimerTask = new TimerTask() {
            public void run() {
              MainModel.this.readData();
            }
        };

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
        mReadDataTimer.scheduleAtFixedRate(mReadDataTimerTask, 0, interval);
    }

    public void stopContinuousReadData() {
        mReadDataTimer.cancel();
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
}
