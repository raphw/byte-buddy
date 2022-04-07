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
package net.bytebuddy.implementation;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.build.AccessControllerPlugin;
import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.utility.JavaModule;
import net.bytebuddy.utility.nullability.MaybeNull;
import net.bytebuddy.utility.privilege.SetAccessibleAction;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Implementations of this interface explicitly initialize a loaded type. Usually, such implementations inject runtime
 * context into an instrumented type which cannot be defined by the means of the Java class file format.
 */
public interface LoadedTypeInitializer {

    /**
     * Callback that is invoked on the creation of an instrumented type. If the loaded type initializer is alive, this
     * method should be implemented empty instead of throwing an exception.
     *
     * @param type The manifestation of the instrumented type.
     */
    void onLoad(Class<?> type);

    /**
     * Indicates if this initializer is alive and needs to be invoked. This is only meant as a mark. A loaded type
     * initializer that is not alive might still be called and must therefore not throw an exception but rather
     * provide an empty implementation.
     *
     * @return {@code true} if this initializer is alive.
     */
    boolean isAlive();

    /**
     * A loaded type initializer that does not do anything.
     */
    enum NoOp implements LoadedTypeInitializer {

        /**
         * The singleton instance.
         */
        INSTANCE;

        /**
         * {@inheritDoc}
         */
        public void onLoad(Class<?> type) {
            /* do nothing */
        }

        /**
         * {@inheritDoc}
         */
        public boolean isAlive() {
            return false;
        }
    }

    /**
     * A type initializer for setting a value for a static field.
     */
    @HashCodeAndEqualsPlugin.Enhance
    class ForStaticField implements LoadedTypeInitializer, Serializable {

        /**
         * This class's serial version UID.
         */
        private static final long serialVersionUID = 1L;

        /**
         * The name of the field.
         */
        private final String fieldName;

        /**
         * The value of the field.
         */
        @SuppressWarnings("serial")
        private final Object value;

        /**
         * The access control context to use for loading classes or {@code null} if the
         * access controller is not available on the current VM.
         */
        @MaybeNull
        @HashCodeAndEqualsPlugin.ValueHandling(HashCodeAndEqualsPlugin.ValueHandling.Sort.IGNORE)
        private final transient Object accessControlContext;

        /**
         * Creates a new {@link LoadedTypeInitializer} for setting a static field.
         *
         * @param fieldName the name of the field.
         * @param value     The value to be set.
         */
        public ForStaticField(String fieldName, Object value) {
            this.fieldName = fieldName;
            this.value = value;
            accessControlContext = getContext();
        }

        /**
         * A proxy for {@code java.security.AccessController#getContext} that is activated if available.
         *
         * @return The current access control context or {@code null} if the current VM does not support it.
         */
        @MaybeNull
        @AccessControllerPlugin.Enhance
        private static Object getContext() {
            return null;
        }

        /**
         * A proxy for {@code java.security.AccessController#doPrivileged} that is activated if available.
         *
         * @param action  The action to execute from a privileged context.
         * @param context The access control context or {@code null} if the current VM does not support it.
         * @param <T>     The type of the action's resolved value.
         * @return The action's resolved value.
         */
        @AccessControllerPlugin.Enhance
        private static <T> T doPrivileged(PrivilegedAction<T> action, @MaybeNull @SuppressWarnings("unused") Object context) {
            return action.run();
        }

        /**
         * Resolves this instance after deserialization to assure the access control context is set.
         *
         * @return A resolved instance of this instance that includes an appropriate access control context.
         */
        private Object readResolve() {
            return new ForStaticField(fieldName, value);
        }

        /**
         * {@inheritDoc}
         */
        @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "Modules are assumed available when module system is supported")
        public void onLoad(Class<?> type) {
            try {
                Field field = type.getDeclaredField(fieldName);
                if (!Modifier.isPublic(field.getModifiers())
                        || !Modifier.isPublic(field.getDeclaringClass().getModifiers())
                        || JavaModule.isSupported()
                        && !JavaModule.ofType(type).isExported(TypeDescription.ForLoadedType.of(type).getPackage(), JavaModule.ofType(ForStaticField.class))) {
                    doPrivileged(new SetAccessibleAction<Field>(field), accessControlContext);
                }
                field.set(null, value);
            } catch (IllegalAccessException exception) {
                throw new IllegalArgumentException("Cannot access " + fieldName + " from " + type, exception);
            } catch (NoSuchFieldException exception) {
                throw new IllegalStateException("There is no field " + fieldName + " defined on " + type, exception);
            }
        }

        /**
         * {@inheritDoc}
         */
        public boolean isAlive() {
            return true;
        }
    }

    /**
     * A compound loaded type initializer that combines several type initializers.
     */
    @SuppressFBWarnings(value = "SE_BAD_FIELD", justification = "Serialization is considered opt-in for a rare use case")
    @HashCodeAndEqualsPlugin.Enhance
    class Compound implements LoadedTypeInitializer, Serializable {

        /**
         * This class's serial version UID.
         */
        private static final long serialVersionUID = 1L;

        /**
         * The loaded type initializers that are represented by this compound type initializer.
         */
        @SuppressWarnings("serial")
        private final List<LoadedTypeInitializer> loadedTypeInitializers;

        /**
         * Creates a new compound loaded type initializer.
         *
         * @param loadedTypeInitializer A number of loaded type initializers in their invocation order.
         */
        public Compound(LoadedTypeInitializer... loadedTypeInitializer) {
            this(Arrays.asList(loadedTypeInitializer));
        }

        /**
         * Creates a new compound loaded type initializer.
         *
         * @param loadedTypeInitializers A number of loaded type initializers in their invocation order.
         */
        public Compound(List<? extends LoadedTypeInitializer> loadedTypeInitializers) {
            this.loadedTypeInitializers = new ArrayList<LoadedTypeInitializer>();
            for (LoadedTypeInitializer loadedTypeInitializer : loadedTypeInitializers) {
                if (loadedTypeInitializer instanceof Compound) {
                    this.loadedTypeInitializers.addAll(((Compound) loadedTypeInitializer).loadedTypeInitializers);
                } else if (!(loadedTypeInitializer instanceof NoOp)) {
                    this.loadedTypeInitializers.add(loadedTypeInitializer);
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        public void onLoad(Class<?> type) {
            for (LoadedTypeInitializer loadedTypeInitializer : loadedTypeInitializers) {
                loadedTypeInitializer.onLoad(type);
            }
        }

        /**
         * {@inheritDoc}
         */
        public boolean isAlive() {
            for (LoadedTypeInitializer loadedTypeInitializer : loadedTypeInitializers) {
                if (loadedTypeInitializer.isAlive()) {
                    return true;
                }
            }
            return false;
        }
    }
}
