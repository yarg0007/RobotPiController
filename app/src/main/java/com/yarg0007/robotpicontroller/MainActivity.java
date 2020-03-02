package com.yarg0007.robotpicontroller;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.ToggleButton;

import com.yarg0007.robotpicontroller.audio.AudioStreamClient;
import com.yarg0007.robotpicontroller.input.ControllerInputData;
import com.yarg0007.robotpicontroller.input.ControllerInputThread;
import com.yarg0007.robotpicontroller.settings.SettingKeys;
import com.yarg0007.robotpicontroller.ssh.SshCommandCompletionObserver;
import com.yarg0007.robotpicontroller.ssh.SshCommandPayload;
import com.yarg0007.robotpicontroller.ssh.commands.SshServerCommands;
import com.yarg0007.robotpicontroller.ssh.SshManager;
import com.yarg0007.robotpicontroller.widgets.Joypad;

import java.io.IOException;
import java.net.UnknownHostException;

public class MainActivity extends AppCompatActivity implements ControllerInputData, SshCommandCompletionObserver {

    private static final boolean USE_TEXTURE_VIEW = false;
    private static final boolean ENABLE_SUBTITLES = false;

    WebView webVideoView;

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
    boolean connected = false;

    AlertDialog alert = null;

    // OnCreate / OnResume - create (if not null) and start (if not connected) - ssh, video, audio and server connections
    // OnPause / OnDestroy - stop (if connected) ssh, video, audio, and server connections - destroy not null

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        webVideoView = findViewById(R.id.video_layout);

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

        // Wire up actions

        connectButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked) {

                    getConfigurationValues();

                    if (savedVideoUrlValue == null || savedVideoUrlValue.isEmpty()) {
                        alert.setMessage(getResources().getString(R.string.connect_alert_message_rtsp));
                        alert.show();
                    } else if (savedRobotAudioport == null || savedRobotAudioport.isEmpty()) {
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

                        // Step 1: Connect to server and startup the video stream and server app
                        // Step 2: Wait for successful response from SSH operations and then:
                        //      - Start video connection
                        //      - Start controller connection - which starts audio connection

                        if (sshManager == null) {
                            try {
                                sshManager = new SshManager(savedSshHostValue, savedSshUsernameValue, savedSshPasswordValue);
                                sshManager.addObserver(MainActivity.this);
                                sshManager.openSshConnection();

                                if (sshManager.isRunning()) {
                                    // Start video stream & start the server
                                    sshManager.queuePayload(SshServerCommands.getStartVideoPayload());
                                }
                            } catch (Exception e) { // CORRECT THIS: WE MAY NOT NEED THIS EXCEPTION
                                sshManager = null;
                                alert.setMessage(getResources().getString(R.string.ssh_connection_failure));
                                alert.show();
                                return;
                            }
                        }

                        connected = true;
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
                            switch (which){
                                case DialogInterface.BUTTON_POSITIVE:
                                    sshManager.queuePayload(SshServerCommands.getShutdownRaspberryPiPayload());
                                    break;

                                case DialogInterface.BUTTON_NEGATIVE:
                                    sshManager.queuePayload(SshServerCommands.getStopVideoPayload());
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

    private void createOrRestoreConnections()  {

        // Start audio connection
        if (audioStreamClient == null) {
            try {
                audioStreamClient = new AudioStreamClient(savedSshHostValue, Integer.parseInt(savedRobotAudioport));
            } catch (UnknownHostException e) {

            }
            audioStreamClient.startConnection();
        }

        // Start controller connection
        if (controllerInputThread == null) {
            controllerInputThread = new ControllerInputThread(MainActivity.this, savedSshHostValue, Integer.valueOf(savedRobotAudioport));
            controllerInputThread.setAudioControls(audioStreamClient);
            controllerInputThread.startControllerInputThread();
        }

        // Start video
        if (webVideoView == null) {
            webVideoView = findViewById(R.id.video_layout);
        }

        webVideoView.loadUrl(savedVideoUrlValue);
    }

    private void stopConnections() {

        // Stop controller connection
        if (controllerInputThread != null) {
            controllerInputThread.stopControllerInputThread();
            controllerInputThread = null;
        }

        // Stop audio connection
        if (audioStreamClient != null) {
            audioStreamClient.stopConnection();
            audioStreamClient = null;
        }

        // Stop video
        if (webVideoView != null) {
            webVideoView.loadUrl("about:blank");
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
    public void commandsCompleted(SshCommandPayload payload) {

        if (payload.getId().equals(SshServerCommands.startVideoStreamId)) {
            sshManager.queuePayload(SshServerCommands.getStartServerPayload());
        } else if (payload.getId().equals(SshServerCommands.startServerId)) {
            createOrRestoreConnections();
        } else if (payload.getId().equals(SshServerCommands.stopVideoStreamId)) {
            sshManager.queuePayload(SshServerCommands.getStopServerPayload());
        } else if (payload.getId().equals(SshServerCommands.stopServerId)) {
            alert.setMessage("Server video and controller services have been stopped.");
            alert.show();
        } else if (payload.getId().equals(SshServerCommands.shutdownRaspberryPiId)) {
            sshManager.removeObserver(MainActivity.this);
            try {
                sshManager.closeSshConnection();
            } catch (IOException e) {
                ;
            }

            alert.setMessage("Raspberry Pi has been shutdown.");
            alert.show();
        }
    }

    @Override
    public void commandsCompletedWithError(SshCommandPayload payload, String errorMessage) {

        if (payload.getId().equals(SshServerCommands.startVideoStreamId)) {
            alert.setMessage(String.format("ERROR: %s | %s", getResources().getString(R.string.video_server_start_failure), errorMessage));
            alert.show();
        } else if (payload.getId().equals(SshServerCommands.startServerId)) {
            alert.setMessage(String.format("ERROR: %s | %s", getResources().getString(R.string.robot_server_start_failure), errorMessage));
            alert.show();
        } else if (payload.getId().equals(SshServerCommands.stopServerId)) {
            alert.setMessage(String.format("ERROR: %s | %s", getResources().getString(R.string.robot_server_stop_failure), errorMessage));
            alert.show();
        } else if (payload.getId().equals(SshServerCommands.stopVideoStreamId)) {
            alert.setMessage(String.format("ERROR: %s | %s", getResources().getString(R.string.video_server_stop_failure), errorMessage));
            alert.show();
        } else {
            alert.setMessage(String.format("ERROR: %s", errorMessage));
            alert.show();
        }
    }
}
