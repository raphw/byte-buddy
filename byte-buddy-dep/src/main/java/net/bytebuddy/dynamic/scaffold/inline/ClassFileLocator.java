package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.instrumentation.type.TypeDescription;

import java.io.InputStream;

public interface ClassFileLocator {

    static enum ForClassPathType implements ClassFileLocator {

        INSTANCE;

        private static final String CLASS_FILE_EXTENSION = ".class";

        @Override
        public InputStream classFileFor(TypeDescription typeDescription) {
            InputStream classFileStream = ClassLoader.getSystemResourceAsStream(typeDescription.getInternalName() + CLASS_FILE_EXTENSION);
            if (classFileStream == null) {
                throw new IllegalArgumentException("Cannot locate type " + typeDescription + " on the class path");
            }
            return classFileStream;
        }
    }

    InputStream classFileFor(TypeDescription typeDescription);
}
