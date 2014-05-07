package net.bytebuddy.utility;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class PrecompiledTypeClassLoader extends ClassLoader {

    private static final int BUFFER_SIZE = 2 << 12;

    private static final String SUFFIX = ".class.raw";

    private final Map<String, URL> precompiledType;

    public PrecompiledTypeClassLoader(ClassLoader parent, String... types) {
        super(parent);
        this.precompiledType = new HashMap<String, URL>(types.length);
        for (String type : types) {
            URL resource = parent.getResource(type + SUFFIX);
            if (resource == null) {
                throw new IllegalArgumentException("Cannot locate " + type);
            }
            precompiledType.put(type, resource);
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        URL resource = precompiledType.remove(name);
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
