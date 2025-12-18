package com.example.scsaattend.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.example.scsaattend.R;
import com.example.scsaattend.network.ApiService;
import com.example.scsaattend.network.RetrofitClient;
import com.example.scsaattend.dto.SearchAttendanceRequest;
import com.example.scsaattend.dto.AttendanceInfoResponse;
import com.example.scsaattend.util.EventDecorator;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyAttendanceFragment extends Fragment {
    private static final String TAG = "MyAttendanceFragment";
    private TextView tvCurrentMonth;
    private MaterialCalendarView calendarView;
    private ImageButton btnPrevMonth, btnNextMonth;
    private ProgressBar pbLoading;
    private ApiService apiService;
    private Call<List<AttendanceInfoResponse>> currentCall; 

    private View cardDetail;
    private TextView tvSelectedDate, tvDetailCheckIn, tvDetailCheckOut, tvDetailStatus;
    private Button btnViewDetail;

    private List<AttendanceInfoResponse> monthlyAttendanceList = new ArrayList<>();
    private AttendanceInfoResponse selectedAttendanceInfo; 
    private boolean isInitialLoad = true; 
    private String userRole;

    private static final int COLOR_NORMAL = Color.parseColor("#A5D6A7"); 
    private static final int COLOR_LATE = Color.parseColor("#FFCC80");   
    private static final int COLOR_ABSENT = Color.parseColor("#EF9A9A");
    private static final int COLOR_HOLIDAY = Color.parseColor("#E0E0E0");
    private static final int COLOR_DISABLED = Color.parseColor("#757575");

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_attendance, container, false);
        apiService = RetrofitClient.getClient().create(ApiService.class);

        SharedPreferences prefs = requireActivity().getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE);
        userRole = prefs.getString("user_role", "ROLE_USER");

        tvCurrentMonth = view.findViewById(R.id.tvCurrentMonth);
        calendarView = view.findViewById(R.id.calendarView);
        btnPrevMonth = view.findViewById(R.id.btnPrevMonth);
        btnNextMonth = view.findViewById(R.id.btnNextMonth);
        pbLoading = view.findViewById(R.id.pbLoading);
        
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
        fetchMonthlyAttendance(today, false);

        btnPrevMonth.setOnClickListener(v -> moveMonth(-1));
        btnNextMonth.setOnClickListener(v -> moveMonth(1));
        
        btnViewDetail.setOnClickListener(v -> {
            if (selectedAttendanceInfo != null) showEditDialog(selectedAttendanceInfo);
            else Toast.makeText(getContext(), "데이터가 없습니다.", Toast.LENGTH_SHORT).show();
        });

        return view;
    }

    private void setupCalendarListeners() {
        calendarView.setTopbarVisible(false); 
        calendarView.setPagingEnabled(false); 
        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            if (selected) updateDetailCard(date);
        });
    }

    private void showEditDialog(AttendanceInfoResponse info) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View v = getLayoutInflater().inflate(R.layout.dialog_attendance_edit, null);
        builder.setView(v);
        AlertDialog dialog = builder.create();

        TextView tvTitle = v.findViewById(R.id.tvEditTitle);
        TextView tvDate = v.findViewById(R.id.tvEditDate);
        TextView tvType = v.findViewById(R.id.tvEditTypeName);
        TextView tvInTime = v.findViewById(R.id.tvEditInTime);
        TextView tvOutTime = v.findViewById(R.id.tvEditOutTime);
        Spinner spStatus = v.findViewById(R.id.spStatus);
        Spinner spApproval = v.findViewById(R.id.spApproval);
        Spinner spOfficial = v.findViewById(R.id.spOfficial);
        EditText etMemNote = v.findViewById(R.id.etMemNote);
        EditText etAdminNote = v.findViewById(R.id.etAdminNote);
        Button btnSave = v.findViewById(R.id.btnEditSave);

        tvTitle.setText(info.getMember().getName() + "님 출결 상세");
        tvDate.setText("날짜 : " + info.getADate());
        
        String typeInfo = "-";
        if (info.getAttendanceType() != null) {
            typeInfo = info.getAttendanceType().getName() + " (" + formatShortTime(info.getAttendanceType().getStartTime()) + " ~ " + formatShortTime(info.getAttendanceType().getEndTime()) + ")";
        }
        tvType.setText("유형 : " + typeInfo);

        tvInTime.setText(formatLongTime(info.getArrivalTime()));
        tvOutTime.setText(formatLongTime(info.getLeavingTime()));

        setupStatusSpinner(spStatus, info.getStatus());
        setupApprovalSpinner(spApproval, info.getIsApproved());
        setupOfficialSpinner(spOfficial, info.getIsOfficial());

        etMemNote.setText(info.getMemNote() != null ? info.getMemNote() : "");
        etAdminNote.setText(info.getAdminNote() != null ? info.getAdminNote() : "");

        boolean isAdmin = "ROLE_ADMIN".equals(userRole);
        tvInTime.setEnabled(false); tvInTime.setTextColor(COLOR_DISABLED);
        tvOutTime.setEnabled(false); tvOutTime.setTextColor(COLOR_DISABLED);
        spStatus.setEnabled(false);
        spApproval.setEnabled(false);
        spOfficial.setEnabled(false);
        etAdminNote.setEnabled(false); etAdminNote.setTextColor(COLOR_DISABLED);
        tvType.setTextColor(COLOR_DISABLED);
        tvDate.setTextColor(COLOR_DISABLED);

        etMemNote.setEnabled(true);
        etMemNote.setTextColor(Color.BLACK);

        btnSave.setOnClickListener(view -> {
            String newNote = etMemNote.getText().toString();
            Map<String, String> requestData = new HashMap<>();
            requestData.put("memNote", newNote);

            apiService.updateMemberNote(info.getAinfoId(), requestData).enqueue(new Callback<Object>() {
                @Override
                public void onResponse(Call<Object> call, Response<Object> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(getContext(), "사유가 수정되었습니다.", Toast.LENGTH_SHORT).show();
                        info.setMemNote(newNote); 
                        dialog.dismiss();
                    } else { Toast.makeText(getContext(), "수정 실패", Toast.LENGTH_SHORT).show(); }
                }
                @Override
                public void onFailure(Call<Object> call, Throwable t) {
                    Toast.makeText(getContext(), "네트워크 오류", Toast.LENGTH_SHORT).show();
                }
            });
        });

        v.findViewById(R.id.btnEditCancel).setOnClickListener(view -> dialog.dismiss());
        dialog.show();
    }

    private void setupStatusSpinner(Spinner sp, String current) {
        String[] options = {"출석", "지각/조퇴", "결석", "미정"};
        sp.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, options));
        if ("normal".equals(current)) sp.setSelection(0);
        else if ("late/early".equals(current)) sp.setSelection(1);
        else if ("absent".equals(current)) sp.setSelection(2);
        else sp.setSelection(3);
    }

    private void setupApprovalSpinner(Spinner sp, String current) {
        String[] options = {"승인", "불허", "미정"};
        sp.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, options));
        if ("approved".equalsIgnoreCase(current)) sp.setSelection(0);
        else if ("denied".equalsIgnoreCase(current)) sp.setSelection(1);
        else sp.setSelection(2);
    }

    private void setupOfficialSpinner(Spinner sp, String current) {
        String[] options = {"O (공결)", "X (일반)", "미정"};
        sp.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, options));
        if ("Y".equalsIgnoreCase(current)) sp.setSelection(0);
        else if ("N".equalsIgnoreCase(current)) sp.setSelection(1);
        else sp.setSelection(2);
    }

    private void moveMonth(int offset) {
        CalendarDay current = calendarView.getCurrentDate();
        Calendar cal = Calendar.getInstance();
        cal.set(current.getYear(), current.getMonth() - 1, 1);
        cal.add(Calendar.MONTH, offset);
        CalendarDay targetDay = CalendarDay.from(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, 1);
        fetchMonthlyAttendance(targetDay, true);
    }

    private void fetchMonthlyAttendance(CalendarDay targetDate, boolean shouldMove) {
        if (currentCall != null) currentCall.cancel();
        SharedPreferences prefs = requireContext().getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE);
        long memId = prefs.getLong("user_numeric_id", -1);
        if (memId == -1) return;

        pbLoading.setVisibility(View.VISIBLE);
        btnPrevMonth.setEnabled(false); btnNextMonth.setEnabled(false);

        Calendar cal = Calendar.getInstance();
        cal.set(targetDate.getYear(), targetDate.getMonth() - 1, 1);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String start = sdf.format(cal.getTime());
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        String end = sdf.format(cal.getTime());

        List<Long> idList = new ArrayList<>(); idList.add(memId);
        currentCall = apiService.searchAttendance(new SearchAttendanceRequest(start, end, idList));
        currentCall.enqueue(new Callback<List<AttendanceInfoResponse>>() {
            @Override
            public void onResponse(Call<List<AttendanceInfoResponse>> call, Response<List<AttendanceInfoResponse>> response) {
                pbLoading.setVisibility(View.GONE);
                btnPrevMonth.setEnabled(true); btnNextMonth.setEnabled(true);
                if (response.isSuccessful() && response.body() != null) {
                    monthlyAttendanceList = response.body();
                    updateCalendarDecorators(monthlyAttendanceList);
                    if (shouldMove) { calendarView.setCurrentDate(targetDate); updateMonthDisplay(); }
                    if (isInitialLoad) { updateDetailCard(calendarView.getSelectedDate()); isInitialLoad = false; }
                }
            }
            @Override
            public void onFailure(Call<List<AttendanceInfoResponse>> call, Throwable t) {
                pbLoading.setVisibility(View.GONE);
                btnPrevMonth.setEnabled(true); btnNextMonth.setEnabled(true);
            }
        });
    }

    private void updateCalendarDecorators(List<AttendanceInfoResponse> list) {
        HashSet<CalendarDay> normal = new HashSet<>(), late = new HashSet<>(), absent = new HashSet<>(), holiday = new HashSet<>();
        for (AttendanceInfoResponse info : list) {
            String d = info.getADate();
            if (d == null || d.length() < 10) continue;
            try {
                int y = Integer.parseInt(d.substring(0, 4)), m = Integer.parseInt(d.substring(5, 7)), day = Integer.parseInt(d.substring(8, 10));
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
        String dateStr = String.format(Locale.getDefault(), "%d-%02d-%02d", date.getYear(), date.getMonth(), date.getDay());
        tvSelectedDate.setText(dateStr);
        selectedAttendanceInfo = null;
        for (AttendanceInfoResponse info : monthlyAttendanceList) { if (dateStr.equals(info.getADate())) { selectedAttendanceInfo = info; break; } }
        
        if (selectedAttendanceInfo != null) {
            if ("Y".equalsIgnoreCase(selectedAttendanceInfo.getIsOff())) {
                setDetailText("-", "-", "휴일", COLOR_HOLIDAY);
                setDetailButtonEnabled(false); 
            } else {
                String s = getKoreanStatus(selectedAttendanceInfo.getStatus());
                setDetailText(formatLongTime(selectedAttendanceInfo.getArrivalTime()), formatLongTime(selectedAttendanceInfo.getLeavingTime()), s, getStatusColor(s));
                setDetailButtonEnabled(true); 
            }
        } else {
            setDetailText("-", "-", "기록 없음", Color.TRANSPARENT);
            setDetailButtonEnabled(false);
        }
    }

    private void setDetailButtonEnabled(boolean enabled) {
        btnViewDetail.setEnabled(enabled);
        btnViewDetail.setBackgroundTintList(ColorStateList.valueOf(enabled ? ContextCompat.getColor(requireContext(), R.color.scsa_blue) : COLOR_DISABLED));
    }

    private void setDetailText(String in, String out, String status, int color) {
        tvDetailCheckIn.setText(in); tvDetailCheckOut.setText(out); tvDetailStatus.setText(status);
        if (color != Color.TRANSPARENT) {
            tvDetailStatus.setBackgroundResource(R.drawable.bg_status_label);
            tvDetailStatus.setBackgroundTintList(ColorStateList.valueOf(color));
        } else { tvDetailStatus.setBackground(null); }
    }

    private int getStatusColor(String status) {
        if ("출석".equals(status)) return COLOR_NORMAL;
        if ("지각/조퇴".equals(status)) return COLOR_LATE;
        if ("결석".equals(status)) return COLOR_ABSENT;
        return Color.TRANSPARENT;
    }

    private String getKoreanStatus(String status) {
        if (status == null) return "기록 없음";
        String lower = status.toLowerCase();
        if (lower.contains("normal") || lower.contains("정상")) return "출석";
        if (lower.contains("late") || lower.contains("early") || lower.contains("지각") || lower.contains("조퇴")) return "지각/조퇴";
        if (lower.contains("absent") || lower.contains("결석")) return "결석";
        return "기록 없음";
    }

    private String formatShortTime(String time) {
        return (time == null || time.length() < 5) ? "-" : time.substring(0, 5);
    }

    private String formatLongTime(String time) {
        if (time == null || !time.contains("T")) return "-";
        try { return time.substring(11, 19); } catch (Exception e) { return "-"; }
    }

    private void updateMonthDisplay() {
        CalendarDay current = calendarView.getCurrentDate();
        tvCurrentMonth.setText(String.format(Locale.KOREAN, "%d년 %02d월", current.getYear(), current.getMonth()));
    }
}
