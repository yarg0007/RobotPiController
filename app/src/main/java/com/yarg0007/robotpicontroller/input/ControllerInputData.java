package com.yarg0007.robotpicontroller.input;

public interface ControllerInputData {

    /**
     * Confirm that the controller is connected.
     * @return True if controller is connected and functioning, false otherwise.
     */
    public boolean controllerConnected();

    /**
     * Get the name of the connected device.
     * @return Name of the connected device.
     */
    public String getNameOfConnectedDevice();

    /**
     * Poll device for data.
     */
    public void pollDevice();

    /**
     * Get drive direction input data. Values are expected to be in the range
     * from 1.0f to -1.0f inclusive. Bounds equate to full throttle forwards and
     * backwards respectively.
     * @return Drive input value.
     */
    public float getDriveInput();

    /**
     * Get turn direction input data. Values are expected to be in the range
     * from 1.0f to -1.0f inclusive. Bounds equate to full turn right or left
     * respectively.
     * @return Turn input value.
     */
    public float getTurnInput();

    /**
     * Get head lift direction input data. Values are expected to be in the
     * range from 1.0f to -1.0f inclusive. Bounds equate to full look up or down
     * respectively.
     * @return Head lift input value.
     */
    public float getHeadLiftInput();

    /**
     * Get head turn direction input value. Values are expected to be in the
     * range from 1.0f to -1.0f inclusive. Bounds equate to full turn right or
     * left respectively.
     * @return Head turn input value.
     */
    public float getHeadTurnInput();

    /**
     * Open mouth fully and hold it there while this returns true.
     * @return True to open mouth, false to close mouth.
     */
    public boolean getOpenMouth();

    /**
     * Returns true when audio input should be sent to the robot.
     * @return True to send audio input, false otherwise.
     */
    public boolean getTalking();

    /**
     * True to play sound.
     * @return True to play sound, false otherwise.
     */
    public boolean getPlaySound();

    /**
     * Get the full path of the selected audio file.
     * @return Full path of the selected audio file. Null if none selected.
     */
    public String getSelectedAudioFilePath();

    /**
     * Serialize input data for debugging purposes.
     * @return Serialize data.
     */
    public String serializeData();
}
