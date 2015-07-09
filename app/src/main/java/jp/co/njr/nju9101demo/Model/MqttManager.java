package jp.co.njr.nju9101demo;

import android.content.Context;
import android.util.Log;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;

public class MqttManager implements MqttCallback
{
    private IMqttToken mMqttConnectToken;

    private MqttAndroidClient mMqttAndroidClient;

    private MqttConfigure mMqttConfigure;

    // MqttCallback
    public void connectionLost(Throwable cause) {
        // This method is called when the connection to the server is lost.
    }
    public void deliveryComplete(IMqttDeliveryToken token) {
        // Called when delivery for a message has been completed, and all acknowledgments have been received.
    }
    public void messageArrived(String topic, MqttMessage message) {
        // This method is called when a message arrives from the server.
    }

    public MqttManager(Context context, MqttConfigure mqttConfigure) {
        this.mMqttConfigure = mqttConfigure;
        initialize(context);
    }

    public void initialize(Context context) {
        String broker = "tcp://" + mMqttConfigure.host + ":" +  mMqttConfigure.port;
        this.mMqttAndroidClient = new MqttAndroidClient(context, broker, "nju9101demo");
        this.mMqttAndroidClient.setCallback(this);
        this.mMqttAndroidClient.registerResources(context);
    }

    public void connect() {
        MqttConnectOptions option = new MqttConnectOptions();
        option.setUserName(mMqttConfigure.user);
        option.setPassword(mMqttConfigure.password.toCharArray());
        try {
            this.mMqttConnectToken = this.mMqttAndroidClient.connect(option);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        try {
            if (this.mMqttAndroidClient != null) {
                if (this.mMqttAndroidClient.isConnected()) {
                    this.mMqttAndroidClient.disconnect();
                }
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void publish(String topic, String payload) {
        try {
            this.mMqttConnectToken.waitForCompletion(500);
            if (this.mMqttAndroidClient.isConnected()) {
                this.mMqttAndroidClient.publish(topic, payload.getBytes(), 0, false);
            }
            else {
                Log.d("BLE", "not connected to MQTT Broker");
            }
        } catch (MqttPersistenceException e) {
            e.printStackTrace();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void release() {
        this.mMqttAndroidClient.unregisterResources();
        disconnect();
    }
}
