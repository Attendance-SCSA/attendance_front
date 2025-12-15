package com.example.scsaattend;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.example.scsaattend.user.TodayAttendanceFragment;
import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // 시스템 바 인셋 처리
        drawerLayout = findViewById(R.id.drawer_layout);
        ViewCompat.setOnApplyWindowInsetsListener(drawerLayout, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            return insets;
        });
        
        // 툴바 설정
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        // 툴바 마진 설정
        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
             Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
             android.view.ViewGroup.MarginLayoutParams params = (android.view.ViewGroup.MarginLayoutParams) v.getLayoutParams();
             params.topMargin = systemBars.top;
             v.setLayoutParams(params);
             return insets;
        });

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // 저장된 권한(Role) 확인
        SharedPreferences prefs = getSharedPreferences("AuthPrefs", MODE_PRIVATE);
        String role = prefs.getString("user_role", "ROLE_USER");
        String userId = prefs.getString("user_id", "Unknown User");
        String userName = prefs.getString("user_name", "Unknown Name");

        // 네비게이션 뷰 설정
        NavigationView navigationView = findViewById(R.id.nav_view);
        
        // 1. 기존 메뉴 비우기
        navigationView.getMenu().clear();
        
        // 2. 권한에 따라 다른 메뉴 XML 인플레이트
        String displayRole;
        if ("ROLE_ADMIN".equals(role)) {
            navigationView.inflateMenu(R.menu.activity_main_drawer_admin);
            displayRole = "관리자";
        } else {
            navigationView.inflateMenu(R.menu.activity_main_drawer_user);
            displayRole = "사용자";
        }

        // 3. 초기 설정 (첫 번째 메뉴 선택 및 화면 로드)
        if (navigationView.getMenu().size() > 0) {
            MenuItem firstItem = navigationView.getMenu().getItem(0);
            
            // 툴바 제목을 첫 번째 메뉴의 이름으로 설정
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(firstItem.getTitle());
            }
            firstItem.setChecked(true);
            
            // 사용자인 경우 첫 화면으로 '오늘의 출결' 프래그먼트 표시
            if ("ROLE_USER".equals(role)) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new TodayAttendanceFragment())
                        .commit();
            }
        }

        // 메뉴 아이템 클릭 리스너 설정
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            
            // 툴바 제목을 클릭한 메뉴의 이름으로 변경
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(item.getTitle());
            }
            
            // "오늘의 출결" 메뉴 클릭 시 프래그먼트 교체
            if (id == R.id.nav_user_today) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new TodayAttendanceFragment())
                        .commit();
            } 
            // 다른 메뉴에 대한 프래그먼트 교체 로직은 여기에 추가하면 됩니다.
            // else if (id == R.id.nav_user_my_status) { ... }
            
            drawerLayout.closeDrawer(GravityCompat.END);
            return true;
        });

        // 헤더 뷰 업데이트
        View headerView = navigationView.getHeaderView(0);
        TextView navHeaderTitle = headerView.findViewById(R.id.tv_nav_header_title);
        TextView navHeaderSubtitle = headerView.findViewById(R.id.tv_nav_header_subtitle);

        if(navHeaderTitle != null) navHeaderTitle.setText(userName); // 사용자 이름
        if(navHeaderSubtitle != null) navHeaderSubtitle.setText(displayRole); // ROLE_ADMIN -> 관리자

        // 헤더 클릭 시 정보 출력 (필요 시 주석 해제)
        /*
        headerView.setOnClickListener(v -> {
            Toast.makeText(MainActivity.this, "이름: " + userName + "\nRole: " + displayRole, Toast.LENGTH_LONG).show();
        });
        */
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed(); 
            return true;
        }
        
        if (id == R.id.action_menu) {
            if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                drawerLayout.closeDrawer(GravityCompat.END);
            } else {
                drawerLayout.openDrawer(GravityCompat.END);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END);
        } else {
            super.onBackPressed();
        }
    }
}