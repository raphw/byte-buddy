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

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.NamedElement;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.annotation.AnnotationSource;
import net.bytebuddy.description.type.PackageDescription;

import java.io.InputStream;
import java.lang.reflect.AnnotatedElement;
import java.security.AccessController;

/**
 * Type-safe representation of a {@code java.lang.Module}. On platforms that do not support the module API, modules are represented by {@code null}.
 */
public class JavaModule implements NamedElement.WithOptionalName, AnnotationSource {

    /**
     * Canonical representation of a Java module on a JVM that does not support the module API.
     */
    public static final JavaModule UNSUPPORTED = null;

    /**
     * A dispatcher to resolve a {@link Class}'s {@code java.lang.Module}.
     */
    protected static final Resolver RESOLVER = AccessController.doPrivileged(JavaDispatcher.of(Resolver.class));

    /**
     * A dispatcher to interact with {@code java.lang.Module}.
     */
    protected static final Module MODULE = AccessController.doPrivileged(JavaDispatcher.of(Module.class));

    /**
     * The {@code java.lang.Module} instance this wrapper represents.
     */
    private final AnnotatedElement module;

    /**
     * Creates a new Java module representation.
     *
     * @param module The {@code java.lang.Module} instance this wrapper represents.
     */
    protected JavaModule(AnnotatedElement module) {
        this.module = module;
    }

    /**
     * Returns a representation of the supplied type's {@code java.lang.Module} or {@code null} if the current VM does not support modules.
     *
     * @param type The type for which to describe the module.
     * @return A representation of the type's module or {@code null} if the current VM does not support modules.
     */
    public static JavaModule ofType(Class<?> type) {
        Object module = RESOLVER.getModule(type);
        return module == null
                ? UNSUPPORTED
                : new JavaModule((AnnotatedElement) module);
    }

    /**
     * Represents the supplied {@code java.lang.Module} as an instance of this class and validates that the
     * supplied instance really represents a Java {@code Module}.
     *
     * @param module The module to represent.
     * @return A representation of the supplied Java module.
     */
    public static JavaModule of(Object module) {
        if (!MODULE.isInstance(module)) {
            throw new IllegalArgumentException("Not a Java module: " + module);
        }
        return new JavaModule((AnnotatedElement) module);
    }

    /**
     * Checks if the current VM supports the {@code java.lang.Module} API.
     *
     * @return {@code true} if the current VM supports modules.
     */
    public static boolean isSupported() {
        return ClassFileVersion.ofThisVm().isAtLeast(ClassFileVersion.JAVA_V9);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isNamed() {
        return MODULE.isNamed(module);
    }

    /**
     * {@inheritDoc}
     */
    public String getActualName() {
        return MODULE.getName(module);
    }

    /**
     * Returns a resource stream for this module for a resource of the given name or {@code null} if such a resource does not exist.
     *
     * @param name The name of the resource.
     * @return An input stream for the resource or {@code null} if it does not exist.
     */
    public InputStream getResourceAsStream(String name) {
        return MODULE.getResourceAsStream(module, name);
    }

    /**
     * Returns the class loader of this module.
     *
     * @return The class loader of the represented module.
     */
    public ClassLoader getClassLoader() {
        return MODULE.getClassLoader(module);
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
        return MODULE.canRead(this.module, module.unwrap());
    }

    /**
     * Returns {@code true} if this module exports the supplied package to this module.
     *
     * @param packageDescription The package to check for
     * @param module             The target module.
     * @return {@code true} if this module exports the supplied package to this module.
     */
    public boolean isExported(PackageDescription packageDescription, JavaModule module) {
        return packageDescription == null || MODULE.isExported(this.module, module.unwrap(), packageDescription.getName());
    }

    /**
     * Returns {@code true} if this module opens the supplied package to this module.
     *
     * @param packageDescription The package to check for.
     * @param module             The target module.
     * @return {@code true} if this module opens the supplied package to this module.
     */
    public boolean isOpened(PackageDescription packageDescription, JavaModule module) {
        return packageDescription == null || MODULE.isOpened(this.module, module.unwrap(), packageDescription.getName());
    }

    /**
     * {@inheritDoc}
     */
    public AnnotationList getDeclaredAnnotations() {
        return new AnnotationList.ForLoadedAnnotations(module.getDeclaredAnnotations());
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
     * A proxy for resolving a {@link Class}'s {@code java.lang.Module}.
     */
    @JavaDispatcher.Proxied("java.lang.Class")
    protected interface Resolver {

        /**
         * Resolves the {@code java.lang.Module} of the supplied type.
         *
         * @param type The type for which to resolve the module.
         * @return The type's module or {@code null} if the module system is not supported.
         */
        @JavaDispatcher.Static
        @JavaDispatcher.Defaults
        Object getModule(Class<?> type);
    }

    /**
     * A proxy for interacting with {@code java.lang.Module}.
     */
    @JavaDispatcher.Proxied("java.lang.Module")
    protected interface Module {

        /**
         * Returns {@code true} if the supplied instance is of type {@code java.lang.Module}.
         *
         * @param value The instance to investigate.
         * @return {@code true} if the supplied value is a {@code java.lang.Module}.
         */
        @JavaDispatcher.Instance
        boolean isInstance(Object value);

        /**
         * Returns {@code true} if the supplied module is named.
         *
         * @param value The {@code java.lang.Module} to check for the existence of a name.
         * @return {@code true} if the supplied module is named.
         */
        boolean isNamed(Object value);

        /**
         * Returns the module's name.
         *
         * @param value The {@code java.lang.Module} to check for its name.
         * @return The module's (implicit or explicit) name.
         */
        String getName(Object value);

        /**
         * Returns a resource stream for this module for a resource of the given name or {@code null} if such a resource does not exist.
         *
         * @param value The {@code java.lang.Module} instance to apply this method upon.
         * @param name  The name of the resource.
         * @return An input stream for the resource or {@code null} if it does not exist.
         */
        InputStream getResourceAsStream(Object value, String name);

        /**
         * Returns the module's class loader.
         *
         * @param value The {@code java.lang.Module}
         * @return The module's class loader.
         */
        ClassLoader getClassLoader(Object value);

        /**
         * Returns {@code true} if the source module exports the supplied package to the target module.
         *
         * @param value    The source module.
         * @param target   The target module.
         * @param aPackage The name of the package to check.
         * @return {@code true} if the source module exports the supplied package to the target module.
         */
        boolean isExported(Object value, @JavaDispatcher.Proxied("java.lang.Module") Object target, String aPackage);

        /**
         * Returns {@code true} if the source module opens the supplied package to the target module.
         *
         * @param value    The source module.
         * @param target   The target module.
         * @param aPackage The name of the package to check.
         * @return {@code true} if the source module opens the supplied package to the target module.
         */
        boolean isOpened(Object value, @JavaDispatcher.Proxied("java.lang.Module") Object target, String aPackage);

        /**
         * Checks if the source module can read the target module.
         *
         * @param value  The source module.
         * @param target The target module.
         * @return {@code true} if the source module can read the target module.
         */
        boolean canRead(Object value, @JavaDispatcher.Proxied("java.lang.Module") Object target);
    }
}
