package com.yarg0007.robotpicontroller;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.Nullable;

import com.yarg0007.robotpicontroller.settings.SettingKeys;

public class ConfigActivity extends Activity {

    EditText videoUrl;
    EditText robotAudioPort;
    EditText host;
    Button saveButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);

        videoUrl = findViewById(R.id.video_stream_input);
        robotAudioPort = findViewById(R.id.robot_server_port_input);
        host = findViewById(R.id.host_field);
        saveButton = findViewById(R.id.save_button);

        final SharedPreferences sharedPreferences = getSharedPreferences("appsettings", MODE_PRIVATE);
        String savedRtspUrlValue = sharedPreferences.getString(SettingKeys.videoUrl, getResources().getString(R.string.video_stream_input));
        String savedRobotPortValue = sharedPreferences.getString(SettingKeys.robotAudioPort, "");
        String savedSshHostValue = sharedPreferences.getString(SettingKeys.host, getResources().getString(R.string.host_name_label));

        videoUrl.setText(savedRtspUrlValue);
        robotAudioPort.setText(savedRobotPortValue);
        host.setText(savedSshHostValue);

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();

                String rtspValue = videoUrl.getText().toString();
                String robotPortValue = robotAudioPort.getText().toString();
                String sshHostValue = host.getText().toString();

                sharedPreferencesEditor.putString(SettingKeys.videoUrl, rtspValue);
                sharedPreferencesEditor.putString(SettingKeys.robotAudioPort, robotPortValue);
                sharedPreferencesEditor.putString(SettingKeys.host, sshHostValue);;
                sharedPreferencesEditor.commit();

                finish();
            }
        });
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (hasWindowFocus) {
            hideSystemUI();
        }
    }

    private void hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        // Set the content to appear under the system bars so that the
                        // content doesn't resize when the system bars hide and show.
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        // Hide the nav bar and status bar
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }
}
