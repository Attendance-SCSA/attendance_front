package com.example.scsaattend.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.scsaattend.R;
import com.example.scsaattend.dto.MemberResponse;
import com.example.scsaattend.network.ApiService;
import com.example.scsaattend.network.RetrofitClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserSelectionDialogFragment extends DialogFragment {

    private static final String TAG = "UserSelectionDialog";
    private CheckBox cbSelectAll;
    private RecyclerView rvUserList;
    private UserAdapter adapter;
    private List<SelectableUser> userList = new ArrayList<>();
    private OnUsersSelectedListener listener;
    private ApiService apiService;

    public interface OnUsersSelectedListener {
        void onUsersSelected(ArrayList<SelectableUser> selectedUsers);
    }

    public static UserSelectionDialogFragment newInstance(ArrayList<SelectableUser> users) {
        UserSelectionDialogFragment fragment = new UserSelectionDialogFragment();
        Bundle args = new Bundle();
        args.putParcelableArrayList("users", users);
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnUsersSelectedListener(OnUsersSelectedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        apiService = RetrofitClient.getClient().create(ApiService.class);

        ArrayList<SelectableUser> passedUsers = new ArrayList<>();
        if (getArguments() != null) {
            getArguments().setClassLoader(SelectableUser.class.getClassLoader());
            passedUsers = getArguments().getParcelableArrayList("users");
        }

        Map<Long, SelectableUser> previousSelectionMap = passedUsers.stream()
                .collect(Collectors.toMap(u -> u.id, Function.identity()));

        fetchMembers(previousSelectionMap);
    }

    private void fetchMembers(Map<Long, SelectableUser> previousSelectionMap) {
        apiService.getMembers().enqueue(new Callback<List<MemberResponse>>() {
            @Override
            public void onResponse(Call<List<MemberResponse>> call, Response<List<MemberResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    userList.clear();
                    for (MemberResponse member : response.body()) {
                        boolean isSelected = previousSelectionMap.containsKey(member.getId()) ?
                                previousSelectionMap.get(member.getId()).isSelected : false;
                        userList.add(new SelectableUser(member.getId(), member.getName(), isSelected));
                    }
                    adapter.notifyDataSetChanged();
                    updateSelectAllCheckBoxState();
                } else {
                    Log.e(TAG, "Failed to fetch members: " + response.code());
                    Toast.makeText(getContext(), "사용자 목록을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<MemberResponse>> call, Throwable t) {
                Log.e(TAG, "Error fetching members", t);
                Toast.makeText(getContext(), "서버와 통신할 수 없습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_user_selection, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        cbSelectAll = view.findViewById(R.id.cb_select_all);
        rvUserList = view.findViewById(R.id.rv_user_list);
        Button btnReset = view.findViewById(R.id.btn_reset);
        Button btnApply = view.findViewById(R.id.btn_apply);

        adapter = new UserAdapter(userList);
        rvUserList.setLayoutManager(new LinearLayoutManager(getContext()));
        rvUserList.setAdapter(adapter);

        cbSelectAll.setOnClickListener(v -> {
            boolean isChecked = cbSelectAll.isChecked();
            for (SelectableUser user : userList) {
                user.isSelected = isChecked;
            }
            adapter.notifyDataSetChanged();
        });

        btnReset.setOnClickListener(v -> {
            for (SelectableUser user : userList) {
                user.isSelected = false;
            }
            updateSelectAllCheckBoxState();
            adapter.notifyDataSetChanged();
        });

        btnApply.setOnClickListener(v -> {
            if (listener != null) {
                listener.onUsersSelected(new ArrayList<>(userList));
            }
            dismiss();
        });
    }

    private void updateSelectAllCheckBoxState() {
        if (userList == null || userList.isEmpty()) return;
        long selectedCount = userList.stream().filter(u -> u.isSelected).count();
        if (selectedCount == userList.size()) {
            cbSelectAll.setChecked(true);
        } else if (selectedCount == 0) {
            cbSelectAll.setChecked(false);
        } else {
            cbSelectAll.setChecked(false);
        }
    }

    private class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {
        private final List<SelectableUser> users;

        UserAdapter(List<SelectableUser> users) {
            this.users = users;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_selection, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            SelectableUser user = users.get(position);
            holder.tvUserName.setText(user.name);
            holder.cbUser.setChecked(user.isSelected);

            holder.itemView.setOnClickListener(v -> holder.cbUser.performClick());

            holder.cbUser.setOnClickListener(v -> {
                user.isSelected = holder.cbUser.isChecked();
                updateSelectAllCheckBoxState();
            });
        }

        @Override
        public int getItemCount() {
            return users.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            CheckBox cbUser;
            TextView tvUserName;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                cbUser = itemView.findViewById(R.id.cb_user);
                tvUserName = itemView.findViewById(R.id.tv_user_name);
            }
        }
    }
}
