package com.example.scsaattend.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

    // 월별 출결 데이터 저장
    private List<AttendanceInfoResponse> monthlyAttendanceList = new ArrayList<>();

    // 상태별 색상
    private static final int COLOR_NORMAL = Color.parseColor("#A5D6A7"); 
    private static final int COLOR_LATE = Color.parseColor("#FFCC80");   
    private static final int COLOR_ABSENT = Color.parseColor("#EF9A9A");

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
        
        // 상세 정보 UI 연결
        cardDetail = view.findViewById(R.id.cardDetail);
        tvSelectedDate = view.findViewById(R.id.tvSelectedDate);
        tvDetailCheckIn = view.findViewById(R.id.tvDetailCheckIn);
        tvDetailCheckOut = view.findViewById(R.id.tvDetailCheckOut);
        tvDetailStatus = view.findViewById(R.id.tvDetailStatus);

        // 오늘 날짜를 기본 선택으로 설정
        CalendarDay today = CalendarDay.today();
        calendarView.setSelectedDate(today);
        calendarView.setCurrentDate(today);

        // 초기 설정
        updateMonthDisplay();
        setupCalendarListeners();
        fetchMonthlyAttendance();

        btnPrevMonth.setOnClickListener(v -> calendarView.goToPrevious());
        btnNextMonth.setOnClickListener(v -> calendarView.goToNext());

        return view;
    }

    private void setupCalendarListeners() {
        calendarView.setTopbarVisible(false); 
        
        // 월 변경 리스너
        calendarView.setOnMonthChangedListener((widget, date) -> {
            currentCalendar.set(date.getYear(), date.getMonth() - 1, 1);
            updateMonthDisplay();
            fetchMonthlyAttendance();
        });

        // 날짜 선택 리스너
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

        if (memId == -1) {
            Toast.makeText(getContext(), "로그인 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

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
                    monthlyAttendanceList = response.body(); // 월별 데이터 저장
                    updateCalendarDecorators(monthlyAttendanceList);

                    // 선택된 날짜(기본값 오늘)의 상세 정보 업데이트
                    CalendarDay selectedDate = calendarView.getSelectedDate();
                    if (selectedDate != null) {
                        updateDetailCard(selectedDate);
                    }
                } else {
                    Log.e(TAG, "Failed to fetch attendance: " + response.code());
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
        if (!normalDates.isEmpty()) {
            calendarView.addDecorator(new EventDecorator(COLOR_NORMAL, normalDates));
        }
        if (!lateDates.isEmpty()) {
            calendarView.addDecorator(new EventDecorator(COLOR_LATE, lateDates));
        }
        if (!absentDates.isEmpty()) {
            calendarView.addDecorator(new EventDecorator(COLOR_ABSENT, absentDates));
        }
    }
    
    private void updateDetailCard(CalendarDay date) {
        // 직접 년, 월, 일을 받아서 "yyyy-MM-dd" 형식으로 만듭니다.
        String selectedDateStr = String.format(Locale.getDefault(), "%d-%02d-%02d",
                date.getYear(),
                date.getMonth(),
                date.getDay());

        tvSelectedDate.setText(selectedDateStr);

        // 저장된 리스트에서 해당 날짜 정보 찾기
        AttendanceInfoResponse selectedInfo = null;
        for (AttendanceInfoResponse info : monthlyAttendanceList) {
            if (selectedDateStr.equals(info.getADate())) {
                selectedInfo = info;
                break;
            }
        }

        if (selectedInfo != null) {
            String inTime = selectedInfo.getArrivalTime();
            String outTime = selectedInfo.getLeavingTime();
            
            // 시간 포맷팅
            if (inTime != null && inTime.contains("T")) inTime = inTime.substring(11, 19); 
            if (outTime != null && outTime.contains("T")) outTime = outTime.substring(11, 19);

            tvDetailCheckIn.setText(inTime != null ? inTime : "-");
            tvDetailCheckOut.setText(outTime != null ? outTime : "-");
            tvDetailStatus.setText(selectedInfo.getStatus() != null ? selectedInfo.getStatus() : "기록 없음");
        } else {
            // 해당 날짜에 데이터가 없는 경우
            tvDetailCheckIn.setText("-");
            tvDetailCheckOut.setText("-");
            tvDetailStatus.setText("기록 없음");
        }
    }
}
