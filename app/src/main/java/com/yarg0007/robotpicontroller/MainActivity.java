package com.yarg0007.robotpicontroller;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Log;
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
import com.yarg0007.robotpicontroller.widgets.Joypad;

import java.io.IOException;
import java.net.InetAddress;
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

    ControllerInputThread controllerInputThread;
    AudioStreamClient audioStreamClient;

    String savedVideoUrlValue;
    String savedControllerInputPort;
    String savedAudioInputPort;
    String savedAudioOutputPort;
    String savedHostValue;
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
                    } else if (savedControllerInputPort == null || savedControllerInputPort.isEmpty()) {
                        alert.setMessage(getResources().getString(R.string.connect_alert_message_robot_controller_port));
                        alert.show();
                    } else if (savedAudioInputPort == null || savedAudioInputPort.isEmpty()) {
                        alert.setMessage(getResources().getString(R.string.connect_alert_message_robot_audio_input_port));
                        alert.show();
                    } else if (savedAudioOutputPort == null || savedAudioOutputPort.isEmpty()) {
                        alert.setMessage(getResources().getString(R.string.connect_alert_message_robot_audio_output_port));
                        alert.show();
                    } else if (savedHostValue == null || savedHostValue.isEmpty()) {
                        alert.setMessage(getResources().getString(R.string.connect_alert_host));
                        alert.show();
                    } else {
                        createOrRestoreConnections();
                    }

                    connected = true;

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
                                    // TODO
                                    break;

                                case DialogInterface.BUTTON_NEGATIVE:
                                    // TODO
                                    break;
                           }
                        }
                    };
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
            audioStreamClient = new AudioStreamClient(savedHostValue, Integer.parseInt(savedAudioInputPort), Integer.parseInt(savedAudioOutputPort), getApplicationContext());
            if (!audioStreamClient.startConnection()) {
                alert.setMessage(getResources().getString(R.string.connect_alert_message_unknown_host));
                alert.show();
                return;
            }
        }
        // Start controller connection
        if (controllerInputThread == null) {
            controllerInputThread = new ControllerInputThread(MainActivity.this, savedHostValue, Integer.parseInt(savedControllerInputPort));
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
        savedControllerInputPort = sharedPreferences.getString(SettingKeys.controllerInputPort, null);
        savedAudioInputPort = sharedPreferences.getString(SettingKeys.savedAudioInputPort, null);
        savedAudioOutputPort = sharedPreferences.getString(SettingKeys.savedAudioOutputPort, null);
        savedHostValue = sharedPreferences.getString(SettingKeys.host, null);
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
