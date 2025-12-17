package com.example.scsaattend.user;

import static com.example.scsaattend.common.Config.TARGET_BEACON_ADDRESS;

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
import androidx.fragment.app.Fragment;
import com.example.scsaattend.R;
import com.example.scsaattend.beacon.BeaconScanner;
import com.example.scsaattend.network.ApiService;
import com.example.scsaattend.network.RetrofitClient;
import com.example.scsaattend.dto.ErrorResponse;
import com.example.scsaattend.dto.TodayMyAttendanceRequest;
import com.example.scsaattend.dto.TodayMyAttendanceResponse;
import com.example.scsaattend.dto.CheckInResponse;
import com.example.scsaattend.dto.CheckOutRequest;
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

    // 스캔된 비콘 정보 저장용 변수
    private String lastScannedMacAddress = null;
    private int lastScannedRssi = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_today_attendance, container, false);

        // API 서비스 초기화 (Config 사용)
        apiService = RetrofitClient.getClient().create(ApiService.class);

        // UI 요소 연결
        viewStatusIndicator = view.findViewById(R.id.viewStatusIndicator);
        tvCurrentStatus = view.findViewById(R.id.tvCurrentStatus);
        tvCheckInTime = view.findViewById(R.id.tvCheckInTime);
        tvCheckOutTime = view.findViewById(R.id.tvCheckOutTime);
        cardHistory = view.findViewById(R.id.cardHistory);

        // 레이아웃 찾기
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

        // 출근 버튼
        View btnCheckIn = view.findViewById(R.id.btnCheckIn);
        if (btnCheckIn != null) {
            btnCheckIn.setOnClickListener(v -> {
                Log.d(TAG, "Check-in Button Clicked");
                // 비콘 검증 로직
                if (lastScannedMacAddress == null) {
                    Toast.makeText(getContext(), "비콘을 먼저 스캔해주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 필요 시 타겟 비콘인지 추가 확인
                /*
                if (!TARGET_BEACON_ADDRESS.equalsIgnoreCase(lastScannedMacAddress)) {
                    Toast.makeText(getContext(), "올바른 비콘이 아닙니다. (" + lastScannedMacAddress + ")", Toast.LENGTH_SHORT).show();
                    return;
                }
                */
                requestCheckIn();
            });
        }

        // 퇴근 버튼
        View btnCheckOut = view.findViewById(R.id.btnCheckOut);
        if (btnCheckOut != null) {
            btnCheckOut.setOnClickListener(v -> {
                Log.d(TAG, "Check-out Button Clicked");

                // 비콘 검증 로직
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
        int ainfoId = prefs.getInt("today_ainfo_id", -1);

        Log.d(TAG, "requestCheckIn - ainfoId: " + ainfoId);

        if (ainfoId != -1) {
            performCheckIn(ainfoId);
        } else {
            Log.d(TAG, "ainfoId missing, fetching attendance info...");
            fetchAttendanceAndCheckIn();
        }
    }

    private void fetchAttendanceAndCheckIn() {
        SharedPreferences prefs = requireContext().getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE);
        int memId = prefs.getInt("mem_pk", -1);

        if (memId == -1) {
            Toast.makeText(getContext(), "로그인 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayDate = sdf.format(new Date());

        List<Integer> memIdList = new ArrayList<>();
        memIdList.add(memId + 1); // User request: memId + 1

        Log.d(TAG, "fetchAttendanceAndCheckIn: Requesting for memId=" + (memId + 1) + ", date=" + todayDate);

        TodayMyAttendanceRequest request = new TodayMyAttendanceRequest(todayDate, todayDate, memIdList);

        apiService.getAttendance(request).enqueue(new Callback<List<TodayMyAttendanceResponse>>() {
            @Override
            public void onResponse(Call<List<TodayMyAttendanceResponse>> call, Response<List<TodayMyAttendanceResponse>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    TodayMyAttendanceResponse attendance = response.body().get(0);
                    int newAinfoId = attendance.getAinfoId();

                    Log.d(TAG, "fetchAttendanceAndCheckIn: Success. ainfoId=" + newAinfoId);

                    // ainfoId 저장
                    prefs.edit().putInt("today_ainfo_id", newAinfoId).apply();

                    // UI 업데이트
                    updateAttendanceUI(attendance);

                    // 출근 요청 진행
                    performCheckIn(newAinfoId);
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
            public void onFailure(Call<List<TodayMyAttendanceResponse>> call, Throwable t) {
                Log.e(TAG, "fetchAttendanceAndCheckIn Network Error", t);
                Toast.makeText(getContext(), "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void performCheckIn(int ainfoId) {
        apiService.checkIn(ainfoId, new Object()).enqueue(new Callback<CheckInResponse>() {
            @Override
            public void onResponse(Call<CheckInResponse> call, Response<CheckInResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    CheckInResponse res = response.body();

                    // DTO 필드가 attendanceInfo 내부에 있다면 구조에 맞게 수정 필요
                    // 현재 CheckInResponse는 attendanceId, newArrivalTime 필드를 가짐
                    // 만약 서버 응답이 바뀌었다면 DTO도 수정해야 하지만,
                    // 여기서는 기존 로직대로 처리하거나 res에서 필요한 값을 꺼냄
                    String newTime = res.getNewArrivalTime();

                    Log.d(TAG, "Check-in Success: " + newTime);

                    // UI 업데이트
                    updateUIForCheckIn(newTime);
                    Toast.makeText(getContext(), "출근 처리되었습니다.", Toast.LENGTH_SHORT).show();

                    // 출근 성공 후 최신 정보 갱신을 위해 재조회
                    fetchTodayAttendance();
                } else {
                    // 실패 응답 처리
                    String errorMessage = "출근 요청 실패: " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            String errorBodyStr = response.errorBody().string();
                            Log.e(TAG, "Error Body: " + errorBodyStr);

                            Gson gson = new Gson();
                            ErrorResponse errorResponse = gson.fromJson(errorBodyStr, ErrorResponse.class);
                            if (errorResponse != null && errorResponse.getMessage() != null) {
                                errorMessage = errorResponse.getMessage();
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    Log.e(TAG, "Check-in Failed: " + errorMessage);
                    Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
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
        int ainfoId = prefs.getInt("today_ainfo_id", -1);

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

        apiService.checkOut(ainfoId, request).enqueue(new Callback<CheckInResponse>() {
            @Override
            public void onResponse(Call<CheckInResponse> call, Response<CheckInResponse> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "퇴근 처리되었습니다.", Toast.LENGTH_SHORT).show();

                    // 퇴근 성공 시에도 최신 정보 갱신
                    fetchTodayAttendance();
                } else {
                    Log.e(TAG, "Check-out Failed. Code: " + response.code());
                    Toast.makeText(getContext(), "퇴근 요청 실패: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<CheckInResponse> call, Throwable t) {
                Log.e(TAG, "Check-out Network Error", t);
                Toast.makeText(getContext(), "네트워크 오류", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUIForCheckIn(String arrivalTime) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (viewStatusIndicator != null) {
                    viewStatusIndicator.setBackgroundTintList(
                        requireContext().getResources().getColorStateList(R.color.attendance_light_green, null)
                    );
                }
                if (tvCurrentStatus != null) {
                    tvCurrentStatus.setText("출근 완료");
                }
                if (cardHistory != null) cardHistory.setVisibility(View.VISIBLE);
                if (layoutCheckInHistory != null) layoutCheckInHistory.setVisibility(View.VISIBLE);
                if (tvCheckInTime != null) tvCheckInTime.setText(arrivalTime);
            });
        }
    }

    private void fetchTodayAttendance() {
        SharedPreferences prefs = requireContext().getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE);
        int memId = prefs.getInt("mem_pk", -1);

        if (memId == -1) {
            Log.e(TAG, "Invalid Member ID");
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayDate = sdf.format(new Date());

        List<Integer> memIdList = new ArrayList<>();
        memIdList.add(memId);

        Log.d(TAG, "fetchTodayAttendance: Requesting for memId=" + (memId));

        TodayMyAttendanceRequest request = new TodayMyAttendanceRequest(todayDate, todayDate, memIdList);

        apiService.getAttendance(request).enqueue(new Callback<List<TodayMyAttendanceResponse>>() {
            @Override
            public void onResponse(Call<List<TodayMyAttendanceResponse>> call, Response<List<TodayMyAttendanceResponse>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    TodayMyAttendanceResponse attendance = response.body().get(0);
                    
                    Log.d(TAG, "fetchTodayAttendance: Success. ainfoId=" + attendance.getAinfoId());

                    SharedPreferences prefs = requireContext().getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE);
                    prefs.edit().putInt("today_ainfo_id", attendance.getAinfoId()).apply();
                    
                    updateAttendanceUI(attendance);
                } else {
                    String reason = "Unknown";
                    if (!response.isSuccessful()) reason = "HTTP Error " + response.code() + " " + response.message();
                    else if (response.body() == null) reason = "Body is null";
                    else if (response.body().isEmpty()) reason = "Empty List returned";
                    
                    Log.d(TAG, "fetchTodayAttendance: No Data (" + reason + ")");
                    updateUIAsAbsent();
                }
            }

            @Override
            public void onFailure(Call<List<TodayMyAttendanceResponse>> call, Throwable t) {
                Log.e(TAG, "Fetch Attendance Failed", t);
                updateUIAsAbsent();
            }
        });
    }

    private void updateAttendanceUI(TodayMyAttendanceResponse attendance) {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            String arrivalTime = attendance.getArrivalTime();
            String leavingTime = attendance.getLeavingTime();

            if (cardHistory != null) cardHistory.setVisibility(View.VISIBLE);

            if (arrivalTime == null) {
                if (viewStatusIndicator != null) {
                    viewStatusIndicator.setBackgroundTintList(
                        requireContext().getResources().getColorStateList(android.R.color.darker_gray, null)
                    );
                }
                if (tvCurrentStatus != null) {
                    tvCurrentStatus.setText("부재중");
                }
                if (tvCheckInTime != null) tvCheckInTime.setText("-"); 
            } else {
                if (viewStatusIndicator != null) {
                    viewStatusIndicator.setBackgroundTintList(
                        requireContext().getResources().getColorStateList(R.color.attendance_light_green, null)
                    );
                }
                if (tvCurrentStatus != null) {
                    tvCurrentStatus.setText("출근 완료");
                }
                if (tvCheckInTime != null) tvCheckInTime.setText(arrivalTime);
            }

            if (layoutCheckInHistory != null) layoutCheckInHistory.setVisibility(View.VISIBLE);
            if (layoutCheckOutHistory != null) layoutCheckOutHistory.setVisibility(View.VISIBLE);

            if (leavingTime != null) {
                if (tvCheckOutTime != null) tvCheckOutTime.setText(leavingTime);
            } else {
                if (tvCheckOutTime != null) tvCheckOutTime.setText("-");
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
                tvCurrentStatus.setText("부재중");
            }
            
            if (cardHistory != null) cardHistory.setVisibility(View.VISIBLE);
            if (layoutCheckInHistory != null) layoutCheckInHistory.setVisibility(View.VISIBLE);
            if (layoutCheckOutHistory != null) layoutCheckOutHistory.setVisibility(View.VISIBLE);
            
            if (tvCheckInTime != null) tvCheckInTime.setText("-");
            if (tvCheckOutTime != null) tvCheckOutTime.setText("-");
        });
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (beaconScanner != null) {
            beaconScanner.stopScan();
        }
    }

    @Override
    public void onBeaconFound(BluetoothDevice device, int rssi, byte[] scanRecord) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                Log.d(TAG, "Beacon Found: " + device.getAddress());
                
                lastScannedMacAddress = device.getAddress();
                lastScannedRssi = rssi;

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