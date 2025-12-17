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
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.scsaattend.R;
import com.example.scsaattend.dto.AttendanceTypeResponse;
import com.example.scsaattend.network.ApiService;
import com.example.scsaattend.network.RetrofitClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AttendanceTypeManagementFragment extends Fragment {

    private static final String TAG = "AttendanceTypeFrag";
    private RecyclerView recyclerView;
    private TypeAdapter adapter;
    private List<AttendanceTypeResponse> typeList = new ArrayList<>();
    private TextView btnAddType;
    private Button btnCancel, btnSubmitChange;
    private ApiService apiService;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_attendance_type_management, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recycler_view_types);
        btnAddType = view.findViewById(R.id.btn_add_type);
        btnCancel = view.findViewById(R.id.btn_cancel);
        btnSubmitChange = view.findViewById(R.id.btn_submit_change_type);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TypeAdapter(typeList);
        recyclerView.setAdapter(adapter);

        apiService = RetrofitClient.getClient().create(ApiService.class);

        btnAddType.setOnClickListener(v -> {
            Toast.makeText(getContext(), "출근 유형 추가 기능은 아직 구현되지 않았습니다.", Toast.LENGTH_SHORT).show();
        });
        
        btnCancel.setOnClickListener(v -> {
             if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                 getParentFragmentManager().popBackStack();
             }
        });

        btnSubmitChange.setOnClickListener(v -> {
             Toast.makeText(getContext(), "출결 타입 변경 완료", Toast.LENGTH_SHORT).show();
        });

        fetchAttendanceTypes();
    }

    private void fetchAttendanceTypes() {
        apiService.getAttendanceTypes().enqueue(new Callback<List<AttendanceTypeResponse>>() {
            @Override
            public void onResponse(Call<List<AttendanceTypeResponse>> call, Response<List<AttendanceTypeResponse>> response) {
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
            public void onFailure(Call<List<AttendanceTypeResponse>> call, Throwable t) {
                Toast.makeText(getContext(), "서버와 통신할 수 없습니다.", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error fetching types", t);
            }
        });
    }

    private class TypeAdapter extends RecyclerView.Adapter<TypeAdapter.ViewHolder> {
        private final List<AttendanceTypeResponse> types;

        public TypeAdapter(List<AttendanceTypeResponse> types) {
            this.types = types;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_attendance_type, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            AttendanceTypeResponse item = types.get(position);
            holder.tvTypeName.setText(item.getName());
            
            // 시간 형식이 HH:mm:ss로 올 경우 앞 5자리만 사용 (HH:mm)
            String startWork = item.getEarliestTime() != null && item.getEarliestTime().length() >= 5 ? item.getEarliestTime().substring(0, 5) : item.getEarliestTime();
            String startClass = item.getStartTime() != null && item.getStartTime().length() >= 5 ? item.getStartTime().substring(0, 5) : item.getStartTime();
            String endClass = item.getEndTime() != null && item.getEndTime().length() >= 5 ? item.getEndTime().substring(0, 5) : item.getEndTime();
            String endWork = item.getLatestTime() != null && item.getLatestTime().length() >= 5 ? item.getLatestTime().substring(0, 5) : item.getLatestTime();

            holder.tvStartWork.setText(startWork);
            holder.tvStartClass.setText(startClass);
            holder.tvEndClass.setText(endClass);
            holder.tvEndWork.setText(endWork);

            if ("기본".equals(item.getName())) {
                holder.btnDeleteType.setVisibility(View.INVISIBLE);
                holder.btnDeleteType.setOnClickListener(null);
            } else {
                holder.btnDeleteType.setVisibility(View.VISIBLE);
                holder.btnDeleteType.setOnClickListener(v -> {
                    Toast.makeText(v.getContext(), item.getName() + " 유형 삭제 (기능 미구현)", Toast.LENGTH_SHORT).show();
                });
            }
        }

        @Override
        public int getItemCount() {
            return types.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTypeName, tvStartWork, tvStartClass, tvEndClass, tvEndWork;
            View btnDeleteType; // ImageButton

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