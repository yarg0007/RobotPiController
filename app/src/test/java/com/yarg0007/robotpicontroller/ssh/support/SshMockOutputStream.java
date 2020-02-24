package com.yarg0007.robotpicontroller.ssh.support;

import java.io.IOException;
import java.io.OutputStream;

public class SshMockOutputStream extends OutputStream {

    @Override
    public void write(int b) throws IOException {
        return;
    }
}
