package com.example.scsaattend.fragment;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.NumberPicker;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.scsaattend.R;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class BatchUpdateDialogFragment extends DialogFragment implements AttendanceTypeSelectionDialogFragment.OnAttendanceTypeSelectedListener {

    private CheckBox cbAttendanceType, cbIsOff, cbArrivalTime, cbLeavingTime, cbStatus, cbApproval, cbIsOfficial, cbAdminNote;
    private TextView tvAttendanceType, tvArrivalTime, tvLeavingTime;
    private Spinner spinnerStatus, spinnerApproval;
    private RadioGroup rgIsOff, rgIsOfficial;
    private EditText etAdminNote;
    private ImageButton btnClearLeavingTime;

    private Long selectedTypeId = null;
    private String arrivalTime = null;
    private String leavingTime = null;

    public interface OnUpdateListener {
        void onUpdate(Map<String, Object> updateData);
    }

    private OnUpdateListener listener;

    public static BatchUpdateDialogFragment newInstance() {
        return new BatchUpdateDialogFragment();
    }

    public void setOnUpdateListener(OnUpdateListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_batch_update, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupViews(view);
        setupSpinners();
        setupClickListeners();
        updateUIState();
    }

    private void setupViews(View view) {
        cbAttendanceType = view.findViewById(R.id.cb_attendance_type);
        cbIsOff = view.findViewById(R.id.cb_is_off);
        cbArrivalTime = view.findViewById(R.id.cb_arrival_time);
        cbLeavingTime = view.findViewById(R.id.cb_leaving_time);
        cbStatus = view.findViewById(R.id.cb_status);
        cbApproval = view.findViewById(R.id.cb_approval);
        cbIsOfficial = view.findViewById(R.id.cb_is_official);
        cbAdminNote = view.findViewById(R.id.cb_admin_note);

        tvAttendanceType = view.findViewById(R.id.tv_attendance_type);
        spinnerStatus = view.findViewById(R.id.spinner_status);
        spinnerApproval = view.findViewById(R.id.spinner_approval);

        rgIsOff = view.findViewById(R.id.rg_is_off);
        rgIsOfficial = view.findViewById(R.id.rg_is_official);

        tvArrivalTime = view.findViewById(R.id.tv_arrival_time);
        tvLeavingTime = view.findViewById(R.id.tv_leaving_time);
        etAdminNote = view.findViewById(R.id.et_admin_note);
        btnClearLeavingTime = view.findViewById(R.id.btn_clear_leaving_time);

        Button btnCancel = view.findViewById(R.id.btn_cancel);
        Button btnConfirm = view.findViewById(R.id.btn_confirm);
        btnCancel.setOnClickListener(v -> dismiss());
        btnConfirm.setOnClickListener(v -> onConfirm());
    }

    private void setupSpinners() {
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, new String[]{"상태 선택", "출석", "지각/조퇴", "결석"});
        spinnerStatus.setAdapter(statusAdapter);

        ArrayAdapter<String> approvalAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, new String[]{"승인 여부 선택", "승인", "불허", "미정"});
        spinnerApproval.setAdapter(approvalAdapter);
    }

    private void setupClickListeners() {
        CheckBox[] checkBoxes = {cbAttendanceType, cbIsOff, cbArrivalTime, cbLeavingTime, cbStatus, cbApproval, cbIsOfficial, cbAdminNote};
        for (CheckBox cb : checkBoxes) {
            cb.setOnCheckedChangeListener((buttonView, isChecked) -> updateUIState());
        }
        
        tvAttendanceType.setOnClickListener(v -> {
            AttendanceTypeSelectionDialogFragment dialog = AttendanceTypeSelectionDialogFragment.newInstance();
            dialog.setOnAttendanceTypeSelectedListener(this);
            dialog.show(getParentFragmentManager(), "AttendanceTypeSelectionDialog");
        });

        tvArrivalTime.setOnClickListener(v -> showCustomTimePicker(true));
        tvLeavingTime.setOnClickListener(v -> showCustomTimePicker(false));
        btnClearLeavingTime.setOnClickListener(v -> {
            leavingTime = null;
            tvLeavingTime.setText("퇴근 시간 선택");
            tvLeavingTime.setHint("퇴근 시간을 null로 설정");
        });
    }

    @Override
    public void onAttendanceTypeSelected(long typeId, String typeName) {
        selectedTypeId = typeId;
        tvAttendanceType.setText(typeName);
    }

    private void showCustomTimePicker(boolean isArrival) {
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
            if (isArrival) {
                arrivalTime = formattedTime;
                tvArrivalTime.setText(arrivalTime);
            } else {
                leavingTime = formattedTime;
                tvLeavingTime.setText(leavingTime);
            }
        });
        builder.setNegativeButton("취소", (dialog, which) -> dialog.cancel());

        builder.create().show();
    }

    private void updateUIState() {
        tvAttendanceType.setEnabled(cbAttendanceType.isChecked());
        for (int i = 0; i < rgIsOff.getChildCount(); i++) {
            rgIsOff.getChildAt(i).setEnabled(cbIsOff.isChecked());
        }
        tvArrivalTime.setEnabled(cbArrivalTime.isChecked());
        tvLeavingTime.setEnabled(cbLeavingTime.isChecked());
        btnClearLeavingTime.setEnabled(cbLeavingTime.isChecked());
        spinnerStatus.setEnabled(cbStatus.isChecked());
        spinnerApproval.setEnabled(cbApproval.isChecked());
        for (int i = 0; i < rgIsOfficial.getChildCount(); i++) {
            rgIsOfficial.getChildAt(i).setEnabled(cbIsOfficial.isChecked());
        }
        etAdminNote.setEnabled(cbAdminNote.isChecked());
    }

    private void onConfirm() {
        Map<String, Object> updateData = new HashMap<>();

        if (cbAttendanceType.isChecked()) {
            if (selectedTypeId != null) {
                updateData.put("aTypeId", selectedTypeId);
            }
        }

        if (cbIsOff.isChecked()) {
            updateData.put("isOff", ((RadioButton)getView().findViewById(rgIsOff.getCheckedRadioButtonId())).getText().toString().equals("휴일") ? "Y" : "N");
        }

        if (cbArrivalTime.isChecked()) {
            if (arrivalTime == null) {
                Toast.makeText(getContext(), "출근 시간을 선택해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            updateData.put("arrivalTime", arrivalTime);
        }

        if (cbLeavingTime.isChecked()) {
            updateData.put("leavingTime", leavingTime);
        }

        if (cbStatus.isChecked()) {
            String status = null;
            switch (spinnerStatus.getSelectedItemPosition()) {
                case 1: status = "normal"; break;
                case 2: status = "late/early"; break;
                case 3: status = "absent"; break;
            }
            if (spinnerStatus.getSelectedItemPosition() > 0) {
                updateData.put("status", status);
            }
        }

        if (cbApproval.isChecked()) {
            String approval = null;
            switch (spinnerApproval.getSelectedItemPosition()) {
                case 1: approval = "approved"; break;
                case 2: approval = "denied"; break;
                case 3: approval = null;
            }
            if (spinnerApproval.getSelectedItemPosition() > 0) {
                updateData.put("isApproved", approval);
            }
        }

        if (cbIsOfficial.isChecked()) {
            updateData.put("isOfficial", ((RadioButton)getView().findViewById(rgIsOfficial.getCheckedRadioButtonId())).getText().toString().equals("공결 (Y)") ? "Y" : "N");
        }

        if (cbAdminNote.isChecked()) {
            updateData.put("adminNote", etAdminNote.getText().toString().isEmpty() ? null : etAdminNote.getText().toString());
        }

        if (updateData.isEmpty()) {
            Toast.makeText(getContext(), "변경할 항목을 선택해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (listener != null) {
            listener.onUpdate(updateData);
        }
        dismiss();
    }
}
