package com.example.scsaattend.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.scsaattend.R;
import com.example.scsaattend.dto.AttendanceTypeDto;
import com.example.scsaattend.network.ApiService;
import com.example.scsaattend.network.RetrofitClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AttendanceTypeSelectionDialogFragment extends DialogFragment {

    private static final String TAG = "AttendanceTypeDialog";
    private RecyclerView rvAttendanceTypes;
    private AttendanceTypeAdapter adapter;
    private List<AttendanceTypeDto> typeList = new ArrayList<>();
    private ApiService apiService;

    public interface OnAttendanceTypeSelectedListener {
        void onAttendanceTypeSelected(long typeId, String typeName);
    }

    private OnAttendanceTypeSelectedListener listener;

    public static AttendanceTypeSelectionDialogFragment newInstance() {
        return new AttendanceTypeSelectionDialogFragment();
    }

    public void setOnAttendanceTypeSelectedListener(OnAttendanceTypeSelectedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        apiService = RetrofitClient.getClient().create(ApiService.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_attendance_type_selection, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvAttendanceTypes = view.findViewById(R.id.rv_attendance_types);
        Button btnClose = view.findViewById(R.id.btn_close);

        rvAttendanceTypes.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AttendanceTypeAdapter(typeList);
        rvAttendanceTypes.setAdapter(adapter);

        btnClose.setOnClickListener(v -> dismiss());

        fetchAttendanceTypes();
    }
    
    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private void fetchAttendanceTypes() {
        apiService.getAttendanceTypes().enqueue(new Callback<List<AttendanceTypeDto>>() {
            @Override
            public void onResponse(Call<List<AttendanceTypeDto>> call, Response<List<AttendanceTypeDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    typeList.clear();
                    typeList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(getContext(), "유형 목록을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Failed to fetch types: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<AttendanceTypeDto>> call, Throwable t) {
                Toast.makeText(getContext(), "서버와 통신할 수 없습니다.", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error fetching types", t);
            }
        });
    }

    private class AttendanceTypeAdapter extends RecyclerView.Adapter<AttendanceTypeAdapter.ViewHolder> {
        private List<AttendanceTypeDto> items;

        public AttendanceTypeAdapter(List<AttendanceTypeDto> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_attendance_type, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            AttendanceTypeDto item = items.get(position);
            holder.tvTypeName.setText(item.getName());
            holder.tvEarliestTime.setText(item.getEarliestTime().substring(0, 5));
            holder.tvStartTime.setText(item.getStartTime().substring(0, 5));
            holder.tvEndTime.setText(item.getEndTime().substring(0, 5));
            holder.tvLatestTime.setText(item.getLatestTime().substring(0, 5));

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAttendanceTypeSelected(item.getId(), item.getName());
                }
                dismiss();
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTypeName, tvEarliestTime, tvStartTime, tvEndTime, tvLatestTime;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTypeName = itemView.findViewById(R.id.tv_type_name);
                tvEarliestTime = itemView.findViewById(R.id.tv_earliest_time);
                tvStartTime = itemView.findViewById(R.id.tv_start_time);
                tvEndTime = itemView.findViewById(R.id.tv_end_time);
                tvLatestTime = itemView.findViewById(R.id.tv_latest_time);
            }
        }
    }
}
