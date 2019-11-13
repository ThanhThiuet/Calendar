package com.example.calendar;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import com.example.simplecalendar.activities.MainActivity;

public class FirstActivity extends AppCompatActivity
{
    private Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first);

        button = findViewById(R.id.button);
        button.setOnClickListener(v -> startActivity(new Intent(FirstActivity.this, MainActivity.class)));
    }
}
