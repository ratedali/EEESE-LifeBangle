package io.ratedali.eeese.lifebangle.devices;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.ratedali.eeese.lifebangle.R;
import io.ratedali.eeese.lifebangle.SettingsActivity;
import io.ratedali.eeese.lifebangle.listener.AlertsService;

public class AddDeviceActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 0;
    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private static final String TAG = AddDeviceActivity.class.getName();

    private DeviceAdapter mDeviceAdapter;
    private BroadcastReceiver mDeviceDiscovery = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF)
                        == BluetoothAdapter.STATE_ON) {
                    startDiscovery();
                } else {
                    mDeviceAdapter.clearDevices();
                }
            } else if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                mDeviceAdapter.addDevice(device);
            } else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_STARTED)) {
                mDeviceAdapter.clearDevices();
            }
        }
    };
    private BroadcastReceiver mConnectionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(AlertsService.ACTION_CONNECTED)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                mDeviceAdapter.deviceConnected(device);
                Toast.makeText(AddDeviceActivity.this,
                        context.getString(R.string.device_connected, device.getName()),
                        Toast.LENGTH_SHORT)
                        .show();

            } else if (action.equals(AlertsService.ACTION_DISCONNECTED)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                mDeviceAdapter.deviceDisconnected(device);
                Toast.makeText(AddDeviceActivity.this,
                        context.getString(R.string.device_disconnected, device.getName()),
                        Toast.LENGTH_SHORT)
                        .show();
            } else if (action.equals(AlertsService.ACTION_DISCONNECTED_ALL)) {
                mDeviceAdapter.allDevicesDisconnected();
                Toast.makeText(AddDeviceActivity.this,
                        R.string.all_devices_disconnected,
                        Toast.LENGTH_SHORT)
                        .show();
            }
        }
    };
    private BluetoothAdapter mBluetoothAdapter;

    @BindView(R.id.content_view)
    View mContentView;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.device_list)
    RecyclerView deviceList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_device);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BT);
        }


        if (checkLocationPermissions() != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermissions();
        }

        deviceList.setLayoutManager(new LinearLayoutManager(this));
        mDeviceAdapter = new DeviceAdapter(this::listenToDevice);
        deviceList.setAdapter(mDeviceAdapter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_CANCELED) {
            Snackbar.make(mContentView, R.string.bluetooth_not_enabled, Snackbar.LENGTH_LONG)
                    .show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_LOCATION_PERMISSION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startDiscovery();
                } else {
                    finish();
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter discoveryFilter = new IntentFilter();
        discoveryFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        discoveryFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        discoveryFilter.addAction(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mDeviceDiscovery, discoveryFilter);
        startDiscovery();

        IntentFilter connectionsFilter = new IntentFilter();
        connectionsFilter.addAction(AlertsService.ACTION_CONNECTED);
        connectionsFilter.addAction(AlertsService.ACTION_DISCONNECTED);
        connectionsFilter.addAction(AlertsService.ACTION_DISCONNECTED_ALL);
        registerReceiver(mConnectionReceiver, connectionsFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mDeviceDiscovery);
        unregisterReceiver(mConnectionReceiver);
        disableDiscovery();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_add_device, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void startDiscovery() {
        if (mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.startDiscovery();
        }
    }

    private void disableDiscovery() {
        mBluetoothAdapter.cancelDiscovery();
    }

    private void listenToDevice(final BluetoothDevice device) {
        disableDiscovery();
        Intent intent = new Intent(AddDeviceActivity.this, AlertsService.class);
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
        intent.setAction(AlertsService.ACTION_LISTEN_FOR_ALERTS);
        startService(intent);
    }

    private void requestLocationPermissions() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)) {
            Snackbar.make(mContentView,
                    R.string.location_permission_rationale,
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok, view -> requestLocationPermissions());
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
        }
    }

    private int checkLocationPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
    }
}
