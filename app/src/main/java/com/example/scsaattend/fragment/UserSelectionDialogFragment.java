package com.example.scsaattend.fragment;

import android.os.Bundle;
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

import java.util.ArrayList;
import java.util.List;

public class UserSelectionDialogFragment extends DialogFragment {

    private CheckBox cbSelectAll;
    private RecyclerView rvUserList;
    private UserAdapter adapter;
    private List<User> userList;
    private OnUsersSelectedListener listener;

    public interface OnUsersSelectedListener {
        void onUsersSelected(ArrayList<User> selectedUsers);
    }

    public static UserSelectionDialogFragment newInstance(ArrayList<User> users) {
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
        if (getArguments() != null) {
            getArguments().setClassLoader(User.class.getClassLoader());
            userList = getArguments().getParcelableArrayList("users");
        }
        if (userList == null) {
            userList = new ArrayList<>();
        }
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
            for (User user : userList) {
                user.isSelected = isChecked;
            }
            adapter.notifyDataSetChanged();
        });

        btnReset.setOnClickListener(v -> {
            for (User user : userList) {
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
        
        updateSelectAllCheckBoxState();
    }
    
    private void updateSelectAllCheckBoxState() {
        long selectedCount = userList.stream().filter(u -> u.isSelected).count();
        if (selectedCount == userList.size()) {
            cbSelectAll.setChecked(true);
        } else if (selectedCount == 0) {
            cbSelectAll.setChecked(false);
        } else {
            cbSelectAll.setChecked(false); // 일부 선택 상태
        }
    }

    private class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {
        private final List<User> users;

        UserAdapter(List<User> users) {
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
            User user = users.get(position);
            holder.tvUserName.setText(user.name);
            holder.cbUser.setChecked(user.isSelected);

            // 행 전체 클릭 리스너
            holder.itemView.setOnClickListener(v -> {
                // 체크박스를 프로그래매틱하게 클릭하여 체크박스의 리스너 로직을 재사용
                holder.cbUser.performClick();
            });

            // 체크박스 클릭 리스너
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
