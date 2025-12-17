package com.example.scsaattend.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.scsaattend.R;
import com.example.scsaattend.dto.AttendanceInfoResponse;
import com.example.scsaattend.dto.AttendanceRequest;
import com.example.scsaattend.dto.BatchUpdateRequest;
import com.example.scsaattend.dto.BatchUpdateResponse;
import com.example.scsaattend.dto.ErrorResponse;
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AttendanceDetailFragment extends Fragment implements UserSelectionDialogFragment.OnUsersSelectedListener, BatchUpdateDialogFragment.OnUpdateListener {

    private static final String TAG = "AttendanceDetailFrag";
    private Button btnStartDate, btnEndDate, btnUserSelect, btnQuery;
    private TextView tvRecordCount;
    private RecyclerView recyclerView;
    private AttendanceDetailAdapter adapter;
    private List<AttendanceDetailItem> attendanceDetailList = new ArrayList<>();
    private ArrayList<User> userList = new ArrayList<>();
    private long selectedStartDate = -1;
    private long selectedEndDate = -1;
    private ApiService apiService;
    private LinearLayout filterContentLayout, bottomActionBar;
    private ImageButton btnToggleFilter;
    private Button btnSelectAll, btnDeselectAll, btnBatchChange;

    private boolean isSelectionMode = false;
    private Set<Long> selectedItems = new HashSet<>();

    public AttendanceDetailFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_attendance_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 뷰 초기화
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
        btnEndDate.setEnabled(false);

        // 리스너 설정
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
            BatchUpdateDialogFragment dialog = BatchUpdateDialogFragment.newInstance();
            dialog.setOnUpdateListener(this);
            dialog.show(getParentFragmentManager(), "BatchUpdateDialog");
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AttendanceDetailAdapter(attendanceDetailList, this);
        recyclerView.setAdapter(adapter);

        updateUserSelectionButtonText();
        updateRecordCount();
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
        if (selectedItems.contains(aInfoId)) {
            selectedItems.remove(aInfoId);
        } else {
            selectedItems.add(aInfoId);
        }
        adapter.notifyDataSetChanged();

        if (selectedItems.isEmpty()) {
            exitSelectionMode();
        }
    }

    private void exitSelectionMode() {
        isSelectionMode = false;
        selectedItems.clear();
        bottomActionBar.setVisibility(View.GONE);
        updateAllItems();
    }

    private void selectAllItems() {
        for (AttendanceDetailItem item : attendanceDetailList) {
            selectedItems.add(item.aInfoId);
        }
        updateAllItems();
    }

    private void updateAllItems() {
        adapter.notifyDataSetChanged();
    }
    
    private void searchAttendanceData() {
        if (isSelectionMode) {
            exitSelectionMode();
        }

        if (selectedStartDate == -1 || selectedEndDate == -1) {
            Toast.makeText(getContext(), "시작일과 종료일을 선택해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String startDate = sdf.format(new Date(selectedStartDate));
        String endDate = sdf.format(new Date(selectedEndDate));

        List<Long> selectedMemberIds = userList.stream()
                .filter(u -> u.isSelected)
                .map(u -> u.id)
                .collect(Collectors.toList());

        if (selectedMemberIds.isEmpty()) {
            selectedMemberIds = userList.stream().map(u -> u.id).collect(Collectors.toList());
        }
        
        AttendanceRequest request = new AttendanceRequest(startDate, endDate, selectedMemberIds);

        apiService.searchAttendance(request).enqueue(new Callback<List<AttendanceInfoResponse>>() {
            @Override
            public void onResponse(Call<List<AttendanceInfoResponse>> call, Response<List<AttendanceInfoResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<AttendanceInfoResponse> responseData = response.body();
                    Collections.sort(responseData, Comparator.comparing(AttendanceInfoResponse::getADate)
                                                        .thenComparing(res -> res.getMember().getId()));
                    
                    attendanceDetailList.clear();
                    for (AttendanceInfoResponse res : responseData) {
                        attendanceDetailList.add(new AttendanceDetailItem(
                                res.getAinfoId(),
                                res.getADate().substring(5),
                                res.getMember().getName(),
                                res.getIsOff(),
                                res.getArrivalTime(),
                                res.getLeavingTime(),
                                res.getStatus(),
                                res.getIsApproved(),
                                res.getIsOfficial()
                        ));
                    }
                    adapter.notifyDataSetChanged();
                    updateRecordCount();
                } else {
                    Log.e(TAG, "Search failed: " + response.code());
                    Toast.makeText(getContext(), "데이터 조회에 실패했습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<AttendanceInfoResponse>> call, Throwable t) {
                Log.e(TAG, "Search error", t);
                Toast.makeText(getContext(), "서버와 통신할 수 없습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onUsersSelected(ArrayList<User> selectedUsers) {
        this.userList = selectedUsers;
        updateUserSelectionButtonText();
    }
    
    @Override
    public void onUpdate(Map<String, Object> updateData) {
        List<Long> aInfoIdList = new ArrayList<>(selectedItems);
        BatchUpdateRequest request = new BatchUpdateRequest(aInfoIdList, updateData);

        apiService.batchUpdateAttendance(request).enqueue(new Callback<BatchUpdateResponse>() {
            @Override
            public void onResponse(Call<BatchUpdateResponse> call, Response<BatchUpdateResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(getContext(), response.body().getMessage(), Toast.LENGTH_LONG).show();
                    searchAttendanceData(); // Refresh the list
                } else {
                    String errorMessage = "일괄 변경에 실패했습니다.";
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
                exitSelectionMode();
            }

            @Override
            public void onFailure(Call<BatchUpdateResponse> call, Throwable t) {
                Log.e(TAG, "Batch update error", t);
                Toast.makeText(getContext(), "서버와 통신할 수 없습니다.", Toast.LENGTH_SHORT).show();
                exitSelectionMode();
            }
        });
    }

    private void updateUserSelectionButtonText() {
        long selectedCount = userList.stream().filter(u -> u.isSelected).count();
        if (selectedCount == userList.size() || selectedCount == 0) {
            btnUserSelect.setText("사용자 선택: 전체");
        } else {
            btnUserSelect.setText("사용자 선택: " + selectedCount + "명");
        }
    }
    
    private void updateRecordCount() {
        tvRecordCount.setText("출결 기록 (" + attendanceDetailList.size() + "건)");
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

    private static class AttendanceDetailItem {
        long aInfoId;
        String date, name, isOff, checkIn, checkOut, status, approval, publicLeave;

        public AttendanceDetailItem(long aInfoId, String date, String name, String isOff, String checkIn, String checkOut, String status, String approval, String publicLeave) {
            this.aInfoId = aInfoId;
            this.date = date;
            this.name = name;
            this.isOff = isOff;
            this.checkIn = checkIn;
            this.checkOut = checkOut;
            this.status = status;
            this.approval = approval;
            this.publicLeave = publicLeave;
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

            if (fragment.isSelectionMode) {
                if (fragment.selectedItems.contains(item.aInfoId)) {
                    holder.cardView.setCardBackgroundColor(Color.WHITE);
                } else {
                    holder.cardView.setCardBackgroundColor(Color.LTGRAY);
                }
            } else {
                holder.cardView.setCardBackgroundColor(Color.WHITE);
            }
            
            holder.itemView.setOnClickListener(v -> {
                if(fragment.isSelectionMode) {
                    fragment.toggleSelection(holder.getAdapterPosition());
                } else {
                    Toast.makeText(v.getContext(), item.name + "님의 기록 수정 화면으로 이동합니다.", Toast.LENGTH_SHORT).show();
                }
            });

            holder.itemView.setOnLongClickListener(v -> {
                if (!fragment.isSelectionMode) {
                    fragment.startSelectionMode(holder.getAdapterPosition());
                } else {
                    fragment.toggleSelection(holder.getAdapterPosition());
                }
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

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
                    tvStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), android.R.color.black));
                    tvCheckIn.setText("-");
                    tvCheckOut.setText("-");
                    tvApproval.setText("-");
                    tvPublicLeave.setText("-");
                    tvApproval.setBackground(null);
                } else {
                    tvCheckIn.setText(fragment.formatTime(item.checkIn));
                    tvCheckOut.setText(fragment.formatTime(item.checkOut));
                    tvPublicLeave.setText(item.publicLeave != null ? item.publicLeave : "-");

                    String status = item.status != null ? item.status : "";
                    switch (status) {
                        case "normal":
                            tvStatus.setText("출석");
                            tvStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.status_text_normal));
                            break;
                        case "late/early":
                            tvStatus.setText("지각/조퇴");
                            tvStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.status_text_late_early));
                            break;
                        case "absent":
                            tvStatus.setText("결석");
                            tvStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.status_text_absent));
                            break;
                        default:
                            tvStatus.setText("-");
                            tvStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), android.R.color.black));
                            break;
                    }

                    String approval = item.approval != null ? item.approval : "";
                    tvApproval.setBackground(null);
                    if ("approved".equalsIgnoreCase(approval)) {
                        tvApproval.setText("승인");
                    } else if ("denied".equalsIgnoreCase(approval)) {
                        tvApproval.setText("불허");
                    } else {
                        tvApproval.setText("-");
                    }
                }
            }
        }
    }
    
    private String formatTime(String time) {
        if (time == null || time.length() < 16) return "-"; 
        return time.substring(11, 16);
    }
}
