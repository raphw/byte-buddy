package com.blogspot.mydailyjava.bytebuddy;

import java.util.Collection;
import java.util.Random;

public interface NameMaker {

    static interface UnnamedType {

        Class<?> getSuperClass();

        Collection<Class<?>> getInterfaces();

        Visibility getVisibility();

        TypeManifestation getTypeManifestation();

        SyntheticState getSyntheticState();

        int getClassVersion();
    }

    static class PrefixingRandom implements NameMaker {

        private static final String JAVA_LANG_PACKAGE = "java.lang.";
        private static final String BYTE_BUDDY_RENAME_PACKAGE = "com.blogspot.mydailyjava.bytebuddy.renamed.";

        private final String prefix;
        private final Random random;

        public PrefixingRandom(String prefix) {
            this.prefix = prefix;
            this.random = new Random();
        }

        @Override
        public String getName(UnnamedType unnamedType) {
            String superClassName = unnamedType.getSuperClass().getName();
            if (superClassName.startsWith(JAVA_LANG_PACKAGE)) {
                superClassName = BYTE_BUDDY_RENAME_PACKAGE + superClassName;
            }
            return String.format("%s$$%s$$%d", superClassName, prefix, random.nextInt());
        }
    }

    static class Fixed implements NameMaker {

        private final String name;

        public Fixed(String name) {
            this.name = name;
        }

        @Override
        public String getName(UnnamedType UnnamedType) {
            return name;
        }
    }

    String getName(UnnamedType unnamedType);
}
