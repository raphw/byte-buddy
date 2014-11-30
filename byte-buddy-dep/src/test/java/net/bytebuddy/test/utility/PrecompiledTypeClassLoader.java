package net.bytebuddy.test.utility;

import java.io.IOException;
import java.net.URL;

public class PrecompiledTypeClassLoader extends ClassLoader {

    private static final int BUFFER_SIZE = 2 << 12;

    private static final String SUFFIX = ".class";

    public PrecompiledTypeClassLoader(ClassLoader parent) {
        super(parent);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        URL resource = getSystemResource(name.replace('.', '/') + SUFFIX);
        if (resource != null) {
            byte[] binaryRepresentation = new byte[BUFFER_SIZE];
            int readLength;
            try {
                readLength = resource.openStream().read(binaryRepresentation);
            } catch (IOException e) {
                throw new RuntimeException("Could not read from " + resource + " to manifest " + name, e);
            }
            if (readLength == BUFFER_SIZE) {
                throw new AssertionError(name + " compilation overflowed buffer");
            }
            return defineClass(name, binaryRepresentation, 0, readLength);
        }
        return super.findClass(name);
    }
}
