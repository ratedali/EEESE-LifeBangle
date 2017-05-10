package io.ratedali.eeese.lifebangle.listener;

import android.bluetooth.BluetoothDevice;
import android.support.annotation.NonNull;

public class Alert {
    private BluetoothDevice sender;
    private int heartRate;

    private Alert(BluetoothDevice sender, int heartRate) {
        this.sender = sender;
        this.heartRate = heartRate;
    }

    @NonNull
    public static Alert from(BluetoothDevice sender) {
        return new Alert(sender, -1);
    }

    public Alert setHearRate(int hearRate) {
        this.heartRate = hearRate;
        return this;
    }

    public BluetoothDevice getSender() {
        return sender;
    }

    public int getHeartRate() {
        if (heartRate == -1) {
            throw new
                    IllegalStateException("The heart rate should be set using Alert.setHeartRate");
        }
        return heartRate;
    }
}
