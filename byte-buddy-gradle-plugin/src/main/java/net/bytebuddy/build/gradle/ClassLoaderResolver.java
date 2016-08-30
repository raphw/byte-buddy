package net.bytebuddy.build.gradle;

import java.io.Closeable;
import java.io.IOException;

public class ClassLoaderResolver implements Closeable {

    public ClassLoader resolve(Transformation transformation) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() throws IOException {
    }
}
