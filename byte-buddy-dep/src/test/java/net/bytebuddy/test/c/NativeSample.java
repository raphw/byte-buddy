package net.bytebuddy.test.c;

import java.io.*;

public class NativeSample {

    static {
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            throw new IllegalStateException("Test is not implemented for Windows");
        }
        try {
            InputStream inputStream = NativeSample.class.getResourceAsStream("/net_bytebuddy_test_c_NativeSample.so");
            File file;
            try {
                file = File.createTempFile("native_sample", ".so");
                file.deleteOnExit();
                OutputStream outputStream = new FileOutputStream(file);
                try {
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, length);
                    }
                } finally {
                    outputStream.close();
                }
            } finally {
                inputStream.close();
            }
            System.load(file.getAbsolutePath());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public native int foo(int left, int right);
}
