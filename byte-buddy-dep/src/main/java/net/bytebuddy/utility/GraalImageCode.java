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
import net.bytebuddy.utility.privilege.GetSystemPropertyAction;

import java.security.PrivilegedAction;

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
     * Indicates that the property
     */
    UNKNOWN(false, false),

    /**
     * Indicates that
     */
    NONE(false, false);

    /**
     * The current image code.
     */
    private static GraalImageCode CURRENT;

    /**
     * Resolves the status of the Graal image code.
     *
     * @return The status of the Graal image code.
     */
    @SuppressFBWarnings(value = "LI_LAZY_INIT_STATIC", justification = "This behaviour is intended to avoid early binding in native images.")
    public static GraalImageCode getCurrent() {
        GraalImageCode current = CURRENT;
        if (current == null) {
            String value = doPrivileged(new GetSystemPropertyAction("org.graalvm.nativeimage.imagecode"));
            if (value == null) {
                current = GraalImageCode.NONE;
            } else if (value.equalsIgnoreCase("agent")) {
                current = GraalImageCode.AGENT;
            } else if (value.equalsIgnoreCase("runtime")) {
                current = GraalImageCode.RUNTIME;
            } else if (value.equalsIgnoreCase("buildtime")) {
                current = GraalImageCode.BUILD;
            } else {
                current = GraalImageCode.UNKNOWN;
            }
            CURRENT = current;
        }
        return current;
    }

    /**
     * A proxy for {@code java.security.AccessController#doPrivileged} that is activated if available.
     *
     * @param action The action to execute from a privileged context.
     * @param <T>    The type of the action's resolved value.
     * @return The action's resolved value.
     */
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
}
