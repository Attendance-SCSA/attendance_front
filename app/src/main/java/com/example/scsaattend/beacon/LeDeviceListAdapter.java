package com.example.scsaattend.beacon;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.bluetooth.BluetoothDevice;
import com.example.scsaattend.R;
import java.util.List;

public class LeDeviceListAdapter extends BaseAdapter {
    private List<BluetoothDevice> mLeDevices;
    private LayoutInflater mInflator;

    public LeDeviceListAdapter(Context context, List<BluetoothDevice> devices) {
        super();
        mLeDevices = devices;
        mInflator = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return mLeDevices.size();
    }

    @Override
    public Object getItem(int i) {
        return mLeDevices.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = mInflator.inflate(android.R.layout.simple_list_item_1, null);
        }
        
        BluetoothDevice device = mLeDevices.get(i);
        TextView deviceName = view.findViewById(android.R.id.text1);
        
        String name = device.getName();
        if (name != null && name.length() > 0) {
            deviceName.setText(name);
        } else {
            deviceName.setText("Unknown device");
        }
        
        return view;
    }
}