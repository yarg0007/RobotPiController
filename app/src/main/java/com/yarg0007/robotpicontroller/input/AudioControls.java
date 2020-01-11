package com.yarg0007.robotpicontroller.input;

import java.io.File;

public interface AudioControls {

    /**
     * Play an audio file.
     * @param audioFile Audio file to play.
     */
    public void playAudioFile(String audioFile);

    /**
     * Stop playing audio file.
     */
    public void stopAudioFile();

    /**
     * Take audio from microphone.
     */
    public void playMicrophone();

    /**
     * Stop sending stream from microphone.
     */
    public void stopMicrophone();

    /**
     * Set the millisecond delay between packets that are sent to the robot
     * for playing audio files. This does not apply to microphone input.
     * @param milliseconds Delay to apply.
     */
    public void setAudioFilePacketDelay(long milliseconds);
}
