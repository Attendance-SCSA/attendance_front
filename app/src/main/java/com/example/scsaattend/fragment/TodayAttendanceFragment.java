package com.example.scsaattend.fragment;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.example.scsaattend.R;
import com.example.scsaattend.beacon.BeaconScanner;
import com.example.scsaattend.common.Config;
import com.example.scsaattend.dto.AttendanceInfoResponse;
import com.example.scsaattend.dto.CheckInApiResponse;
import com.example.scsaattend.dto.CheckOutRequest;
import com.example.scsaattend.dto.ErrorResponse;
import com.example.scsaattend.dto.SearchAttendanceRequest;
import com.example.scsaattend.network.ApiService;
import com.example.scsaattend.network.RetrofitClient;
import com.google.gson.Gson;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
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
    private TextView tvCheckInStatus, tvCheckInTime, tvCheckOutStatus, tvCheckOutTime, tvClassTime;

    private String lastScannedMacAddress = null;
    private int lastScannedRssi = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_today_attendance, container, false);

        apiService = RetrofitClient.getClient().create(ApiService.class);

        tvCheckInStatus = view.findViewById(R.id.tv_check_in_status);
        tvCheckInTime = view.findViewById(R.id.tv_check_in_time);
        tvCheckOutStatus = view.findViewById(R.id.tv_check_out_status);
        tvCheckOutTime = view.findViewById(R.id.tv_check_out_time);
        tvClassTime = view.findViewById(R.id.tv_class_time);

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
                if (Config.IS_USING_BEACON && lastScannedMacAddress == null) {
                    Toast.makeText(getContext(), "비콘을 먼저 스캔해주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }
                requestCheckIn();
            });
        }

        View btnCheckOut = view.findViewById(R.id.btnCheckOut);
        if (btnCheckOut != null) {
            btnCheckOut.setOnClickListener(v -> {
                if (Config.IS_USING_BEACON && lastScannedMacAddress == null) {
                    Toast.makeText(getContext(), "비콘을 먼저 스캔해주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }
                requestCheckOut();
            });
        }

        fetchTodayAttendance();

        return view;
    }

    private void requestCheckIn() {
        SharedPreferences prefs = requireContext().getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE);
        long ainfoId = prefs.getLong("today_ainfo_id", -1);

        if (ainfoId != -1) {
            performCheckIn((int)ainfoId);
        } else {
            fetchAttendanceAndCheckIn();
        }
    }

    private void fetchAttendanceAndCheckIn() {
        SharedPreferences prefs = requireContext().getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE);
        long memId = prefs.getLong("user_numeric_id", -1);

        if (memId == -1) {
            Toast.makeText(getContext(), "로그인 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayDate = sdf.format(new Date());

        List<Long> memIdList = new ArrayList<>();
        memIdList.add(memId);

        SearchAttendanceRequest request = new SearchAttendanceRequest(todayDate, todayDate, memIdList);

        apiService.searchAttendance(request).enqueue(new Callback<List<AttendanceInfoResponse>>() {
            @Override
            public void onResponse(Call<List<AttendanceInfoResponse>> call, Response<List<AttendanceInfoResponse>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    AttendanceInfoResponse attendance = response.body().get(0);
                    long newAinfoId = attendance.getAinfoId();
                    prefs.edit().putLong("today_ainfo_id", newAinfoId).apply();
                    updateAttendanceUI(attendance);
                    performCheckIn((int)newAinfoId);
                } else {
                    Toast.makeText(getContext(), "오늘의 출결 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<AttendanceInfoResponse>> call, Throwable t) {
                Toast.makeText(getContext(), "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void performCheckIn(int ainfoId) {
        apiService.checkIn(ainfoId, new Object()).enqueue(new Callback<CheckInApiResponse>() {
            @Override
            public void onResponse(Call<CheckInApiResponse> call, Response<CheckInApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(getContext(), "출근 처리되었습니다.", Toast.LENGTH_SHORT).show();
                    fetchTodayAttendance(); // Refresh UI
                } else {
                    handleApiError(response, "출근 요청 실패");
                }
            }

            @Override
            public void onFailure(Call<CheckInApiResponse> call, Throwable t) {
                Toast.makeText(getContext(), "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void requestCheckOut() {
        SharedPreferences prefs = requireContext().getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE);
        long ainfoId = prefs.getLong("today_ainfo_id", -1);

        if (ainfoId == -1) {
            Toast.makeText(getContext(), "오늘의 출결 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
            fetchTodayAttendance();
            return;
        }

        if (Config.IS_USING_BEACON && lastScannedMacAddress == null) {
             Toast.makeText(getContext(), "비콘 스캔 정보가 없습니다. 비콘을 먼저 스캔해주세요.", Toast.LENGTH_SHORT).show();
             return;
        }

        String currentLeavingTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(new Date());
        CheckOutRequest request = new CheckOutRequest(currentLeavingTime, lastScannedMacAddress, lastScannedRssi);

        apiService.checkOut((int)ainfoId, request).enqueue(new Callback<CheckInApiResponse>() {
            @Override
            public void onResponse(Call<CheckInApiResponse> call, Response<CheckInApiResponse> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "퇴근 처리되었습니다.", Toast.LENGTH_SHORT).show();
                    fetchTodayAttendance(); // Refresh UI
                } else {
                     handleApiError(response, "퇴근 요청 실패");
                }
            }

            @Override
            public void onFailure(Call<CheckInApiResponse> call, Throwable t) {
                Toast.makeText(getContext(), "네트워크 오류", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchTodayAttendance() {
        SharedPreferences prefs = requireContext().getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE);
        long memId = prefs.getLong("user_numeric_id", -1);

        if (memId == -1) {
            updateUIAsNotEntered();
            return;
        }

        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        List<Long> memIdList = new ArrayList<>();
        memIdList.add(memId);
        SearchAttendanceRequest request = new SearchAttendanceRequest(todayDate, todayDate, memIdList);

        apiService.searchAttendance(request).enqueue(new Callback<List<AttendanceInfoResponse>>() {
            @Override
            public void onResponse(Call<List<AttendanceInfoResponse>> call, Response<List<AttendanceInfoResponse>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    AttendanceInfoResponse attendance = response.body().get(0);
                    prefs.edit().putLong("today_ainfo_id", attendance.getAinfoId()).apply();
                    updateAttendanceUI(attendance);
                } else {
                    updateUIAsNotEntered();
                }
            }

            @Override
            public void onFailure(Call<List<AttendanceInfoResponse>> call, Throwable t) {
                updateUIAsNotEntered();
            }
        });
    }

    private void updateAttendanceUI(AttendanceInfoResponse attendance) {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            String arrivalTimeStr = attendance.getArrivalTime();
            String leavingTimeStr = attendance.getLeavingTime();
            
            if (attendance.getAttendanceType() == null) {
                updateUIAsNotEntered(); // 출결 유형 정보가 없으면 처리 불가
                return;
            }
            String classStartTimeStr = attendance.getAttendanceType().getStartTime();
            String classEndTimeStr = attendance.getAttendanceType().getEndTime();

            // 오늘의 수업 정보 업데이트
            tvClassTime.setText("수업 시간 : " + classStartTimeStr + " ~ " + classEndTimeStr);

            // 출근 상태 업데이트
            if (arrivalTimeStr != null) {
                tvCheckInTime.setText(formatTime(arrivalTimeStr));
                try {
                    LocalTime arrivalTime = LocalTime.parse(arrivalTimeStr.substring(11, 19));
                    LocalTime classStartTime = LocalTime.parse(classStartTimeStr);
                    if (arrivalTime.isAfter(classStartTime)) {
                        tvCheckInStatus.setText("지각");
                        tvCheckInStatus.setTextColor(ContextCompat.getColor(getContext(), R.color.status_text_late_early));
                    } else {
                        tvCheckInStatus.setText("입실");
                        tvCheckInStatus.setTextColor(ContextCompat.getColor(getContext(), R.color.status_text_normal));
                    }
                } catch (Exception e) {
                     tvCheckInStatus.setText("오류");
                }
            } else {
                tvCheckInStatus.setText("미입력");
                tvCheckInTime.setText("--:--");
                tvCheckInStatus.setTextColor(ContextCompat.getColor(getContext(), android.R.color.darker_gray));
            }

            // 퇴근 상태 업데이트
            if (leavingTimeStr != null) {
                tvCheckOutTime.setText(formatTime(leavingTimeStr));
                try {
                    LocalTime leavingTime = LocalTime.parse(leavingTimeStr.substring(11, 19));
                    LocalTime classEndTime = LocalTime.parse(classEndTimeStr);
                    if (leavingTime.isBefore(classEndTime)) {
                        tvCheckOutStatus.setText("조퇴");
                        tvCheckOutStatus.setTextColor(ContextCompat.getColor(getContext(), R.color.status_text_late_early));
                    } else {
                        tvCheckOutStatus.setText("퇴실");
                        tvCheckOutStatus.setTextColor(ContextCompat.getColor(getContext(), R.color.status_text_normal));
                    }
                } catch (Exception e) {
                    tvCheckOutStatus.setText("오류");
                }
            } else {
                tvCheckOutStatus.setText("미입력");
                tvCheckOutTime.setText("--:--");
                tvCheckOutStatus.setTextColor(ContextCompat.getColor(getContext(), android.R.color.darker_gray));
            }
        });
    }

    private void updateUIAsNotEntered() {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            tvClassTime.setText("수업 시간 : 정보 없음");
            tvCheckInStatus.setText("미입력");
            tvCheckInTime.setText("--:--");
            tvCheckOutStatus.setText("미입력");
            tvCheckOutTime.setText("--:--");
            tvCheckInStatus.setTextColor(ContextCompat.getColor(getContext(), android.R.color.darker_gray));
            tvCheckOutStatus.setTextColor(ContextCompat.getColor(getContext(), android.R.color.darker_gray));
        });
    }

    private void handleApiError(Response<?> response, String defaultMessage) {
        String errorMessage = defaultMessage;
        if (response.errorBody() != null) {
            try {
                String errorBodyString = response.errorBody().string();
                Gson gson = new Gson();
                ErrorResponse errorResponse = gson.fromJson(errorBodyString, ErrorResponse.class);
                if (errorResponse != null && errorResponse.getMessage() != null) {
                    errorMessage = errorResponse.getMessage();
                }
            } catch (IOException e) {
                Log.e(TAG, "Error parsing error body", e);
            }
        }
        Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
    }

    private String formatTime(String dateTime) {
        if (dateTime == null || dateTime.length() < 16) return "--:--";
        return dateTime.substring(11, 16);
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
                    tvConnectedBeacon.setBackgroundTintList(getContext().getResources().getColorStateList(R.color.attendance_light_green, null));
                    tvConnectedBeacon.setText("연결된 비콘: " + deviceName + "\n(RSSI: " + rssi + ")");
                    beaconScanner.stopScan();
                    isScanning = false;
                    lastScannedMacAddress = device.getAddress();
                    lastScannedRssi = rssi;
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
