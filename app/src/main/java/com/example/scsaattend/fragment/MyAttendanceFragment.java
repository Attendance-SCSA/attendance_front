package com.example.scsaattend.fragment;

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
import java.text.ParseException;
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
    private Calendar currentCalendar;
    private ApiService apiService;

    // 상세 정보 UI
    private View cardDetail;
    private TextView tvSelectedDate, tvDetailCheckIn, tvDetailCheckOut, tvDetailStatus;
    private Button btnViewDetail;

    // 월별 출결 데이터 저장
    private List<AttendanceInfoResponse> monthlyAttendanceList = new ArrayList<>();
    private boolean isInitialLoad = true; 

    // 상태별 색상
    private static final int COLOR_NORMAL = Color.parseColor("#A5D6A7"); 
    private static final int COLOR_LATE = Color.parseColor("#FFCC80");   
    private static final int COLOR_ABSENT = Color.parseColor("#EF9A9A");
    private static final int COLOR_HOLIDAY = Color.parseColor("#E0E0E0");

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_attendance, container, false);

        apiService = RetrofitClient.getClient().create(ApiService.class);
        currentCalendar = Calendar.getInstance();

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
        
        btnViewDetail.setOnClickListener(v -> {
            CalendarDay selectedDate = calendarView.getSelectedDate();
            if (selectedDate == null) return;

            String dateStr = String.format(Locale.getDefault(), "%d-%02d-%02d",
                    selectedDate.getYear(), selectedDate.getMonth(), selectedDate.getDay());

            AttendanceInfoResponse selectedInfo = null;
            for (AttendanceInfoResponse info : monthlyAttendanceList) {
                if (dateStr.equals(info.getADate())) {
                    selectedInfo = info;
                    break;
                }
            }

            if (selectedInfo != null) {
                showDetailDialog(selectedInfo);
            } else {
                Toast.makeText(getContext(), "해당 날짜의 상세 데이터가 없습니다.", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    private void showDetailDialog(AttendanceInfoResponse info) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_attendance_detail_simple, null);
        builder.setView(dialogView);

        TextView tvTitle = dialogView.findViewById(R.id.tvTitle);
        TextView tvDate = dialogView.findViewById(R.id.tvDate);
        TextView tvType = dialogView.findViewById(R.id.tvType);
        TextView tvInTime = dialogView.findViewById(R.id.tvInTime);
        TextView tvOutTime = dialogView.findViewById(R.id.tvOutTime);
        TextView tvStatus = dialogView.findViewById(R.id.tvStatus);
        TextView tvApproval = dialogView.findViewById(R.id.tvApproval);
        TextView tvOfficial = dialogView.findViewById(R.id.tvOfficial);
        TextView tvMemNote = dialogView.findViewById(R.id.tvMemNote);
        TextView tvAdminNote = dialogView.findViewById(R.id.tvAdminNote);
        Button btnClose = dialogView.findViewById(R.id.btnClose);

        tvTitle.setText(info.getMember().getName() + "님 출결 상세");
        
        // 날짜
        tvDate.setText("날짜 : " + info.getADate());
        
        // 유형 : 이름 (수업시작 : HH:mm / 수업종료 : HH:mm)
        String typeInfo = "-";
        if (info.getAttendanceType() != null) {
            String startTime = formatShortTime(info.getAttendanceType().getStartTime());
            String endTime = formatShortTime(info.getAttendanceType().getEndTime());
            typeInfo = info.getAttendanceType().getName() + " (수업시작 : " + startTime + " / 수업종료 : " + endTime + ")";
        }
        tvType.setText("유형 : " + typeInfo);
        
        // 출근/퇴근 시간 : HH:mm:SS
        tvInTime.setText("출근 시간 : " + formatLongTime(info.getArrivalTime()));
        tvOutTime.setText("퇴근 시간 : " + formatLongTime(info.getLeavingTime()));
        
        // 상태 (휴일인 경우 휴일)
        if ("Y".equalsIgnoreCase(info.getIsOff())) {
            tvStatus.setText("상태 : 휴일");
        } else {
            tvStatus.setText("상태 : " + getKoreanStatus(info.getStatus()));
        }
        
        // 승인 : 승인, 불허, 미정(-)
        String approvalStr = "-";
        if ("approved".equalsIgnoreCase(info.getIsApproved())) approvalStr = "승인";
        else if ("denied".equalsIgnoreCase(info.getIsApproved())) approvalStr = "불허";
        tvApproval.setText("승인 : " + approvalStr);
        
        // 공결 : O, X, -
        String officialStr = "-";
        if ("Y".equalsIgnoreCase(info.getIsOfficial())) officialStr = "O";
        else if ("N".equalsIgnoreCase(info.getIsOfficial())) officialStr = "X";
        tvOfficial.setText("공결 : " + officialStr);
        
        // 사유 및 관리자 메세지
        tvMemNote.setText("사유 : " + (info.getMemNote() != null ? info.getMemNote() : "-"));
        tvAdminNote.setText("관리자메세지 : " + (info.getAdminNote() != null ? info.getAdminNote() : "-"));

        AlertDialog dialog = builder.create();
        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
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
        if (time == null || time.length() < 5) return "-";
        return time.substring(0, 5); // HH:mm
    }

    private String formatLongTime(String time) {
        if (time == null || !time.contains("T")) return "-";
        try {
            // T 이후부터 8자리 (HH:mm:SS)
            return time.substring(11, 19);
        } catch (Exception e) {
            return "-";
        }
    }

    private void setupCalendarListeners() {
        calendarView.setTopbarVisible(false); 
        calendarView.setOnMonthChangedListener((widget, date) -> {
            currentCalendar.set(date.getYear(), date.getMonth() - 1, 1);
            updateMonthDisplay();
            fetchMonthlyAttendance();
        });

        calendarView.setOnDateChangedListener(new OnDateSelectedListener() {
            @Override
            public void onDateSelected(@NonNull MaterialCalendarView widget, @NonNull CalendarDay date, boolean selected) {
                if (selected) {
                    updateDetailCard(date);
                }
            }
        });
    }

    private void updateMonthDisplay() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy년 MM월", Locale.KOREAN);
        CalendarDay currentDate = calendarView.getCurrentDate();
        Calendar cal = Calendar.getInstance();
        cal.set(currentDate.getYear(), currentDate.getMonth() - 1, 1);
        tvCurrentMonth.setText(sdf.format(cal.getTime()));
    }

    private void fetchMonthlyAttendance() {
        SharedPreferences prefs = requireContext().getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE);
        long memId = prefs.getLong("user_numeric_id", -1);

        if (memId == -1) return;

        CalendarDay currentDate = calendarView.getCurrentDate();
        Calendar calendar = Calendar.getInstance();
        calendar.set(currentDate.getYear(), currentDate.getMonth() - 1, 1);
        
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        Date startDate = calendar.getTime();

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        Date endDate = calendar.getTime();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String strStartDate = sdf.format(startDate);
        String strEndDate = sdf.format(endDate);

        List<Long> memIdList = new ArrayList<>();
        memIdList.add(memId);

        SearchAttendanceRequest request = new SearchAttendanceRequest(strStartDate, strEndDate, memIdList);

        apiService.searchAttendance(request).enqueue(new Callback<List<AttendanceInfoResponse>>() {
            @Override
            public void onResponse(Call<List<AttendanceInfoResponse>> call, Response<List<AttendanceInfoResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    monthlyAttendanceList = response.body();
                    updateCalendarDecorators(monthlyAttendanceList);

                    if (isInitialLoad) {
                        CalendarDay selectedDate = calendarView.getSelectedDate();
                        if (selectedDate != null) {
                            updateDetailCard(selectedDate);
                        }
                        isInitialLoad = false;
                    }
                }
            }

            @Override
            public void onFailure(Call<List<AttendanceInfoResponse>> call, Throwable t) {
                Log.e(TAG, "Network Error", t);
            }
        });
    }

    private void updateCalendarDecorators(List<AttendanceInfoResponse> list) {
        HashSet<CalendarDay> normalDates = new HashSet<>();
        HashSet<CalendarDay> lateDates = new HashSet<>();
        HashSet<CalendarDay> absentDates = new HashSet<>();
        HashSet<CalendarDay> holidayDates = new HashSet<>();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        for (AttendanceInfoResponse info : list) {
            try {
                String dateString = info.getADate();
                if (dateString == null) continue;
                
                Date date = sdf.parse(dateString);
                if (date != null) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(date);
                    CalendarDay day = CalendarDay.from(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
                    
                    if ("Y".equalsIgnoreCase(info.getIsOff())) {
                        holidayDates.add(day);
                        continue;
                    }

                    String status = info.getStatus();
                    if (status != null) {
                        String lowerStatus = status.toLowerCase();
                        if (lowerStatus.contains("normal") || lowerStatus.contains("정상")) {
                            normalDates.add(day);
                        } else if (lowerStatus.contains("late") || lowerStatus.contains("early") || 
                                   lowerStatus.contains("지각") || lowerStatus.contains("조퇴")) {
                            lateDates.add(day);
                        } else if (lowerStatus.contains("absent") || lowerStatus.contains("결석")) {
                            absentDates.add(day);
                        }
                    }
                }
            } catch (ParseException e) {
                Log.e(TAG, "Date parse error: " + info.getADate(), e);
            }
        }

        calendarView.removeDecorators();
        if (!normalDates.isEmpty()) calendarView.addDecorator(new EventDecorator(COLOR_NORMAL, normalDates));
        if (!lateDates.isEmpty()) calendarView.addDecorator(new EventDecorator(COLOR_LATE, lateDates));
        if (!absentDates.isEmpty()) calendarView.addDecorator(new EventDecorator(COLOR_ABSENT, absentDates));
        if (!holidayDates.isEmpty()) calendarView.addDecorator(new EventDecorator(COLOR_HOLIDAY, holidayDates));
    }
    
    private void updateDetailCard(CalendarDay date) {
        String selectedDateStr = String.format(Locale.getDefault(), "%d-%02d-%02d",
                date.getYear(),
                date.getMonth(),
                date.getDay());

        tvSelectedDate.setText(selectedDateStr);

        AttendanceInfoResponse selectedInfo = null;
        for (AttendanceInfoResponse info : monthlyAttendanceList) {
            if (selectedDateStr.equals(info.getADate())) {
                selectedInfo = info;
                break;
            }
        }

        if (selectedInfo != null) {
            if ("Y".equalsIgnoreCase(selectedInfo.getIsOff())) {
                tvDetailCheckIn.setText("-");
                tvDetailCheckOut.setText("-");
                tvDetailStatus.setText("휴일");
                tvDetailStatus.setBackgroundResource(R.drawable.bg_status_label);
                tvDetailStatus.setBackgroundTintList(ColorStateList.valueOf(COLOR_HOLIDAY));
            } else {
                String inTime = selectedInfo.getArrivalTime();
                String outTime = selectedInfo.getLeavingTime();
                
                if (inTime != null && inTime.contains("T")) inTime = inTime.substring(11, 19); 
                if (outTime != null && outTime.contains("T")) outTime = outTime.substring(11, 19);

                tvDetailCheckIn.setText(inTime != null ? inTime : "-");
                tvDetailCheckOut.setText(outTime != null ? outTime : "-");

                String status = selectedInfo.getStatus();
                String displayStatus = "기록 없음";
                int bgColor = Color.TRANSPARENT;

                if (status != null) {
                    String lowerStatus = status.toLowerCase();
                    if (lowerStatus.contains("normal") || lowerStatus.contains("정상")) {
                        displayStatus = "출석";
                        bgColor = COLOR_NORMAL;
                    } else if (lowerStatus.contains("late") || lowerStatus.contains("early") || 
                               lowerStatus.contains("지각") || lowerStatus.contains("조퇴")) {
                        displayStatus = "지각/조퇴";
                        bgColor = COLOR_LATE;
                    } else if (lowerStatus.contains("absent") || lowerStatus.contains("결석")) {
                        displayStatus = "결석";
                        bgColor = COLOR_ABSENT;
                    } else {
                        displayStatus = status;
                    }
                }
                
                tvDetailStatus.setText(displayStatus);
                if (bgColor != Color.TRANSPARENT) {
                    tvDetailStatus.setBackgroundResource(R.drawable.bg_status_label);
                    tvDetailStatus.setBackgroundTintList(ColorStateList.valueOf(bgColor));
                } else {
                    tvDetailStatus.setBackground(null);
                }
            }
        } else {
            tvDetailCheckIn.setText("-");
            tvDetailCheckOut.setText("-");
            tvDetailStatus.setText("기록 없음");
            tvDetailStatus.setBackground(null);
        }
    }
}
