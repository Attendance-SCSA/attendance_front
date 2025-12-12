package com.example.scsaattend.login;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.example.scsaattend.MainActivity;
import com.example.scsaattend.R;

public class LoginActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 로그인 버튼 클릭 이벤트 설정
        Button loginButton = findViewById(R.id.loginButton);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 로그인 로직 (여기서는 단순히 메인으로 이동)
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                // 메인으로 이동 시 뒤로가기로 로그인 화면으로 못 돌아오게 하려면
                // finish()를 여기서 호출하거나 플래그를 사용할 수 있음
                startActivity(intent);
                finish();
            }
        });
    }
}