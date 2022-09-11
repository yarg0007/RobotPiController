package com.yarg0007.robotpicontroller;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.Nullable;

import com.yarg0007.robotpicontroller.settings.SettingKeys;

public class ConfigActivity extends Activity {

    EditText videoUrl;
    EditText robotControllerPort;
    EditText robotAudioInputPort;
    EditText robotAudioOutputPort;
    EditText host;
    Button saveButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);

        videoUrl = findViewById(R.id.video_stream_input);
        robotControllerPort = findViewById(R.id.server_port_controller_text);
        robotAudioInputPort = findViewById(R.id.server_port_audio_input_text);
        robotAudioOutputPort = findViewById(R.id.server_port_audio_output_text);
        host = findViewById(R.id.host_field);
        saveButton = findViewById(R.id.save_button);

        final SharedPreferences sharedPreferences = getSharedPreferences("appsettings", MODE_PRIVATE);
        String savedRtspUrlValue = sharedPreferences.getString(SettingKeys.videoUrl, getResources().getString(R.string.video_stream_input));
        String savedRobotControllerPortValue = sharedPreferences.getString(SettingKeys.controllerInputPort, "");
        String savedRobotAudioInputPortValue = sharedPreferences.getString(SettingKeys.savedAudioInputPort, "");
        String savedRobotAudioOutputPortValue = sharedPreferences.getString(SettingKeys.savedAudioOutputPort, "");
        String savedHostValue = sharedPreferences.getString(SettingKeys.host, getResources().getString(R.string.host_name_label));

        videoUrl.setText(savedRtspUrlValue);
        robotControllerPort.setText(savedRobotControllerPortValue);
        robotAudioInputPort.setText(savedRobotAudioInputPortValue);
        robotAudioOutputPort.setText(savedRobotAudioOutputPortValue);
        host.setText(savedHostValue);

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();

                String rtspValue = videoUrl.getText().toString();
                String robotControllerPortValue = robotControllerPort.getText().toString();
                String robotPortInputValue = robotAudioInputPort.getText().toString();
                String robotPortOutputValue = robotAudioOutputPort.getText().toString();
                String sshHostValue = host.getText().toString();

                sharedPreferencesEditor.putString(SettingKeys.videoUrl, rtspValue);
                sharedPreferencesEditor.putString(SettingKeys.controllerInputPort, robotControllerPortValue);
                sharedPreferencesEditor.putString(SettingKeys.savedAudioInputPort, robotPortInputValue);
                sharedPreferencesEditor.putString(SettingKeys.savedAudioOutputPort, robotPortOutputValue);
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
