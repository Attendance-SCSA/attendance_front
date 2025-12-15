package com.example.scsaattend.user;

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
import com.example.scsaattend.dto.CheckInResponse;
import com.example.scsaattend.network.ApiService;
import com.example.scsaattend.network.RetrofitClient;
// dto 패키지 경로 변경 반영
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
    private View layoutCheckInHistory;
    private View layoutCheckOutHistory;
    private View cardHistory;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_today_attendance, container, false);
        
        // API 서비스 초기화 (실제 IP 확인 필수)
        apiService = RetrofitClient.getClient("http://10.10.0.56:8888").create(ApiService.class);

        // UI 요소 연결
        viewStatusIndicator = view.findViewById(R.id.viewStatusIndicator);
        tvCurrentStatus = view.findViewById(R.id.tvCurrentStatus);
        tvCheckInTime = view.findViewById(R.id.tvCheckInTime);
        tvCheckOutTime = view.findViewById(R.id.tvCheckOutTime);
        cardHistory = view.findViewById(R.id.cardHistory);
        
        if (tvCheckInTime != null && tvCheckInTime.getParent() != null && tvCheckInTime.getParent() instanceof View) {
            layoutCheckInHistory = (View) tvCheckInTime.getParent();
        }
        if (tvCheckOutTime != null && tvCheckOutTime.getParent() != null && tvCheckOutTime.getParent() instanceof View) {
            layoutCheckOutHistory = (View) tvCheckOutTime.getParent();
        }

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

        // 3. 출근 버튼 클릭 시 출근 요청
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
                SimpleDateFormat timeFormat = new SimpleDateFormat("a h:mm:ss", Locale.KOREAN);
                String currentTime = timeFormat.format(new Date());
                Toast.makeText(getContext(), "퇴근 처리되었습니다. (" + currentTime + ")", Toast.LENGTH_SHORT).show();
            });
        }

        // 화면 진입 시 오늘 출석 정보 조회
        fetchTodayAttendance();

        return view;
    }

    private void requestCheckIn() {
        SharedPreferences prefs = requireContext().getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE);
        // 저장된 ainfoId 가져오기
        int ainfoId = prefs.getInt("today_ainfo_id", -1);
        
        Log.d(TAG, "Attempting Check-in with ainfoId: " + ainfoId);

        if (ainfoId == -1) {
            Toast.makeText(getContext(), "오늘의 출결 정보를 아직 불러오지 못했습니다. 잠시 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
            // 재시도 차원에서 다시 조회를 시도
            fetchTodayAttendance();
            return;
        }

        // API 호출
        apiService.checkIn(ainfoId, new Object()).enqueue(new Callback<CheckInResponse>() {
            @Override
            public void onResponse(Call<CheckInResponse> call, Response<CheckInResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    CheckInResponse checkInResponse = response.body();
                    String newArrivalTime = checkInResponse.getNewArrivalTime();
                    
                    Log.d(TAG, "Check-in Success. Time: " + newArrivalTime);

                    updateUIForCheckIn(newArrivalTime);
                    Toast.makeText(getContext(), "출근 처리되었습니다.", Toast.LENGTH_SHORT).show();
                } else {
                    Log.e(TAG, "Check-in Failed. Code: " + response.code());
                    Toast.makeText(getContext(), "출근 요청 실패: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<CheckInResponse> call, Throwable t) {
                Log.e(TAG, "Check-in Network Error", t);
                Toast.makeText(getContext(), "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUIForCheckIn(String arrivalTime) {
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
        if (tvCheckInTime != null) {
             // 서버 시간 포맷 그대로 표시
             tvCheckInTime.setText(arrivalTime);
        }
    }

    private void fetchTodayAttendance() {
        SharedPreferences prefs = requireContext().getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE);
        // LoginActivity에서 저장한 PK값 사용
        int memId = prefs.getInt("mem_pk", -1);
        
        Log.d(TAG, "Fetching Today Attendance for memId: " + memId);
        
        if (memId == -1) {
            Log.e(TAG, "Invalid Member ID (mem_pk is -1)");
            // 로그인 정보가 없으면 처리 중단
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayDate = sdf.format(new Date());

        AttendanceRequest request = new AttendanceRequest(todayDate, todayDate, memId);

        apiService.getAttendance(request).enqueue(new Callback<List<AttendanceResponse>>() {
            @Override
            public void onResponse(Call<List<AttendanceResponse>> call, Response<List<AttendanceResponse>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    AttendanceResponse attendance = response.body().get(0);
                    
                    // ainfoId 저장 (중요!)
                    SharedPreferences prefs = requireContext().getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putInt("today_ainfo_id", attendance.getAinfoId());
                    editor.apply();
                    
                    Log.d(TAG, "Attendance Fetched. ainfoId: " + attendance.getAinfoId());

                    updateAttendanceUI(attendance);
                } else {
                    Log.d(TAG, "No Attendance Data Found or List Empty");
                    updateUIAsAbsent();
                }
            }

            @Override
            public void onFailure(Call<List<AttendanceResponse>> call, Throwable t) {
                Log.e(TAG, "Fetch Attendance Failed", t);
                updateUIAsAbsent();
            }
        });
    }

    private void updateAttendanceUI(AttendanceResponse attendance) {
        String arrivalTime = attendance.getArrivalTime();
        String leavingTime = attendance.getLeavingTime();

        if (arrivalTime == null) {
            updateUIAsAbsent();
        } else {
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

            if (leavingTime != null) {
                if (layoutCheckOutHistory != null) layoutCheckOutHistory.setVisibility(View.VISIBLE);
                if (tvCheckOutTime != null) tvCheckOutTime.setText(leavingTime);
            } else {
                if (layoutCheckOutHistory != null) layoutCheckOutHistory.setVisibility(View.GONE);
            }
        }
    }

    private void updateUIAsAbsent() {
        if (viewStatusIndicator != null) {
            viewStatusIndicator.setBackgroundTintList(
                requireContext().getResources().getColorStateList(android.R.color.darker_gray, null)
            );
        }
        if (tvCurrentStatus != null) {
            tvCurrentStatus.setText("부재중");
        }
        if (cardHistory != null) {
            cardHistory.setVisibility(View.GONE);
        }
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