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
import androidx.fragment.app.Fragment;
import com.example.scsaattend.R;
import com.example.scsaattend.beacon.BeaconScanner;
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
    private View layoutCheckInHistory;
    private View layoutCheckOutHistory;
    private View cardHistory;

    private String lastScannedMacAddress = null;
    private int lastScannedRssi = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_today_attendance, container, false);

        apiService = RetrofitClient.getClient().create(ApiService.class);

        viewStatusIndicator = view.findViewById(R.id.viewStatusIndicator);
        tvCurrentStatus = view.findViewById(R.id.tvCurrentStatus);
        tvCheckInTime = view.findViewById(R.id.tvCheckInTime);
        tvCheckOutTime = view.findViewById(R.id.tvCheckOutTime);
        cardHistory = view.findViewById(R.id.cardHistory);

        layoutCheckInHistory = view.findViewById(R.id.layoutCheckInHistory);
        layoutCheckOutHistory = view.findViewById(R.id.layoutCheckOutHistory);

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
                if (lastScannedMacAddress == null) {
                    Toast.makeText(getContext(), "비콘을 먼저 스캔해주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }
                requestCheckIn();
            });
        }

        View btnCheckOut = view.findViewById(R.id.btnCheckOut);
        if (btnCheckOut != null) {
            btnCheckOut.setOnClickListener(v -> {
                Log.d(TAG, "Check-out Button Clicked");
                if (lastScannedMacAddress == null) {
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

        Log.d(TAG, "requestCheckIn - ainfoId: " + ainfoId);

        if (ainfoId != -1) {
            performCheckIn((int)ainfoId);
        } else {
            Log.d(TAG, "ainfoId missing, fetching attendance info...");
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

                    Log.d(TAG, "fetchAttendanceAndCheckIn: Success. ainfoId=" + newAinfoId);

                    prefs.edit().putLong("today_ainfo_id", newAinfoId).apply();

                    updateAttendanceUI(attendance);

                    performCheckIn((int)newAinfoId);
                } else {
                    String reason = "Unknown";
                    if (!response.isSuccessful()) reason = "HTTP Error " + response.code() + " " + response.message();
                    else if (response.body() == null) reason = "Body is null";
                    else if (response.body().isEmpty()) reason = "Empty List returned";

                    Log.e(TAG, "fetchAttendanceAndCheckIn Failed: " + reason);
                    Toast.makeText(getContext(), "오늘의 출결 정보를 불러올 수 없습니다. (" + reason + ")", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<AttendanceInfoResponse>> call, Throwable t) {
                Log.e(TAG, "fetchAttendanceAndCheckIn Network Error", t);
                Toast.makeText(getContext(), "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void performCheckIn(int ainfoId) {
        apiService.checkIn(ainfoId, new Object()).enqueue(new Callback<CheckInApiResponse>() {
            @Override
            public void onResponse(Call<CheckInApiResponse> call, Response<CheckInApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    CheckInApiResponse res = response.body();
                    String newArrivalTime = res.getAttendanceInfo().getArrivalTime();

                    Toast.makeText(getContext(), "출근 처리되었습니다.", Toast.LENGTH_SHORT).show();
                    tvCheckInTime.setText(formatTime(newArrivalTime));
                    viewStatusIndicator.setBackgroundTintList(requireContext().getResources().getColorStateList(R.color.attendance_light_green, null));
                    tvCurrentStatus.setText("출근 완료");

                } else {
                    String errorMessage = "출근 요청 실패";
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
            }

            @Override
            public void onFailure(Call<CheckInApiResponse> call, Throwable t) {
                Log.e(TAG, "Check-in Network Error", t);
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

        if (lastScannedMacAddress == null) {
             Toast.makeText(getContext(), "비콘 스캔 정보가 없습니다. 비콘을 먼저 스캔해주세요.", Toast.LENGTH_SHORT).show();
             return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
        String currentLeavingTime = sdf.format(new Date());

        CheckOutRequest request = new CheckOutRequest(currentLeavingTime, lastScannedMacAddress, lastScannedRssi);

        apiService.checkOut((int)ainfoId, request).enqueue(new Callback<CheckInApiResponse>() {
            @Override
            public void onResponse(Call<CheckInApiResponse> call, Response<CheckInApiResponse> response) {
                if (response.isSuccessful()) {
                    String newLeavingTime = response.body().getAttendanceInfo().getLeavingTime();
                    Toast.makeText(getContext(), "퇴근 처리되었습니다.", Toast.LENGTH_SHORT).show();
                    tvCheckOutTime.setText(formatTime(newLeavingTime));
                } else {
                     String errorMessage = "퇴근 요청 실패";
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
            }

            @Override
            public void onFailure(Call<CheckInApiResponse> call, Throwable t) {
                Log.e(TAG, "Check-out Network Error", t);
                Toast.makeText(getContext(), "네트워크 오류", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchTodayAttendance() {
        SharedPreferences prefs = requireContext().getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE);
        long memId = prefs.getLong("user_numeric_id", -1);

        if (memId == -1) {
            Log.e(TAG, "Invalid user_numeric_id in SharedPrefs");
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
                Log.d(TAG, "API Response Code: " + response.code());
                if (response.isSuccessful() && response.body() != null) {
                    List<AttendanceInfoResponse> list = response.body();
                    Log.d(TAG, "API Response Body Size: " + list.size());

                    if (!list.isEmpty()) {
                        AttendanceInfoResponse attendance = list.get(0);

                        Log.d(TAG, "Attendance Found. ainfoId: " + attendance.getAinfoId() + ", arrivalTime: " + attendance.getArrivalTime());

                        prefs.edit().putLong("today_ainfo_id", attendance.getAinfoId()).apply();

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
            if (layoutCheckInHistory != null) layoutCheckInHistory.setVisibility(View.VISIBLE);
            if (layoutCheckOutHistory != null) layoutCheckOutHistory.setVisibility(View.VISIBLE);

            if (tvCheckInTime != null) {
                tvCheckInTime.setText(arrivalTime != null ? formatTime(arrivalTime) : "출근 정보 없음");
            }
            if (tvCheckOutTime != null) {
                tvCheckOutTime.setText(leavingTime != null ? formatTime(leavingTime) : "퇴근 정보 없음");
            }

            if (arrivalTime == null) {
                if (viewStatusIndicator != null) {
                    viewStatusIndicator.setBackgroundTintList(requireContext().getResources().getColorStateList(android.R.color.darker_gray, null));
                }
                if (tvCurrentStatus != null) {
                    tvCurrentStatus.setText("미출근");
                }
            } else {
                if (viewStatusIndicator != null) {
                    viewStatusIndicator.setBackgroundTintList(requireContext().getResources().getColorStateList(R.color.attendance_light_green, null));
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
                viewStatusIndicator.setBackgroundTintList(requireContext().getResources().getColorStateList(android.R.color.darker_gray, null));
            }
            if (tvCurrentStatus != null) {
                tvCurrentStatus.setText("정보 없음");
            }

            if (cardHistory != null) cardHistory.setVisibility(View.VISIBLE);
            if (layoutCheckInHistory != null) layoutCheckInHistory.setVisibility(View.VISIBLE);
            if (layoutCheckOutHistory != null) layoutCheckOutHistory.setVisibility(View.VISIBLE);

            if (tvCheckInTime != null) tvCheckInTime.setText("출근 정보 없음");
            if (tvCheckOutTime != null) tvCheckOutTime.setText("퇴근 정보 없음");
        });
    }

    private String formatTime(String dateTime) {
        if (dateTime == null) return "-";
        return dateTime;
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