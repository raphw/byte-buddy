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
package net.bytebuddy.agent;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.agent.utility.nullability.MaybeNull;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.InvocationTargetException;
import java.security.Permission;

/**
 * An installer class which defined the hook-in methods that are required by the Java agent specification.
 */
public class Installer {

    /**
     * The name of the {@link Installer} class that is stored in an obfuscated format which will not be relocated.
     */
    public static final String NAME = new StringBuilder("rellatsnI.tnega.yddubetyb.ten").reverse().toString();

    /**
     * A field for carrying the {@link java.lang.instrument.Instrumentation} that was loaded by the Byte Buddy
     * agent. Note that this field must never be accessed directly as the agent is injected into the VM's
     * system class loader. This way, the field of this class might be {@code null} even after the installation
     * of the Byte Buddy agent as this class might be loaded by a different class loader than the system class
     * loader.
     */
    @MaybeNull
    private static volatile Instrumentation instrumentation;

    /**
     * The installer provides only {@code static} hook-in methods and should not be instantiated.
     */
    private Installer() {
        throw new UnsupportedOperationException("This class is a utility class and not supposed to be instantiated");
    }

    /**
     * <p>
     * Returns the instrumentation that was loaded by the Byte Buddy agent. When a security manager is active,
     * the {@link RuntimePermission} for {@code getInstrumentation} is required by the caller.
     * </p>
     * <p>
     * <b>Important</b>: This method must only be invoked via the {@link ClassLoader#getSystemClassLoader()} where any
     * Java agent is loaded. It is possible that two versions of this class exist for different class loaders.
     * </p>
     *
     * @return The instrumentation instance of the Byte Buddy agent.
     */
    public static Instrumentation getInstrumentation() {
        try {
            Object securityManager = System.class.getMethod("getSecurityManager").invoke(null);
            if (securityManager != null) {
                Class.forName("java.lang.SecurityManager")
                        .getMethod("checkPermission", Permission.class)
                        .invoke(securityManager, new RuntimePermission("net.bytebuddy.agent.getInstrumentation"));
            }
        } catch (NoSuchMethodException ignored) {
            /* security manager not available on current VM */
        } catch (ClassNotFoundException ignored) {
            /* security manager not available on current VM */
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getTargetException();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else {
                throw new IllegalStateException("Failed to assert access rights using security manager", cause);
            }
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Failed to access security manager", exception);
        }
        Instrumentation instrumentation = Installer.instrumentation;
        if (instrumentation == null) {
            throw new IllegalStateException("The Byte Buddy agent is not loaded or this method is not called via the system class loader");
        }
        return instrumentation;
    }

    /**
     * Allows the installation of this agent via a command line argument.
     *
     * @param arguments       The unused agent arguments.
     * @param instrumentation The instrumentation instance.
     */
    public static void premain(String arguments, Instrumentation instrumentation) {
        doMain(instrumentation);
    }

    /**
     * Allows the installation of this agent via the attach API.
     *
     * @param arguments       The unused agent arguments.
     * @param instrumentation The instrumentation instance.
     */
    public static void agentmain(String arguments, Instrumentation instrumentation) {
        doMain(instrumentation);
    }

    /**
     * Installs the {@link Instrumentation} in the current class and possibly obfuscated class.
     *
     * @param instrumentation The instrumentation instance.
     */
    @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION", justification = "Exception should not interrupt agent attachment.")
    private static synchronized void doMain(Instrumentation instrumentation) {
        if (Installer.instrumentation != null) {
            return;
        }
        Installer.instrumentation = instrumentation;
        try {
            if (!Installer.class.getName().equals(NAME)) {
                Class.forName(NAME, false, ClassLoader.getSystemClassLoader())
                        .getField("instrumentation")
                        .set(null, instrumentation);
            }
        } catch (Throwable ignored) {
            /* do nothing */
        }
    }
}
