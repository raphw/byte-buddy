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
package net.bytebuddy.agent;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.lang.reflect.InvocationTargetException;

/**
 * A Java program that attaches a Java agent to an external process.
 */
public class Attacher {

    /**
     * Base for access to a reflective member to make the code more readable.
     */
    private static final Object STATIC_MEMBER = null;

    /**
     * The name of the {@code attach} method of the  {@code VirtualMachine} class.
     */
    private static final String ATTACH_METHOD_NAME = "attach";

    /**
     * The name of the {@code loadAgent} method of the  {@code VirtualMachine} class.
     */
    private static final String LOAD_AGENT_METHOD_NAME = "loadAgent";

    /**
     * The name of the {@code detach} method of the  {@code VirtualMachine} class.
     */
    private static final String DETACH_METHOD_NAME = "detach";

    /**
     * The attacher provides only {@code static} utility methods and should not be instantiated.
     */
    private Attacher() {
        throw new UnsupportedOperationException("This class is a utility class and not supposed to be instantiated");
    }

    /**
     * Runs the attacher as a Java application.
     *
     * @param args A list containing the fully qualified name of the virtual machine type,
     *             the process id, the fully qualified name of the Java agent jar followed by
     *             an empty string if the argument to the agent is {@code null} or any number
     *             of strings where the first argument is proceeded by any single character
     *             which is stripped off.
     */
    @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION", justification = "Exception should not be rethrown but trigger a fallback")
    public static void main(String[] args) {
        try {
            String argument;
            if (args.length < 4 || args[3].length() == 0) {
                argument = null;
            } else {
                StringBuilder stringBuilder = new StringBuilder(args[3].substring(1));
                for (int index = 4; index < args.length; index++) {
                    stringBuilder.append(' ').append(args[index]);
                }
                argument = stringBuilder.toString();
            }
            install(Class.forName(args[0]), args[1], args[2], argument);
        } catch (Exception ignored) {
            System.exit(1);
        }
    }

    /**
     * Installs a Java agent on a target VM.
     *
     * @param virtualMachineType The virtual machine type to use for the external attachment.
     * @param processId          The id of the process being target of the external attachment.
     * @param agent              The Java agent to attach.
     * @param argument           The argument to provide or {@code null} if no argument is provided.
     * @throws NoSuchMethodException     If the virtual machine type does not define an expected method.
     * @throws InvocationTargetException If the virtual machine type raises an error.
     * @throws IllegalAccessException    If a method of the virtual machine type cannot be accessed.
     */
    protected static void install(Class<?> virtualMachineType,
                                  String processId,
                                  String agent,
                                  String argument) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Object virtualMachineInstance = virtualMachineType
                .getMethod(ATTACH_METHOD_NAME, String.class)
                .invoke(STATIC_MEMBER, processId);
        try {
            virtualMachineType
                    .getMethod(LOAD_AGENT_METHOD_NAME, String.class, String.class)
                    .invoke(virtualMachineInstance, agent, argument);
        } finally {
            virtualMachineType
                    .getMethod(DETACH_METHOD_NAME)
                    .invoke(virtualMachineInstance);
        }
    }
}
