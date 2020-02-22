package com.yarg0007.robotpicontroller.ssh;

public interface SshCommandCompletionObserver {

    /**
     * Called when the SSH operations have completed without error.
     * @param payload Payload that was executed.
     */
    void commandsCompleted(SshCommandPayload payload);

    /**
     * Called when the SSH operations have completed, but there was an error.
     * @param payload Payload that was executed.
     * @param errorMessage Description of the issue that occurred.
     */
    void commandsCompletedWithError(SshCommandPayload payload, String errorMessage);
}
