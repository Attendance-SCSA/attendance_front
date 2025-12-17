package com.example.scsaattend.fragment;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.scsaattend.R;
import com.example.scsaattend.network.ApiService;
import com.example.scsaattend.network.RetrofitClient;
import com.example.scsaattend.network.dto.MemberRegisterRequest;
import com.example.scsaattend.network.dto.MemberResponse;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MemberManagementFragment extends Fragment {

    private static final String TAG = "MemberManagementFrag";
    private RecyclerView recyclerView;
    private MemberAdapter adapter;
    private List<MemberResponse> memberList = new ArrayList<>();
    private TextView tvMemberCount;
    private Button btnAddMember;
    private ApiService apiService;
    private long currentUserId = -1;

    public MemberManagementFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_member_management, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvMemberCount = view.findViewById(R.id.tv_member_count);
        btnAddMember = view.findViewById(R.id.btn_add_member);
        recyclerView = view.findViewById(R.id.recycler_view_members);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new MemberAdapter(memberList);
        recyclerView.setAdapter(adapter);

        apiService = RetrofitClient.getClient("http://10.10.0.76:8888").create(ApiService.class);

        // 현재 로그인한 사용자 ID 가져오기
        SharedPreferences prefs = requireActivity().getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE);
        int dbId = prefs.getInt("db_id", -1);
        currentUserId = (long) dbId;

        btnAddMember.setOnClickListener(v -> showAddMemberDialog());

        loadMembers();
    }

    private void showAddMemberDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_member, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        EditText etName = dialogView.findViewById(R.id.et_name);
        EditText etId = dialogView.findViewById(R.id.et_id);
        EditText etPassword = dialogView.findViewById(R.id.et_password);
        EditText etPasswordConfirm = dialogView.findViewById(R.id.et_password_confirm);
        Spinner spinnerCompany = dialogView.findViewById(R.id.spinner_company);
        TextView tvStartDate = dialogView.findViewById(R.id.tv_start_date);
        TextView tvEndDate = dialogView.findViewById(R.id.tv_end_date);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnSubmit = dialogView.findViewById(R.id.btn_submit_add);

        // 회사 목록 스피너 설정
        String[] companies = {"DS", "DX", "SDS"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, companies);
        spinnerCompany.setAdapter(adapter);

        // 날짜 선택 리스너 설정
        tvStartDate.setOnClickListener(v -> showDatePicker(tvStartDate));
        tvEndDate.setOnClickListener(v -> showDatePicker(tvEndDate));

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSubmit.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String id = etId.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String passwordConfirm = etPasswordConfirm.getText().toString().trim();
            String company = spinnerCompany.getSelectedItem().toString();
            String startDay = tvStartDate.getText().toString().trim();
            String endDay = tvEndDate.getText().toString().trim();

            if (name.isEmpty() || id.isEmpty() || password.isEmpty() || company.isEmpty() || startDay.isEmpty() || endDay.isEmpty()) {
                Toast.makeText(getContext(), "모든 필드를 입력해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(passwordConfirm)) {
                Toast.makeText(getContext(), "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            MemberRegisterRequest request = new MemberRegisterRequest(name, id, password, company, startDay, endDay);
            apiService.registerMember(currentUserId, request).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(getContext(), "사용자가 추가되었습니다.", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        loadMembers(); // 목록 갱신
                    } else {
                        Toast.makeText(getContext(), "추가 실패: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    Toast.makeText(getContext(), "서버 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }

    private void showDatePicker(TextView textView) {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        new DatePickerDialog(requireContext(), (view, selectedYear, selectedMonth, selectedDay) -> {
            String selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay);
            textView.setText(selectedDate);
        }, year, month, day).show();
    }

    private void loadMembers() {
        if (currentUserId == -1) {
            Toast.makeText(getContext(), "로그인 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        apiService.getMembers(currentUserId).enqueue(new Callback<List<MemberResponse>>() {
            @Override
            public void onResponse(Call<List<MemberResponse>> call, Response<List<MemberResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    memberList.clear();
                    memberList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                    updateMemberCount();
                } else {
                    String errorMsg = "코드: " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            errorMsg += "\n" + response.errorBody().string();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Log.e(TAG, "Failed to load members: " + errorMsg);
                    Toast.makeText(getContext(), "로드 실패: " + errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<List<MemberResponse>> call, Throwable t) {
                Log.e(TAG, "Network error", t);
                Toast.makeText(getContext(), "서버와 통신할 수 없습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateMemberCount() {
        tvMemberCount.setText("사용자 목록 (" + memberList.size() + "명)");
    }

    private void deleteMember(MemberResponse member, int position) {
        if (currentUserId == -1) return;

        new AlertDialog.Builder(requireContext())
                .setTitle("사용자 삭제")
                .setMessage(member.getName() + " (" + member.getLoginId() + ") 사용자를 정말 삭제하시겠습니까?")
                .setPositiveButton("삭제", (dialog, which) -> {
                    apiService.deleteMember(currentUserId, member.getId()).enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(getContext(), "삭제되었습니다.", Toast.LENGTH_SHORT).show();
                                memberList.remove(position);
                                adapter.notifyItemRemoved(position);
                                adapter.notifyItemRangeChanged(position, memberList.size() - position);
                                updateMemberCount();
                            } else {
                                Toast.makeText(getContext(), "삭제 실패: " + response.code(), Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            Toast.makeText(getContext(), "서버 통신 오류", Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Delete error", t);
                        }
                    });
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.ViewHolder> {
        private final List<MemberResponse> members;

        public MemberAdapter(List<MemberResponse> members) {
            this.members = members;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_member_management, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            MemberResponse member = members.get(position);
            holder.tvName.setText(member.getName());
            holder.tvLoginId.setText(member.getLoginId());
            holder.tvCompany.setText(member.getCompany());

            holder.btnEditPassword.setOnClickListener(v -> {
                Toast.makeText(v.getContext(), member.getName() + "님 비밀번호 변경", Toast.LENGTH_SHORT).show();
            });

            holder.btnDeleteMember.setOnClickListener(v -> {
                int currentPos = holder.getAdapterPosition();
                if (currentPos != RecyclerView.NO_POSITION) {
                    deleteMember(member, currentPos);
                }
            });
        }

        @Override
        public int getItemCount() {
            return members.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvLoginId, tvCompany;
            ImageButton btnEditPassword, btnDeleteMember;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_name);
                tvLoginId = itemView.findViewById(R.id.tv_login_id);
                tvCompany = itemView.findViewById(R.id.tv_company);
                btnEditPassword = itemView.findViewById(R.id.btn_edit_password);
                btnDeleteMember = itemView.findViewById(R.id.btn_delete_member);
            }
        }
    }
}