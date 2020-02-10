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
import com.yarg0007.robotpicontroller.ssh.SshManager;
import com.yarg0007.robotpicontroller.widgets.Joypad;

import java.net.UnknownHostException;

public class MainActivity extends AppCompatActivity implements ControllerInputData {

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

    String savedRtspUrlValue;
    String savedRobotHost;
    String savedRobotport;
    String savedSshHostValue;
    String savedSshPortValue;
    String savedSshUsernameValue;
    String savedSshPasswordValue;
    boolean connected = false;

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

        // Wire up actions

        connectButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked) {

                    getConfigurationValues();

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
                    AlertDialog alert = alertBuilder.create();

                    if (savedRtspUrlValue == null || savedRtspUrlValue.isEmpty()) {
                        alert.setMessage(getResources().getString(R.string.connect_alert_message_rtsp));
                        alert.show();
                    } else if (savedRobotHost == null || savedRobotHost.isEmpty()) {
                        alert.setMessage(getResources().getString(R.string.connect_alert_message_robot_host));
                        alert.show();
                    } else if (savedRobotport == null || savedRobotport.isEmpty()) {
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
                            sshManager = new SshManager();
                        }

                        if (!sshManager.startRobotServer()) {
                            alert.setMessage(getResources().getString(R.string.robot_server_start_failure));
                            alert.show();
                            return;
                        }

                        try {
                            startAudio();
                        } catch (UnknownHostException e) {
                            alert.setMessage(String.format("The host %s cannot be resolved. Try the ip address?", savedRobotHost));
                            alert.show();
                            return;
                        }

                        startController();
                        startVideo();

                        connected = true;
                    }

                } else { // Disconnect
                    connected = false;

                    stopAudio();
                    stopController();
                    stopVideo();

                    // TODO: add a shutdown operation somewhere for ssh commands
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

            try {
                startAudio();
            } catch (UnknownHostException e) {
                return;
            }
            startController();
            startVideo();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopController();
        stopAudio();
        stopVideo();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopController();
        stopAudio();
        stopVideo();
        // ToSO: stop all
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (hasWindowFocus) {
            hideSystemUI();
        }
    }

    private void startAudio() throws UnknownHostException {
        audioStreamClient = new AudioStreamClient(savedRobotHost, Integer.parseInt(savedRobotport));
        audioStreamClient.startConnection();
    }

    private void stopAudio() {
        if (audioStreamClient != null) {
            audioStreamClient.stopConnection();
        }
    }

    private void startController() {
        controllerInputThread = new ControllerInputThread(MainActivity.this, savedRobotHost, Integer.valueOf(savedRobotport));
        controllerInputThread.setAudioControls(audioStreamClient);
        controllerInputThread.startControllerInputThread();
    }

    private void stopController() {
        if (controllerInputThread != null) {
            controllerInputThread.stopControllerInputThread();
        }
    }

    private void startVideo() {

        if (webVideoView == null) {
            webVideoView = findViewById(R.id.video_layout);
        }

        webVideoView.loadUrl(savedRtspUrlValue);
    }

    private void stopVideo() {

        if (webVideoView != null) {
            webVideoView.loadUrl("about:blank");
        }
    }

    private void getConfigurationValues() {

        final SharedPreferences sharedPreferences = getSharedPreferences("appsettings", MODE_PRIVATE);
        savedRtspUrlValue = sharedPreferences.getString(SettingKeys.rtspUrl, null);
        savedRobotHost = sharedPreferences.getString(SettingKeys.robotHost, null);
        savedRobotport = sharedPreferences.getString(SettingKeys.robotPort, null);
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
}
