package com.example.scsaattend.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.scsaattend.R;
import com.example.scsaattend.network.ApiService;
import com.example.scsaattend.network.RetrofitClient;
import com.example.scsaattend.network.dto.MemberResponse;

import java.util.ArrayList;
import java.util.List;

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

        btnAddMember.setOnClickListener(v -> {
            Toast.makeText(getContext(), "새 사용자 추가 기능은 아직 구현되지 않았습니다.", Toast.LENGTH_SHORT).show();
        });

        loadMembers();
    }

    private void loadMembers() {
        // SharedPreferences에서 저장된 사용자 정보 가져오기
        SharedPreferences prefs = requireActivity().getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE);
        
        // 로그인 시 저장한 PK(id) 값 불러오기
        int dbId = prefs.getInt("db_id", -1);
        long userId = (long) dbId;

        if (dbId == -1) {
            Toast.makeText(getContext(), "로그인 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        apiService.getMembers(userId).enqueue(new Callback<List<MemberResponse>>() {
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
                Toast.makeText(v.getContext(), member.getName() + "님 삭제", Toast.LENGTH_SHORT).show();
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