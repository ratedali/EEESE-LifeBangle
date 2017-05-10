package io.ratedali.eeese.lifebangle.alerter;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import io.ratedali.eeese.lifebangle.R;
import io.ratedali.eeese.lifebangle.listener.AlertsService;

public class AlertsReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(AlertsService.ACTION_RECIEVED_ALERT)) {

            BluetoothDevice device =
                    intent.getParcelableExtra(AlertsService.EXTRA_ALERT_SENDER);
            int heartRate = intent.getIntExtra(AlertsService.EXTRA_ALERT_HEART_RATE, -1);

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            String preferenceAlertThreshold = context.getString(R.string.pref_alert_threshold);
            String defaultThreshold = context.getString(R.string.pref_default_alert_threshold);
            int alertThreshold = Integer.parseInt(
                    preferences.getString(preferenceAlertThreshold, defaultThreshold));
            if (heartRate >= alertThreshold) {
                Intent alertIntent = new Intent(context, AlertsActivity.class);
                alertIntent.putExtra(AlertsActivity.EXTRA_ALERT_SENDER, device);
                alertIntent.putExtra(AlertsActivity.EXTRA_HEART_RATE, heartRate);
                alertIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME);
                context.startActivity(alertIntent);
            }
        }
    }
}
