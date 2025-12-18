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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.scsaattend.R;
import com.example.scsaattend.dto.AttendanceInfoResponse;
import com.example.scsaattend.dto.AttendanceTypeResponse;
import com.example.scsaattend.dto.BatchUpdateRequest;
import com.example.scsaattend.dto.BatchUpdateResponse;
import com.example.scsaattend.dto.CalculateStatusRequest;
import com.example.scsaattend.dto.ErrorResponse;
import com.example.scsaattend.dto.MemberResponse;
import com.example.scsaattend.dto.SearchAttendanceRequest;
import com.example.scsaattend.dto.StatusResponse;
import com.example.scsaattend.network.ApiService;
import com.example.scsaattend.network.RetrofitClient;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.gson.Gson;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AttendanceDetailFragment extends Fragment implements UserSelectionDialogFragment.OnUsersSelectedListener, BatchUpdateDialogFragment.OnUpdateListener, AttendanceTypeSelectionDialogFragment.OnAttendanceTypeSelectedListener {

    private static final String TAG = "AttendanceDetailFrag";
    private Button btnStartDate, btnEndDate, btnUserSelect, btnQuery;
    private TextView tvRecordCount;
    private RecyclerView recyclerView;
    private AttendanceDetailAdapter adapter;
    private List<AttendanceDetailItem> attendanceDetailList = new ArrayList<>();
    private ArrayList<SelectableUser> userList = new ArrayList<>();
    private long selectedStartDate = -1;
    private long selectedEndDate = -1;
    private ApiService apiService;
    private LinearLayout filterContentLayout, bottomActionBar;
    private ImageButton btnToggleFilter;
    private Button btnSelectAll, btnDeselectAll, btnBatchChange;

    private boolean isSelectionMode = false;
    private Set<Long> selectedItems = new HashSet<>();
    private final Gson gson = new Gson();
    private String userRole;

    public AttendanceDetailFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_attendance_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SharedPreferences prefs = requireActivity().getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE);
        userRole = prefs.getString("user_role", "ROLE_USER");

        btnStartDate = view.findViewById(R.id.btn_start_date);
        btnEndDate = view.findViewById(R.id.btn_end_date);
        btnUserSelect = view.findViewById(R.id.btn_user_select);
        btnQuery = view.findViewById(R.id.btn_query);
        recyclerView = view.findViewById(R.id.recycler_view_attendance_detail);
        tvRecordCount = view.findViewById(R.id.tv_record_count);
        filterContentLayout = view.findViewById(R.id.filter_content_layout);
        btnToggleFilter = view.findViewById(R.id.btn_toggle_filter);
        bottomActionBar = view.findViewById(R.id.bottom_action_bar);

        btnSelectAll = view.findViewById(R.id.btn_select_all);
        btnDeselectAll = view.findViewById(R.id.btn_deselect_all);
        btnBatchChange = view.findViewById(R.id.btn_batch_change);
        
        bottomActionBar.setVisibility(View.GONE);

        apiService = RetrofitClient.getClient().create(ApiService.class);

        btnStartDate.setOnClickListener(v -> showDatePicker(true));
        btnEndDate.setOnClickListener(v -> showDatePicker(false));
        btnUserSelect.setOnClickListener(v -> {
            UserSelectionDialogFragment dialog = UserSelectionDialogFragment.newInstance(userList);
            dialog.setOnUsersSelectedListener(this);
            dialog.show(getParentFragmentManager(), "UserSelectionDialog");
        });
        btnQuery.setOnClickListener(v -> searchAttendanceData());
        btnToggleFilter.setOnClickListener(v -> toggleFilterVisibility());
        btnSelectAll.setOnClickListener(v -> selectAllItems());
        btnDeselectAll.setOnClickListener(v -> exitSelectionMode()); 
        btnBatchChange.setOnClickListener(v -> {
            if (selectedItems.isEmpty()) {
                Toast.makeText(getContext(), "변경할 항목을 선택해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            showBatchChangeOptions();
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AttendanceDetailAdapter(attendanceDetailList, this);
        recyclerView.setAdapter(adapter);

        initializeAndLoadData();
    }

    private void initializeAndLoadData() {
        selectedStartDate = MaterialDatePicker.todayInUtcMilliseconds();
        selectedEndDate = MaterialDatePicker.todayInUtcMilliseconds();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String today = sdf.format(new Date());
        btnStartDate.setText(today);
        btnEndDate.setText(today);
        btnEndDate.setEnabled(true);

        fetchAllUsersAndSearch();
    }

    private void fetchAllUsersAndSearch() {
        apiService.getMembers().enqueue(new Callback<List<MemberResponse>>() {
            @Override
            public void onResponse(Call<List<MemberResponse>> call, Response<List<MemberResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    userList.clear();
                    for (MemberResponse member : response.body()) {
                        userList.add(new SelectableUser(member.getId(), member.getName(), false));
                    }
                    updateUserSelectionButtonText();
                    searchAttendanceData();
                }
            }
            @Override
            public void onFailure(Call<List<MemberResponse>> call, Throwable t) {}
        });
    }

    private void showBatchChangeOptions() {
        final CharSequence[] options = {"출결 자동 결정", "출결 유형 변경", "휴일 여부 변경"};
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("일괄 변경 작업 선택");
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0: calculateStatus(); break;
                case 1:
                    AttendanceTypeSelectionDialogFragment typeDialog = AttendanceTypeSelectionDialogFragment.newInstance();
                    typeDialog.setOnAttendanceTypeSelectedListener(AttendanceDetailFragment.this);
                    typeDialog.show(getParentFragmentManager(), "AttendanceTypeSelectionDialog");
                    break;
                case 2: showIsOffSelectionDialog(); break;
            }
        });
        builder.show();
    }

    private void calculateStatus() {
        List<Long> aInfoIdList = new ArrayList<>(selectedItems);
        CalculateStatusRequest request = new CalculateStatusRequest(aInfoIdList);
        apiService.calculateAttendanceStatus(request).enqueue(new Callback<StatusResponse>() {
            @Override
            public void onResponse(@NonNull Call<StatusResponse> call, @NonNull Response<StatusResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(getContext(), response.body().getMessage(), Toast.LENGTH_LONG).show();
                    searchAttendanceData();
                }
                exitSelectionMode();
            }
            @Override
            public void onFailure(@NonNull Call<StatusResponse> call, @NonNull Throwable t) { exitSelectionMode(); }
        });
    }

    private void showIsOffSelectionDialog() {
        final CharSequence[] isOffOptions = {"휴일", "수업일"};
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("휴일 여부 변경");
        builder.setItems(isOffOptions, (dialog, which) -> {
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("isOff", which == 0 ? "Y" : "N");
            onUpdate(updateData);
        });
        builder.show();
    }

    private void toggleFilterVisibility() {
        if (filterContentLayout.getVisibility() == View.VISIBLE) {
            filterContentLayout.setVisibility(View.GONE);
            btnToggleFilter.setImageResource(R.drawable.ic_arrow_down);
        } else {
            filterContentLayout.setVisibility(View.VISIBLE);
            btnToggleFilter.setImageResource(R.drawable.ic_arrow_up);
        }
    }
    
    public void startSelectionMode(int position) {
        isSelectionMode = true;
        selectedItems.clear();
        selectedItems.add(attendanceDetailList.get(position).aInfoId);
        bottomActionBar.setVisibility(View.VISIBLE);
        updateAllItems();
    }

    public void toggleSelection(int position) {
        if (position == -1) return; 
        long aInfoId = attendanceDetailList.get(position).aInfoId;
        if (selectedItems.contains(aInfoId)) selectedItems.remove(aInfoId);
        else selectedItems.add(aInfoId);
        adapter.notifyDataSetChanged();
        if (selectedItems.isEmpty()) exitSelectionMode();
    }

    private void exitSelectionMode() {
        isSelectionMode = false;
        selectedItems.clear();
        bottomActionBar.setVisibility(View.GONE);
        updateAllItems();
    }

    private void selectAllItems() {
        for (AttendanceDetailItem item : attendanceDetailList) selectedItems.add(item.aInfoId);
        updateAllItems();
    }

    private void updateAllItems() { adapter.notifyDataSetChanged(); }
    
    private void searchAttendanceData() {
        if (isSelectionMode) exitSelectionMode();
        if (selectedStartDate == -1 || selectedEndDate == -1) return;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String startDate = sdf.format(new Date(selectedStartDate));
        String endDate = sdf.format(new Date(selectedEndDate));

        List<Long> selectedMemberIds = userList.stream().filter(u -> u.isSelected).map(u -> u.id).collect(Collectors.toList());
        if (selectedMemberIds.isEmpty() && !userList.isEmpty()) selectedMemberIds = userList.stream().map(u -> u.id).collect(Collectors.toList());
        if (selectedMemberIds.isEmpty()) return;

        SearchAttendanceRequest request = new SearchAttendanceRequest(startDate, endDate, selectedMemberIds);
        apiService.searchAttendance(request).enqueue(new Callback<List<AttendanceInfoResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<AttendanceInfoResponse>> call, @NonNull Response<List<AttendanceInfoResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<AttendanceInfoResponse> responseData = response.body();
                    Collections.sort(responseData, Comparator.comparing(AttendanceInfoResponse::getADate).thenComparing(res -> res.getMember().getId()));
                    
                    attendanceDetailList.clear();
                    for (AttendanceInfoResponse res : responseData) {
                        attendanceDetailList.add(new AttendanceDetailItem(
                                res.getAinfoId(), res.getADate(), res.getMember().getName(), res.getIsOff(),
                                res.getArrivalTime(), res.getLeavingTime(), res.getStatus(), res.getIsApproved(),
                                res.getIsOfficial(), res.getAttendanceType(), res.getMemNote(), res.getAdminNote()
                        ));
                    }
                    adapter.notifyDataSetChanged();
                    updateRecordCount();
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<AttendanceInfoResponse>> call, @NonNull Throwable t) {}
        });
    }

    private void showEditDialog(AttendanceDetailItem item) {
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
        Button btnCancel = v.findViewById(R.id.btnEditCancel), btnSave = v.findViewById(R.id.btnEditSave);

        tvTitle.setText(item.name + "님 출결 수정");
        tvDate.setText("날짜 : " + item.fullDate);
        
        // 유형 이름 옆에 시간 정보 추가 (학생 모드와 동일)
        String typeInfo = "-";
        if (item.atype != null) {
            typeInfo = item.atype.getName() + " (" + formatShortTime(item.atype.getStartTime()) + " ~ " + formatShortTime(item.atype.getEndTime()) + ")";
        }
        tvType.setText("유형 : " + typeInfo);

        tvInTime.setText(formatLongTime(item.checkIn));
        tvOutTime.setText(formatLongTime(item.checkOut));

        setupStatusSpinner(spStatus, item.status);
        setupApprovalSpinner(spApproval, item.approval);
        setupOfficialSpinner(spOfficial, item.publicLeave);

        etMemNote.setText(item.memNote != null ? item.memNote : "");
        etAdminNote.setText(item.adminNote != null ? item.adminNote : "");

        boolean isAdmin = "ROLE_ADMIN".equals(userRole);
        tvInTime.setEnabled(isAdmin);
        tvOutTime.setEnabled(isAdmin);
        spStatus.setEnabled(isAdmin);
        spApproval.setEnabled(isAdmin);
        spOfficial.setEnabled(isAdmin);
        etAdminNote.setEnabled(isAdmin);
        etMemNote.setEnabled(!isAdmin);

        if (isAdmin) {
            tvInTime.setOnClickListener(view -> showCustomTimePicker(tvInTime));
            tvOutTime.setOnClickListener(view -> showCustomTimePicker(tvOutTime));
        }

        btnCancel.setOnClickListener(view -> dialog.dismiss());
        btnSave.setOnClickListener(view -> {
            Map<String, Object> updateData = new HashMap<>();
            if (isAdmin) {
                String inStr = tvInTime.getText().toString();
                String outStr = tvOutTime.getText().toString();
                updateData.put("arrivalTime", "-".equals(inStr) ? null : inStr);
                updateData.put("leavingTime", "-".equals(outStr) ? null : outStr);
                updateData.put("status", getSelectedStatus(spStatus));
                updateData.put("isApproved", getSelectedApproval(spApproval));
                updateData.put("isOfficial", getSelectedOfficial(spOfficial));
                updateData.put("adminNote", etAdminNote.getText().toString());
            } else {
                updateData.put("memNote", etMemNote.getText().toString());
            }
            saveAttendanceUpdate(item.aInfoId, updateData, dialog);
        });

        dialog.show();
    }

    private void showCustomTimePicker(TextView targetTextView) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_custom_time_picker, null);
        builder.setView(dialogView);

        NumberPicker hourPicker = dialogView.findViewById(R.id.picker_hour);
        NumberPicker minutePicker = dialogView.findViewById(R.id.picker_minute);
        NumberPicker secondPicker = dialogView.findViewById(R.id.picker_second);
        CheckBox cbClear = dialogView.findViewById(R.id.cb_clear_time);

        NumberPicker.Formatter formatter = value -> String.format(Locale.getDefault(), "%02d", value);
        hourPicker.setMinValue(0); hourPicker.setMaxValue(23); hourPicker.setFormatter(formatter);
        minutePicker.setMinValue(0); minutePicker.setMaxValue(59); minutePicker.setFormatter(formatter);
        secondPicker.setMinValue(0); secondPicker.setMaxValue(59); secondPicker.setFormatter(formatter);

        String currentTime = targetTextView.getText().toString();
        if ("-".equals(currentTime)) {
            cbClear.setChecked(true);
            hourPicker.setEnabled(false); minutePicker.setEnabled(false); secondPicker.setEnabled(false);
        } else if (currentTime.length() >= 8) {
            hourPicker.setValue(Integer.parseInt(currentTime.substring(0, 2)));
            minutePicker.setValue(Integer.parseInt(currentTime.substring(3, 5)));
            secondPicker.setValue(Integer.parseInt(currentTime.substring(6, 8)));
        }

        cbClear.setOnCheckedChangeListener((buttonView, isChecked) -> {
            hourPicker.setEnabled(!isChecked);
            minutePicker.setEnabled(!isChecked);
            secondPicker.setEnabled(!isChecked);
        });

        builder.setTitle("시간 선택");
        builder.setPositiveButton("확인", (dialog, which) -> {
            if (cbClear.isChecked()) {
                targetTextView.setText("-");
            } else {
                String time = String.format(Locale.getDefault(), "%02d:%02d:%02d", hourPicker.getValue(), minutePicker.getValue(), secondPicker.getValue());
                targetTextView.setText(time);
            }
        });
        builder.setNegativeButton("취소", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void setupStatusSpinner(Spinner sp, String current) {
        String[] options = {"출석", "지각/조퇴", "결석", "미정"};
        sp.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, options));
        if ("normal".equals(current)) sp.setSelection(0);
        else if ("late/early".equals(current)) sp.setSelection(1);
        else if ("absent".equals(current)) sp.setSelection(2);
        else sp.setSelection(3);
    }

    private String getSelectedStatus(Spinner sp) {
        int pos = sp.getSelectedItemPosition();
        if (pos == 0) return "normal";
        if (pos == 1) return "late/early";
        if (pos == 2) return "absent";
        return null; 
    }

    private void setupApprovalSpinner(Spinner sp, String current) {
        String[] options = {"승인", "불허", "미정"};
        sp.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, options));
        if ("approved".equalsIgnoreCase(current)) sp.setSelection(0);
        else if ("denied".equalsIgnoreCase(current)) sp.setSelection(1);
        else sp.setSelection(2);
    }

    private String getSelectedApproval(Spinner sp) {
        int pos = sp.getSelectedItemPosition();
        if (pos == 0) return "approved";
        if (pos == 1) return "denied";
        return null;
    }

    private void setupOfficialSpinner(Spinner sp, String current) {
        String[] options = {"O (공결)", "X (일반)", "미정"};
        sp.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, options));
        if ("Y".equalsIgnoreCase(current)) sp.setSelection(0);
        else if ("N".equalsIgnoreCase(current)) sp.setSelection(1);
        else sp.setSelection(2);
    }

    private String getSelectedOfficial(Spinner sp) {
        int pos = sp.getSelectedItemPosition();
        if (pos == 0) return "Y";
        if (pos == 1) return "N";
        return null;
    }

    private void saveAttendanceUpdate(long id, Map<String, Object> data, AlertDialog dialog) {
        apiService.updateAttendanceDetail(id, data).enqueue(new Callback<BatchUpdateResponse>() {
            @Override
            public void onResponse(@NonNull Call<BatchUpdateResponse> call, @NonNull Response<BatchUpdateResponse> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "수정되었습니다.", Toast.LENGTH_SHORT).show();
                    searchAttendanceData();
                    dialog.dismiss();
                } else { handleErrorResponse(response); }
            }
            @Override
            public void onFailure(@NonNull Call<BatchUpdateResponse> call, @NonNull Throwable t) {
                Toast.makeText(getContext(), "네트워크 오류", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleErrorResponse(Response<?> response) {
        String msg = "실패: " + response.code();
        if (response.errorBody() != null) {
            try {
                ErrorResponse err = gson.fromJson(response.errorBody().string(), ErrorResponse.class);
                if (err != null && err.getMessage() != null) msg = err.getMessage();
            } catch (Exception ignored) {}
        }
        Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();
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

    @Override
    public void onUsersSelected(ArrayList<SelectableUser> selectedUsers) {
        this.userList = selectedUsers;
        updateUserSelectionButtonText();
    }

    @Override
    public void onAttendanceTypeSelected(long typeId, String typeName) {
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("aTypeId", typeId);
        updateData.put("arrivalTime", null);
        updateData.put("leavingTime", null);
        updateData.put("status", null);
        updateData.put("isApproved", null);
        updateData.put("isOfficial", null);
        onUpdate(updateData);
    }
    
    @Override
    public void onUpdate(Map<String, Object> updateData) {
        List<Long> aInfoIdList = new ArrayList<>(selectedItems);
        apiService.batchUpdateAttendance(new BatchUpdateRequest(aInfoIdList, updateData)).enqueue(new Callback<BatchUpdateResponse>() {
            @Override
            public void onResponse(@NonNull Call<BatchUpdateResponse> call, @NonNull Response<BatchUpdateResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(getContext(), response.body().getMessage(), Toast.LENGTH_LONG).show();
                    searchAttendanceData();
                } else { handleErrorResponse(response); }
                exitSelectionMode();
            }
            @Override
            public void onFailure(@NonNull Call<BatchUpdateResponse> call, @NonNull Throwable t) { exitSelectionMode(); }
        });
    }

    private void updateUserSelectionButtonText() {
        long selectedCount = userList.stream().filter(u -> u.isSelected).count();
        btnUserSelect.setText((selectedCount == userList.size() || selectedCount == 0) ? "학생 선택: 전체" : "학생 선택: " + selectedCount + "명");
    }
    
    private void updateRecordCount() { tvRecordCount.setText("출결 목록 (" + attendanceDetailList.size() + "건)"); }

    private void showDatePicker(boolean isStartDate) {
        MaterialDatePicker.Builder<Long> builder = MaterialDatePicker.Builder.datePicker();
        builder.setTitleText(isStartDate ? "시작일 선택" : "종료일 선택");
        if (isStartDate && selectedStartDate != -1) builder.setSelection(selectedStartDate);
        else if (!isStartDate && selectedEndDate != -1) builder.setSelection(selectedEndDate);
        if (!isStartDate && selectedStartDate != -1) {
            CalendarConstraints.Builder constraintsBuilder = new CalendarConstraints.Builder();
            constraintsBuilder.setValidator(DateValidatorPointForward.from(selectedStartDate));
            builder.setCalendarConstraints(constraintsBuilder.build());
        }
        MaterialDatePicker<Long> datePicker = builder.build();
        datePicker.addOnPositiveButtonClickListener(selection -> {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String selectedDate = sdf.format(new Date(selection));
            if (isStartDate) {
                btnStartDate.setText(selectedDate);
                btnEndDate.setText(selectedDate);
                selectedStartDate = selection;
                selectedEndDate = selection;
                btnEndDate.setEnabled(true);
            } else {
                btnEndDate.setText(selectedDate);
                selectedEndDate = selection;
            }
        });
        datePicker.show(getParentFragmentManager(), isStartDate ? "START_DATE_PICKER" : "END_DATE_PICKER");
    }

    private static class AttendanceDetailItem {
        long aInfoId;
        String date, fullDate, name, isOff, checkIn, checkOut, status, approval, publicLeave, memNote, adminNote;
        AttendanceTypeResponse atype;

        public AttendanceDetailItem(long aInfoId, String fullDate, String name, String isOff, String checkIn, String checkOut, String status, String approval, String publicLeave, AttendanceTypeResponse atype, String memNote, String adminNote) {
            this.aInfoId = aInfoId;
            this.fullDate = fullDate;
            this.date = fullDate.length() >= 10 ? fullDate.substring(5) : fullDate;
            this.name = name;
            this.isOff = isOff;
            this.checkIn = checkIn;
            this.checkOut = checkOut;
            this.status = status;
            this.approval = approval;
            this.publicLeave = publicLeave;
            this.atype = atype;
            this.memNote = memNote;
            this.adminNote = adminNote;
        }
    }

    private class AttendanceDetailAdapter extends RecyclerView.Adapter<AttendanceDetailAdapter.ViewHolder> {
        private final List<AttendanceDetailItem> items;
        private final AttendanceDetailFragment fragment;

        public AttendanceDetailAdapter(List<AttendanceDetailItem> items, AttendanceDetailFragment fragment) {
            this.items = items;
            this.fragment = fragment;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_attendance_detail, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            AttendanceDetailItem item = items.get(position);
            holder.bind(item);
            holder.cardView.setCardBackgroundColor(fragment.isSelectionMode && fragment.selectedItems.contains(item.aInfoId) ? Color.LTGRAY : Color.WHITE);
            
            holder.itemView.setOnClickListener(v -> {
                if(fragment.isSelectionMode) fragment.toggleSelection(holder.getAdapterPosition());
                else fragment.showEditDialog(item); 
            });

            holder.itemView.setOnLongClickListener(v -> {
                if (!fragment.isSelectionMode) fragment.startSelectionMode(holder.getAdapterPosition());
                else fragment.toggleSelection(holder.getAdapterPosition());
                return true;
            });
        }

        @Override
        public int getItemCount() { return items.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvDate, tvName, tvCheckIn, tvCheckOut, tvStatus, tvApproval, tvPublicLeave;
            CardView cardView;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvDate = itemView.findViewById(R.id.tv_date);
                tvName = itemView.findViewById(R.id.tv_name);
                tvCheckIn = itemView.findViewById(R.id.tv_check_in);
                tvCheckOut = itemView.findViewById(R.id.tv_check_out);
                tvStatus = itemView.findViewById(R.id.tv_status);
                tvApproval = itemView.findViewById(R.id.tv_approval);
                tvPublicLeave = itemView.findViewById(R.id.tv_public_leave);
                cardView = (CardView) itemView; 
            }
            
            public void bind(AttendanceDetailItem item) {
                tvDate.setText(item.date);
                tvName.setText(item.name);
                if ("Y".equals(item.isOff)) {
                    tvStatus.setText("휴일");
                    tvStatus.setTextColor(Color.BLACK);
                    tvCheckIn.setText("-"); tvCheckOut.setText("-"); tvApproval.setText("-"); tvPublicLeave.setText("-");
                } else {
                    String formattedIn = fragment.formatLongTime(item.checkIn);
                    String formattedOut = fragment.formatLongTime(item.checkOut);
                    
                    tvCheckIn.setText("-".equals(formattedIn) ? "-" : (formattedIn.length() >= 5 ? formattedIn.substring(0, 5) : formattedIn));
                    tvCheckOut.setText("-".equals(formattedOut) ? "-" : (formattedOut.length() >= 5 ? formattedOut.substring(0, 5) : formattedOut));
                    
                    tvPublicLeave.setText(item.publicLeave != null ? ("Y".equals(item.publicLeave) ? "O" : "X") : "-");
                    
                    String koreanStatus = fragment.getKoreanStatus(item.status);
                    tvStatus.setText(koreanStatus);
                    
                    if ("기록 없음".equals(koreanStatus)) {
                        tvStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.gray_text));
                    } else {
                        tvStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), 
                            "normal".equals(item.status) ? R.color.status_text_normal : 
                            ("absent".equals(item.status) ? R.color.status_text_absent : R.color.status_text_late_early)));
                    }
                    
                    String approval = item.approval != null ? item.approval : "";
                    if ("approved".equalsIgnoreCase(approval)) {
                        tvApproval.setText("승인");
                        tvApproval.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.status_approved));
                    } else if ("denied".equalsIgnoreCase(approval)) {
                        tvApproval.setText("불허");
                        tvApproval.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.status_rejected));
                    } else {
                        tvApproval.setText("-");
                        tvApproval.setTextColor(Color.BLACK);
                    }
                }
            }
        }
    }
}
