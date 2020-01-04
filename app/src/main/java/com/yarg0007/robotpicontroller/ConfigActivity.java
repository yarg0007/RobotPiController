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

    EditText rtspUrl;
    EditText sshHost;
    EditText sshPort;
    EditText sshUsername;
    EditText sshPassword;
    Button saveButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);

        rtspUrl = findViewById(R.id.video_stream_input);
        sshHost = findViewById(R.id.ssh_host_field);
        sshPort = findViewById(R.id.ssh_port_field);
        sshUsername = findViewById(R.id.ssh_username_field);
        sshPassword = findViewById(R.id.ssh_password_field);
        saveButton = findViewById(R.id.save_button);

        final SharedPreferences sharedPreferences = getSharedPreferences("appsettings", MODE_PRIVATE);
        String savedRtspUrlValue = sharedPreferences.getString(SettingKeys.rtspUrl, getResources().getString(R.string.video_stream_input));
        String savedSshHostValue = sharedPreferences.getString(SettingKeys.sshHost, getResources().getString(R.string.ssh_host_name_label));
        String savedSshPortValue = sharedPreferences.getString(SettingKeys.sshPort, getResources().getString(R.string.ssh_host_port_input));
        String savedSshUsernameValue = sharedPreferences.getString(SettingKeys.sshUsername, getResources().getString(R.string.ssh_userame_label));
        String savedSshPasswordValue = sharedPreferences.getString(SettingKeys.sshPassword, "");

        rtspUrl.setText(savedRtspUrlValue);
        sshHost.setText(savedSshHostValue);
        sshPort.setText(savedSshPortValue);
        sshUsername.setText(savedSshUsernameValue);
        sshPassword.setText(savedSshPasswordValue);

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();

                String rtspValue = rtspUrl.getText().toString();
                String sshHostValue = sshHost.getText().toString();
                String sshPortValue = sshPort.getText().toString();
                String sshUsernameValue = sshUsername.getText().toString();
                String sshPasswordValue = sshPassword.getText().toString();

                sharedPreferencesEditor.putString(SettingKeys.rtspUrl, rtspValue);
                sharedPreferencesEditor.putString(SettingKeys.sshHost, sshHostValue);
                sharedPreferencesEditor.putString(SettingKeys.sshPort, sshPortValue);
                sharedPreferencesEditor.putString(SettingKeys.sshUsername, sshUsernameValue);
                sharedPreferencesEditor.putString(SettingKeys.sshPassword, sshPasswordValue);
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
