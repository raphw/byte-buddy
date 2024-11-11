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
package net.bytebuddy.build.gradle;

import net.bytebuddy.utility.nullability.AlwaysNull;
import net.bytebuddy.utility.nullability.MaybeNull;
import org.gradle.api.Project;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * A object factory for Gradle that uses the {@code org.gradle.api.model.ObjectFactory} API if available.
 */
public class ObjectFactory {

    /**
     * The dispatcher to use.
     */
    private static final Dispatcher DISPATCHER;

    /*
     * Resolves the dispatcher for the current Gradle version.
     */
    static {
        Dispatcher dispatcher;
        try {
            dispatcher = new Dispatcher.ForApi4CapableGradle(Project.class.getMethod("getObjects"),
                    Class.forName("org.gradle.api.model.ObjectFactory").getMethod("newInstance", Class.class, Object[].class));
        } catch (Throwable ignored) {
            dispatcher = Dispatcher.ForLegacyGradle.INSTANCE;
        }
        DISPATCHER = dispatcher;
    }

    /**
     * A private constructor that is not supposed to be invoked.
     */
    private ObjectFactory() {
        throw new UnsupportedOperationException("This class is a utility class and not supposed to be instantiated");
    }

    /**
     * Returns a new instance of the supplied class by invoking a constructor or returns {@code null} if the feature
     * is not available.
     *
     * @param project  The Gradle project to use.
     * @param type     The type of the class to be instantiated.
     * @param argument The arguments to supply.
     * @param <T>      The type of the created class.
     * @return The created instance or {@code null} if the feature is not available.
     */
    @SuppressWarnings("unchecked")
    @MaybeNull
    public static <T> T newInstance(Project project, Class<T> type, Object... argument) {
        return (T) DISPATCHER.newInstance(project, type, argument);
    }

    /**
     * A dispatcher for using the {@code org.gradle.api.model.ObjectFactory} if available.
     */
    protected interface Dispatcher {

        /**
         * Returns a new instance of the supplied class by invoking a constructor or returns {@code null} if the feature
         * is not available.
         *
         * @param project  The Gradle project to use.
         * @param type     The type of the class to be instantiated.
         * @param argument The arguments to supply.
         * @return The created instance or {@code null} if the feature is not available.
         */
        @MaybeNull
        Object newInstance(Project project, Class<?> type, Object... argument);

        /**
         * A dispatcher for a legacy version of Gradle that does not support the object factory API.
         */
        enum ForLegacyGradle implements Dispatcher {

            /**
             * The singleton instance.
             */
            INSTANCE;

            /**
             * {@inheritDoc}
             */
            @AlwaysNull
            public Object newInstance(Project project, Class<?> type, Object... argument) {
                return null;
            }
        }

        /**
         * A dispatcher for a Gradle version that supports the object factory API.
         */
        class ForApi4CapableGradle implements Dispatcher {

            /**
             * The {@code org.gradle.api.Project#getObjects()} method.
             */
            private final Method getObjects;

            /**
             * The {@code org.gradle.api.model.ObjectFactory#newInstance} method.
             */
            private final Method newInstance;

            /**
             * Creates a new dispatcher.
             *
             * @param getObjects  The {@code org.gradle.api.Project#getObjects()} method.
             * @param newInstance The {@code org.gradle.api.model.ObjectFactory#newInstance} method.
             */
            protected ForApi4CapableGradle(Method getObjects, Method newInstance) {
                this.getObjects = getObjects;
                this.newInstance = newInstance;
            }

            /**
             * {@inheritDoc}
             */
            public Object newInstance(Project project, Class<?> type, Object... argument) {
                try {
                    return newInstance.invoke(getObjects.invoke(project), type, argument);
                } catch (IllegalAccessException exception) {
                    throw new IllegalStateException(exception);
                } catch (InvocationTargetException exception) {
                    Throwable cause = exception.getCause();
                    if (cause instanceof RuntimeException) {
                        throw (RuntimeException) cause;
                    } else if (cause instanceof Error) {
                        throw (Error) cause;
                    } else {
                        throw new RuntimeException(exception);
                    }
                }
            }
        }
    }
}
