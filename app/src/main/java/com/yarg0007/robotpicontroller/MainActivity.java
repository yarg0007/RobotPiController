package com.yarg0007.robotpicontroller;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.ToggleButton;

import com.yarg0007.robotpicontroller.audio.AudioStreamClient;
import com.yarg0007.robotpicontroller.input.ControllerInputData;
import com.yarg0007.robotpicontroller.input.ControllerInputThread;
import com.yarg0007.robotpicontroller.server.ServerConnectionObserver;
import com.yarg0007.robotpicontroller.server.ServerConnectionThread;
import com.yarg0007.robotpicontroller.settings.SettingKeys;
import com.yarg0007.robotpicontroller.ssh.SshCommandCompletionObserver;
import com.yarg0007.robotpicontroller.ssh.SshCommandPayload;
import com.yarg0007.robotpicontroller.ssh.commands.SshServerCommands;
import com.yarg0007.robotpicontroller.ssh.SshManager;
import com.yarg0007.robotpicontroller.widgets.Joypad;
import com.yarg0007.robotpicontroller.widgets.VideoStream;

import java.io.IOException;
import java.net.UnknownHostException;

public class MainActivity extends AppCompatActivity implements ControllerInputData, SshCommandCompletionObserver, ServerConnectionObserver {

    VideoStream videoStreamView;

    Button configButton;
    ToggleButton connectButton;
    Spinner audioSpinner;
    Switch stickyHead;
    Button openMouthButton;
    ToggleButton playAudioToggleButton;
    Button speakButton;

    Joypad leftJoypad;
    Joypad rightJoypad;

    SshManager sshManager;
    ControllerInputThread controllerInputThread;
    AudioStreamClient audioStreamClient;

    String savedVideoUrlValue;
    String savedRobotAudioport;
    String savedSshHostValue;
    String savedSshPortValue;
    String savedSshUsernameValue;
    String savedSshPasswordValue;
    String savedServerHttpPort;
    String savedVideoStreamPort;
    boolean connected = false;

    AlertDialog alert = null;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, 1);
        }

        videoStreamView = findViewById(R.id.video_layout);

        configButton = findViewById(R.id.config_button);
        connectButton = findViewById(R.id.connect_button);
        audioSpinner = findViewById(R.id.audio_spinner);
        openMouthButton = findViewById(R.id.open_mouth_button);
        playAudioToggleButton = findViewById(R.id.play_audio_toggle_button);
        speakButton = findViewById(R.id.speak_button);

        stickyHead = findViewById(R.id.stickyhead);

        leftJoypad = findViewById(R.id.left_joystick);
        rightJoypad = findViewById(R.id.right_joystick);

        rightJoypad.setIsSticky(true);
        stickyHead.setChecked(true);

        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(MainActivity.this);
        alertBuilder.setTitle(R.string.connect_alert_title);
        alertBuilder.setCancelable(false);
        alertBuilder.setPositiveButton(R.string.connect_alert_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                connectButton.setChecked(false);
                dialog.cancel();
            }
        });
        alert = alertBuilder.create();

        connectButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked) {

                    getConfigurationValues();

                    if (savedRobotAudioport == null || savedRobotAudioport.isEmpty()) {
                        alert.setMessage(getResources().getString(R.string.connect_alert_message_robot_port));
                        alert.show();
                    } else if (savedSshHostValue == null || savedSshHostValue.isEmpty()) {
                        alert.setMessage(getResources().getString(R.string.connect_alert_ssh_host));
                        alert.show();
                    } else if (savedSshPortValue == null || savedSshPortValue.isEmpty()) {
                        alert.setMessage(getResources().getString(R.string.connect_alert_ssh_port));
                        alert.show();
                    } else if (savedSshUsernameValue == null || savedSshUsernameValue.isEmpty()) {
                        alert.setMessage(getResources().getString(R.string.connect_alert_ssh_username));
                        alert.show();
                    } else if (savedSshPasswordValue == null || savedSshPasswordValue.isEmpty()) {
                        alert.setMessage(getResources().getString(R.string.connect_alert_ssh_password));
                        alert.show();
                    } else {

                        if (sshManager == null) {
                            try {
                                sshManager = new SshManager(savedSshHostValue, Integer.parseInt(savedSshPortValue), savedSshUsernameValue, savedSshPasswordValue);
                                sshManager.addObserver(MainActivity.this);
                                sshManager.openSshConnection();
                            } catch (Exception e) {
                                sshManager = null;
                                alert.setMessage(getResources().getString(R.string.ssh_connection_failure));
                                alert.show();
                                return;
                            }
                        }
                    }

                } else { // Disconnect

                    if (!connected) {
                        return;
                    }

                    connected = false;

                    stopConnections();

                    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which) {
                                case DialogInterface.BUTTON_POSITIVE:
                                    new ServerConnectionThread(savedSshHostValue, Integer.parseInt(savedServerHttpPort), false, true, MainActivity.this).start();
                                    break;

                                case DialogInterface.BUTTON_NEGATIVE:
                                    new ServerConnectionThread(savedSshHostValue, Integer.parseInt(savedServerHttpPort), false, false, MainActivity.this).start();
                                    break;
                            }
                        }
                    };

                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setMessage("Would you like to shutdown the robot?").setPositiveButton("Yes", dialogClickListener)
                            .setNegativeButton("No", dialogClickListener).show();
                }
            }
        });

        stickyHead.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                rightJoypad.setIsSticky(isChecked);
            }
        });

        configButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ConfigActivity.class);
                MainActivity.this.startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        getConfigurationValues();
        if (connected) {
            createOrRestoreConnections();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopConnections();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopConnections();
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (hasWindowFocus) {
            hideSystemUI();
        }
    }

    private void createOrRestoreConnections() {

        if (audioStreamClient == null) {
            try {
                audioStreamClient = new AudioStreamClient(savedSshHostValue, Integer.parseInt(savedRobotAudioport));
                audioStreamClient.startConnection();
            } catch (UnknownHostException e) {
                audioStreamClient = null;
            } catch (Exception e) {
                audioStreamClient = null;
            }
        }

        if (controllerInputThread == null) {
            controllerInputThread = new ControllerInputThread(MainActivity.this, savedSshHostValue, Integer.valueOf(savedRobotAudioport));
            controllerInputThread.setAudioControls(audioStreamClient);
            controllerInputThread.startControllerInputThread();
        }

        if (videoStreamView == null) {
            videoStreamView = findViewById(R.id.video_layout);
        }

        videoStreamView.configure(savedSshHostValue, Integer.parseInt(savedVideoStreamPort));
        videoStreamView.startVideoStream();
    }

    private void stopConnections() {

        if (controllerInputThread != null) {
            controllerInputThread.stopControllerInputThread();
            controllerInputThread = null;
        }

        if (audioStreamClient != null) {
            audioStreamClient.stopConnection();
            audioStreamClient = null;
        }

        if (videoStreamView != null) {
            videoStreamView.stopVideoStream();
        }
    }

    private void getConfigurationValues() {
        final SharedPreferences sharedPreferences = getSharedPreferences("appsettings", MODE_PRIVATE);
        savedVideoUrlValue = sharedPreferences.getString(SettingKeys.videoUrl, null);
        savedRobotAudioport = sharedPreferences.getString(SettingKeys.robotAudioPort, null);
        savedSshHostValue = sharedPreferences.getString(SettingKeys.sshHost, null);
        savedSshPortValue = sharedPreferences.getString(SettingKeys.sshPort, null);
        savedSshUsernameValue = sharedPreferences.getString(SettingKeys.sshUsername, null);
        savedSshPasswordValue = sharedPreferences.getString(SettingKeys.sshPassword, null);
        savedServerHttpPort = sharedPreferences.getString(SettingKeys.serverHttpPort, "8001");
        savedVideoStreamPort = sharedPreferences.getString(SettingKeys.videoStreamPort, "8554");
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

    @Override
    public float getDriveInput() {
        return leftJoypad.getUserInputYPercentage();
    }

    @Override
    public float getTurnInput() {
        return leftJoypad.getUserInputXPercentage();
    }

    @Override
    public float getHeadLiftInput() {
        return rightJoypad.getUserInputYPercentage();
    }

    @Override
    public float getHeadTurnInput() {
        return rightJoypad.getUserInputXPercentage();
    }

    @Override
    public boolean getOpenMouth() {
        return openMouthButton.isPressed();
    }

    @Override
    public boolean getTalking() {
        return speakButton.isPressed();
    }

    @Override
    public boolean getPlaySound() {
        return playAudioToggleButton.isChecked();
    }

    @Override
    public String getSelectedAudioFilePath() {
        return audioSpinner.getSelectedItem().toString();
    }

    @Override
    public void commandsCompleted(SshCommandPayload payloadValue) {
        final SshCommandPayload payload = payloadValue;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (payload.getId().equals(SshServerCommands.connectedId)) {
                    connected = true;
                    sshManager.queuePayload(SshServerCommands.getStartServerPayload());
                } else if (payload.getId().equals(SshServerCommands.startServerId)) {
                    new ServerConnectionThread(savedSshHostValue, Integer.parseInt(savedServerHttpPort), true, false, MainActivity.this).start();
                }
            }
        });
    }

    @Override
    public void commandsCompletedWithError(SshCommandPayload payloadValue, String errorMessageValue) {
        final SshCommandPayload payload = payloadValue;
        final String errorMessage = errorMessageValue;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (payload.getId().equals(SshServerCommands.connectedId)) {
                    connected = false;
                    stopConnections();
                    sshManager = null;
                    connectButton.setChecked(false);
                    alert.setMessage(String.format("ERROR: %s | %s", getResources().getString(R.string.ssh_connection_timeout), errorMessage));
                    alert.show();
                } else if (payload.getId().equals(SshServerCommands.startServerId)) {
                    alert.setMessage(String.format("ERROR: %s | %s", getResources().getString(R.string.robot_server_start_failure), errorMessage));
                    alert.show();
                } else {
                    alert.setMessage(String.format("ERROR: %s", errorMessage));
                    alert.show();
                }
            }
        });
    }

    // ServerConnectionObserver callbacks

    @Override
    public void onConnectSuccess() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                createOrRestoreConnections();
            }
        });
    }

    @Override
    public void onConnectFailure(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                connected = false;
                connectButton.setChecked(false);
                alert.setMessage("Failed to connect to robot server: " + message);
                alert.show();
            }
        });
    }

    @Override
    public void onDisconnectComplete() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (sshManager != null) {
                    sshManager.removeObserver(MainActivity.this);
                    try {
                        sshManager.closeSshConnection();
                    } catch (IOException e) {
                        // ignore
                    }
                    sshManager = null;
                }
            }
        });
    }

    @Override
    public void onDisconnectFailure(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                alert.setMessage("Disconnect error: " + message);
                alert.show();
            }
        });
    }
}
