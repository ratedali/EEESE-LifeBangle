package io.ratedali.eeese.lifebangle.listener;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.UUID;

import io.ratedali.eeese.lifebangle.R;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import io.reactivex.internal.disposables.ListCompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class AlertsService extends Service {
    public static final String ACTION_LISTEN_FOR_ALERTS =
            "io.ratedali.eeese.lifebangle.alerts.AlertsService.ACTION_LISTEN_FOR_ALERTS";
    public static final String ACTION_STOP_LISTENING =
            "io.ratedali.eeese.lifebangle.alerts.AlertsService.ACTION_STOP_LISTENING";

    public static final String ACTION_CONNECTED =
            "io.ratedali.eeese.lifebangle.alerts.AlertsService.ACTION_CONNECTED";
    public static final String ACTION_DISCONNECTED =
            "io.ratedali.eeese.lifebangle.alerts.AlertsService.ACTION_DISCONNECTED";
    public static final String ACTION_DISCONNECTED_ALL =
            "io.ratedali.eeese.lifebangle.alerts.AlertsService.ACTION_DISCONNECTED_ALL";

    public static final String ACTION_RECIEVED_ALERT =
            "io.ratedali.eeese.lifebangle.alerts.AlertsService.ACTION_RECEIVED_ALERT";

    public static final String EXTRA_ALERT_SENDER =
            "io.ratedali.eeese.lifebangle.alerts.AlertsService.EXTRA_ALERT_SENDER";
    public static final String EXTRA_ALERT_HEART_RATE =
            "io.ratedali.eeese.lifebangle.alerts.AlertsService.EXTRA_ALERT_HEART_RATE";

    private static final String TAG = AlertsService.class.getName();
    private static final int ALERTS_NOTIFICATION_ID = 100;

    private static final int BUFFER_SIZE = 128;
    private static final String SEPRATOR = "\n";

    private CompositeDisposable mAlertsListeners;

    public AlertsService() {
        mAlertsListeners = new CompositeDisposable();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        if (action.equals(ACTION_STOP_LISTENING)) {
            stopListening();

        } else if (action.equals(ACTION_LISTEN_FOR_ALERTS)) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            startListening(device);
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mAlertsListeners.clear();
    }

    private void startListening(BluetoothDevice device) {
        listeningNotification();
        Intent connectionIntent = new Intent();
        connectionIntent.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
        Disposable alertsSubscription = deviceAlerts(device)
                .subscribeOn(Schedulers.io())
                .subscribe(alert -> {
                            Intent receivedAlert = new Intent(ACTION_RECIEVED_ALERT);
                            receivedAlert.putExtra(EXTRA_ALERT_SENDER, alert.getSender());
                            receivedAlert.putExtra(EXTRA_ALERT_HEART_RATE, alert.getHeartRate());
                            sendBroadcast(receivedAlert);
                        },
                        // On Error
                        throwable -> announceConnectionChange(ACTION_DISCONNECTED, device),
                        // On Complete
                        () -> announceConnectionChange(ACTION_DISCONNECTED, device)
                );
        mAlertsListeners.add(alertsSubscription);
    }

    private void stopListening() {
        mAlertsListeners.clear();
        stopForeground(true);
        sendBroadcast(new Intent(ACTION_DISCONNECTED_ALL));
        stopSelf();
    }

    private void listeningNotification() {
        String title = getString(R.string.app_name);
        String content = getString(R.string.listening_for_alerts);
        Intent stopIntent = new Intent(this, AlertsService.class);
        stopIntent.setAction(ACTION_STOP_LISTENING);
        Notification notification = new Notification.Builder(this)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true)
                .setContentIntent(PendingIntent.getService(this, 0, stopIntent, 0))
                .build();
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(ALERTS_NOTIFICATION_ID, notification);
        startForeground(ALERTS_NOTIFICATION_ID, notification);
    }

    private Observable<Alert> deviceAlerts(BluetoothDevice device) {
        return Single.<BluetoothSocket>create(emitter -> {
            // Use the SERIAL Profile and UUID
            BluetoothSocket socket = device.createRfcommSocketToServiceRecord(
                    UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
            socket.connect();
            emitter.setDisposable(Disposables.fromAction(socket::close));

            // Announce connection sucess
            announceConnectionChange(ACTION_CONNECTED, device);

            emitter.onSuccess(socket);

        }).flatMap(socket -> Single.<BufferedReader>create(emitter -> {
            BufferedReader inputStream = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            emitter.setDisposable(Disposables.fromAction(inputStream::close));
            emitter.onSuccess(inputStream);

        })).flatMapObservable(inputStream -> Observable.<Alert>create(emitter -> {
            char[] buffer = new char[BUFFER_SIZE];
            StringBuilder message = new StringBuilder();

            int numChars;
            while (!emitter.isDisposed()
                    && (numChars = inputStream.read(buffer)) != -1) {
                message.append(buffer, 0, numChars);
                /*
                Emit all the received *complete* alerts.
                A complete alerts message is a one that ends in SEPARATOR.
                NOTE: check for disposal again, because the observer might have unsubscribed while
                waiting for data
                 */
                int separatorIndex;
                while (!emitter.isDisposed()
                        && (separatorIndex = message.indexOf(SEPRATOR)) != -1) {
                    // The expected message consists only of the heart rate in bpm
                    int heartRate = Integer.parseInt(message.substring(0, separatorIndex));
                    message.delete(0, separatorIndex + 1);

                    Alert alert = Alert.from(device)
                            .setHearRate(heartRate);
                    emitter.onNext(alert);
                }
            }
            emitter.onComplete();
        }));
    }

    private void announceConnectionChange(String action, BluetoothDevice device) {
        Intent connectionChange = new Intent(action);
        connectionChange.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
        sendBroadcast(connectionChange);
        if (mAlertsListeners.size() == 0) {
            stopListening();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

}
