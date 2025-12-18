package com.example.scsaattend.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.example.scsaattend.R;
import com.example.scsaattend.network.ApiService;
import com.example.scsaattend.network.RetrofitClient;
import com.example.scsaattend.dto.SearchAttendanceRequest;
import com.example.scsaattend.dto.AttendanceInfoResponse;
import com.example.scsaattend.util.EventDecorator;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyAttendanceFragment extends Fragment {
    private static final String TAG = "MyAttendanceFragment";
    private TextView tvCurrentMonth;
    private MaterialCalendarView calendarView;
    private ImageButton btnPrevMonth, btnNextMonth;
    private ApiService apiService;
    private Call<List<AttendanceInfoResponse>> currentCall; 

    private View cardDetail;
    private TextView tvSelectedDate, tvDetailCheckIn, tvDetailCheckOut, tvDetailStatus;
    private Button btnViewDetail;

    private List<AttendanceInfoResponse> monthlyAttendanceList = new ArrayList<>();
    private AttendanceInfoResponse selectedAttendanceInfo; // 현재 선택된 날짜의 데이터 유지
    private boolean isInitialLoad = true; 

    private static final int COLOR_NORMAL = Color.parseColor("#A5D6A7"); 
    private static final int COLOR_LATE = Color.parseColor("#FFCC80");   
    private static final int COLOR_ABSENT = Color.parseColor("#EF9A9A");
    private static final int COLOR_HOLIDAY = Color.parseColor("#E0E0E0");

    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable fetchRunnable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_attendance, container, false);

        apiService = RetrofitClient.getClient().create(ApiService.class);

        tvCurrentMonth = view.findViewById(R.id.tvCurrentMonth);
        calendarView = view.findViewById(R.id.calendarView);
        btnPrevMonth = view.findViewById(R.id.btnPrevMonth);
        btnNextMonth = view.findViewById(R.id.btnNextMonth);
        
        cardDetail = view.findViewById(R.id.cardDetail);
        tvSelectedDate = view.findViewById(R.id.tvSelectedDate);
        tvDetailCheckIn = view.findViewById(R.id.tvDetailCheckIn);
        tvDetailCheckOut = view.findViewById(R.id.tvDetailCheckOut);
        tvDetailStatus = view.findViewById(R.id.tvDetailStatus);
        btnViewDetail = view.findViewById(R.id.btnViewDetail);

        CalendarDay today = CalendarDay.today();
        calendarView.setSelectedDate(today);
        calendarView.setCurrentDate(today);

        isInitialLoad = true;
        updateMonthDisplay();
        setupCalendarListeners();
        
        fetchMonthlyAttendance();

        btnPrevMonth.setOnClickListener(v -> calendarView.goToPrevious());
        btnNextMonth.setOnClickListener(v -> calendarView.goToNext());
        
        // 상세 보기 버튼 클릭 시 저장된 selectedAttendanceInfo 사용
        btnViewDetail.setOnClickListener(v -> {
            if (selectedAttendanceInfo != null) {
                showDetailDialog(selectedAttendanceInfo);
            } else {
                Toast.makeText(getContext(), "데이터가 없습니다.", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    private void setupCalendarListeners() {
        calendarView.setTopbarVisible(false); 
        
        calendarView.setOnMonthChangedListener((widget, date) -> {
            updateMonthDisplay();
            // 월 변경 시 상세 카드는 유지하고 데이터만 백그라운드 로드
            debounceHandler.removeCallbacks(fetchRunnable);
            fetchRunnable = this::fetchMonthlyAttendance;
            debounceHandler.postDelayed(fetchRunnable, 300);
        });

        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            if (selected) updateDetailCard(date);
        });
    }

    private void fetchMonthlyAttendance() {
        if (currentCall != null) currentCall.cancel();

        SharedPreferences prefs = requireContext().getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE);
        long memId = prefs.getLong("user_numeric_id", -1);
        if (memId == -1) return;

        CalendarDay current = calendarView.getCurrentDate();
        Calendar cal = Calendar.getInstance();
        cal.set(current.getYear(), current.getMonth() - 1, 1);
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String strStartDate = sdf.format(cal.getTime());
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        String strEndDate = sdf.format(cal.getTime());

        List<Long> memIdList = new ArrayList<>();
        memIdList.add(memId);

        SearchAttendanceRequest request = new SearchAttendanceRequest(strStartDate, strEndDate, memIdList);
        currentCall = apiService.searchAttendance(request);
        currentCall.enqueue(new Callback<List<AttendanceInfoResponse>>() {
            @Override
            public void onResponse(Call<List<AttendanceInfoResponse>> call, Response<List<AttendanceInfoResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    monthlyAttendanceList = response.body();
                    updateCalendarDecorators(monthlyAttendanceList);
                    
                    // 초기 로드 시에만 현재 선택된 날짜(오늘)를 기반으로 상세 정보를 설정
                    if (isInitialLoad) {
                        updateDetailCard(calendarView.getSelectedDate());
                        isInitialLoad = false;
                    }
                }
                currentCall = null;
            }

            @Override
            public void onFailure(Call<List<AttendanceInfoResponse>> call, Throwable t) {
                if (!call.isCanceled()) Log.e(TAG, "Error", t);
            }
        });
    }

    private void updateCalendarDecorators(List<AttendanceInfoResponse> list) {
        HashSet<CalendarDay> normal = new HashSet<>(), late = new HashSet<>(), absent = new HashSet<>(), holiday = new HashSet<>();

        for (AttendanceInfoResponse info : list) {
            String d = info.getADate();
            if (d == null || d.length() < 10) continue;
            try {
                int y = Integer.parseInt(d.substring(0, 4));
                int m = Integer.parseInt(d.substring(5, 7));
                int day = Integer.parseInt(d.substring(8, 10));
                CalendarDay cDay = CalendarDay.from(y, m, day);

                if ("Y".equalsIgnoreCase(info.getIsOff())) holiday.add(cDay);
                else {
                    String s = info.getStatus() != null ? info.getStatus().toLowerCase() : "";
                    if (s.contains("normal") || s.contains("정상")) normal.add(cDay);
                    else if (s.contains("late") || s.contains("early") || s.contains("지각") || s.contains("조퇴")) late.add(cDay);
                    else if (s.contains("absent") || s.contains("결석")) absent.add(cDay);
                }
            } catch (Exception ignored) {}
        }

        calendarView.removeDecorators();
        List<EventDecorator> decorators = new ArrayList<>();
        if (!normal.isEmpty()) decorators.add(new EventDecorator(COLOR_NORMAL, normal));
        if (!late.isEmpty()) decorators.add(new EventDecorator(COLOR_LATE, late));
        if (!absent.isEmpty()) decorators.add(new EventDecorator(COLOR_ABSENT, absent));
        if (!holiday.isEmpty()) decorators.add(new EventDecorator(COLOR_HOLIDAY, holiday));
        calendarView.addDecorators(decorators);
    }

    private void updateDetailCard(CalendarDay date) {
        if (date == null) return;
        String dateStr = formatDate(date);
        tvSelectedDate.setText(dateStr);

        // 현재 선택된 날짜의 데이터를 전역 변수에 저장
        selectedAttendanceInfo = findInfoByDate(dateStr);
        
        if (selectedAttendanceInfo != null) {
            if ("Y".equalsIgnoreCase(selectedAttendanceInfo.getIsOff())) {
                setDetailText("-", "-", "휴일", COLOR_HOLIDAY);
            } else {
                String status = getKoreanStatus(selectedAttendanceInfo.getStatus());
                int color = getStatusColor(status);
                setDetailText(formatLongTime(selectedAttendanceInfo.getArrivalTime()), formatLongTime(selectedAttendanceInfo.getLeavingTime()), status, color);
            }
        } else {
            setDetailText("-", "-", "기록 없음", Color.TRANSPARENT);
        }
    }

    private void setDetailText(String in, String out, String status, int color) {
        tvDetailCheckIn.setText(in);
        tvDetailCheckOut.setText(out);
        tvDetailStatus.setText(status);
        if (color != Color.TRANSPARENT) {
            tvDetailStatus.setBackgroundResource(R.drawable.bg_status_label);
            tvDetailStatus.setBackgroundTintList(ColorStateList.valueOf(color));
        } else {
            tvDetailStatus.setBackground(null);
        }
    }

    private int getStatusColor(String status) {
        if ("출석".equals(status)) return COLOR_NORMAL;
        if ("지각/조퇴".equals(status)) return COLOR_LATE;
        if ("결석".equals(status)) return COLOR_ABSENT;
        return Color.TRANSPARENT;
    }

    private String formatDate(CalendarDay date) {
        return String.format(Locale.getDefault(), "%d-%02d-%02d", date.getYear(), date.getMonth(), date.getDay());
    }

    private AttendanceInfoResponse findInfoByDate(String dateStr) {
        for (AttendanceInfoResponse info : monthlyAttendanceList) {
            if (dateStr.equals(info.getADate())) return info;
        }
        return null;
    }

    private void updateMonthDisplay() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy년 MM월", Locale.KOREAN);
        CalendarDay current = calendarView.getCurrentDate();
        Calendar cal = Calendar.getInstance();
        cal.set(current.getYear(), current.getMonth() - 1, 1);
        tvCurrentMonth.setText(sdf.format(cal.getTime()));
    }

    private String getKoreanStatus(String status) {
        if (status == null) return "기록 없음";
        String lower = status.toLowerCase();
        if (lower.contains("normal") || lower.contains("정상")) return "출석";
        if (lower.contains("late") || lower.contains("early") || lower.contains("지각") || lower.contains("조퇴")) return "지각/조퇴";
        if (lower.contains("absent") || lower.contains("결석")) return "결석";
        return status;
    }

    private String formatShortTime(String time) {
        return (time == null || time.length() < 5) ? "-" : time.substring(0, 5);
    }

    private String formatLongTime(String time) {
        if (time == null || !time.contains("T")) return "-";
        try { return time.substring(11, 19); } catch (Exception e) { return "-"; }
    }

    private void showDetailDialog(AttendanceInfoResponse info) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View v = getLayoutInflater().inflate(R.layout.dialog_attendance_detail_simple, null);
        builder.setView(v);

        ((TextView)v.findViewById(R.id.tvTitle)).setText(info.getMember().getName() + "님 상세");
        ((TextView)v.findViewById(R.id.tvDate)).setText("날짜 : " + info.getADate());
        
        String typeStr = "-";
        if (info.getAttendanceType() != null) {
            typeStr = info.getAttendanceType().getName() + " (" + formatShortTime(info.getAttendanceType().getStartTime()) + " ~ " + formatShortTime(info.getAttendanceType().getEndTime()) + ")";
        }
        ((TextView)v.findViewById(R.id.tvType)).setText("유형 : " + typeStr);
        ((TextView)v.findViewById(R.id.tvInTime)).setText("출근 : " + formatLongTime(info.getArrivalTime()));
        ((TextView)v.findViewById(R.id.tvOutTime)).setText("퇴근 : " + formatLongTime(info.getLeavingTime()));
        ((TextView)v.findViewById(R.id.tvStatus)).setText("상태 : " + ("Y".equalsIgnoreCase(info.getIsOff()) ? "휴일" : getKoreanStatus(info.getStatus())));
        
        String appStr = "approved".equalsIgnoreCase(info.getIsApproved()) ? "승인" : ("denied".equalsIgnoreCase(info.getIsApproved()) ? "불허" : "-");
        ((TextView)v.findViewById(R.id.tvApproval)).setText("승인 : " + appStr);
        String offStr = "Y".equalsIgnoreCase(info.getIsOfficial()) ? "O" : ("N".equalsIgnoreCase(info.getIsOfficial()) ? "X" : "-");
        ((TextView)v.findViewById(R.id.tvOfficial)).setText("공결 : " + offStr);
        ((TextView)v.findViewById(R.id.tvMemNote)).setText("사유 : " + (info.getMemNote() != null ? info.getMemNote() : "-"));
        ((TextView)v.findViewById(R.id.tvAdminNote)).setText("관리자 : " + (info.getAdminNote() != null ? info.getAdminNote() : "-"));

        AlertDialog d = builder.create();
        v.findViewById(R.id.btnClose).setOnClickListener(view -> d.dismiss());
        d.show();
    }
}
