package com.yarg0007.robotpicontroller.ssh;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class SshMockInputStream extends InputStream {

    final Byte[] input;
    int index = 0;

//    public SshMockInputStream(byte[] input) {
//        this.input = input;
//    }

    public SshMockInputStream(List<String> commandResponses) {

        ArrayList<Byte> byteData = new ArrayList<>();

        for (int i = 0; i < commandResponses.size(); i++) {

            byte[] commandBytes = commandResponses.get(i).getBytes();

            for (byte b : commandBytes) {
                byteData.add(b);
            }

            if (i < (commandResponses.size() - 1)) {
                byteData.add((byte) -1);
            }
        }

        this.input = byteData.toArray(new Byte[0]);
    }

    @Override
    public int read() throws IOException {

        // Return -1 when the byte array has been exhausted.
        if (index >= input.length) {
            return -1;
        }

        byte b = input[index++];
        return b;
    }
}
