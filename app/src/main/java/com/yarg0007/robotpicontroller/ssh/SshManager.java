package com.yarg0007.robotpicontroller.ssh;

import com.yarg0007.robotpicontroller.log.Logger;
import com.yarg0007.robotpicontroller.ssh.commands.CommandExpectPair;

import net.schmizz.keepalive.KeepAliveProvider;
import net.schmizz.sshj.AndroidConfig;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.sf.expectit.Expect;
import net.sf.expectit.ExpectBuilder;
import net.sf.expectit.matcher.Matchers;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.IOException;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SshManager implements Runnable {

    private final static String TAG = "SSH_MANAGER";

    private boolean running = false;
    private boolean stopped = true;

    private SSHClient ssh;
    private Session session;

    private final String sshHost;
    private final String sshUsername;
    private final String sshPassword;

    private final List<SshCommandCompletionObserver> observers;
    private final List<SshCommandPayload> payloads;

    /**
     * Create a new instance with the specified login settings.
     * @param sshHost SSH host server
     * @param username SSH username
     * @param password SSH password
     */
    public SshManager(String sshHost, String username, String password) {
        this(sshHost, username, password, null);
    }

    /**
     * Use for test injection.
     * @param sshHost SSH host server
     * @param username SSH username
     * @param password SSH password
     * @param ssh Injected SSH Client instance (can be null)
     */
    protected SshManager(String sshHost, String username, String password, SSHClient ssh) {

        this.sshHost = sshHost;
        this.sshUsername = username;
        this.sshPassword = password;

        observers = new ArrayList<>();
        payloads = new ArrayList<>();

        this.ssh = ssh;
        this.session = session;
    }

    /**
     * Add observer that would like to received post ssh execution notifications.
     * @param observer Observer to send notifications to.
     */
    public void addObserver(SshCommandCompletionObserver observer) {
        observers.add(observer);
    }

    /**
     * Remove observer that no longer wishes to receive post ssh execution notifications.
     * @param observer Observer to stop sending notifications to.
     */
    public void removeObserver(SshCommandCompletionObserver observer) {
        observers.remove(observer);
    }

    /**
     * Open the SSH connection thread. Returns once the thread is ready to receive commands.
     */
    public void openSshConnection() {
        new Thread(this).start();

        while (!running) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Close down the SSH connection thread and stop all command execution.
     * @throws IOException Pass through of SSH related exceptions.
     */
    public void closeSshConnection() throws IOException {

        running = false;
        Thread.currentThread().interrupt();

        while(!stopped) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {

            }
        }

        if (session != null) {
            session.close();
        } else if (ssh != null) {
            ssh.disconnect();
        }
    }

    /**
     * Queue the specified payload for execution.
     * @param payload Payload containing the SSH commands to execute.
     */
    public void queuePayload(SshCommandPayload payload) {
        if (!running) {
            throw new IllegalStateException("Cannot add payload for execution until thread has been started.");
        } else {
            payloads.add(payload);
        }
    }

    /**
     * Check if the SshManager thread is running.
     * @return True if running, false otherwise.
     */
    public boolean isRunning() {
        return running;
    }

    @Override
    public void run() {

        stopped = false;

        if (ssh == null) {
            AndroidConfig androidConfig = new AndroidConfig();
            androidConfig.setKeepAliveProvider(KeepAliveProvider.KEEP_ALIVE);
            ssh = new SSHClient(androidConfig);
            ssh.addHostKeyVerifier(new PromiscuousVerifier());

            Security.removeProvider("BC");
            Security.insertProviderAt(new BouncyCastleProvider(), Security.getProviders().length + 1);
        }

        try {
            ssh.connect(sshHost);
            ssh.getConnection().getKeepAlive().setKeepAliveInterval(5);
            ssh.authPassword(sshUsername, sshPassword);
            running = true;
        } catch (IOException e) {
            running = false;
            Logger.e(TAG, "Error creating ssh connection. " + e.getMessage());
        }

        while (running) {

            SshCommandPayload payload = null;

            // Look for a payload to process.
            // If not found, sleep for a bit and try again.
            if (payloads.size() > 0) {
                synchronized (payloads) {
                    if (payloads.size() > 0) {
                        payload = payloads.get(0);
                        payloads.remove(0);
                    }
                }
            } else {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    continue;
                }
                continue;
            }

            if (payload == null) {
                continue;
            }

            List<CommandExpectPair> commands = payload.getCommands();
            CommandExecutionResult result = executeCommands(commands);

            if (result.wasSuccessful()) {
                notifyObserversOfCompletion(payload);
            } else {
                notifyObservsersOfError(payload, result.getMessage());
            }
        }

        stopped = true;
    }

    /**
     * Execute the command list.
     * @param commands List of commands to execute.
     * @return Result of executing the commands.
     */
    private CommandExecutionResult executeCommands(List<CommandExpectPair> commands) {

        Expect expect = null;
        CommandExecutionResult result = null;

        try {
            session = ssh.startSession();
            session.allocateDefaultPTY();
            Session.Shell shell = session.startShell();

            expect = new ExpectBuilder()
                    .withOutput(shell.getOutputStream())
                    .withInputs(shell.getInputStream(), shell.getErrorStream())
                    .withExceptionOnFailure()
                    .withTimeout(8, TimeUnit.SECONDS)
                    .build();

        } catch (IOException e) {
            String message = String.format("Error creating interactive shell. Exception: %s", e.getMessage());
            Logger.e(TAG, message);
            result = new CommandExecutionResult(false, message);
        }

        if (expect != null) {
            result = processCommands(expect, commands);

            try {
                expect.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (session != null) {
            try {
                session.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return result;
    }

    /**
     * Execute the specified commands. SHOULD ONLY BE CALLED BY executeCommands()!
     * @param expect Expect instance to use.
     * @param commands Commands to execute.
     * @return Result of executing the commmands.
     */
    private CommandExecutionResult processCommands(Expect expect, List<CommandExpectPair> commands) {

        // Always start by confirming we are at a prompt
        try {
            expect.expect(Matchers.contains("$"));
        } catch (IOException e) {
            String message = "SSH prompt is not ready for input. $ not found.";
            Logger.e(TAG, message);
            return new CommandExecutionResult(false, message);
        }

        for (CommandExpectPair command : commands) {

            // Facilitate early exit.
            if (!running) {
                break;
            }

            try {
                String execute = command.getCommandToExecute();
                String expected = command.getExpectedResult();

                Logger.d(TAG, String.format("Executing command [%s] and waiting for [%s]", execute, expected));

                expect.sendLine(execute);
                expect.expect(Matchers.contains(expected));

            } catch (IOException e) {
                String message = String.format("Error occurred executing statement %s. Exception message: %s", command.getCommandToExecute(), e.getMessage());
                Logger.e(TAG, message);
                return new CommandExecutionResult(false, message);
            }
        }

        return new CommandExecutionResult(true, "Command executed successfully.");
    }

    /**
     * Notify observers of an error during execution.
     * @param payload Payload that was executed.
     * @param errorMessage Error message.
     */
    private void notifyObservsersOfError(SshCommandPayload payload, String errorMessage) {
        for (SshCommandCompletionObserver observer : observers) {
            observer.commandsCompletedWithError(payload, errorMessage);
        }
    }

    /**
     * Notify observers of a successful execution.
     * @param payload Payload that was executed.
     */
    private void notifyObserversOfCompletion(SshCommandPayload payload) {
        for (SshCommandCompletionObserver observer : observers) {
            observer.commandsCompleted(payload);
        }
    }

    /**
     * Communicates back the result of executing commands.
     */
    public class CommandExecutionResult {

        private boolean wasSuccessful;
        private String message;

        CommandExecutionResult(boolean wasSuccessful, String message) {
            this.wasSuccessful = wasSuccessful;
            this.message = message;
        }

        boolean wasSuccessful() {
            return wasSuccessful;
        }

        String getMessage() {
            return message;
        }
    }
}
