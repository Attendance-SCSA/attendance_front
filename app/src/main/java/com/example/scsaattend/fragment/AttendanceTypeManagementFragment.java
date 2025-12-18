package com.example.scsaattend.fragment;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.scsaattend.R;
import com.example.scsaattend.dto.AttendanceTypeRequest;
import com.example.scsaattend.dto.AttendanceTypeResponse;
import com.example.scsaattend.network.ApiService;
import com.example.scsaattend.network.RetrofitClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AttendanceTypeManagementFragment extends Fragment {

    private static final String TAG = "AttendanceTypeFrag";
    private RecyclerView recyclerView;
    private TypeAdapter adapter;
    private List<AttendanceTypeResponse> typeList = new ArrayList<>();
    private Button btnAddType;
    private TextView tvTypeCount;
    private ApiService apiService;

    private String earliestTime, startTime, endTime, latestTime;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_attendance_type_management, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recycler_view_types);
        btnAddType = view.findViewById(R.id.btn_add_type);
        tvTypeCount = view.findViewById(R.id.tv_type_count);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TypeAdapter(typeList);
        recyclerView.setAdapter(adapter);

        apiService = RetrofitClient.getClient().create(ApiService.class);
        btnAddType.setOnClickListener(v -> showAddTypeDialog());
        fetchAttendanceTypes();
    }

    private void updateTypeCount() {
        tvTypeCount.setText("출결 유형 목록 (" + typeList.size() + "건)");
    }

    private void fetchAttendanceTypes() {
        apiService.getAttendanceTypes().enqueue(new Callback<List<AttendanceTypeResponse>>() {
            @Override
            public void onResponse(Call<List<AttendanceTypeResponse>> call, Response<List<AttendanceTypeResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    typeList.clear();
                    typeList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                    updateTypeCount();
                }
            }
            @Override
            public void onFailure(Call<List<AttendanceTypeResponse>> call, Throwable t) {}
        });
    }

    private void showAddTypeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = requireActivity().getLayoutInflater().inflate(R.layout.dialog_add_attendance_type, null);
        builder.setView(dialogView);

        final EditText etTypeName = dialogView.findViewById(R.id.et_type_name);
        final TextView tvEarliestTime = dialogView.findViewById(R.id.tv_earliest_time);
        final TextView tvStartTime = dialogView.findViewById(R.id.tv_start_time);
        final TextView tvEndTime = dialogView.findViewById(R.id.tv_end_time);
        final TextView tvLatestTime = dialogView.findViewById(R.id.tv_latest_time);

        earliestTime = "08:00:00"; startTime = "09:00:00"; endTime = "18:00:00"; latestTime = "19:00:00";
        tvEarliestTime.setText(earliestTime); tvStartTime.setText(startTime); tvEndTime.setText(endTime); tvLatestTime.setText(latestTime);

        tvEarliestTime.setOnClickListener(v -> showCustomTimePicker(8, 0, 0, time -> {
            earliestTime = time; tvEarliestTime.setText(earliestTime);
        }));
        tvStartTime.setOnClickListener(v -> showCustomTimePicker(9, 0, 0, time -> {
            startTime = time; tvStartTime.setText(startTime);
        }));
        tvEndTime.setOnClickListener(v -> showCustomTimePicker(18, 0, 0, time -> {
            endTime = time; tvEndTime.setText(endTime);
        }));
        tvLatestTime.setOnClickListener(v -> showCustomTimePicker(19, 0, 0, time -> {
            latestTime = time; tvLatestTime.setText(latestTime);
        }));

        AlertDialog dialog = builder.create();
        dialogView.findViewById(R.id.btn_cancel_add).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btn_confirm_add).setOnClickListener(v -> {
            String name = etTypeName.getText().toString();
            if (name.isEmpty()) { Toast.makeText(getContext(), "유형 이름을 입력해주세요.", Toast.LENGTH_SHORT).show(); return; }
            addAttendanceType(new AttendanceTypeRequest(name, earliestTime, startTime, endTime, latestTime));
            dialog.dismiss();
        });
        dialog.show();
    }

    private void showCustomTimePicker(int h, int m, int s, OnTimeSelectedListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View v = requireActivity().getLayoutInflater().inflate(R.layout.dialog_custom_time_picker, null);
        builder.setView(v);

        NumberPicker hp = v.findViewById(R.id.picker_hour), mp = v.findViewById(R.id.picker_minute), sp = v.findViewById(R.id.picker_second);
        View cbClear = v.findViewById(R.id.cb_clear_time);

        if (cbClear != null) cbClear.setVisibility(View.GONE);

        NumberPicker.Formatter fmt = value -> String.format(Locale.getDefault(), "%02d", value);
        hp.setMinValue(0); hp.setMaxValue(23); hp.setFormatter(fmt); hp.setValue(h);
        mp.setMinValue(0); mp.setMaxValue(59); mp.setFormatter(fmt); mp.setValue(m);
        sp.setMinValue(0); sp.setMaxValue(59); sp.setFormatter(fmt); sp.setValue(s);

        builder.setTitle("시간 선택");
        builder.setPositiveButton("확인", (dialog, which) -> {
            listener.onTimeSelected(String.format(Locale.getDefault(), "%02d:%02d:%02d", hp.getValue(), mp.getValue(), sp.getValue()));
        });
        builder.setNegativeButton("취소", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    interface OnTimeSelectedListener { void onTimeSelected(String time); }

    private void addAttendanceType(AttendanceTypeRequest request) {
        apiService.addAttendanceType(request).enqueue(new Callback<AttendanceTypeResponse>() {
            @Override
            public void onResponse(Call<AttendanceTypeResponse> call, Response<AttendanceTypeResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    typeList.add(response.body());
                    adapter.notifyItemInserted(typeList.size() - 1);
                    updateTypeCount();
                    Toast.makeText(getContext(), "추가되었습니다.", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<AttendanceTypeResponse> call, Throwable t) {}
        });
    }

    private void deleteAttendanceType(long typeId, int position) {
        apiService.deleteAttendanceType(typeId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    typeList.remove(position);
                    adapter.notifyItemRemoved(position);
                    updateTypeCount();
                    Toast.makeText(getContext(), "삭제되었습니다.", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {}
        });
    }

    private class TypeAdapter extends RecyclerView.Adapter<TypeAdapter.ViewHolder> {
        private final List<AttendanceTypeResponse> types;
        public TypeAdapter(List<AttendanceTypeResponse> types) { this.types = types; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_attendance_type, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            AttendanceTypeResponse item = types.get(position);
            holder.tvTypeName.setText(item.getName());
            holder.tvStartWork.setText(format(item.getEarliestTime()));
            holder.tvStartClass.setText(format(item.getStartTime()));
            holder.tvEndClass.setText(format(item.getEndTime()));
            holder.tvEndWork.setText(format(item.getLatestTime()));

            if ("기본".equals(item.getName())) {
                holder.btnDeleteType.setVisibility(View.INVISIBLE);
            } else {
                holder.btnDeleteType.setVisibility(View.VISIBLE);
                holder.btnDeleteType.setOnClickListener(v -> {
                    new AlertDialog.Builder(getContext()).setTitle("삭제 확인").setMessage("삭제하시겠습니까?")
                            .setPositiveButton("삭제", (d, w) -> deleteAttendanceType(item.getId(), holder.getAdapterPosition()))
                            .setNegativeButton("취소", null).show();
                });
            }
        }

        private String format(String t) { return (t != null && t.length() >= 5) ? t.substring(0, 5) : t; }

        @Override
        public int getItemCount() { return types.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTypeName, tvStartWork, tvStartClass, tvEndClass, tvEndWork;
            View btnDeleteType;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTypeName = itemView.findViewById(R.id.tv_type_name);
                tvStartWork = itemView.findViewById(R.id.tv_start_work_time);
                tvStartClass = itemView.findViewById(R.id.tv_start_class_time);
                tvEndClass = itemView.findViewById(R.id.tv_end_class_time);
                tvEndWork = itemView.findViewById(R.id.tv_end_work_time);
                btnDeleteType = itemView.findViewById(R.id.btn_delete_type);
            }
        }
    }
}
