package com.example.scsaattend;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.example.scsaattend.login.LoginActivity;
import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            // 툴바 좌측에 홈 버튼(여기서는 로그아웃 버튼으로 사용) 활성화
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        drawerLayout = findViewById(R.id.drawer_layout);

        // DrawerLayout의 상태바 배경색을 툴바와 동일한 파란색으로 강제 설정
        // 이렇게 하면 테마의 기본색(붉은색) 대신 파란색이 적용됩니다.
        drawerLayout.setStatusBarBackgroundColor(getColor(R.color.scsa_blue));

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // 뒤로가기 버튼 처리 (최신 방식)
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                    drawerLayout.closeDrawer(GravityCompat.END);
                } else {
                    setEnabled(false); // 콜백 비활성화 후 기본 뒤로가기 동작 수행
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // 툴바 우측 메뉴 아이콘 클릭 시
        if (item.getItemId() == R.id.action_menu) {
            if (!drawerLayout.isDrawerOpen(GravityCompat.END)) {
                drawerLayout.openDrawer(GravityCompat.END);
            } else {
                drawerLayout.closeDrawer(GravityCompat.END);
            }
            return true;
        }

        // 툴바 좌측 아이콘(홈/업 버튼) 클릭 시 -> 로그아웃 처리
        if (item.getItemId() == android.R.id.home) {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            // 기존의 백스택을 모두 지우고 로그인 화면을 새로 시작 (로그아웃 효과)
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            // 홈 이동 로직
        } else if (id == R.id.nav_attend) {
            // 출결 확인 로직
        } else if (id == R.id.nav_settings) {
            // 설정 로직
        }

        drawerLayout.closeDrawer(GravityCompat.END);
        return true;
    }
}