package com.example.scsaattend.beacon;

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
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
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

    // 타겟 비콘 MAC 주소 (원하는 비콘 주소로 변경 필요)
    private static final String TARGET_BEACON_ADDRESS = "C3:00:00:1C:65:03"; 

    public interface BeaconScanCallback {
        void onBeaconFound(BluetoothDevice device, int rssi, byte[] scanRecord);
        void onScanFailed(int errorCode);
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
    
    // 블루투스 활성화 확인
    public boolean isBluetoothEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    // 권한 확인 및 요청
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
        
        // 스캔 필터 설정 (특정 MAC 주소만 검색)
        // 주의: 일부 기기에서는 MAC 주소 필터링이 제대로 동작하지 않을 수 있어, 필터 없이 스캔 후 onScanResult에서 직접 필터링하는 방식도 고려해야 합니다.
        // 여기서는 일단 필터를 적용하되, 필터링 문제 가능성을 염두에 둡니다.
        List<ScanFilter> filters = new ArrayList<>();
        if (TARGET_BEACON_ADDRESS != null && !TARGET_BEACON_ADDRESS.isEmpty()) {
            ScanFilter filter = new ScanFilter.Builder()
                    .setDeviceAddress(TARGET_BEACON_ADDRESS)
                    .build();
            filters.add(filter);
        }

        // 스캔 설정: 저지연 모드 (빠른 검색)
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        bluetoothLeScanner.startScan(filters, settings, scanCallback);
    }

    @SuppressLint("MissingPermission")
    public void stopScan() {
        if (bluetoothLeScanner != null) {
            Log.d(TAG, "Stop Scan");
            try {
                bluetoothLeScanner.stopScan(scanCallback);
            } catch (IllegalStateException e) {
                // 이미 스캔이 중지되었거나 블루투스가 꺼진 경우 등 예외 처리
                Log.e(TAG, "Stop scan failed: " + e.getMessage());
            }
        }
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            
            BluetoothDevice device = result.getDevice();
            if (device == null) return;
            
            String deviceAddress = device.getAddress();
            String deviceName = device.getName();
            int rssi = result.getRssi();

            // 모든 검색된 기기 로그 출력 (디버깅용)
            Log.d(TAG, "Scanned Device -> Name: " + deviceName + ", Mac: " + deviceAddress + ", RSSI: " + rssi);
            
            // 타겟 비콘인지 확인 (필터가 적용되어 있어도 이중 확인)
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
            if (beaconScanCallback != null) {
                beaconScanCallback.onScanFailed(errorCode);
            }
        }
    };
}