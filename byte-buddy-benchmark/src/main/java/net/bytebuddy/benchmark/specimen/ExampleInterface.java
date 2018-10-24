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
package net.bytebuddy.benchmark.specimen;

/**
 * An example interface with several methods which is used as a specimen in benchmarks.
 */
public interface ExampleInterface {

    /**
     * An example method.
     *
     * @param arg An argument.
     * @return The input argument.
     */
    boolean method(boolean arg);

    /**
     * An example method.
     *
     * @param arg An argument.
     * @return The input argument.
     */
    byte method(byte arg);

    /**
     * An example method.
     *
     * @param arg An argument.
     * @return The input argument.
     */
    short method(short arg);

    /**
     * An example method.
     *
     * @param arg An argument.
     * @return The input argument.
     */
    int method(int arg);

    /**
     * An example method.
     *
     * @param arg An argument.
     * @return The input argument.
     */
    char method(char arg);

    /**
     * An example method.
     *
     * @param arg An argument.
     * @return The input argument.
     */
    long method(long arg);

    /**
     * An example method.
     *
     * @param arg An argument.
     * @return The input argument.
     */
    float method(float arg);

    /**
     * An example method.
     *
     * @param arg An argument.
     * @return The input argument.
     */
    double method(double arg);

    /**
     * An example method.
     *
     * @param arg An argument.
     * @return The input argument.
     */
    Object method(Object arg);

    /**
     * An example method.
     *
     * @param arg1 An argument.
     * @param arg2 An argument.
     * @param arg3 An argument.
     * @return All arguments stored in an array.
     */
    boolean[] method(boolean arg1, boolean arg2, boolean arg3);

    /**
     * An example method.
     *
     * @param arg1 An argument.
     * @param arg2 An argument.
     * @param arg3 An argument.
     * @return All arguments stored in an array.
     */
    byte[] method(byte arg1, byte arg2, byte arg3);

    /**
     * An example method.
     *
     * @param arg1 An argument.
     * @param arg2 An argument.
     * @param arg3 An argument.
     * @return All arguments stored in an array.
     */
    short[] method(short arg1, short arg2, short arg3);

    /**
     * An example method.
     *
     * @param arg1 An argument.
     * @param arg2 An argument.
     * @param arg3 An argument.
     * @return All arguments stored in an array.
     */
    int[] method(int arg1, int arg2, int arg3);

    /**
     * An example method.
     *
     * @param arg1 An argument.
     * @param arg2 An argument.
     * @param arg3 An argument.
     * @return All arguments stored in an array.
     */
    char[] method(char arg1, char arg2, char arg3);

    /**
     * An example method.
     *
     * @param arg1 An argument.
     * @param arg2 An argument.
     * @param arg3 An argument.
     * @return All arguments stored in an array.
     */
    long[] method(long arg1, long arg2, long arg3);

    /**
     * An example method.
     *
     * @param arg1 An argument.
     * @param arg2 An argument.
     * @param arg3 An argument.
     * @return All arguments stored in an array.
     */
    float[] method(float arg1, float arg2, float arg3);

    /**
     * An example method.
     *
     * @param arg1 An argument.
     * @param arg2 An argument.
     * @param arg3 An argument.
     * @return All arguments stored in an array.
     */
    double[] method(double arg1, double arg2, double arg3);

    /**
     * An example method.
     *
     * @param arg1 An argument.
     * @param arg2 An argument.
     * @param arg3 An argument.
     * @return All arguments stored in an array.
     */
    Object[] method(Object arg1, Object arg2, Object arg3);
}
