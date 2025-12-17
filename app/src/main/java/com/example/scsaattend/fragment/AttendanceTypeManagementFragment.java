package com.example.scsaattend.fragment;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
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
    private ApiService apiService;

    // 시간 저장을 위한 멤버 변수
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

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TypeAdapter(typeList);
        recyclerView.setAdapter(adapter);

        apiService = RetrofitClient.getClient().create(ApiService.class);

        btnAddType.setOnClickListener(v -> showAddTypeDialog());

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
                }
            }

            @Override
            public void onFailure(Call<List<AttendanceTypeResponse>> call, Throwable t) {
                Toast.makeText(getContext(), "서버와 통신할 수 없습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddTypeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_attendance_type, null);
        builder.setView(dialogView);

        final EditText etTypeName = dialogView.findViewById(R.id.et_type_name);
        final TextView tvEarliestTime = dialogView.findViewById(R.id.tv_earliest_time);
        final TextView tvStartTime = dialogView.findViewById(R.id.tv_start_time);
        final TextView tvEndTime = dialogView.findViewById(R.id.tv_end_time);
        final TextView tvLatestTime = dialogView.findViewById(R.id.tv_latest_time);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel_add);
        Button btnConfirm = dialogView.findViewById(R.id.btn_confirm_add);

        // 시간 변수 초기화
        earliestTime = null;
        startTime = null;
        endTime = null;
        latestTime = null;

        tvEarliestTime.setOnClickListener(v -> showCustomTimePicker(time -> {
            earliestTime = time;
            tvEarliestTime.setText(earliestTime);
        }));
        tvStartTime.setOnClickListener(v -> showCustomTimePicker(time -> {
            startTime = time;
            tvStartTime.setText(startTime);
        }));
        tvEndTime.setOnClickListener(v -> showCustomTimePicker(time -> {
            endTime = time;
            tvEndTime.setText(endTime);
        }));
        tvLatestTime.setOnClickListener(v -> showCustomTimePicker(time -> {
            latestTime = time;
            tvLatestTime.setText(latestTime);
        }));

        AlertDialog dialog = builder.create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            String name = etTypeName.getText().toString();

            if (name.isEmpty() || earliestTime == null || startTime == null || endTime == null || latestTime == null) {
                Toast.makeText(getContext(), "모든 필드를 입력해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            AttendanceTypeRequest request = new AttendanceTypeRequest(name, earliestTime, startTime, endTime, latestTime);
            addAttendanceType(request);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void showCustomTimePicker(OnTimeSelectedListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_custom_time_picker, null);
        builder.setView(dialogView);

        NumberPicker hourPicker = dialogView.findViewById(R.id.picker_hour);
        NumberPicker minutePicker = dialogView.findViewById(R.id.picker_minute);
        NumberPicker secondPicker = dialogView.findViewById(R.id.picker_second);

        hourPicker.setMinValue(0);
        hourPicker.setMaxValue(23);
        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(59);
        secondPicker.setMinValue(0);
        secondPicker.setMaxValue(59);

        Calendar c = Calendar.getInstance();
        hourPicker.setValue(c.get(Calendar.HOUR_OF_DAY));
        minutePicker.setValue(c.get(Calendar.MINUTE));
        secondPicker.setValue(c.get(Calendar.SECOND));

        builder.setTitle("시간 선택");
        builder.setPositiveButton("확인", (dialog, which) -> {
            String formattedTime = String.format(Locale.getDefault(), "%02d:%02d:%02d", hourPicker.getValue(), minutePicker.getValue(), secondPicker.getValue());
            listener.onTimeSelected(formattedTime);
        });
        builder.setNegativeButton("취소", (dialog, which) -> dialog.cancel());

        builder.create().show();
    }

    interface OnTimeSelectedListener {
        void onTimeSelected(String time);
    }

    private void addAttendanceType(AttendanceTypeRequest request) {
        apiService.addAttendanceType(request).enqueue(new Callback<AttendanceTypeResponse>() {
            @Override
            public void onResponse(Call<AttendanceTypeResponse> call, Response<AttendanceTypeResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    typeList.add(response.body());
                    adapter.notifyItemInserted(typeList.size() - 1);
                    Toast.makeText(getContext(), "새로운 출결 유형이 추가되었습니다.", Toast.LENGTH_SHORT).show();
                } else {
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "{\"message\": \"알 수 없는 오류\"}";
                        JSONObject jsonObject = new JSONObject(errorBody);
                        String errorMessage = jsonObject.getString("message");
                        Toast.makeText(getContext(), "추가 실패: " + errorMessage, Toast.LENGTH_LONG).show();
                    } catch (IOException | JSONException e) {
                        Toast.makeText(getContext(), "오류 메시지를 파싱하는 데 실패했습니다.", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<AttendanceTypeResponse> call, Throwable t) {
                Toast.makeText(getContext(), "서버와 통신할 수 없습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteAttendanceType(long typeId, int position) {
        apiService.deleteAttendanceType(typeId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    String responseBody = "";
                    if (response.isSuccessful() && response.body() != null) {
                        responseBody = response.body().string();
                        typeList.remove(position);
                        adapter.notifyItemRemoved(position);
                        adapter.notifyItemRangeChanged(position, typeList.size());
                    } else if (response.errorBody() != null) {
                        responseBody = response.errorBody().string();
                    } else {
                        Toast.makeText(getContext(), "알 수 없는 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    JSONObject jsonObject = new JSONObject(responseBody);
                    String message = jsonObject.getString("message");
                    Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();

                } catch (IOException | JSONException e) {
                    Toast.makeText(getContext(), "응답을 처리하는 데 실패했습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(getContext(), "서버와 통신할 수 없습니다.", Toast.LENGTH_SHORT).show();
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
                    new AlertDialog.Builder(getContext())
                            .setTitle("출결 유형 삭제")
                            .setMessage("해당 출결 유형 삭제 시 관련된 출결 정보는 '기본' 유형으로 변경됩니다. 그래도 삭제하시겠습니까?")
                            .setPositiveButton("삭제", (dialog, which) -> {
                                deleteAttendanceType(item.getId(), holder.getAdapterPosition());
                            })
                            .setNegativeButton("취소", null)
                            .show();
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
