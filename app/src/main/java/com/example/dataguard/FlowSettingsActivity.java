package com.example.dataguard;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class FlowSettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flow_settings);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        EditText etLimit = findViewById(R.id.etLimit);
        EditText etWarning = findViewById(R.id.etWarning);
        Button btnSave = findViewById(R.id.btnSave);

        android.content.SharedPreferences prefs = getSharedPreferences("dataguard", MODE_PRIVATE);
        etLimit.setText(String.valueOf(prefs.getFloat("limit_gb", 1.0f)));
        etWarning.setText(String.valueOf(prefs.getFloat("warning_gb", 0.8f)));

        btnSave.setOnClickListener(v -> {
            try {
                float limit = Float.parseFloat(etLimit.getText().toString());
                float warning = Float.parseFloat(etWarning.getText().toString());
                if (limit <= 0 || warning >= limit) {
                    Toast.makeText(this, "预警值必须小于上限", Toast.LENGTH_SHORT).show();
                    return;
                }
                prefs.edit().putFloat("limit_gb", limit).putFloat("warning_gb", warning).apply();
                Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show();
                finish();
            } catch (Exception e) {
                Toast.makeText(this, "请输入有效数字", Toast.LENGTH_SHORT).show();
            }
        });
    }
}