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
import androidx.fragment.app.Fragment;

import com.example.scsaattend.fragment.MemberManagementFragment;
import com.example.scsaattend.fragment.AttendanceTypeManagementFragment;
import androidx.fragment.app.Fragment;
import com.example.scsaattend.fragment.AttendanceDetailFragment;
import com.example.scsaattend.fragment.MyAttendanceFragment;
import com.example.scsaattend.fragment.TodayAttendanceFragment;
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
            // 관리자의 첫 화면 설정
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new AttendanceDetailFragment())
                    .commit();
        } else {
            navigationView.inflateMenu(R.menu.activity_main_drawer_user);
            displayRole = "사용자";
            // 사용자의 첫 화면 설정
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new TodayAttendanceFragment())
                    .commit();
        }

        // 3. 초기 툴바 제목 설정 (첫 번째 메뉴 아이템 제목으로)
        if (navigationView.getMenu().size() > 0) {
            MenuItem firstItem = navigationView.getMenu().getItem(0);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(firstItem.getTitle());
            }
            firstItem.setChecked(true);
        }

        // 메뉴 아이템 클릭 리스너 설정
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            Fragment fragment = null;

            if (id == R.id.nav_admin_detail) {
                fragment = new AttendanceDetailFragment();
            } else if (id == R.id.nav_user_today) {
                fragment = new TodayAttendanceFragment();
            } else if (id == R.id.nav_user_my_status) {
                fragment = new MyAttendanceFragment();
            } else if (id == R.id.nav_admin_member) {
                fragment = new MemberManagementFragment();
            } else if (id == R.id.nav_admin_type_change) {
                fragment = new AttendanceTypeManagementFragment();
            } else {
                Toast.makeText(MainActivity.this, item.getTitle() + " 선택됨", Toast.LENGTH_SHORT).show();
            }

            if (fragment != null) {
                 getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, fragment).commit();
            }

            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(item.getTitle());
            }
            
            // Toast.makeText(MainActivity.this, item.getTitle() + " 선택됨", Toast.LENGTH_SHORT).show();
            drawerLayout.closeDrawer(GravityCompat.END);
            return true;
        });

        // 헤더 뷰 업데이트
        View headerView = navigationView.getHeaderView(0);
        TextView navHeaderTitle = headerView.findViewById(R.id.tv_nav_header_title);
        TextView navHeaderSubtitle = headerView.findViewById(R.id.tv_nav_header_subtitle);

        if(navHeaderTitle != null) navHeaderTitle.setText(userName);
        if(navHeaderSubtitle != null) navHeaderSubtitle.setText(displayRole);
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