package com.yarg0007.robotpicontroller.input;

import java.io.File;

public class ControllerInputThread extends Thread {

    /** Prefix expected for audio files where mouth should move. */
    private static final String SPEAK_FILE_PREFIX = "speak";

    /** Input controller to poll for data. */
    private ControllerInputData inputData;

    /** Track execution state of this thread. */
    private boolean running;

    private static final float EPSILON = 0.05f;

    /** Sleep duration in milliseconds before getting input device values. */
    private static final int SLEEP = 40;

    /** Drive input value. */
    private float driveInput;

    /** Turn input value. */
    private float turnInput;

    /** Head lift input value. */
    private float headLiftInput;

    /** Head turn input value. */
    private float headTurnInput;

    /** Open mouth input value. */
    private boolean openMouthInput;

    /** Talk input value. */
    private boolean talkingInput;

    /** Play sound input value. */
    private boolean playSoundInput;

    /** Playing of sound file should cause mouth to move. */
    private boolean soundInputShouldMoveMouth;

    /** Path to the sound file to play. */
    private String soundFilePath;

    /** Audio controls for starting and stopping audio file play back. */
    private AudioControls audioControls;

    private ControllerDataClient controllerDataClient;

    /**
     * Create a new controller input thread instance.
     * @param inputData Controller input to get data from.
     * @param robotServerAddress Host/ip address of the robot server to connect to.
     * @param robotServerPort Port of the robot server to connect to..
     */
    public ControllerInputThread(ControllerInputData inputData, String robotServerAddress, int robotServerPort) {

        this.inputData = inputData;
        controllerDataClient = new ControllerDataClient(robotServerAddress, robotServerPort);
        running = false;
    }

    /**
     * Set the audio controls to interface with.
     * @param audioControls Audio controls to interface with.
     */
    public void setAudioControls(AudioControls audioControls) {
        this.audioControls = audioControls;
    }

    /**
     * Start the controller input thread.
     */
    public void startControllerInputThread() {
        running = true;
        this.start();
    }

    /**
     * Stop the controller input thread and safely shut it down.
     */
    public void stopControllerInputThread() {
        running = false;
        this.interrupt();
    }

    /* (non-Javadoc)
     * @see java.lang.Thread#run()
     */
    @Override
    public void run() {

        while (running) {

            driveInput = inputData.getDriveInput();
            turnInput = inputData.getTurnInput();
            headLiftInput = inputData.getHeadLiftInput();
            headTurnInput = inputData.getHeadTurnInput();
            openMouthInput = inputData.getOpenMouth();
            talkingInput = inputData.getTalking();
            playSoundInput = inputData.getPlaySound();
            soundFilePath = inputData.getSelectedAudioFilePath();

            // Clamp drive, turn, head lift and head turn values.
            if (driveInput < EPSILON && driveInput > -EPSILON) {
                driveInput = 0.0f;
            }

            if (turnInput < EPSILON && turnInput > -EPSILON) {
                turnInput = 0.0f;
            }

            if (headLiftInput < EPSILON && headLiftInput > -EPSILON) {
                headLiftInput = 0.0f;
            }

            if (headTurnInput < EPSILON && headTurnInput > -EPSILON) {
                headTurnInput = 0.0f;
            }

            soundInputShouldMoveMouth = false;

            if (playSoundInput) {
                if (soundFilePath != null) {
                    File audioFile = new File(soundFilePath);
                    audioControls.playAudioFile(audioFile);

                    if (audioFile.getName().startsWith(SPEAK_FILE_PREFIX)) {
                        soundInputShouldMoveMouth = true;
                    }

                }
            } else {
                audioControls.stopAudioFile();
            }

            if (talkingInput) {
                audioControls.playMicrophone();
            } else {
                audioControls.stopMicrophone();
            }

            // Override talkingInput to cause robot to speak the audio file
            // being played.
            if (soundInputShouldMoveMouth) {
                talkingInput = true;
            }

            String dataMsg = String.format("%d,%d,%d,%d,%d,%d:", (int)(100*driveInput), (int)(100*turnInput), (int)(100*headLiftInput), (int)(100*headTurnInput), (talkingInput ? 1 : 0), (openMouthInput ? 1 : 0));

            controllerDataClient.sendData(dataMsg);

            try {
                Thread.sleep(SLEEP);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}
