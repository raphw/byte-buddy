package net.bytebuddy.utility;

import java.util.Random;

public class RandomString {

    private static final int DEFAULT_LENGTH = 8;

    private static final char[] SYMBOL;

    static {
        StringBuilder symbol = new StringBuilder();
        for (char character = '0'; character <= '9'; character++) {
            symbol.append(character);
        }
        for (char character = 'a'; character <= 'z'; character++) {
            symbol.append(character);
        }
        for (char character = 'A'; character <= 'Z'; character++) {
            symbol.append(character);
        }
        SYMBOL = symbol.toString().toCharArray();
    }

    public static String make() {
        return make(DEFAULT_LENGTH);
    }

    public static String make(int length) {
        return new RandomString(length).nextString();
    }

    private final Random random;

    private final int length;

    public RandomString() {
        this(DEFAULT_LENGTH);
    }

    public RandomString(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("A random string's length cannot be zero or negative");
        }
        this.length = length;
        random = new Random();
    }

    public String nextString() {
        char[] buffer = new char[length];
        for (int index = 0; index < length; ++index) {
            buffer[index] = SYMBOL[random.nextInt(SYMBOL.length)];
        }
        return new String(buffer);
    }

    @Override
    public String toString() {
        return "RandomString{" +
                "random=" + random +
                ", length=" + length +
                '}';
    }
}
