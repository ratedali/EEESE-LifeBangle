package io.ratedali.eeese.lifebangle.alerter;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import io.ratedali.eeese.lifebangle.R;

public class AlertsActivity extends AppCompatActivity {

    public static final String EXTRA_ALERT_SENDER =
            "io.ratedali.eeese.lifebangle.alerts.AlertsActivity.EXTRA_ALERT_SENDER";
    public static final String EXTRA_HEART_RATE =
            "io.ratedali.eeese.lifebangle.alerts.AlertsActivity.EXTRA_HEART_RATE";


    private static final int NOTIFICATION_ID = 0;

    @BindView(R.id.alert_sender)
    TextView senderView;
    @BindView(R.id.heart_rate)
    TextView heartRateView;
    @BindView(R.id.cancel_alert_button)
    View cancelAlertButton;

    @BindString(R.string.alert_title)
    String notificationTitle;

    // Preferences
    @BindString(R.string.pref_alert_alarm)
    String preferenceAlertAlarm;
    @BindString(R.string.pref_value_name_ringtone_silent)
    String preferenceAlertAlarmSilent;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_alerts);
        ButterKnife.bind(this);

        BluetoothDevice sender = getIntent().getParcelableExtra(EXTRA_ALERT_SENDER);
        int heartRate = getIntent().getIntExtra(EXTRA_HEART_RATE, -1);
        if (heartRate == -1) finish();

        senderView.setText(sender.getName());
        heartRateView.setText(getString(R.string.heart_rate_format, heartRate));

        cancelAlertButton
                .setOnClickListener(view -> cancelAlert());

        notify(sender.getName(), heartRate, false);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        BluetoothDevice sender = intent.getParcelableExtra(EXTRA_ALERT_SENDER);
        int heartRate = intent.getIntExtra(EXTRA_HEART_RATE, -1);
        if (heartRate == -1) finish();

        senderView.setText(sender.getName());
        heartRateView.setText(getString(R.string.heart_rate_format, heartRate));

        notify(sender.getName(), heartRate, true);

    }

    protected void onStop() {
        super.onStop();
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
    }

    private void cancelAlert() {
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);
        finish();
    }

    private void notify(String name, int heartRate, boolean update) {

        String notificationText =
                getString(R.string.alert_notification_text, name, heartRate);

        Notification.Builder notificationBuilder = new Notification.Builder(this)
                .setContentTitle(notificationTitle)
                .setContentText(notificationText)
                .setContentIntent(PendingIntent.getActivity(this, 0, new Intent(), 0))
                .setSmallIcon(R.drawable.ic_alert)
                .setAutoCancel(true);


        /*
         * Add a ringtone depending on the user preference
         */

        if (!update) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

            String alarmRingtone = preferences.getString(preferenceAlertAlarm, null);
            if (alarmRingtone == null) {
                notificationBuilder.setSound(
                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM));
            } else if (!alarmRingtone.equals(preferenceAlertAlarmSilent)) {
                notificationBuilder.setSound(Uri.parse(alarmRingtone));
            }
        }

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
    }
}
