package com.yarg0007.robotpicontroller.input;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class ControllerInputThreadTest {

    // -------------------------------------------------------------------------
    // Test doubles
    // -------------------------------------------------------------------------

    /** Captures every string passed to sendData without touching a real socket. */
    static class CapturingDataClient extends ControllerDataClient {
        final List<String> messages = new ArrayList<>();
        private final CountDownLatch latch;

        CapturingDataClient(CountDownLatch latch) {
            super("localhost", 0);
            this.latch = latch;
        }

        @Override public void open() {}

        @Override
        public void sendData(String data) {
            messages.add(data);
            latch.countDown();
        }
    }

    /** Records which AudioControls methods were called and with what arguments. */
    static class RecordingAudioControls implements AudioControls {
        String lastPlayedFile;
        boolean stopAudioFileCalled;
        boolean playMicrophoneCalled;
        boolean stopMicrophoneCalled;

        @Override public void playAudioFile(String path) { lastPlayedFile = path; }
        @Override public void stopAudioFile() { stopAudioFileCalled = true; }
        @Override public void playMicrophone() { playMicrophoneCalled = true; }
        @Override public void stopMicrophone() { stopMicrophoneCalled = true; }
        @Override public void setAudioFilePacketDelay(long ms) {}
    }

    /** Configurable stub for ControllerInputData. */
    static class FakeInputData implements ControllerInputData {
        float drive, turn, headLift, headTurn;
        boolean openMouth, talking, playSound;
        String filePath = "/data/files/fart.wav";

        @Override public float getDriveInput() { return drive; }
        @Override public float getTurnInput() { return turn; }
        @Override public float getHeadLiftInput() { return headLift; }
        @Override public float getHeadTurnInput() { return headTurn; }
        @Override public boolean getOpenMouth() { return openMouth; }
        @Override public boolean getTalking() { return talking; }
        @Override public boolean getPlaySound() { return playSound; }
        @Override public String getSelectedAudioFilePath() { return filePath; }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Run the thread until it emits one message, then stop it.
     * Returns the captured message string.
     */
    private String runOneIteration(FakeInputData input, RecordingAudioControls audio)
            throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        CapturingDataClient client = new CapturingDataClient(latch);
        ControllerInputThread thread = new ControllerInputThread(input, client);
        thread.setAudioControls(audio);
        thread.startControllerInputThread();
        boolean fired = latch.await(1, TimeUnit.SECONDS);
        thread.stopControllerInputThread();
        assertTrue("Thread should have sent at least one message within 1 s", fired);
        return client.messages.get(0);
    }

    /** Parse the six integer fields out of a control message. */
    private int[] parseFields(String msg) {
        String[] parts = msg.split(":")[0].split(",");
        assertEquals("Expected exactly 6 fields", 6, parts.length);
        int[] values = new int[6];
        for (int i = 0; i < 6; i++) {
            values[i] = Integer.parseInt(parts[i].trim());
        }
        return values;
    }

    // -------------------------------------------------------------------------
    // Message format tests
    // -------------------------------------------------------------------------

    @Test
    public void allZeroInput_sendsAllZeroMessage() throws Exception {
        FakeInputData input = new FakeInputData();
        String msg = runOneIteration(input, new RecordingAudioControls());
        assertEquals("0,0,0,0,0,0:?", msg);
    }

    @Test
    public void fullForwardDrive_encodedAs100() throws Exception {
        FakeInputData input = new FakeInputData();
        input.drive = 1.0f;
        int[] fields = parseFields(runOneIteration(input, new RecordingAudioControls()));
        assertEquals(100, fields[0]);
    }

    @Test
    public void fullReverseDrive_encodedAsNeg100() throws Exception {
        FakeInputData input = new FakeInputData();
        input.drive = -1.0f;
        int[] fields = parseFields(runOneIteration(input, new RecordingAudioControls()));
        assertEquals(-100, fields[0]);
    }

    @Test
    public void halfTurnRight_encodedAs50() throws Exception {
        FakeInputData input = new FakeInputData();
        input.turn = 0.5f;
        int[] fields = parseFields(runOneIteration(input, new RecordingAudioControls()));
        assertEquals(50, fields[1]);
    }

    @Test
    public void headLiftAndTurn_encodedCorrectly() throws Exception {
        FakeInputData input = new FakeInputData();
        input.headLift = 0.75f;
        input.headTurn = -0.25f;
        int[] fields = parseFields(runOneIteration(input, new RecordingAudioControls()));
        assertEquals(75, fields[2]);
        assertEquals(-25, fields[3]);
    }

    @Test
    public void talking_true_encodedAs1InField4() throws Exception {
        FakeInputData input = new FakeInputData();
        input.talking = true;
        int[] fields = parseFields(runOneIteration(input, new RecordingAudioControls()));
        assertEquals(1, fields[4]);
    }

    @Test
    public void talking_false_encodedAs0InField4() throws Exception {
        FakeInputData input = new FakeInputData();
        input.talking = false;
        int[] fields = parseFields(runOneIteration(input, new RecordingAudioControls()));
        assertEquals(0, fields[4]);
    }

    @Test
    public void openMouth_true_encodedAs1InField5() throws Exception {
        FakeInputData input = new FakeInputData();
        input.openMouth = true;
        int[] fields = parseFields(runOneIteration(input, new RecordingAudioControls()));
        assertEquals(1, fields[5]);
    }

    @Test
    public void openMouth_false_encodedAs0InField5() throws Exception {
        FakeInputData input = new FakeInputData();
        input.openMouth = false;
        int[] fields = parseFields(runOneIteration(input, new RecordingAudioControls()));
        assertEquals(0, fields[5]);
    }

    @Test
    public void messageHasSixFieldsBeforeColon() throws Exception {
        FakeInputData input = new FakeInputData();
        input.drive = 0.5f; input.turn = -0.3f; input.headLift = 0.1f;
        input.talking = true; input.openMouth = true;
        String msg = runOneIteration(input, new RecordingAudioControls());
        String[] fields = msg.split(":")[0].split(",");
        assertEquals(6, fields.length);
    }

    @Test
    public void allFieldsAreIntegers() throws Exception {
        FakeInputData input = new FakeInputData();
        input.drive = 0.73f; input.turn = -0.49f; input.headTurn = 0.12f;
        String msg = runOneIteration(input, new RecordingAudioControls());
        // parseFields throws NumberFormatException if any field is not an int
        parseFields(msg);
    }

    // -------------------------------------------------------------------------
    // Dead-zone (EPSILON) tests
    // -------------------------------------------------------------------------

    @Test
    public void driveJustBelowEpsilon_clampedToZero() throws Exception {
        FakeInputData input = new FakeInputData();
        input.drive = 0.04f; // 0.04 < EPSILON (0.05)
        int[] fields = parseFields(runOneIteration(input, new RecordingAudioControls()));
        assertEquals(0, fields[0]);
    }

    @Test
    public void driveAtEpsilon_notClamped() throws Exception {
        FakeInputData input = new FakeInputData();
        input.drive = 0.05f; // exactly EPSILON — boundary: NOT inside dead-zone
        int[] fields = parseFields(runOneIteration(input, new RecordingAudioControls()));
        assertEquals(5, fields[0]); // 0.05 * 100 = 5
    }

    @Test
    public void negativeBelowEpsilon_clampedToZero() throws Exception {
        FakeInputData input = new FakeInputData();
        input.turn = -0.04f; // -0.04 > -EPSILON
        int[] fields = parseFields(runOneIteration(input, new RecordingAudioControls()));
        assertEquals(0, fields[1]);
    }

    @Test
    public void headLift_deadZoneApplied() throws Exception {
        FakeInputData input = new FakeInputData();
        input.headLift = 0.03f;
        int[] fields = parseFields(runOneIteration(input, new RecordingAudioControls()));
        assertEquals(0, fields[2]);
    }

    @Test
    public void headTurn_deadZoneApplied() throws Exception {
        FakeInputData input = new FakeInputData();
        input.headTurn = -0.02f;
        int[] fields = parseFields(runOneIteration(input, new RecordingAudioControls()));
        assertEquals(0, fields[3]);
    }

    // -------------------------------------------------------------------------
    // Audio routing tests
    // -------------------------------------------------------------------------

    @Test
    public void playSound_true_callsPlayAudioFile() throws Exception {
        FakeInputData input = new FakeInputData();
        input.playSound = true;
        input.filePath = "/data/files/fart.wav";
        RecordingAudioControls audio = new RecordingAudioControls();
        runOneIteration(input, audio);
        assertEquals("/data/files/fart.wav", audio.lastPlayedFile);
    }

    @Test
    public void playSound_false_callsStopAudioFile() throws Exception {
        FakeInputData input = new FakeInputData();
        input.playSound = false;
        RecordingAudioControls audio = new RecordingAudioControls();
        runOneIteration(input, audio);
        assertTrue(audio.stopAudioFileCalled);
    }

    @Test
    public void talking_true_callsPlayMicrophone() throws Exception {
        FakeInputData input = new FakeInputData();
        input.talking = true;
        RecordingAudioControls audio = new RecordingAudioControls();
        runOneIteration(input, audio);
        assertTrue(audio.playMicrophoneCalled);
    }

    @Test
    public void talking_false_callsStopMicrophone() throws Exception {
        FakeInputData input = new FakeInputData();
        input.talking = false;
        RecordingAudioControls audio = new RecordingAudioControls();
        runOneIteration(input, audio);
        assertTrue(audio.stopMicrophoneCalled);
    }

    @Test
    public void noAudioControls_doesNotThrow() throws Exception {
        FakeInputData input = new FakeInputData();
        input.playSound = true;
        input.talking = true;
        ControllerInputThread thread = new ControllerInputThread(input,
                new CapturingDataClient(new CountDownLatch(1)));
        // audioControls is null — must not throw NullPointerException
        CountDownLatch latch = new CountDownLatch(1);
        CapturingDataClient client = new CapturingDataClient(latch);
        ControllerInputThread t = new ControllerInputThread(input, client);
        // do NOT call setAudioControls — leave it null
        t.startControllerInputThread();
        latch.await(1, TimeUnit.SECONDS);
        t.stopControllerInputThread();
    }

    // -------------------------------------------------------------------------
    // Speak-file mouth-movement tests
    // -------------------------------------------------------------------------

    @Test
    public void speakPrefixedFile_forcesTalkingFlagInMessage() throws Exception {
        FakeInputData input = new FakeInputData();
        input.playSound = true;
        input.talking = false; // user not holding SPEAK button
        input.filePath = "/data/files/speak_dig.wav";
        int[] fields = parseFields(runOneIteration(input, new RecordingAudioControls()));
        assertEquals("speak-prefixed file should set talking=1", 1, fields[4]);
    }

    @Test
    public void nonSpeakFile_doesNotForceTalkingFlag() throws Exception {
        FakeInputData input = new FakeInputData();
        input.playSound = true;
        input.talking = false;
        input.filePath = "/data/files/fart.wav";
        int[] fields = parseFields(runOneIteration(input, new RecordingAudioControls()));
        assertEquals("non-speak file should not set talking=1", 0, fields[4]);
    }

    @Test
    public void speakFile_withTalkingAlreadyTrue_keepsTalkingFlag() throws Exception {
        FakeInputData input = new FakeInputData();
        input.playSound = true;
        input.talking = true;
        input.filePath = "/data/files/speak_greeting.wav";
        int[] fields = parseFields(runOneIteration(input, new RecordingAudioControls()));
        assertEquals(1, fields[4]);
    }

    // -------------------------------------------------------------------------
    // buildControlMessage unit tests (direct, no threading)
    // -------------------------------------------------------------------------

    @Test
    public void buildControlMessage_fullValues() {
        ControllerInputThread t = new ControllerInputThread(new FakeInputData(),
                new CapturingDataClient(new CountDownLatch(1)));
        String msg = t.buildControlMessage(1.0f, -1.0f, 0.5f, -0.5f, true, false);
        assertEquals("100,-100,50,-50,1,0:?", msg);
    }

    @Test
    public void buildControlMessage_allZero() {
        ControllerInputThread t = new ControllerInputThread(new FakeInputData(),
                new CapturingDataClient(new CountDownLatch(1)));
        String msg = t.buildControlMessage(0f, 0f, 0f, 0f, false, false);
        assertEquals("0,0,0,0,0,0:?", msg);
    }

    @Test
    public void buildControlMessage_openMouth() {
        ControllerInputThread t = new ControllerInputThread(new FakeInputData(),
                new CapturingDataClient(new CountDownLatch(1)));
        String msg = t.buildControlMessage(0f, 0f, 0f, 0f, false, true);
        assertEquals("0,0,0,0,0,1:?", msg);
    }

    @Test
    public void buildControlMessage_serverParserCompatible() {
        // Simulate the server parsing logic: split on ':', then on ','
        ControllerInputThread t = new ControllerInputThread(new FakeInputData(),
                new CapturingDataClient(new CountDownLatch(1)));
        String msg = t.buildControlMessage(0.75f, -0.5f, 0.25f, -0.1f, true, true);
        String fieldSection = msg.split(":")[0];
        String[] tokens = fieldSection.split(",");
        assertEquals(6, tokens.length);
        assertEquals(75,  Integer.parseInt(tokens[0]));
        assertEquals(-50, Integer.parseInt(tokens[1]));
        assertEquals(25,  Integer.parseInt(tokens[2]));
        assertEquals(-10, Integer.parseInt(tokens[3]));
        assertEquals(1,   Integer.parseInt(tokens[4]));
        assertEquals(1,   Integer.parseInt(tokens[5]));
    }
}
