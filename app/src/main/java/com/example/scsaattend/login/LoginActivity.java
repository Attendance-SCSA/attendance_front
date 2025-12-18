package com.example.scsaattend.login;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.scsaattend.MainActivity;
import com.example.scsaattend.R;
import com.example.scsaattend.dto.ErrorResponse;
import com.example.scsaattend.dto.LoginRequest;
import com.example.scsaattend.dto.LoginResponse;
import com.example.scsaattend.network.ApiService;
import com.example.scsaattend.network.RetrofitClient;
import com.google.gson.Gson;
import java.io.IOException;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    private ApiService apiService;
    private static final String TAG = "LoginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        apiService = RetrofitClient.getClient().create(ApiService.class);

        EditText idInput = findViewById(R.id.idInput);
        EditText passwordInput = findViewById(R.id.passwordInput);
        Button loginButton = findViewById(R.id.loginButton);

        loginButton.setOnClickListener(v -> {
            String userId = idInput.getText().toString();
            String userPwd = passwordInput.getText().toString();

            if (userId.isEmpty() || userPwd.isEmpty()) {
                Toast.makeText(LoginActivity.this, "아이디와 비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            LoginRequest request = new LoginRequest(userId, userPwd);

            apiService.login(request).enqueue(new Callback<LoginResponse>() {
                @Override
                public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        LoginResponse loginResponse = response.body();
                        String serverRole = loginResponse.getRole();

                        String appRole = "admin".equalsIgnoreCase(serverRole) ? "ROLE_ADMIN" : "ROLE_USER";

                        SharedPreferences prefs = getSharedPreferences("AuthPrefs", MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString("user_role", appRole);
                        editor.putString("user_id", loginResponse.getLoginId());
                        editor.putString("user_name", loginResponse.getName());
                        editor.putLong("user_numeric_id", loginResponse.getId());
                        editor.commit(); // Use commit() for synchronous save

                        Toast.makeText(LoginActivity.this, "로그인 성공!", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        int statusCode = response.code();
                        String errorMessage = "로그인 실패";
                        if (statusCode == 500) {
                            errorMessage = "서버 에러가 발생했습니다.";
                        } else {
                            try {
                                if (response.errorBody() != null) {
                                    String errorBodyString = response.errorBody().string();
                                    Gson gson = new Gson();
                                    ErrorResponse errorResponse = gson.fromJson(errorBodyString, ErrorResponse.class);
                                    if (errorResponse != null && errorResponse.getMessage() != null) {
                                        errorMessage = errorResponse.getMessage();
                                    }
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<LoginResponse> call, Throwable t) {
                    Log.e(TAG, "Network Error: " + t.getMessage(), t);
                    Toast.makeText(LoginActivity.this, "서버 연결 실패: " + t.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        });
    }
}