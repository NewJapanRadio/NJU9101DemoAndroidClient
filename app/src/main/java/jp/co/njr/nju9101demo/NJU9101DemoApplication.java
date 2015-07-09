package jp.co.njr.nju9101demo;

import android.app.Application;

import gueei.binding.Binder;

public class NJU9101DemoApplication extends Application
{
    @Override
    public void onCreate() {
        super.onCreate();
        Binder.init(this);
    }
}
