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
import com.example.scsaattend.network.ApiService;
import com.example.scsaattend.network.RetrofitClient;
import com.example.scsaattend.dto.ErrorResponse;
import com.example.scsaattend.dto.LoginRequest;
import com.example.scsaattend.dto.LoginResponse;
import com.google.gson.Gson;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.io.IOException;

public class LoginActivity extends AppCompatActivity {
    private ApiService apiService;
    private static final String TAG = "LoginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // API 서비스 초기화
        apiService = RetrofitClient.getClient("http://192.168.50.211:8888").create(ApiService.class);

        EditText idInput = findViewById(R.id.idInput);
        EditText passwordInput = findViewById(R.id.passwordInput);
        Button loginButton = findViewById(R.id.loginButton);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userId = idInput.getText().toString();
                String userPwd = passwordInput.getText().toString();

                if (userId.isEmpty() || userPwd.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "아이디와 비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }

                LoginRequest request = new LoginRequest(userId, userPwd);
                
                Log.d(TAG, "Attempting login with ID: " + userId); 

                apiService.login(request).enqueue(new Callback<LoginResponse>() {
                    @Override
                    public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            LoginResponse loginResponse = response.body();
                            String serverRole = loginResponse.getRole();
                            
                            Log.d(TAG, "Login Success. ID: " + loginResponse.getId() + ", LoginID: " + loginResponse.getLoginId()); 
                            
                            String appRole = "admin".equalsIgnoreCase(serverRole) ? "ROLE_ADMIN" : "ROLE_USER";

                            SharedPreferences prefs = getSharedPreferences("AuthPrefs", MODE_PRIVATE);
                            SharedPreferences.Editor editor = prefs.edit();
                            
                            // 중요: API 요청에 사용할 회원 고유 ID(PK) 저장
                            editor.putInt("mem_pk", loginResponse.getId());
                            
                            editor.putString("user_role", appRole);
                            editor.putString("user_id", loginResponse.getLoginId());
                            editor.putString("user_name", loginResponse.getName());
                            editor.apply();

                            Toast.makeText(LoginActivity.this, "로그인 성공!", Toast.LENGTH_SHORT).show();

                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            // 에러 처리
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
            }
        });
    }
}