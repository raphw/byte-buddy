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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.NamedElement;

import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Type-safe representation of a {@code java.lang.Module}. On platforms that do not support the module API, modules are represented by {@code null}.
 */
public class JavaModule implements NamedElement.WithOptionalName {

    /**
     * Canonical representation of a Java module on a JVM that does not support the module API.
     */
    public static final JavaModule UNSUPPORTED = null;

    /**
     * The dispatcher to use for accessing Java modules, if available.
     */
    private static final Dispatcher DISPATCHER = AccessController.doPrivileged(Dispatcher.CreationAction.INSTANCE);

    /**
     * The {@code java.lang.Module} instance this wrapper represents.
     */
    private final Object module;

    /**
     * Creates a new Java module representation.
     *
     * @param module The {@code java.lang.Module} instance this wrapper represents.
     */
    protected JavaModule(Object module) {
        this.module = module;
    }

    /**
     * Returns a representation of the supplied type's {@code java.lang.Module} or {@code null} if the current VM does not support modules.
     *
     * @param type The type for which to describe the module.
     * @return A representation of the type's module or {@code null} if the current VM does not support modules.
     */
    public static JavaModule ofType(Class<?> type) {
        return DISPATCHER.moduleOf(type);
    }

    /**
     * Represents the supplied {@code java.lang.Module} as an instance of this class and validates that the
     * supplied instance really represents a Java {@code Module}.
     *
     * @param module The module to represent.
     * @return A representation of the supplied Java module.
     */
    public static JavaModule of(Object module) {
        if (!JavaType.MODULE.getTypeStub().isInstance(module)) {
            throw new IllegalArgumentException("Not a Java module: " + module);
        }
        return new JavaModule(module);
    }

    /**
     * Checks if the current VM supports the {@code java.lang.Module} API.
     *
     * @return {@code true} if the current VM supports modules.
     */
    public static boolean isSupported() {
        return DISPATCHER.isAlive();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isNamed() {
        return DISPATCHER.isNamed(module);
    }

    /**
     * {@inheritDoc}
     */
    public String getActualName() {
        return DISPATCHER.getName(module);
    }

    /**
     * Returns a resource stream for this module for a resource of the given name or {@code null} if such a resource does not exist.
     *
     * @param name The name of the resource.
     * @return An input stream for the resource or {@code null} if it does not exist.
     */
    public InputStream getResourceAsStream(String name) {
        return DISPATCHER.getResourceAsStream(module, name);
    }

    /**
     * Returns the class loader of this module.
     *
     * @return The class loader of the represented module.
     */
    public ClassLoader getClassLoader() {
        return DISPATCHER.getClassLoader(module);
    }

    /**
     * Unwraps this instance to a {@code java.lang.Module}.
     *
     * @return The represented {@code java.lang.Module}.
     */
    public Object unwrap() {
        return module;
    }

    /**
     * Checks if this module can read the exported packages of the supplied module.
     *
     * @param module The module to check for its readability by this module.
     * @return {@code true} if this module can read the supplied module.
     */
    public boolean canRead(JavaModule module) {
        return DISPATCHER.canRead(this.module, module.unwrap());
    }

    /**
     * Adds a read-edge to this module to the supplied module using the instrumentation API.
     *
     * @param instrumentation The instrumentation instance to use for adding the edge.
     * @param module          The module to add as a read dependency to this module.
     */
    public void addReads(Instrumentation instrumentation, JavaModule module) {
        DISPATCHER.addReads(instrumentation, this.module, module.unwrap());
    }

    @Override
    public int hashCode() {
        return module.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        } else if (!(other instanceof JavaModule)) {
            return false;
        }
        JavaModule javaModule = (JavaModule) other;
        return module.equals(javaModule.module);
    }

    @Override
    public String toString() {
        return module.toString();
    }

    /**
     * A dispatcher for accessing the {@code java.lang.Module} API if it is available on the current VM.
     */
    protected interface Dispatcher {

        /**
         * Checks if this dispatcher is alive, i.e. supports modules.
         *
         * @return {@code true} if modules are supported on the current VM.
         */
        boolean isAlive();

        /**
         * Extracts the Java {@code Module} for the provided class or returns {@code null} if the current VM does not support modules.
         *
         * @param type The type for which to extract the module.
         * @return The class's {@code Module} or {@code null} if the current VM does not support modules.
         */
        JavaModule moduleOf(Class<?> type);

        /**
         * Returns {@code true} if the supplied module is named.
         *
         * @param module The {@code java.lang.Module} to check for the existence of a name.
         * @return {@code true} if the supplied module is named.
         */
        boolean isNamed(Object module);

        /**
         * Returns the module's name.
         *
         * @param module The {@code java.lang.Module} to check for its name.
         * @return The module's (implicit or explicit) name.
         */
        String getName(Object module);

        /**
         * Returns a resource stream for this module for a resource of the given name or {@code null} if such a resource does not exist.
         *
         * @param module The {@code java.lang.Module} instance to apply this method upon.
         * @param name   The name of the resource.
         * @return An input stream for the resource or {@code null} if it does not exist.
         */
        InputStream getResourceAsStream(Object module, String name);

        /**
         * Returns the module's class loader.
         *
         * @param module The {@code java.lang.Module}
         * @return The module's class loader.
         */
        ClassLoader getClassLoader(Object module);

        /**
         * Checks if the source module can read the target module.
         *
         * @param source The source module.
         * @param target The target module.
         * @return {@code true} if the source module can read the target module.
         */
        boolean canRead(Object source, Object target);

        /**
         * Adds a read-edge from the source to the target module.
         *
         * @param instrumentation The instrumentation instance to use for adding the edge.
         * @param source          The source module.
         * @param target          The target module.
         */
        void addReads(Instrumentation instrumentation, Object source, Object target);

        /**
         * A creation action for a dispatcher.
         */
        enum CreationAction implements PrivilegedAction<Dispatcher> {

            /**
             * The singleton instance.
             */
            INSTANCE;

            /**
             * {@inheritDoc}
             */
            @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION", justification = "Exception should not be rethrown but trigger a fallback")
            public Dispatcher run() {
                try {
                    Class<?> module = Class.forName("java.lang.Module");
                    return new Dispatcher.Enabled(Class.class.getMethod("getModule"),
                            module.getMethod("getClassLoader"),
                            module.getMethod("isNamed"),
                            module.getMethod("getName"),
                            module.getMethod("getResourceAsStream", String.class),
                            module.getMethod("canRead", module),
                            Instrumentation.class.getMethod("isModifiableModule", module),
                            Instrumentation.class.getMethod("redefineModule", module, Set.class, Map.class, Map.class, Set.class, Map.class));
                } catch (Exception ignored) {
                    return Dispatcher.Disabled.INSTANCE;
                }
            }
        }

        /**
         * A dispatcher for a VM that does support the {@code java.lang.Module} API.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class Enabled implements Dispatcher {

            /**
             * An empty array that can be used to indicate no arguments to avoid an allocation on a reflective call.
             */
            private static final Object[] NO_ARGUMENTS = new Object[0];

            /**
             * The {@code java.lang.Class#getModule()} method.
             */
            private final Method getModule;

            /**
             * The {@code java.lang.Module#getClassLoader()} method.
             */
            private final Method getClassLoader;

            /**
             * The {@code java.lang.Module#isNamed()} method.
             */
            private final Method isNamed;

            /**
             * The {@code java.lang.Module#getName()} method.
             */
            private final Method getName;

            /**
             * The {@code java.lang.Module#getResourceAsStream(String)} method.
             */
            private final Method getResourceAsStream;

            /**
             * The {@code java.lang.Module#canRead(Module)} method.
             */
            private final Method canRead;

            /**
             * The {@code java.lang.instrument.Instrumentation#isModifiableModule} method.
             */
            private final Method isModifiableModule;

            /**
             * The {@code java.lang.instrument.Instrumentation#redefineModule} method.
             */
            private final Method redefineModule;

            /**
             * Creates an enabled dispatcher.
             *
             * @param getModule           The {@code java.lang.Class#getModule()} method.
             * @param getClassLoader      The {@code java.lang.Module#getClassLoader()} method.
             * @param isNamed             The {@code java.lang.Module#isNamed()} method.
             * @param getName             The {@code java.lang.Module#getName()} method.
             * @param getResourceAsStream The {@code java.lang.Module#getResourceAsStream(String)} method.
             * @param canRead             The {@code java.lang.Module#canRead(Module)} method.
             * @param isModifiableModule  The {@code java.lang.instrument.Instrumentation#isModifiableModule} method.
             * @param redefineModule      The {@code java.lang.instrument.Instrumentation#redefineModule} method.
             */
            protected Enabled(Method getModule,
                              Method getClassLoader,
                              Method isNamed,
                              Method getName,
                              Method getResourceAsStream,
                              Method canRead,
                              Method isModifiableModule,
                              Method redefineModule) {
                this.getModule = getModule;
                this.getClassLoader = getClassLoader;
                this.isNamed = isNamed;
                this.getName = getName;
                this.getResourceAsStream = getResourceAsStream;
                this.canRead = canRead;
                this.isModifiableModule = isModifiableModule;
                this.redefineModule = redefineModule;
            }

            /**
             * {@inheritDoc}
             */
            public boolean isAlive() {
                return true;
            }

            /**
             * {@inheritDoc}
             */
            public JavaModule moduleOf(Class<?> type) {
                try {
                    return new JavaModule(getModule.invoke(type, NO_ARGUMENTS));
                } catch (IllegalAccessException exception) {
                    throw new IllegalStateException("Cannot access " + getModule, exception);
                } catch (InvocationTargetException exception) {
                    throw new IllegalStateException("Cannot invoke " + getModule, exception.getCause());
                }
            }

            /**
             * {@inheritDoc}
             */
            public InputStream getResourceAsStream(Object module, String name) {
                try {
                    return (InputStream) getResourceAsStream.invoke(module, name);
                } catch (IllegalAccessException exception) {
                    throw new IllegalStateException("Cannot access " + getResourceAsStream, exception);
                } catch (InvocationTargetException exception) {
                    throw new IllegalStateException("Cannot invoke " + getResourceAsStream, exception.getCause());
                }
            }

            /**
             * {@inheritDoc}
             */
            public ClassLoader getClassLoader(Object module) {
                try {
                    return (ClassLoader) getClassLoader.invoke(module, NO_ARGUMENTS);
                } catch (IllegalAccessException exception) {
                    throw new IllegalStateException("Cannot access " + getClassLoader, exception);
                } catch (InvocationTargetException exception) {
                    throw new IllegalStateException("Cannot invoke " + getClassLoader, exception.getCause());
                }
            }

            /**
             * {@inheritDoc}
             */
            public boolean isNamed(Object module) {
                try {
                    return (Boolean) isNamed.invoke(module, NO_ARGUMENTS);
                } catch (IllegalAccessException exception) {
                    throw new IllegalStateException("Cannot access " + isNamed, exception);
                } catch (InvocationTargetException exception) {
                    throw new IllegalStateException("Cannot invoke " + isNamed, exception.getCause());
                }
            }

            /**
             * {@inheritDoc}
             */
            public String getName(Object module) {
                try {
                    return (String) getName.invoke(module, NO_ARGUMENTS);
                } catch (IllegalAccessException exception) {
                    throw new IllegalStateException("Cannot access " + getName, exception);
                } catch (InvocationTargetException exception) {
                    throw new IllegalStateException("Cannot invoke " + getName, exception.getCause());
                }
            }

            /**
             * {@inheritDoc}
             */
            public boolean canRead(Object source, Object target) {
                try {
                    return (Boolean) canRead.invoke(source, target);
                } catch (IllegalAccessException exception) {
                    throw new IllegalStateException("Cannot access " + canRead, exception);
                } catch (InvocationTargetException exception) {
                    throw new IllegalStateException("Cannot invoke " + canRead, exception.getCause());
                }
            }

            /**
             * {@inheritDoc}
             */
            public void addReads(Instrumentation instrumentation, Object source, Object target) {
                try {
                    if (!(Boolean) isModifiableModule.invoke(instrumentation, source)) {
                        throw new IllegalStateException(source + " is not modifiable");
                    }
                } catch (IllegalAccessException exception) {
                    throw new IllegalStateException("Cannot access " + redefineModule, exception);
                } catch (InvocationTargetException exception) {
                    throw new IllegalStateException("Cannot invoke " + redefineModule, exception.getCause());
                }
                try {
                    redefineModule.invoke(instrumentation, source,
                            Collections.singleton(target),
                            Collections.emptyMap(),
                            Collections.emptyMap(),
                            Collections.emptySet(),
                            Collections.emptyMap());
                } catch (IllegalAccessException exception) {
                    throw new IllegalStateException("Cannot access " + redefineModule, exception);
                } catch (InvocationTargetException exception) {
                    throw new IllegalStateException("Cannot invoke " + redefineModule, exception.getCause());
                }
            }
        }

        /**
         * A disabled dispatcher for a VM that does not support the {@code java.lang.Module} API.
         */
        enum Disabled implements Dispatcher {

            /**
             * The singleton instance.
             */
            INSTANCE;

            /**
             * {@inheritDoc}
             */
            public boolean isAlive() {
                return false;
            }

            /**
             * {@inheritDoc}
             */
            public JavaModule moduleOf(Class<?> type) {
                return UNSUPPORTED;
            }

            /**
             * {@inheritDoc}
             */
            public ClassLoader getClassLoader(Object module) {
                throw new UnsupportedOperationException("Current VM does not support modules");
            }

            /**
             * {@inheritDoc}
             */
            public boolean isNamed(Object module) {
                throw new UnsupportedOperationException("Current VM does not support modules");
            }

            /**
             * {@inheritDoc}
             */
            public String getName(Object module) {
                throw new UnsupportedOperationException("Current VM does not support modules");
            }

            /**
             * {@inheritDoc}
             */
            public InputStream getResourceAsStream(Object module, String name) {
                throw new UnsupportedOperationException("Current VM does not support modules");
            }

            /**
             * {@inheritDoc}
             */
            public boolean canRead(Object source, Object target) {
                throw new UnsupportedOperationException("Current VM does not support modules");
            }

            /**
             * {@inheritDoc}
             */
            public void addReads(Instrumentation instrumentation, Object source, Object target) {
                throw new UnsupportedOperationException("Current VM does not support modules");
            }
        }
    }
}
