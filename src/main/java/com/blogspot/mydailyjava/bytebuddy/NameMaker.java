package com.blogspot.mydailyjava.bytebuddy;

import java.util.Random;

public interface NameMaker {

    static class PrefixingRandom implements NameMaker {

        private final String prefix;
        private final Random random;

        public PrefixingRandom(String prefix) {
            this.prefix = prefix;
            this.random = new Random();
        }

        @Override
        public String getName(Class<?> superClass) {
            return String.format("%s$$%s$$%d", superClass.getName(), prefix, random.nextInt());
        }
    }

    static class Fixed implements NameMaker {

        private final String name;

        public Fixed(String name) {
            this.name = name;
        }

        @Override
        public String getName(Class<?> superClass) {
            return name;
        }
    }

    String getName(Class<?> superClass);
}
