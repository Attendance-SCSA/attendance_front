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
        // 간단한 구현: 여기서는 상세한 레이아웃을 사용하지 않고 기본적인 TextView만 사용한다고 가정하거나
        // 실제로는 별도의 layout 리소스를 만들어야 합니다.
        // 하지만 사용자가 제공한 코드에는 `R.layout.listitem_device` 같은 것이 없으므로
        // 안드로이드 기본 레이아웃을 사용하거나 간단히 구현합니다.
        
        // 여기서는 임시로 간단한 텍스트뷰를 반환하도록 합니다.
        // 실제 스캔 목록을 다이얼로그나 리스트뷰로 보여줄 때 필요합니다.
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