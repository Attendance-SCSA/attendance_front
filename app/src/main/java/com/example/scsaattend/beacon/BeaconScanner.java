package com.example.scsaattend.beacon;

import static com.example.scsaattend.common.Config.TARGET_BEACON_ADDRESS;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresPermission;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class BeaconScanner {
    private static final String TAG = "BeaconScanner";
    private Context context;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private BeaconScanCallback beaconScanCallback;
    private static final long SCAN_PERIOD = 10000; // 10초 스캔

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable timeoutRunnable;
    private boolean isScanningInternal = false;

    public interface BeaconScanCallback {
        void onBeaconFound(BluetoothDevice device, int rssi, byte[] scanRecord);
        void onScanFailed(int errorCode);
        void onScanTimeout();
    }

    public BeaconScanner(Context context, BeaconScanCallback callback) {
        this.context = context;
        this.beaconScanCallback = callback;
        
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            bluetoothAdapter = bluetoothManager.getAdapter();
            if (bluetoothAdapter != null) {
                bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
            }
        }
    }
    
    public boolean isBluetoothEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    public boolean checkPermissions(Activity activity) {
        String[] permissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions = new String[]{
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION
            };
        } else {
            permissions = new String[]{
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION
            };
        }

        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String p : permissions) {
            if (ContextCompat.checkSelfPermission(context, p) != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }

        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(activity, 
                listPermissionsNeeded.toArray(new String[0]), 
                1001);
            return false;
        }
        return true;
    }

    @SuppressLint("MissingPermission")
    public void startScan() {
        if (bluetoothLeScanner == null) {
            Toast.makeText(context, "블루투스 스캐너를 사용할 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Start Scan - Target: " + TARGET_BEACON_ADDRESS);
        
        if (timeoutRunnable != null) {
            handler.removeCallbacks(timeoutRunnable);
        }

        timeoutRunnable = () -> {
            if (isScanningInternal) {
                Log.d(TAG, "Scan timeout reached");
                stopScan();
                if (beaconScanCallback != null) {
                    beaconScanCallback.onScanTimeout();
                }
            }
        };
        handler.postDelayed(timeoutRunnable, SCAN_PERIOD);

        List<ScanFilter> filters = new ArrayList<>();
        if (TARGET_BEACON_ADDRESS != null && !TARGET_BEACON_ADDRESS.isEmpty()) {
            ScanFilter filter = new ScanFilter.Builder()
                    .setDeviceAddress(TARGET_BEACON_ADDRESS)
                    .build();
            filters.add(filter);
        }

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        isScanningInternal = true;
        bluetoothLeScanner.startScan(filters, settings, scanCallback);
    }

    @SuppressLint("MissingPermission")
    public void stopScan() {
        if (timeoutRunnable != null) {
            handler.removeCallbacks(timeoutRunnable);
            timeoutRunnable = null;
        }

        if (bluetoothLeScanner != null && isScanningInternal) {
            Log.d(TAG, "Stop Scan");
            try {
                bluetoothLeScanner.stopScan(scanCallback);
            } catch (Exception e) {
                Log.e(TAG, "Stop scan failed: " + e.getMessage());
            }
            isScanningInternal = false;
        }
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            
            BluetoothDevice device = result.getDevice();
            if (device == null) return;
            
            String deviceAddress = device.getAddress();
            int rssi = result.getRssi();

            if (TARGET_BEACON_ADDRESS.equalsIgnoreCase(deviceAddress)) {
                 Log.d(TAG, "!!! TARGET BEACON FOUND !!!");
                 
                 if (beaconScanCallback != null) {
                    byte[] scanRecord = result.getScanRecord() != null ? result.getScanRecord().getBytes() : null;
                    beaconScanCallback.onBeaconFound(device, rssi, scanRecord);
                }
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.e(TAG, "Scan Failed Error Code: " + errorCode);
            isScanningInternal = false;
            if (beaconScanCallback != null) {
                beaconScanCallback.onScanFailed(errorCode);
            }
        }
    };
}