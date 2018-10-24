/*
 * Copyright 2014 - 2018 Rafael Winterhalter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.bytebuddy.utility;

import java.util.Random;

/**
 * A provider of randomized {@link java.lang.String} values.
 */
public class RandomString {

    /**
     * The default length of a randomized {@link java.lang.String}.
     */
    public static final int DEFAULT_LENGTH = 8;

    /**
     * The symbols which are used to create a random {@link java.lang.String}.
     */
    private static final char[] SYMBOL;

    /**
     * The amount of bits to extract out of an integer for each key generated.
     */
    private static final int KEY_BITS;

    /*
     * Creates the symbol array.
     */
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
        int bits = Integer.SIZE - Integer.numberOfLeadingZeros(SYMBOL.length);
        KEY_BITS = bits - (Integer.bitCount(SYMBOL.length) == bits ? 0 : 1);
    }

    /**
     * A provider of random values.
     */
    private final Random random;

    /**
     * The length of the random strings that are created by this instance.
     */
    private final int length;

    /**
     * Creates a random {@link java.lang.String} provider where each {@link java.lang.String} is of
     * {@link net.bytebuddy.utility.RandomString#DEFAULT_LENGTH} length.
     */
    public RandomString() {
        this(DEFAULT_LENGTH);
    }

    /**
     * Creates a random {@link java.lang.String} provider where each value is of the given length.
     *
     * @param length The length of the random {@link String}.
     */
    public RandomString(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("A random string's length cannot be zero or negative");
        }
        this.length = length;
        random = new Random();
    }

    /**
     * Creates a random {@link java.lang.String} of {@link net.bytebuddy.utility.RandomString#DEFAULT_LENGTH} length.
     *
     * @return A random {@link java.lang.String}.
     */
    public static String make() {
        return make(DEFAULT_LENGTH);
    }

    /**
     * Creates a random {@link java.lang.String} of the given {@code length}.
     *
     * @param length The length of the random {@link String}.
     * @return A random {@link java.lang.String}.
     */
    public static String make(int length) {
        return new RandomString(length).nextString();
    }

    /**
     * Represents an integer value as a string hash. This string is not technically random but generates a fixed character
     * sequence based on the hash provided.
     *
     * @param value The value to represent as a string.
     * @return A string representing the supplied value as a string.
     */
    public static String hashOf(int value) {
        char[] buffer = new char[(Integer.SIZE / KEY_BITS) + ((Integer.SIZE % KEY_BITS) == 0 ? 0 : 1)];
        for (int index = 0; index < buffer.length; index++) {
            buffer[index] = SYMBOL[(value >>> index * KEY_BITS) & (-1 >>> (Integer.SIZE - KEY_BITS))];
        }
        return new String(buffer);
    }

    /**
     * Creates a new random {@link java.lang.String}.
     *
     * @return A random {@link java.lang.String} of the given length for this instance.
     */
    public String nextString() {
        char[] buffer = new char[length];
        for (int index = 0; index < length; index++) {
            buffer[index] = SYMBOL[random.nextInt(SYMBOL.length)];
        }
        return new String(buffer);
    }
}
