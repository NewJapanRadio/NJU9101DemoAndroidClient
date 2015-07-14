package jp.co.njr.nju9101demo;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;

import gueei.binding.Command;

public class MenuViewModel
{
    private Context mContext;

    public Command menuItemSettings = new Command() {
        @Override
        public void Invoke(View view, Object... args) {
            Intent intent = new Intent(mContext, SettingsActivity.class);
            mContext.startActivity(intent);
        }
    };

    public Command menuItemLicenses = new Command() {
        @Override
        public void Invoke(View view, Object... args) {
            Intent intent = new Intent(mContext, LicensesActivity.class);
            mContext.startActivity(intent);
        }
    };

    public MenuViewModel(Context context) {
        this.mContext = context;
    }
}
