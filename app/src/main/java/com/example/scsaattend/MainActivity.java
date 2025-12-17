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

        drawerLayout = findViewById(R.id.drawer_layout);
        ViewCompat.setOnApplyWindowInsetsListener(drawerLayout, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            return insets;
        });
        
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
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

        SharedPreferences prefs = getSharedPreferences("AuthPrefs", MODE_PRIVATE);
        String role = prefs.getString("user_role", "ROLE_USER");
        String userName = prefs.getString("user_name", "Unknown Name");

        NavigationView navigationView = findViewById(R.id.nav_view);
        
        navigationView.getMenu().clear();
        
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

        if (navigationView.getMenu().size() > 0) {
            MenuItem firstItem = navigationView.getMenu().getItem(0);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(firstItem.getTitle());
            }
            firstItem.setChecked(true);
        }

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            Fragment fragment = null;

            if (id == R.id.nav_admin_detail) {
                fragment = new AttendanceDetailFragment();
            } else if (id == R.id.nav_user_today) {
                fragment = new TodayAttendanceFragment();
            } else if (id == R.id.nav_user_my_status) {
                fragment = new MyAttendanceFragment();
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
            
            drawerLayout.closeDrawer(GravityCompat.END);
            return true;
        });

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