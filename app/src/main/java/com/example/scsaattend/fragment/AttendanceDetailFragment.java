package com.example.scsaattend.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.scsaattend.R;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AttendanceDetailFragment extends Fragment implements UserSelectionDialogFragment.OnUsersSelectedListener {

    private Button btnStartDate, btnEndDate, btnUserSelect, btnQuery;
    private RecyclerView recyclerView;
    private AttendanceDetailAdapter adapter;
    private List<AttendanceDetailItem> attendanceDetailList;
    private ArrayList<User> userList = new ArrayList<>();
    private long selectedStartDate = -1;
    private long selectedEndDate = -1;

    public AttendanceDetailFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_attendance_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        btnStartDate = view.findViewById(R.id.btn_start_date);
        btnEndDate = view.findViewById(R.id.btn_end_date);
        btnUserSelect = view.findViewById(R.id.btn_user_select);
        btnQuery = view.findViewById(R.id.btn_query);
        recyclerView = view.findViewById(R.id.recycler_view_attendance_detail);
        TextView tvRecordCount = view.findViewById(R.id.tv_record_count);

        btnEndDate.setEnabled(false);
        loadInitialUsers();

        btnStartDate.setOnClickListener(v -> showDatePicker(true));
        btnEndDate.setOnClickListener(v -> showDatePicker(false));
        btnUserSelect.setOnClickListener(v -> {
            UserSelectionDialogFragment dialog = UserSelectionDialogFragment.newInstance(userList);
            dialog.setOnUsersSelectedListener(this);
            dialog.show(getParentFragmentManager(), "UserSelectionDialog");
        });
        btnQuery.setOnClickListener(v -> Toast.makeText(getContext(), "데이터 조회 예정", Toast.LENGTH_SHORT).show());

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        attendanceDetailList = createDummyData();
        adapter = new AttendanceDetailAdapter(attendanceDetailList);
        recyclerView.setAdapter(adapter);

        tvRecordCount.setText("출결 기록 (" + attendanceDetailList.size() + "건)");
        updateUserSelectionButtonText();
    }

    private void loadInitialUsers() {
        // 초기 사용자 데이터 로드 (API 호출 또는 로컬 DB)
        userList.add(new User("나정원", true));
        userList.add(new User("김숙사", true));
        userList.add(new User("나원빈", true));
        userList.add(new User("이상민", true));
        userList.add(new User("박모바일", true));
        userList.add(new User("이안드", true));
        userList.add(new User("최자바", true));
    }

    @Override
    public void onUsersSelected(ArrayList<User> selectedUsers) {
        this.userList = selectedUsers;
        updateUserSelectionButtonText();
    }

    private void updateUserSelectionButtonText() {
        long selectedCount = userList.stream().filter(u -> u.isSelected).count();
        if (selectedCount == userList.size()) {
            btnUserSelect.setText("사용자 선택: 전체");
        } else {
            btnUserSelect.setText("사용자 선택: " + selectedCount + "명");
        }
    }

    private void showDatePicker(boolean isStartDate) {
        MaterialDatePicker.Builder<Long> builder = MaterialDatePicker.Builder.datePicker();
        builder.setTitleText(isStartDate ? "시작일 선택" : "종료일 선택");

        if (isStartDate && selectedStartDate != -1) {
            builder.setSelection(selectedStartDate);
        } else if (!isStartDate && selectedEndDate != -1) {
            builder.setSelection(selectedEndDate);
        }

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

    private List<AttendanceDetailItem> createDummyData() {
        List<AttendanceDetailItem> items = new ArrayList<>();
        items.add(new AttendanceDetailItem("12/10", "나정원", "08:57:17", "18:01:22", "정상", "Y", "-"));
        items.add(new AttendanceDetailItem("12/10", "김숙사", "09:14:17", "17:59:22", "지각", "N", "-"));
        items.add(new AttendanceDetailItem("12/10", "나원빈", "08:32:57", "18:10:25", "정상", "Y", "-"));
        items.add(new AttendanceDetailItem("12/10", "이상민", "-", "-", "결석", null, "Y"));
        items.add(new AttendanceDetailItem("12/11", "나정원", "08:59:58", "18:00:02", "정상", "Y", "-"));
        items.add(new AttendanceDetailItem("12/11", "김숙사", "08:01:01", "18:21:07", "정상", "Y", "-"));
        items.add(new AttendanceDetailItem("12/11", "나원빈", "08:59:57", "18:00:03", "정상", "Y", "-"));
        items.add(new AttendanceDetailItem("12/11", "이상민", "08:59:59", "18:00:01", "정상", "N", "-"));
        items.add(new AttendanceDetailItem("12/12", "나정원", "-", "-", "결석", null, "N"));
        items.add(new AttendanceDetailItem("12/12", "김숙사", "08:58:10", "18:05:30", "정상", "Y", "-"));
        items.add(new AttendanceDetailItem("12/13", "나원빈", "09:02:00", "18:01:00", "지각", "N", "-"));
        items.add(new AttendanceDetailItem("12/13", "이상민", "08:55:00", "18:02:00", "정상", "Y", "-"));
        items.add(new AttendanceDetailItem("12/14", "나정원", "08:57:00", "18:03:00", "정상", "Y", "-"));
        items.add(new AttendanceDetailItem("12/14", "김숙사", "-", "-", "결석", null, "Y"));
        items.add(new AttendanceDetailItem("12/15", "나원빈", "08:59:00", "18:04:00", "정상", "Y", "-"));
        items.add(new AttendanceDetailItem("12/15", "이상민", "09:05:00", "18:05:00", "지각", "N", "-"));
        items.add(new AttendanceDetailItem("12/16", "나정원", "08:56:00", "18:06:00", "정상", "Y", "-"));
        items.add(new AttendanceDetailItem("12/16", "김숙사", "08:58:00", "18:07:00", "정상", "Y", "-"));
        items.add(new AttendanceDetailItem("12/17", "나원빈", "-", "-", "결석", null, "N"));
        items.add(new AttendanceDetailItem("12/17", "이상민", "09:01:00", "18:08:00", "지각", "Y", "-"));
        return items;
    }

    private static class AttendanceDetailItem {
        String date, name, checkIn, checkOut, status, approval, publicLeave;

        public AttendanceDetailItem(String date, String name, String checkIn, String checkOut, String status, String approval, String publicLeave) {
            this.date = date;
            this.name = name;
            this.checkIn = checkIn;
            this.checkOut = checkOut;
            this.status = status;
            this.approval = approval;
            this.publicLeave = publicLeave;
        }
    }

    private class AttendanceDetailAdapter extends RecyclerView.Adapter<AttendanceDetailAdapter.ViewHolder> {
        private final List<AttendanceDetailItem> items;

        public AttendanceDetailAdapter(List<AttendanceDetailItem> items) {
            this.items = items;
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
            holder.tvDate.setText(item.date);
            holder.tvName.setText(item.name);
            holder.tvCheckIn.setText(formatTime(item.checkIn));
            holder.tvCheckOut.setText(formatTime(item.checkOut));
            holder.tvStatus.setText(item.status);
            holder.tvPublicLeave.setText(item.publicLeave);

            if ("Y".equals(item.approval)) {
                holder.tvApproval.setText("승인");
                holder.tvApproval.setBackground(ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.status_approved_background));
                holder.tvApproval.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), android.R.color.white));
            } else if ("N".equals(item.approval)) {
                holder.tvApproval.setText("미승인");
                holder.tvApproval.setBackground(ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.status_rejected_background));
                holder.tvApproval.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), android.R.color.white));
            } else {
                holder.tvApproval.setText("미확인");
                holder.tvApproval.setBackground(null);
                holder.tvApproval.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), android.R.color.black));
            }

            holder.itemView.setOnClickListener(v -> {
                Toast.makeText(v.getContext(), item.name + "님의 기록 수정 화면으로 이동합니다.", Toast.LENGTH_SHORT).show();
            });
        }
        
        private String formatTime(String time) {
            if (time == null || time.length() < 5 || "-".equals(time)) return "-";
            return time.substring(0, 5);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvDate, tvName, tvCheckIn, tvCheckOut, tvStatus, tvApproval, tvPublicLeave;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvDate = itemView.findViewById(R.id.tv_date);
                tvName = itemView.findViewById(R.id.tv_name);
                tvCheckIn = itemView.findViewById(R.id.tv_check_in);
                tvCheckOut = itemView.findViewById(R.id.tv_check_out);
                tvStatus = itemView.findViewById(R.id.tv_status);
                tvApproval = itemView.findViewById(R.id.tv_approval);
                tvPublicLeave = itemView.findViewById(R.id.tv_public_leave);
            }
        }
    }
}
