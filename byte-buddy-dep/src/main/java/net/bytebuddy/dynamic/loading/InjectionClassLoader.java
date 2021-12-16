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
package net.bytebuddy.dynamic.loading;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.utility.nullability.MaybeNull;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <p>
 * An injection class loader allows for the injection of a class after the class loader was created. Injection is only possible if this class loader is not sealed.
 * </p>
 * <p>
 * <b>Important</b>: Not sealing a class loader allows to break package encapsulation for anybody getting hold of a reference to this class loader.
 * </p>
 */
public abstract class InjectionClassLoader extends ClassLoader {

    /*
     * Register class loader as parallel capable if the current VM supports it.
     */
    static {
        doRegisterAsParallelCapable();
    }

    /**
     * Registers class loader as parallel capable if possible.
     */
    @SuppressFBWarnings(value = "DP_DO_INSIDE_DO_PRIVILEGED", justification = "Must be invoked from targeting class loader type.")
    private static void doRegisterAsParallelCapable() {
        try {
            Method method = ClassLoader.class.getDeclaredMethod("registerAsParallelCapable");
            method.setAccessible(true);
            method.invoke(null);
        } catch (Throwable ignored) {
            /* do nothing */
        }
    }

    /**
     * Indicates if this class loader is sealed, i.e. forbids runtime injection.
     */
    private final AtomicBoolean sealed;

    /**
     * Creates a new injection class loader.
     *
     * @param parent The class loader's parent.
     * @param sealed Indicates if this class loader is sealed, i.e. forbids runtime injection.
     */
    protected InjectionClassLoader(@MaybeNull ClassLoader parent, boolean sealed) {
        super(parent);
        this.sealed = new AtomicBoolean(sealed);
    }

    /**
     * Returns {@code true} if this class loader is sealed.
     *
     * @return {@code true} if this class loader is sealed.
     */
    public boolean isSealed() {
        return sealed.get();
    }

    /**
     * Seals the class loader and returns {@code true} if the class loader was not sealed previously.
     *
     * @return {@code true} if the class loader was not sealed previously.
     */
    public boolean seal() {
        return !sealed.getAndSet(true);
    }

    /**
     * Defines a new type to be loaded by this class loader.
     *
     * @param name                 The name of the type.
     * @param binaryRepresentation The type's binary representation.
     * @return The defined class or a previously defined class.
     * @throws ClassNotFoundException If the class could not be loaded.
     */
    public Class<?> defineClass(String name, byte[] binaryRepresentation) throws ClassNotFoundException {
        return defineClasses(Collections.singletonMap(name, binaryRepresentation)).get(name);
    }

    /**
     * Defines a group of types to be loaded by this class loader. If this class loader is sealed, an {@link IllegalStateException} is thrown.
     *
     * @param typeDefinitions The types binary representations.
     * @return The mapping of defined classes or previously defined classes by their name.
     * @throws ClassNotFoundException If the class could not be loaded.
     */
    public Map<String, Class<?>> defineClasses(Map<String, byte[]> typeDefinitions) throws ClassNotFoundException {
        if (sealed.get()) {
            throw new IllegalStateException("Cannot inject classes into a sealed class loader");
        }
        return doDefineClasses(typeDefinitions);
    }

    /**
     * Defines a group of types to be loaded by this class loader.
     *
     * @param typeDefinitions The types binary representations.
     * @return The mapping of defined classes or previously defined classes by their name.
     * @throws ClassNotFoundException If the class could not be loaded.
     */
    protected abstract Map<String, Class<?>> doDefineClasses(Map<String, byte[]> typeDefinitions) throws ClassNotFoundException;

    /**
     * A class loading strategy for adding a type to an injection class loader.
     */
    public enum Strategy implements ClassLoadingStrategy<InjectionClassLoader> {

        /**
         * The singleton instance.
         */
        INSTANCE;

        /**
         * {@inheritDoc}
         */
        public Map<TypeDescription, Class<?>> load(@MaybeNull InjectionClassLoader classLoader, Map<TypeDescription, byte[]> types) {
            if (classLoader == null) {
                throw new IllegalArgumentException("Cannot add types to bootstrap class loader: " + types);
            }
            Map<String, byte[]> typeDefinitions = new LinkedHashMap<String, byte[]>();
            Map<String, TypeDescription> typeDescriptions = new HashMap<String, TypeDescription>();
            for (Map.Entry<TypeDescription, byte[]> entry : types.entrySet()) {
                typeDefinitions.put(entry.getKey().getName(), entry.getValue());
                typeDescriptions.put(entry.getKey().getName(), entry.getKey());
            }
            Map<TypeDescription, Class<?>> loadedTypes = new HashMap<TypeDescription, Class<?>>();
            try {
                for (Map.Entry<String, Class<?>> entry : classLoader.defineClasses(typeDefinitions).entrySet()) {
                    loadedTypes.put(typeDescriptions.get(entry.getKey()), entry.getValue());
                }
            } catch (ClassNotFoundException exception) {
                throw new IllegalStateException("Cannot load classes: " + types, exception);
            }
            return loadedTypes;
        }
    }
}
