package com.example.scsaattend.user;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import androidx.fragment.app.Fragment;
import com.example.scsaattend.R;
import com.example.scsaattend.beacon.BeaconScanner;
import com.example.scsaattend.dto.AttendanceInfoResponse;
import com.example.scsaattend.dto.AttendanceRequest;
import com.example.scsaattend.dto.CheckInResponse;
import com.example.scsaattend.dto.CheckOutRequest;
import com.example.scsaattend.network.ApiService;
import com.example.scsaattend.network.RetrofitClient;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TodayAttendanceFragment extends Fragment implements BeaconScanner.BeaconScanCallback {
    private static final String TAG = "TodayAttendanceFragment";
    private BeaconScanner beaconScanner;
    private TextView tvConnectedBeacon;
    private boolean isScanning = false;
    private ApiService apiService;

    // UI 요소
    private View viewStatusIndicator;
    private TextView tvCurrentStatus;
    private TextView tvCheckInTime;
    private TextView tvCheckOutTime;
    private LinearLayout layoutCheckIn;
    private LinearLayout layoutCheckOut;
    private View cardHistory;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_today_attendance, container, false);
        
        apiService = RetrofitClient.getClient().create(ApiService.class);

        // UI 요소 연결
        viewStatusIndicator = view.findViewById(R.id.viewStatusIndicator);
        tvCurrentStatus = view.findViewById(R.id.tvCurrentStatus);
        tvCheckInTime = view.findViewById(R.id.tvCheckInTime);
        tvCheckOutTime = view.findViewById(R.id.tvCheckOutTime);
        cardHistory = view.findViewById(R.id.cardHistory);
        layoutCheckIn = view.findViewById(R.id.layoutCheckIn);
        layoutCheckOut = view.findViewById(R.id.layoutCheckOut);
        
        if (cardHistory != null) cardHistory.setVisibility(View.VISIBLE);
        if (layoutCheckIn != null) layoutCheckIn.setVisibility(View.VISIBLE);
        if (layoutCheckOut != null) layoutCheckOut.setVisibility(View.VISIBLE);
        
        if (tvCheckInTime != null) tvCheckInTime.setText("-");
        if (tvCheckOutTime != null) tvCheckOutTime.setText("-");

        beaconScanner = new BeaconScanner(getContext(), this);

        TextView tvTodayDate = view.findViewById(R.id.tvTodayDate);
        if (tvTodayDate != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy년 MM월 dd일 EEEE", Locale.KOREAN);
            tvTodayDate.setText(sdf.format(new Date()));
        }

        View btnScanBeacon = view.findViewById(R.id.btnScanBeacon);
        tvConnectedBeacon = view.findViewById(R.id.tvConnectedBeacon);
        
        if (tvConnectedBeacon != null) {
            tvConnectedBeacon.setBackgroundResource(0);
            tvConnectedBeacon.setText("");
        }

        if (btnScanBeacon != null) {
            btnScanBeacon.setOnClickListener(v -> {
                if (!isScanning) {
                    if (beaconScanner.checkPermissions(getActivity())) {
                        if (!beaconScanner.isBluetoothEnabled()) {
                            Toast.makeText(getContext(), "블루투스를 켜주세요.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        Toast.makeText(getContext(), "비콘 스캔 시작...", Toast.LENGTH_SHORT).show();
                        beaconScanner.startScan();
                        isScanning = true;
                    } else {
                        Toast.makeText(getContext(), "권한이 필요합니다.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    beaconScanner.stopScan();
                    isScanning = false;
                    Toast.makeText(getContext(), "비콘 스캔 중지됨", Toast.LENGTH_SHORT).show();
                }
            });
        }

        View btnCheckIn = view.findViewById(R.id.btnCheckIn);
        if (btnCheckIn != null) {
            btnCheckIn.setOnClickListener(v -> {
                Log.d(TAG, "Check-in Button Clicked");
                requestCheckIn();
            });
        }

        View btnCheckOut = view.findViewById(R.id.btnCheckOut);
        if (btnCheckOut != null) {
            btnCheckOut.setOnClickListener(v -> {
                Log.d(TAG, "Check-out Button Clicked");
                requestCheckOut();
            });
        }

        fetchTodayAttendance();

        return view;
    }

    private void requestCheckIn() {
        SharedPreferences prefs = requireContext().getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE);
        long ainfoId = prefs.getLong("today_ainfo_id", -1);
        
        Log.d(TAG, "Attempting Check-in with ainfoId: " + ainfoId);

        if (ainfoId == -1) {
            Toast.makeText(getContext(), "오늘의 출결 정보를 불러오지 못했습니다. 잠시 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
            fetchTodayAttendance();
            return;
        }

        apiService.checkIn((int)ainfoId, new Object()).enqueue(new Callback<CheckInResponse>() {
            @Override
            public void onResponse(Call<CheckInResponse> call, Response<CheckInResponse> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Check-in PATCH Success. Reloading attendance info...");
                    fetchTodayAttendance();
                    Toast.makeText(getContext(), "출근 처리되었습니다.", Toast.LENGTH_SHORT).show();
                } else {
                    Log.e(TAG, "Check-in Failed. Code: " + response.code());
                    Toast.makeText(getContext(), "출근 요청 실패", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<CheckInResponse> call, Throwable t) {
                Log.e(TAG, "Check-in Network Error", t);
                Toast.makeText(getContext(), "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void requestCheckOut() {
        SharedPreferences prefs = requireContext().getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE);
        long ainfoId = prefs.getLong("today_ainfo_id", -1);

        Log.d(TAG, "Attempting Check-out with ainfoId: " + ainfoId);

        if (ainfoId == -1) {
            Toast.makeText(getContext(), "오늘의 출결 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
            fetchTodayAttendance();
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
        String currentLeavingTime = sdf.format(new Date());
        
        String macAddress = "00:00:00:00:00:00"; 
        int rssi = 0;
        
        CheckOutRequest request = new CheckOutRequest(currentLeavingTime, macAddress, rssi);

        apiService.checkOut((int)ainfoId, request).enqueue(new Callback<CheckInResponse>() {
            @Override
            public void onResponse(Call<CheckInResponse> call, Response<CheckInResponse> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Check-out PATCH Success. Reloading attendance info...");
                    fetchTodayAttendance();
                    Toast.makeText(getContext(), "퇴근 처리되었습니다.", Toast.LENGTH_SHORT).show();
                } else {
                    Log.e(TAG, "Check-out Failed. Code: " + response.code());
                    Toast.makeText(getContext(), "퇴근 요청 실패", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<CheckInResponse> call, Throwable t) {
                Log.e(TAG, "Check-out Network Error", t);
                Toast.makeText(getContext(), "네트워크 오류", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchTodayAttendance() {
        SharedPreferences prefs = requireContext().getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE);
        long memId = prefs.getLong("user_numeric_id", -1);
        
        Log.d(TAG, "fetchTodayAttendance Called. user_numeric_id: " + memId);

        if (memId == -1) {
             Log.e(TAG, "Invalid user_numeric_id in SharedPrefs");
             return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayDate = sdf.format(new Date());
        
        List<Long> memIdList = new ArrayList<>();
        memIdList.add(memId);

        AttendanceRequest request = new AttendanceRequest(todayDate, todayDate, memIdList);

        apiService.searchAttendance(request).enqueue(new Callback<List<AttendanceInfoResponse>>() {
            @Override
            public void onResponse(Call<List<AttendanceInfoResponse>> call, Response<List<AttendanceInfoResponse>> response) {
                Log.d(TAG, "API Response Code: " + response.code());
                if (response.isSuccessful() && response.body() != null) {
                    List<AttendanceInfoResponse> list = response.body();
                    Log.d(TAG, "API Response Body Size: " + list.size());
                    
                    if (!list.isEmpty()) {
                        AttendanceInfoResponse attendance = list.get(0);
                        
                        Log.d(TAG, "Attendance Found. ainfoId: " + attendance.getAinfoId() + 
                              ", arrivalTime: " + attendance.getArrivalTime());

                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putLong("today_ainfo_id", attendance.getAinfoId()).apply();
                        
                        updateAttendanceUI(attendance);
                    } else {
                        Log.d(TAG, "Response List is Empty");
                        updateUIAsAbsent();
                    }
                } else {
                    Log.e(TAG, "Response Not Successful");
                    updateUIAsAbsent();
                }
            }

            @Override
            public void onFailure(Call<List<AttendanceInfoResponse>> call, Throwable t) {
                Log.e(TAG, "API Call Failed", t);
                updateUIAsAbsent();
            }
        });
    }

    private void updateAttendanceUI(AttendanceInfoResponse attendance) {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            String arrivalTime = attendance.getArrivalTime();
            String leavingTime = attendance.getLeavingTime();

            if (cardHistory != null) cardHistory.setVisibility(View.VISIBLE);
            if (layoutCheckIn != null) layoutCheckIn.setVisibility(View.VISIBLE);
            if (layoutCheckOut != null) layoutCheckOut.setVisibility(View.VISIBLE);

            if (tvCheckInTime != null) {
                tvCheckInTime.setText(arrivalTime != null ? arrivalTime : "출근 정보 없음");
            }
            if (tvCheckOutTime != null) {
                tvCheckOutTime.setText(leavingTime != null ? leavingTime : "퇴근 정보 없음");
            }

            if (arrivalTime == null) {
                if (viewStatusIndicator != null) {
                    viewStatusIndicator.setBackgroundTintList(
                        requireContext().getResources().getColorStateList(android.R.color.darker_gray, null)
                    );
                }
                if (tvCurrentStatus != null) {
                    tvCurrentStatus.setText("미출근");
                }
            } else {
                if (viewStatusIndicator != null) {
                    viewStatusIndicator.setBackgroundTintList(
                        requireContext().getResources().getColorStateList(R.color.attendance_light_green, null)
                    );
                }
                if (tvCurrentStatus != null) {
                    tvCurrentStatus.setText("출근 완료");
                }
            }
        });
    }

    private void updateUIAsAbsent() {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            if (viewStatusIndicator != null) {
                viewStatusIndicator.setBackgroundTintList(
                    requireContext().getResources().getColorStateList(android.R.color.darker_gray, null)
                );
            }
            if (tvCurrentStatus != null) {
                tvCurrentStatus.setText("정보 없음");
            }
            
            if (cardHistory != null) cardHistory.setVisibility(View.VISIBLE);
            if (layoutCheckIn != null) layoutCheckIn.setVisibility(View.VISIBLE);
            if (layoutCheckOut != null) layoutCheckOut.setVisibility(View.VISIBLE);
            
            if (tvCheckInTime != null) tvCheckInTime.setText("출근 정보 없음");
            if (tvCheckOutTime != null) tvCheckOutTime.setText("퇴근 정보 없음");
        });
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (beaconScanner != null) {
            beaconScanner.stopScan();
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @Override
    public void onBeaconFound(BluetoothDevice device, int rssi, byte[] scanRecord) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (tvConnectedBeacon != null) {
                    String deviceName = device.getName();
                    if (deviceName == null || deviceName.isEmpty()) deviceName = "Unknown";
                    
                    tvConnectedBeacon.setBackgroundResource(R.drawable.edit_text_border);
                    tvConnectedBeacon.setBackgroundTintList(
                        getContext().getResources().getColorStateList(R.color.attendance_light_green, null)
                    );
                    tvConnectedBeacon.setText("연결된 비콘: " + deviceName + "\n(RSSI: " + rssi + ")");
                    beaconScanner.stopScan();
                    isScanning = false;
                }
            });
        }
    }

    @Override
    public void onScanFailed(int errorCode) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                Toast.makeText(getContext(), "스캔 실패: " + errorCode, Toast.LENGTH_LONG).show();
                isScanning = false;
            });
        }
    }
}
