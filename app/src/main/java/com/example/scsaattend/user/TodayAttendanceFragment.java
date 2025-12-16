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
import com.example.scsaattend.dto.CheckInResponse;
import com.example.scsaattend.dto.CheckOutRequest;
import com.example.scsaattend.network.ApiService;
import com.example.scsaattend.network.RetrofitClient;
import com.example.scsaattend.dto.AttendanceRequest;
import com.example.scsaattend.dto.AttendanceResponse;
import java.text.SimpleDateFormat;
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
        
        // API 서비스 초기화 (중앙 관리되는 Base URL 사용)
        apiService = RetrofitClient.getClient("http://192.168.50.211:8888").create(ApiService.class);

        // UI 요소 연결
        viewStatusIndicator = view.findViewById(R.id.viewStatusIndicator);
        tvCurrentStatus = view.findViewById(R.id.tvCurrentStatus);
        tvCheckInTime = view.findViewById(R.id.tvCheckInTime);
        tvCheckOutTime = view.findViewById(R.id.tvCheckOutTime);
        cardHistory = view.findViewById(R.id.cardHistory);
        layoutCheckIn = view.findViewById(R.id.layoutCheckIn);
        layoutCheckOut = view.findViewById(R.id.layoutCheckOut);
        
        // 초기 상태: 히스토리 카드 및 내부 레이아웃 보여주기
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

        // 출근 버튼
        View btnCheckIn = view.findViewById(R.id.btnCheckIn);
        if (btnCheckIn != null) {
            btnCheckIn.setOnClickListener(v -> {
                Log.d(TAG, "Check-in Button Clicked");
                requestCheckIn();
            });
        }

        // 퇴근 버튼
        View btnCheckOut = view.findViewById(R.id.btnCheckOut);
        if (btnCheckOut != null) {
            btnCheckOut.setOnClickListener(v -> {
                Log.d(TAG, "Check-out Button Clicked");
                requestCheckOut();
            });
        }

        // 화면 진입 시 오늘 출석 정보 조회
        fetchTodayAttendance();

        return view;
    }

    private void requestCheckIn() {
        SharedPreferences prefs = requireContext().getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE);
        // 출근 요청 전에는 ainfoId가 없을 수도 있고, fetchTodayAttendance()가 완료되지 않아 -1일 수도 있음
        int ainfoId = prefs.getInt("today_ainfo_id", -1);
        
        Log.d(TAG, "Attempting Check-in with ainfoId: " + ainfoId);

        if (ainfoId == -1) {
            Toast.makeText(getContext(), "오늘의 출결 정보를 불러오지 못했습니다. 잠시 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
            fetchTodayAttendance();
            return;
        }

        // 1. PATCH 요청: 빈 JSON 객체 {} 전송
        apiService.checkIn(ainfoId, new Object()).enqueue(new Callback<CheckInResponse>() {
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
        int ainfoId = prefs.getInt("today_ainfo_id", -1);

        Log.d(TAG, "Attempting Check-out with ainfoId: " + ainfoId);

        if (ainfoId == -1) {
            Toast.makeText(getContext(), "오늘의 출결 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
            fetchTodayAttendance();
            return;
        }

        // 퇴근 시간 생성 (ISO 8601 형식 등 서버 요구사항에 맞춤, 예시는 현재 시간)
        // 요청된 포맷: "2025-12-11T17:55:00Z"
        // 실제로는 현재 시간을 해당 포맷으로 변환해야 함.
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
        // TimeZone 설정이 필요할 수 있음 (예: sdf.setTimeZone(TimeZone.getTimeZone("UTC")); )
        // 여기서는 로컬 시간 기준으로 포맷팅 예시
        String currentLeavingTime = sdf.format(new Date());
        
        // 비콘 정보는 실제 스캔된 정보를 사용하거나, 없을 경우 기본값/빈값 처리
        // 예시값 사용 (실제 구현 시 BeaconScanner에서 스캔된 마지막 값을 가져오거나 새로 스캔 필요)
        String macAddress = "00:00:00:00:00:00"; 
        int rssi = 0;
        
        // BeaconScanner나 저장된 변수에서 값을 가져오는 로직이 있다면 교체 필요
        // 예: if (lastScannedBeacon != null) { ... }

        CheckOutRequest request = new CheckOutRequest(currentLeavingTime, macAddress, rssi);

        // 1. PATCH 요청: CheckOutRequest 객체 전송
        apiService.checkOut(ainfoId, request).enqueue(new Callback<CheckInResponse>() {
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
        Log.d(TAG, "AuthPrefs contents: " + prefs.getAll());
        int memId = prefs.getInt("mem_pk", -1);
        
        Log.d(TAG, "fetchTodayAttendance Called. mem_pk: " + memId);

        if (memId == -1) {
             Log.e(TAG, "Invalid memId in SharedPrefs");
             return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayDate = sdf.format(new Date());
        
        int targetMemId = memId;

        Log.d(TAG, "Requesting Attendance. Date: " + todayDate + ", targetMemId: " + targetMemId);

        AttendanceRequest request = new AttendanceRequest(todayDate, todayDate, targetMemId);

        apiService.getAttendance(request).enqueue(new Callback<List<AttendanceResponse>>() {
            @Override
            public void onResponse(Call<List<AttendanceResponse>> call, Response<List<AttendanceResponse>> response) {
                Log.d(TAG, "API Response Code: " + response.code());
                if (response.isSuccessful() && response.body() != null) {
                    List<AttendanceResponse> list = response.body();
                    Log.d(TAG, "API Response Body Size: " + list.size());
                    
                    if (!list.isEmpty()) {
                        AttendanceResponse attendance = list.get(0);
                        
                        Log.d(TAG, "Attendance Found. ainfoId: " + attendance.getAinfoId() + 
                              ", arrivalTime: " + attendance.getArrivalTime());

                        SharedPreferences prefs = requireContext().getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE);
                        prefs.edit().putInt("today_ainfo_id", attendance.getAinfoId()).apply();
                        
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
            public void onFailure(Call<List<AttendanceResponse>> call, Throwable t) {
                Log.e(TAG, "API Call Failed", t);
                updateUIAsAbsent();
            }
        });
    }

    private void updateAttendanceUI(AttendanceResponse attendance) {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            String arrivalTime = attendance.getArrivalTime();
            String leavingTime = attendance.getLeavingTime();

            // 1. 카드 및 내부 레이아웃 항상 표시 (값 유무와 상관없이)
            if (cardHistory != null) cardHistory.setVisibility(View.VISIBLE);
            if (layoutCheckIn != null) layoutCheckIn.setVisibility(View.VISIBLE);
            if (layoutCheckOut != null) layoutCheckOut.setVisibility(View.VISIBLE);

            // 값 설정 (값이 없으면 "출근 정보 없음" / "퇴근 정보 없음" 또는 "-")
            if (tvCheckInTime != null) {
                tvCheckInTime.setText(arrivalTime != null ? arrivalTime : "출근 정보 없음");
            }
            if (tvCheckOutTime != null) {
                tvCheckOutTime.setText(leavingTime != null ? leavingTime : "퇴근 정보 없음");
            }

            // 2. 상태 표시등 색상 변경
            if (arrivalTime == null) {
                // 출근 전 (회색)
                if (viewStatusIndicator != null) {
                    viewStatusIndicator.setBackgroundTintList(
                        requireContext().getResources().getColorStateList(android.R.color.darker_gray, null)
                    );
                }
                if (tvCurrentStatus != null) {
                    tvCurrentStatus.setText("미출근");
                }
            } else {
                // 출근 완료 (초록색)
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
            
            // 데이터가 없어도 카드는 숨기지 않음
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