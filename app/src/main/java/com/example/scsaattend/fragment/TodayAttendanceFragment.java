package com.example.scsaattend.fragment;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
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
    private Button btnScanBeacon;
    private boolean isScanning = false;
    private ApiService apiService;

    private TextView tvCheckInStatus, tvCheckInTime, tvCheckOutStatus, tvCheckOutTime, tvClassTime;

    private String lastScannedMacAddress = null;
    private int lastScannedRssi = 0;

    private static final int COLOR_DISABLED = Color.parseColor("#BDBDBD");

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
        btnScanBeacon = view.findViewById(R.id.btnScanBeacon);
        tvConnectedBeacon = view.findViewById(R.id.tvConnectedBeacon);

        beaconScanner = new BeaconScanner(getContext(), this);

        TextView tvTodayDate = view.findViewById(R.id.tvTodayDate);
        if (tvTodayDate != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy년 MM월 dd일 EEEE", Locale.KOREAN);
            tvTodayDate.setText(sdf.format(new Date()));
        }

        if (btnScanBeacon != null) {
            btnScanBeacon.setOnClickListener(v -> {
                if (!isScanning) {
                    if (beaconScanner.checkPermissions(getActivity())) {
                        if (!beaconScanner.isBluetoothEnabled()) {
                            showFullMessage("블루투스를 켜주세요.");
                            return;
                        }
                        startBeaconScanUI();
                        beaconScanner.startScan();
                        isScanning = true;
                    } else {
                        showFullMessage("권한이 필요합니다.");
                    }
                } else {
                    stopBeaconScanUI();
                    beaconScanner.stopScan();
                    isScanning = false;
                }
            });
        }

        View btnCheckIn = view.findViewById(R.id.btnCheckIn);
        if (btnCheckIn != null) {
            btnCheckIn.setOnClickListener(v -> requestCheckIn());
        }

        View btnCheckOut = view.findViewById(R.id.btnCheckOut);
        if (btnCheckOut != null) {
            btnCheckOut.setOnClickListener(v -> requestCheckOut());
        }

        fetchTodayAttendance();

        return view;
    }

    private void showFullMessage(String message) {
        if (getView() != null) {
            Snackbar snackbar = Snackbar.make(getView(), message, Snackbar.LENGTH_LONG);
            View snackbarView = snackbar.getView();
            TextView textView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
            textView.setMaxLines(5); 
            snackbar.show();
        } else {
            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
        }
    }

    private void startBeaconScanUI() {
        btnScanBeacon.setText("비콘 스캔중");
        btnScanBeacon.setEnabled(false);
        btnScanBeacon.setBackgroundTintList(ColorStateList.valueOf(COLOR_DISABLED));
    }

    private void stopBeaconScanUI() {
        btnScanBeacon.setText("비콘 스캔하기");
        btnScanBeacon.setEnabled(true);
        btnScanBeacon.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.beacon_blue)));
    }

    // BeaconScanCallback 구현
    @Override
    public void onBeaconFound(BluetoothDevice device, int rssi, byte[] scanRecord) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                showFullMessage("비콘을 찾았습니다!");
                tvConnectedBeacon.setBackgroundResource(R.drawable.edit_text_border);
                tvConnectedBeacon.setBackgroundTintList(getContext().getResources().getColorStateList(R.color.attendance_light_green, null));
                tvConnectedBeacon.setText("연결된 비콘: " + device.getName() + " (" + device.getAddress() + ")");
                
                lastScannedMacAddress = device.getAddress();
                lastScannedRssi = rssi;
                isScanning = false;
                stopBeaconScanUI();
            });
        }
    }

    @Override
    public void onScanFailed(int errorCode) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                showFullMessage("비콘 스캔 실패 (에러코드: " + errorCode + ")");
                isScanning = false;
                stopBeaconScanUI();
            });
        }
    }

    @Override
    public void onScanTimeout() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                showFullMessage("10초 동안 비콘을 찾지 못했습니다. 위치를 확인해주세요.");
                isScanning = false;
                stopBeaconScanUI();
            });
        }
    }

    private void requestCheckIn() {
        if (Config.IS_USING_BEACON && lastScannedMacAddress == null) {
            showFullMessage("비콘을 먼저 스캔해주세요.");
            return;
        }
        SharedPreferences prefs = requireContext().getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE);
        long ainfoId = prefs.getLong("today_ainfo_id", -1);
        if (ainfoId != -1) performCheckIn((int)ainfoId);
        else fetchTodayAttendance();
    }

    private void performCheckIn(int ainfoId) {
        apiService.checkIn(ainfoId, new Object()).enqueue(new Callback<CheckInApiResponse>() {
            @Override
            public void onResponse(Call<CheckInApiResponse> call, Response<CheckInApiResponse> response) {
                if (response.isSuccessful()) {
                    showFullMessage("출근 처리가 완료되었습니다.");
                    fetchTodayAttendance();
                } else { handleApiError(response, "출근 실패"); }
            }
            @Override
            public void onFailure(Call<CheckInApiResponse> call, Throwable t) { showFullMessage("네트워크 오류"); }
        });
    }

    private void requestCheckOut() {
        if (Config.IS_USING_BEACON && lastScannedMacAddress == null) {
            showFullMessage("비콘을 먼저 스캔해주세요.");
            return;
        }
        SharedPreferences prefs = requireContext().getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE);
        long ainfoId = prefs.getLong("today_ainfo_id", -1);
        if (ainfoId == -1) { fetchTodayAttendance(); return; }

        String time = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(new Date());
        CheckOutRequest request = new CheckOutRequest(time, lastScannedMacAddress, lastScannedRssi);

        apiService.checkOut((int)ainfoId, request).enqueue(new Callback<CheckInApiResponse>() {
            @Override
            public void onResponse(Call<CheckInApiResponse> call, Response<CheckInApiResponse> response) {
                if (response.isSuccessful()) {
                    showFullMessage("퇴근 처리가 완료되었습니다.");
                    fetchTodayAttendance();
                } else { handleApiError(response, "퇴근 실패"); }
            }
            @Override
            public void onFailure(Call<CheckInApiResponse> call, Throwable t) { showFullMessage("네트워크 오류"); }
        });
    }

    private void fetchTodayAttendance() {
        SharedPreferences prefs = requireContext().getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE);
        long memId = prefs.getLong("user_numeric_id", -1);
        if (memId == -1) return;

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        apiService.searchAttendance(new SearchAttendanceRequest(today, today, List.of(memId))).enqueue(new Callback<List<AttendanceInfoResponse>>() {
            @Override
            public void onResponse(Call<List<AttendanceInfoResponse>> call, Response<List<AttendanceInfoResponse>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    AttendanceInfoResponse res = response.body().get(0);
                    prefs.edit().putLong("today_ainfo_id", res.getAinfoId()).apply();
                    updateAttendanceUI(res);
                }
            }
            @Override
            public void onFailure(Call<List<AttendanceInfoResponse>> call, Throwable t) {}
        });
    }

    private void updateAttendanceUI(AttendanceInfoResponse res) {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            if (res.getAttendanceType() == null) return;
            tvClassTime.setText("수업 시간 : " + formatToHm(res.getAttendanceType().getStartTime()) + " ~ " + formatToHm(res.getAttendanceType().getEndTime()));

            if (res.getArrivalTime() != null) {
                tvCheckInTime.setText(formatToHms(res.getArrivalTime()));
                tvCheckInStatus.setText(LocalTime.parse(res.getArrivalTime().substring(11, 19)).isAfter(LocalTime.parse(res.getAttendanceType().getStartTime())) ? "지각" : "입실");
                tvCheckInStatus.setTextColor(ContextCompat.getColor(getContext(), "지각".equals(tvCheckInStatus.getText()) ? R.color.status_text_late_early : R.color.status_text_normal));
            }
            if (res.getLeavingTime() != null) {
                tvCheckOutTime.setText(formatToHms(res.getLeavingTime()));
                tvCheckOutStatus.setText(LocalTime.parse(res.getLeavingTime().substring(11, 19)).isBefore(LocalTime.parse(res.getAttendanceType().getEndTime())) ? "조퇴" : "퇴실");
                tvCheckOutStatus.setTextColor(ContextCompat.getColor(getContext(), "조퇴".equals(tvCheckOutStatus.getText()) ? R.color.status_text_late_early : R.color.status_text_normal));
            }
        });
    }

    private void handleApiError(Response<?> response, String defaultMessage) {
        String msg = defaultMessage;
        if (response.errorBody() != null) {
            try {
                ErrorResponse err = new Gson().fromJson(response.errorBody().string(), ErrorResponse.class);
                if (err != null && err.getMessage() != null) msg = err.getMessage();
            } catch (IOException ignored) {}
        }
        showFullMessage(msg);
    }

    private String formatToHm(String t) { return (t == null || t.length() < 5) ? "--:--" : t.substring(0, 5); }
    private String formatToHms(String dt) { return (dt == null || dt.length() < 19) ? "--:--:--" : dt.substring(11, 19); }
}
