package com.yarg0007.robotpicontroller.ssh.support;

import com.yarg0007.robotpicontroller.ssh.SshCommandCompletionObserver;
import com.yarg0007.robotpicontroller.ssh.SshCommandPayload;

import org.junit.Assert;

public class SshObserverTester implements SshCommandCompletionObserver {

    private long timeout;
    private SshCommandPayload successPayload;
    private SshCommandPayload errorPayload;
    private String errorMessage;

    public SshObserverTester(int timeoutSeconds) {
        timeout = 1000000000L * timeoutSeconds;
    }

    /**
     * Reset the internal fields before waiting for the next notification.
     */
    public void reset() {
        successPayload = null;
        errorPayload = null;
        errorMessage = null;
    }

    public void waitForNoNotification() {

        long startTime = System.nanoTime();

        while(System.nanoTime() - startTime < timeout) {
            if (successPayload != null) {
                Assert.fail("Unexpected success notification received.");
                break;
            } else if (errorPayload != null) {
                Assert.fail("Unexpected error notification received.");
                break;
            }

            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void waitForSuccess(SshCommandPayload expectedPayload) {

        long startTime = System.nanoTime();

        while(System.nanoTime() - startTime < timeout) {
            if (successPayload != null) {
                break;
            }

            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (successPayload != null) {
            Assert.assertEquals("Success payload IDs must match.", expectedPayload.getId(), successPayload.getId());
            Assert.assertEquals("Success payload commands must match.", expectedPayload.getCommands(), successPayload.getCommands());
        } else {
            Assert.fail("Expected success notification, but never received notification.");
        }
    }

    public void waitForError(SshCommandPayload expectedPayload, String expectedErrorMessage) {

        long startTime = System.nanoTime();

        while(System.nanoTime() - startTime < timeout) {
            if (errorPayload != null) {
                break;
            }

            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (errorPayload != null) {
            Assert.assertEquals("Error payload IDs must match.", expectedPayload.getId(), errorPayload.getId());
            Assert.assertEquals("Error payload commands must match.", expectedPayload.getCommands(), errorPayload.getCommands());
            Assert.assertTrue(String.format("Error message must contain expected message. Expected: %s, Actual: %s", expectedErrorMessage, errorMessage), errorMessage.contains(expectedErrorMessage));
        } else {
            Assert.fail("Expected error notification, but never received notification.");
        }
    }

    @Override
    public void commandsCompleted(SshCommandPayload payload) {
        this.successPayload = payload;
    }

    @Override
    public void commandsCompletedWithError(SshCommandPayload payload, String errorMessage) {
        this.errorPayload = payload;
        this.errorMessage = errorMessage;
    }
}
