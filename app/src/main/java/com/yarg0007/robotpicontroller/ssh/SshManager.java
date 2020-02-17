package com.yarg0007.robotpicontroller.ssh;

import android.util.Log;

import net.schmizz.keepalive.KeepAliveProvider;
import net.schmizz.sshj.DefaultConfig;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.ConnectionException;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.TransportException;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SshManager extends Thread {

    private final static String TAG = "SSH_MANAGER";

    private boolean running = false;

    private SSHClient ssh;
    private Session session;

    private final String sshHost;
    private final String sshUsername;
    private final String sshPassword;

    private List<SshCommandCompletionObserver> observers;
    private List<SshCommandCompletionPayload> payloads;

    public SshManager(String sshHost, String username, String password) throws IOException {

        this.sshHost = sshHost;
        this.sshUsername = username;
        this.sshPassword = password;

        observers = new ArrayList<>();
        payloads = new ArrayList<>();
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

    public void openSshConnection() {

        this.start();
    }

    public void closeSshConnection() throws IOException {

        running = false;
        this.interrupt();

        if (session != null) {
            session.close();
        } else if (ssh != null) {
            ssh.disconnect();
        }
    }

    private void executeCommands(SshCommandCompletionPayload payload) throws IOException {

        synchronized (payloads) {
            payloads.add(payload);
        }
    }

    @Override
    public void run() {

        DefaultConfig defaultConfig = new DefaultConfig();
        defaultConfig.setKeepAliveProvider(KeepAliveProvider.KEEP_ALIVE);
        ssh = new SSHClient(defaultConfig);
        ssh.addHostKeyVerifier(new PromiscuousVerifier());
        try {
            ssh.connect(sshHost);
            ssh.getConnection().getKeepAlive().setKeepAliveInterval(5);
            ssh.authPassword(sshUsername, sshPassword);
            session = ssh.startSession();
        } catch (IOException e) {
            running = false;
            Log.e(TAG, "Error creating ssh connection. " + e.getMessage());
        }

        while (running) {

            SshCommandCompletionPayload payload = null;

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

            List<String> commands = payload.getCommands();

            for (String command : commands) {
                try {
                    final Session.Command cmd = session.exec("ping -c 1 google.com");
                    Log.d(TAG, String.format("Executing SSH command: %s", command));
                    Log.d(TAG, String.format("Response: %s", IOUtils.readFully(cmd.getInputStream()).toString()));
                    cmd.join(5, TimeUnit.SECONDS);
                    Log.d(TAG, "exit status: " + cmd.getExitStatus());
                } catch (IOException e) {
                    notifyObservsersOfError(payload, String.format("Error occurred executing statement %s. Exception message: %s", command, e.getMessage()));
                    break;
                }
            }

            notifyObserversOfCompletion(payload);
        }
    }

    private void notifyObservsersOfError(SshCommandCompletionPayload payload, String errorMessage) {
        for (SshCommandCompletionObserver observer : observers) {
            observer.commandsCompletedWithError(payload, errorMessage);
        }
    }

    private void notifyObserversOfCompletion(SshCommandCompletionPayload payload) {
        for (SshCommandCompletionObserver observer : observers) {
            observer.commandsCompleted(payload);
        }
    }
}
