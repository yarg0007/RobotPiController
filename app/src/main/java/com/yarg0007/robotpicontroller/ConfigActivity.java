package com.yarg0007.robotpicontroller;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.yarg0007.robotpicontroller.server.ServerConnectionClient;
import com.yarg0007.robotpicontroller.settings.SettingKeys;
import com.yarg0007.robotpicontroller.ssh.SshCommandCompletionObserver;
import com.yarg0007.robotpicontroller.ssh.SshCommandPayload;
import com.yarg0007.robotpicontroller.ssh.SshManager;
import com.yarg0007.robotpicontroller.ssh.commands.SshServerCommands;

import java.io.IOException;

public class ConfigActivity extends AppCompatActivity implements SshCommandCompletionObserver {

    EditText videoUrl;
    EditText robotAudioPort;
    EditText serverHttpPort;
    EditText videoStreamPort;
    EditText sshHost;
    EditText sshPort;
    EditText sshUsername;
    EditText sshPassword;
    Button backButton;
    Button saveButton;
    Button startServerButton;

    private SshManager sshManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);

        backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());

        videoUrl = findViewById(R.id.video_stream_input);
        robotAudioPort = findViewById(R.id.robot_server_port_input);
        serverHttpPort = findViewById(R.id.server_http_port_input);
        videoStreamPort = findViewById(R.id.video_stream_port_input);
        sshHost = findViewById(R.id.ssh_host_field);
        sshPort = findViewById(R.id.ssh_port_field);
        sshUsername = findViewById(R.id.ssh_username_field);
        sshPassword = findViewById(R.id.ssh_password_field);
        saveButton = findViewById(R.id.save_button);
        startServerButton = findViewById(R.id.start_server_button);

        final SharedPreferences sharedPreferences = getSharedPreferences("appsettings", MODE_PRIVATE);
        String savedRtspUrlValue = sharedPreferences.getString(SettingKeys.videoUrl, getResources().getString(R.string.video_stream_input));
        String savedRobotPortValue = sharedPreferences.getString(SettingKeys.robotAudioPort, "");
        String savedServerHttpPortValue = sharedPreferences.getString(SettingKeys.serverHttpPort, "8001");
        String savedVideoStreamPortValue = sharedPreferences.getString(SettingKeys.videoStreamPort, "8554");
        String savedSshHostValue = sharedPreferences.getString(SettingKeys.sshHost, getResources().getString(R.string.ssh_host_name_label));
        String savedSshPortValue = sharedPreferences.getString(SettingKeys.sshPort, getResources().getString(R.string.ssh_host_port_input));
        String savedSshUsernameValue = sharedPreferences.getString(SettingKeys.sshUsername, getResources().getString(R.string.ssh_userame_label));
        String savedSshPasswordValue = sharedPreferences.getString(SettingKeys.sshPassword, "");

        videoUrl.setText(savedRtspUrlValue);
        robotAudioPort.setText(savedRobotPortValue);
        serverHttpPort.setText(savedServerHttpPortValue);
        videoStreamPort.setText(savedVideoStreamPortValue);
        sshHost.setText(savedSshHostValue);
        sshPort.setText(savedSshPortValue);
        sshUsername.setText(savedSshUsernameValue);
        sshPassword.setText(savedSshPasswordValue);

        saveButton.setOnClickListener(v -> {
            SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();

            sharedPreferencesEditor.putString(SettingKeys.videoUrl, videoUrl.getText().toString());
            sharedPreferencesEditor.putString(SettingKeys.robotAudioPort, robotAudioPort.getText().toString());
            sharedPreferencesEditor.putString(SettingKeys.serverHttpPort, serverHttpPort.getText().toString());
            sharedPreferencesEditor.putString(SettingKeys.videoStreamPort, videoStreamPort.getText().toString());
            sharedPreferencesEditor.putString(SettingKeys.sshHost, sshHost.getText().toString());
            sharedPreferencesEditor.putString(SettingKeys.sshPort, sshPort.getText().toString());
            sharedPreferencesEditor.putString(SettingKeys.sshUsername, sshUsername.getText().toString());
            sharedPreferencesEditor.putString(SettingKeys.sshPassword, sshPassword.getText().toString());
            sharedPreferencesEditor.commit();

            finish();
        });

        startServerButton.setOnClickListener(v -> onStartServerButtonClicked());
    }

    private void onStartServerButtonClicked() {
        String httpHost = sshHost.getText().toString().trim();
        String httpPortStr = serverHttpPort.getText().toString().trim();
        String sshHostValue = sshHost.getText().toString().trim();
        String sshPortValue = sshPort.getText().toString().trim();
        String sshUsernameValue = sshUsername.getText().toString().trim();
        String sshPasswordValue = sshPassword.getText().toString().trim();

        if (httpHost.isEmpty() || httpPortStr.isEmpty()) {
            showAlert("SSH host and HTTP port must be set before checking server status.");
            return;
        }

        int httpPort;
        try {
            httpPort = Integer.parseInt(httpPortStr);
        } catch (NumberFormatException e) {
            showAlert("HTTP port must be a valid number.");
            return;
        }

        startServerButton.setEnabled(false);
        startServerButton.setText(R.string.checking_server);

        final int finalHttpPort = httpPort;
        new Thread(() -> {
            boolean running = new ServerConnectionClient().isServerRunning(httpHost, finalHttpPort);
            runOnUiThread(() -> {
                if (running) {
                    startServerButton.setEnabled(true);
                    startServerButton.setText(R.string.start_server_button);
                    new AlertDialog.Builder(this)
                            .setTitle("Server Running")
                            .setMessage(R.string.server_already_running)
                            .setPositiveButton("OK", null)
                            .show();
                } else {
                    startServerButton.setEnabled(true);
                    startServerButton.setText(R.string.start_server_button);
                    new AlertDialog.Builder(this)
                            .setTitle("Start Server?")
                            .setMessage(R.string.server_not_running_prompt)
                            .setPositiveButton("Start", (dialog, which) ->
                                    startServerViaSsh(sshHostValue, sshPortValue, sshUsernameValue, sshPasswordValue))
                            .setNegativeButton("Cancel", null)
                            .show();
                }
            });
        }).start();
    }

    private void startServerViaSsh(String host, String portStr, String username, String password) {
        if (host.isEmpty() || portStr.isEmpty() || username.isEmpty() || password.isEmpty()) {
            showAlert("SSH credentials (host, port, username, password) must be filled in to start the server.");
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            showAlert("SSH port must be a valid number.");
            return;
        }

        startServerButton.setEnabled(false);
        startServerButton.setText(R.string.starting_server);

        sshManager = new SshManager(host, port, username, password);
        sshManager.addObserver(this);
        sshManager.openSshConnection();
    }

    @Override
    public void commandsCompleted(SshCommandPayload payload) {
        if (payload.getId().equals(SshServerCommands.connectedId)) {
            sshManager.queuePayload(SshServerCommands.getStartServerPayload());
        } else if (payload.getId().equals(SshServerCommands.startServerId)) {
            closeSshManager();
            runOnUiThread(() -> {
                startServerButton.setEnabled(true);
                startServerButton.setText(R.string.start_server_button);
                new AlertDialog.Builder(this)
                        .setTitle("Server Started")
                        .setMessage(R.string.server_started)
                        .setPositiveButton("OK", null)
                        .show();
            });
        }
    }

    @Override
    public void commandsCompletedWithError(SshCommandPayload payload, String errorMessage) {
        closeSshManager();
        runOnUiThread(() -> {
            startServerButton.setEnabled(true);
            startServerButton.setText(R.string.start_server_button);
            showAlert("Failed to start server: " + errorMessage);
        });
    }

    private void closeSshManager() {
        if (sshManager != null) {
            try {
                sshManager.closeSshConnection();
            } catch (IOException ignored) {
            }
            sshManager = null;
        }
    }

    private void showAlert(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeSshManager();
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (hasWindowFocus) {
            hideSystemUI();
        }
    }

    private void hideSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }
}
