package io.ratedali.eeese.lifebangle.devices;

import android.bluetooth.BluetoothDevice;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.ratedali.eeese.lifebangle.R;


class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.ViewHolder> {

    private List<BluetoothDevice> mDevices;
    private List<BluetoothDevice> connectedDevices;
    private OnDeviceSelectedListener mListener;

    private int NORMAL_TYPE = 0;
    private int CONNECTED_TYPE = 1;

    DeviceAdapter(OnDeviceSelectedListener onDeviceSelectedListener) {
        mDevices = new ArrayList<>();
        mListener = onDeviceSelectedListener;
        connectedDevices = new LinkedList<>();
    }

    @Override
    public int getItemViewType(int position) {
        BluetoothDevice device = mDevices.get(position);
        if (connectedDevices.contains(device)) {
            return CONNECTED_TYPE;
        } else {
            return NORMAL_TYPE;
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View itemView;
        if (viewType == NORMAL_TYPE) {
            itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.device_list_item, parent, false);
        } else {
            itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.connected_device_list_item, parent, false);
        }
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.deviceName.setText(mDevices.get(position).getName());
        holder.itemView.setOnClickListener(view -> {
            BluetoothDevice device = mDevices.get(holder.getAdapterPosition());
            mListener.onDeviceSelected(device);
        });
    }

    @Override
    public int getItemCount() {
        return mDevices.size();
    }

    void addDevice(BluetoothDevice device) {
        if(!mDevices.contains(device)) {
            mDevices.add(device);
            notifyItemInserted(mDevices.size() - 1);
        }
    }

    void deviceConnected(BluetoothDevice device) {
        connectedDevices.add(device);
        notifyItemChanged(mDevices.indexOf(device));
    }

    void deviceDisconnected(BluetoothDevice device) {
        connectedDevices.remove(device);
        notifyItemChanged(mDevices.indexOf(device));
    }

    void allDevicesDisconnected() {
        connectedDevices.clear();
        notifyItemRangeChanged(0, mDevices.size());
    }

    void clearDevices() {
        int numOfDevices = mDevices.size();
        mDevices.clear();
        connectedDevices.clear();
        notifyItemRangeRemoved(0, numOfDevices);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.device_name)
        TextView deviceName;

        ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    interface OnDeviceSelectedListener {
        void onDeviceSelected(BluetoothDevice device);
    }
}
