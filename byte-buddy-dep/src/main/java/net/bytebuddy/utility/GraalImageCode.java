/*
 * Copyright 2014 - Present Rafael Winterhalter
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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.build.AccessControllerPlugin;
import net.bytebuddy.utility.nullability.MaybeNull;
import net.bytebuddy.utility.privilege.GetSystemPropertyAction;

import java.lang.reflect.Method;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * A utility that resolves Graal VM native image properties.
 */
public enum GraalImageCode {

    /**
     * Indicates that a Graal VM assisted configuration agent is running.
     */
    AGENT(true, false),

    /**
     * Indicates that a Graal VM native image build is executed.
     */
    BUILD(true, false),

    /**
     * Indicates that a Graal VM native image is being executed.
     */
    RUNTIME(true, true),

    /**
     * Indicates that a Graal VM property is set to an unknown value.
     */
    UNKNOWN(false, false),

    /**
     * Indicates that no Graal VM property is set.
     */
    NONE(false, false);

    /**
     * The current image code or {@code null} if the image code was not yet resolved. The image code must be
     * initialized lazily to avoid that it's bound to a value during native compilation.
     */
    @MaybeNull
    private static GraalImageCode current;

    /**
     * Resolves the status of the Graal image code.
     *
     * @return The status of the Graal image code.
     */
    @SuppressFBWarnings(value = {"LI_LAZY_INIT_STATIC", "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE"}, justification = "This behaviour is intended to avoid early binding in native images.")
    public static GraalImageCode getCurrent() {
        GraalImageCode current = GraalImageCode.current;
        if (current == null) {
            String value = doPrivileged(new GetSystemPropertyAction("org.graalvm.nativeimage.imagecode"));
            if (value == null) {
                String vendor = doPrivileged(new GetSystemPropertyAction("java.vm.vendor"));
                current = vendor != null && vendor.toLowerCase(Locale.US).contains("graalvm")
                        ? doPrivileged(ImageCodeContextAction.INSTANCE)
                        : GraalImageCode.NONE;
            } else if (value.equalsIgnoreCase("agent")) {
                current = GraalImageCode.AGENT;
            } else if (value.equalsIgnoreCase("runtime")) {
                current = GraalImageCode.RUNTIME;
            } else if (value.equalsIgnoreCase("buildtime")) {
                current = GraalImageCode.BUILD;
            } else {
                current = GraalImageCode.UNKNOWN;
            }
            GraalImageCode.current = current;
        }
        return current;
    }

    /**
     * Sorts the provided values only if an active Graal image code is set.
     *
     * @param value      The values to sort.
     * @param comparator the comparator to use.
     * @param <T>        The array component type.
     * @return The supplied array, potentially sorted.
     */
    public <T> T[] sorted(T[] value, Comparator<? super T> comparator) {
        if (defined) {
            Arrays.sort(value, comparator);
        }
        return value;
    }

    /**
     * A proxy for {@code java.security.AccessController#doPrivileged} that is activated if available.
     *
     * @param action The action to execute from a privileged context.
     * @param <T>    The type of the action's resolved value.
     * @return The action's resolved value.
     */
    @MaybeNull
    @AccessControllerPlugin.Enhance
    private static <T> T doPrivileged(PrivilegedAction<T> action) {
        return action.run();
    }

    /**
     * {@code true} if this image code indicates that a valid Graal related property is set.
     */
    private final boolean defined;

    /**
     * {@code true} if this image code indicates that a Graal native image build is executed.
     */
    private final boolean nativeImageExecution;

    /**
     * Creates a new Graal image code.
     *
     * @param defined              {@code true} if this image code indicates that a valid Graal related property is set.
     * @param nativeImageExecution {@code true} if this image code indicates that a Graal native image build is executed.
     */
    GraalImageCode(boolean defined, boolean nativeImageExecution) {
        this.defined = defined;
        this.nativeImageExecution = nativeImageExecution;
    }

    /**
     * Returns {@code true} if this image code indicates that a valid Graal related property is set.
     *
     * @return {@code true} if this image code indicates that a valid Graal related property is set.
     */
    public boolean isDefined() {
        return defined;
    }

    /**
     * Returns {@code true} if this image code indicates that a Graal native image build is executed.
     *
     * @return {@code true} if this image code indicates that a Graal native image build is executed.
     */
    public boolean isNativeImageExecution() {
        return nativeImageExecution;
    }

    /**
     * A privileged action to resolve the image code via the current JVM processes input arguments, if available.
     */
    protected enum ImageCodeContextAction implements PrivilegedAction<GraalImageCode> {

        /**
         * The singleton instance.
         */
        INSTANCE;

        @Override
        public GraalImageCode run() {
            try {
                Method method = Class.forName("java.lang.management.ManagementFactory").getMethod("getRuntimeMXBean");
                @SuppressWarnings("unchecked")
                List<String> arguments = (List<String>) method.getReturnType().getMethod("getInputArguments").invoke(method.invoke(null));
                for (String argument : arguments) {
                    if (argument.startsWith("-agentlib:native-image-agent")) {
                        return GraalImageCode.AGENT;
                    }
                }
            } catch (Throwable ignored) {
                /* do nothing */
            }
            return GraalImageCode.NONE;
        }
    }
}
