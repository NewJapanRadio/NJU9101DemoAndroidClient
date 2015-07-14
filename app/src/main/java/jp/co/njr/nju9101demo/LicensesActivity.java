package jp.co.njr.nju9101demo;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.widget.TextView;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Map;

public class LicensesActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.licenses);

        TextView header = (TextView)findViewById(R.id.header);
        StringBuilder hsb = new StringBuilder();
        hsb.append("Using libraries.\n\n");
        hsb.append("Paho MQTT Java Client\n");
        hsb.append("Paho MQTT Android Service Client\n");
        hsb.append("https://github.com/eclipse/paho.mqtt.java\n\n");
        hsb.append("AndroidBinding\n");
        hsb.append("https://github.com/CosminRadu/AndroidBinding\n\n");
        hsb.append("Android-Bootstrap\n");
        hsb.append("https://github.com/Bearded-Hen/Android-Bootstrap\n\n");
        header.setText(hsb.toString());

        TextView title = null;
        title = (TextView)findViewById(R.id.paho_mqtt_java_lic_title);
        title.setText("Paho MQTT Java Client\nPaho MQTT Android Service Client");
        title = (TextView)findViewById(R.id.android_binding_lic_title);
        title.setText("AndroidBinding");
        title = (TextView)findViewById(R.id.android_bootstrap_lic_title);
        title.setText("Android-Bootstrap");

        Map<String, Integer> licenses = new HashMap<String, Integer>();
        licenses.put("paho.mqtt.java.lic", R.id.paho_mqtt_java_lic);
        licenses.put("AndroidBinding.lic", R.id.android_binding_lic);
        licenses.put("Android-Bootstrap.lic",  R.id.android_bootstrap_lic);
        InputStream inputStream = null;
        BufferedReader reader = null;
        for (Map.Entry<String, Integer> entry : licenses.entrySet()) {
            try {
                inputStream = getResources().getAssets().open(entry.getKey());
                reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                StringBuilder sb = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                    sb.append("\n");
                }
                TextView tv = (TextView)findViewById(entry.getValue());
                tv.setText(sb.toString());
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    reader.close();
                    inputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
