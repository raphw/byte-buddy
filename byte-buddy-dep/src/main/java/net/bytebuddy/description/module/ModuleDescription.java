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
package net.bytebuddy.description.module;

import net.bytebuddy.build.AccessControllerPlugin;
import net.bytebuddy.description.ModifierReviewable;
import net.bytebuddy.description.NamedElement;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.annotation.AnnotationSource;
import net.bytebuddy.utility.dispatcher.JavaDispatcher;
import net.bytebuddy.utility.nullability.AlwaysNull;
import net.bytebuddy.utility.nullability.MaybeNull;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.AnnotatedElement;
import java.security.PrivilegedAction;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Description of a named Java {@code java.lang.Module}.
 */
public interface ModuleDescription extends NamedElement,
        ModifierReviewable.ForModuleDescription,
        AnnotationSource {

    /**
     * Defines a module that is not resolved.
     */
    @AlwaysNull
    ModuleDescription UNDEFINED = null;

    /**
     * Returns the version of this module.
     *
     * @return The module's version or {@code null} if no version is specified.
     */
    @MaybeNull
    String getVersion();

    /**
     * Returns the main class of this module.
     *
     * @return The module's main class or {@code null} if no main class is specified.
     */
    @MaybeNull
    String getMainClass();

    /**
     * Returns all packages contained in this module.
     *
     * @return A set of all package names within this module.
     */
    Set<String> getPackages();

    /**
     * Returns all package exports of this module.
     *
     * @return A mapping of package names to their export declarations.
     */
    Map<String, Exports> getExports();

    /**
     * Returns all package opens of this module.
     *
     * @return A mapping of package names to their opens declarations.
     */
    Map<String, Opens> getOpens();

    /**
     * Returns all module dependencies of this module.
     *
     * @return A mapping of module names to their require declarations.
     */
    Map<String, Requires> getRequires();

    /**
     * Returns all service types that this module uses.
     *
     * @return A set of service class names that this module uses.
     */
    Set<String> getUses();

    /**
     * Returns all service implementations provided by this module.
     *
     * @return A mapping of service names to their provider declarations.
     */
    Map<String, Provides> getProvides();

    /**
     * Represents an exported package declaration in a module. Exports control which packages
     * are accessible to other modules.
     */
    interface Exports extends ModifierReviewable.OfMandatable {

        /**
         * Returns the target modules that this package is exported to.
         *
         * @return A set of module names that can access this exported package, or an empty set if exported to all modules.
         */
        Set<String> getTargets();

        /**
         * Determines if this export is qualified (exported to specific modules only).
         *
         * @return {@code true} if this export has specific target modules, {@code false} if exported to all modules.
         */
        boolean isQualified();

        /**
         * An abstract base implementation of {@link Exports} that provides a default implementation
         * for {@link #isQualified()}.
         */
        abstract class AbstractBase extends ModifierReviewable.AbstractBase implements Exports {

            /**
             * {@inheritDoc}
             */
            public boolean isQualified() {
                return !getTargets().isEmpty();
            }

            @Override
            public int hashCode() {
                int hashCode = getModifiers();
                return hashCode + 17 * getTargets().hashCode();
            }

            @Override
            public boolean equals(Object other) {
                if (!(other instanceof Exports)) return false;
                Exports exports = (Exports) other;
                return getModifiers() == exports.getModifiers() && getTargets().equals(exports.getTargets());
            }

            @Override
            public String toString() {
                return "Opens{"
                        + "targets=" + getTargets()
                        + ",modifiers=" + getModifiers()
                        + '}';
            }
        }

        /**
         * A simple implementation of {@link Exports} that stores the target modules and modifiers.
         */
        class Simple extends AbstractBase {

            /**
             * The target modules for this export.
             */
            private final Set<String> targets;

            /**
             * The modifiers for this export.
             */
            protected final int modifiers;

            /**
             * Creates a new simple export declaration.
             *
             * @param targets   The target modules for this export.
             * @param modifiers The modifiers for this export.
             */
            public Simple(Set<String> targets, int modifiers) {
                this.targets = targets;
                this.modifiers = modifiers;
            }

            /**
             * {@inheritDoc}
             */
            public Set<String> getTargets() {
                return targets;
            }

            /**
             * {@inheritDoc}
             */
            public int getModifiers() {
                return modifiers;
            }
        }
    }

    /**
     * Represents an opened package declaration in a module. Opens allow deep reflective access
     * to packages for other modules.
     */
    interface Opens extends ModifierReviewable.OfMandatable {

        /**
         * Returns the target modules that this package is opened to.
         *
         * @return A set of module names that can reflectively access this opened package, or an empty set if opened to all modules.
         */
        Set<String> getTargets();

        /**
         * Determines if this opens declaration is qualified (opened to specific modules only).
         *
         * @return {@code true} if this opens has specific target modules, {@code false} if opened to all modules.
         */
        boolean isQualified();

        /**
         * An abstract base implementation of {@link Opens}.
         */
        abstract class AbstractBase extends ModifierReviewable.AbstractBase implements Opens {

            /**
             * {@inheritDoc}
             */
            public boolean isQualified() {
                return !getTargets().isEmpty();
            }

            @Override
            public int hashCode() {
                int hashCode = getModifiers();
                return hashCode + 17 * getTargets().hashCode();
            }

            @Override
            public boolean equals(Object other) {
                if (!(other instanceof Opens)) return false;
                Opens opens = (Opens) other;
                return getModifiers() == opens.getModifiers() && getTargets().equals(opens.getTargets());
            }

            @Override
            public String toString() {
                return "Opens{"
                        + "targets=" + getTargets()
                        + ",modifiers=" + getModifiers()
                        + '}';
            }
        }

        /**
         * A simple implementation of {@link Opens}.
         */
        class Simple extends AbstractBase {

            /**
             * The target modules for this opens declaration.
             */
            private final Set<String> targets;

            /**
             * The modifiers for this opens declaration.
             */
            protected final int modifiers;

            /**
             * Creates a new simple opens declaration.
             *
             * @param targets   The target modules for this opens declaration.
             * @param modifiers The modifiers for this opens declaration.
             */
            public Simple(Set<String> targets, int modifiers) {
                this.targets = targets;
                this.modifiers = modifiers;
            }

            /**
             * {@inheritDoc}
             */
            public Set<String> getTargets() {
                return targets;
            }

            /**
             * {@inheritDoc}
             */
            public int getModifiers() {
                return modifiers;
            }
        }
    }

    /**
     * Represents a module dependency declaration. Requires specify which modules this module
     * depends on for compilation and runtime.
     */
    interface Requires extends ModifierReviewable.ForModuleRequirement {

        /**
         * Returns the version of the required module.
         *
         * @return The required module's version or {@code null} if no specific version is required.
         */
        @MaybeNull
        String getVersion();

        /**
         * An abstract base implementation of {@link Requires}.
         */
        abstract class AbstractBase extends ModifierReviewable.AbstractBase implements Requires {

            @Override
            public int hashCode() {
                int hashCode = getModifiers();
                String version = getVersion();
                return version == null ? hashCode : (hashCode + 17 * version.hashCode());
            }

            @Override
            public boolean equals(Object other) {
                if (!(other instanceof Requires)) return false;
                Requires requires = (Requires) other;
                String version = getVersion();
                return getModifiers() == requires.getModifiers() && version == null ? requires.getVersion() == null : version.equals(requires.getVersion());
            }

            @Override
            public String toString() {
                String version = getVersion();
                return "Requires{"
                        + "version=" + (version == null ? "" : '"' + version + '\'')
                        + ",modifiers=" + getModifiers()
                        + '}';
            }
        }

        /**
         * A simple implementation of {@link Requires}.
         */
        class Simple extends AbstractBase {

            /**
             * The version of the required module.
             */
            @MaybeNull
            private final String version;

            /**
             * The modifiers for this requires declaration.
             */
            private final int modifiers;

            /**
             * Creates a new simple requires declaration.
             *
             * @param version   The version of the required module or {@code null} if no specific version is required.
             * @param modifiers The modifiers for this requires declaration.
             */
            public Simple(@MaybeNull String version, int modifiers) {
                this.version = version;
                this.modifiers = modifiers;
            }

            /**
             * {@inheritDoc}
             */
            @MaybeNull
            public String getVersion() {
                return version;
            }

            /**
             * {@inheritDoc}
             */
            public int getModifiers() {
                return modifiers;
            }
        }
    }

    /**
     * Represents a service provider declaration in a module. Provides specify which service
     * implementations this module offers to other modules.
     */
    interface Provides {

        /**
         * Returns the implementation classes that provide the service.
         *
         * @return A set of class names that implement the service.
         */
        Set<String> getProviders();

        /**
         * An abstract base implementation of {@link Provides}.
         */
        abstract class AbstractBase implements Provides {

            @Override
            public int hashCode() {
                return getProviders().hashCode();
            }

            @Override
            public boolean equals(Object other) {
                if (!(other instanceof Provides)) return false;
                Provides provides = (Provides) other;
                return getProviders().equals(provides.getProviders());
            }

            @Override
            public String toString() {
                return "Provides{providers=" + getProviders() + '}';
            }
        }

        /**
         * A simple implementation of {@link Provides}.
         */
        class Simple extends AbstractBase {

            /**
             * The implementation classes that provide the service.
             */
            private final Set<String> providers;

            /**
             * Creates a new simple provides declaration.
             *
             * @param providers The implementation classes that provide the service.
             */
            public Simple(Set<String> providers) {
                this.providers = providers;
            }

            /**
             * {@inheritDoc}
             */
            public Set<String> getProviders() {
                return providers;
            }
        }
    }

    /**
     * An abstract base implementation of a {@link ModuleDescription}.
     */
    abstract class AbstractBase extends ModifierReviewable.AbstractBase implements ModuleDescription {

        @Override
        public int hashCode() {
            return 17 * getActualName().hashCode();
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof ModuleDescription)) return false;
            ModuleDescription module = (ModuleDescription) other;
            return getActualName().equals(module.getActualName());
        }

        @Override
        public String toString() {
            return "module " + getActualName();
        }
    }

    /**
     * A {@link ModuleDescription} implementation that represents a loaded Java module.
     * This implementation uses reflection and Java dispatchers to access module information
     * from the runtime module system.
     */
    class ForLoadedModule extends AbstractBase {

        /**
         * A dispatcher for accessing {@code java.lang.Module} methods.
         */
        protected static final Module MODULE = doPrivileged(JavaDispatcher.of(Module.class));

        /**
         * A dispatcher for accessing {@code java.lang.ModuleDescriptor} methods.
         */
        protected static final ModuleDescriptor MODULE_DESCRIPTOR = doPrivileged(JavaDispatcher.of(ModuleDescriptor.class));

        /**
         * A dispatcher for accessing {@code java.lang.ModuleDescriptor.Exports} methods.
         */
        protected static final ModuleDescriptor.Exports MODULE_DESCRIPTOR_EXPORTS = doPrivileged(JavaDispatcher.of(ModuleDescriptor.Exports.class));

        /**
         * A dispatcher for accessing {@code java.lang.ModuleDescriptor.Opens} methods.
         */
        protected static final ModuleDescriptor.Opens MODULE_DESCRIPTOR_OPENS = doPrivileged(JavaDispatcher.of(ModuleDescriptor.Opens.class));

        /**
         * A dispatcher for accessing {@code java.lang.ModuleDescriptor.Requires} methods.
         */
        protected static final ModuleDescriptor.Requires MODULE_DESCRIPTOR_REQUIRES = doPrivileged(JavaDispatcher.of(ModuleDescriptor.Requires.class));

        /**
         * A dispatcher for accessing {@code java.lang.ModuleDescriptor.Provides} methods.
         */
        protected static final ModuleDescriptor.Provides MODULE_DESCRIPTOR_PROVIDES = doPrivileged(JavaDispatcher.of(ModuleDescriptor.Provides.class));

        /**
         * A dispatcher for accessing {@code java.util.Optional} methods.
         */
        protected static final Optional OPTIONAL = doPrivileged(JavaDispatcher.of(Optional.class));

        /**
         * The module represented by this description.
         */
        private final AnnotatedElement module;

        /**
         * Creates a module description for the supplied module.
         *
         * @param module The module to represent.
         * @return A module description for the supplied module.
         * @throws IllegalArgumentException If the supplied instance is not a module or if the module is unnamed.
         */
        public static ForLoadedModule of(Object module) {
            if (!MODULE.isInstance(module)) {
                throw new IllegalArgumentException("Not a Java module: " + module);
            } else if (!MODULE.isNamed(module)) {
                throw new IllegalArgumentException("Not a named module: " + module);
            }
            return new ForLoadedModule((AnnotatedElement) module);
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
         * Creates a new module description for the supplied module.
         *
         * @param module The module to represent.
         */
        protected ForLoadedModule(AnnotatedElement module) {
            this.module = module;
        }

        /**
         * {@inheritDoc}
         */
        @MaybeNull
        public String getVersion() {
            return (String) OPTIONAL.orElse(MODULE_DESCRIPTOR.rawVersion(MODULE.getDescriptor(module)), null);
        }

        /**
         * {@inheritDoc}
         */
        @MaybeNull
        public String getMainClass() {
            return (String) OPTIONAL.orElse(MODULE_DESCRIPTOR.mainClass(MODULE.getDescriptor(module)), null);
        }

        /**
         * {@inheritDoc}
         */
        public boolean isOpen() {
            return MODULE_DESCRIPTOR.isOpen(MODULE.getDescriptor(module));
        }

        /**
         * {@inheritDoc}
         */
        public Set<String> getPackages() {
            return MODULE_DESCRIPTOR.packages(MODULE.getDescriptor(module));
        }

        /**
         * {@inheritDoc}
         */
        public Set<String> getUses() {
            return MODULE_DESCRIPTOR.uses(MODULE.getDescriptor(module));
        }

        /**
         * {@inheritDoc}
         */
        public Map<String, Exports> getExports() {
            Map<String, Exports> exports = new LinkedHashMap<String, Exports>();
            for (Object export : MODULE_DESCRIPTOR.exports(MODULE.getDescriptor(module))) {
                int modifiers = 0;
                for (Object modifier : MODULE_DESCRIPTOR_EXPORTS.modifiers(export)) {
                    String name = ((Enum<?>) modifier).name();
                    if (name.equals("SYNTHETIC")) {
                        modifiers |= Opcodes.ACC_SYNTHETIC;
                    } else if (name.equals("MANDATED")) {
                        modifiers |= Opcodes.ACC_MANDATED;
                    } else {
                        throw new IllegalStateException("Unknown export modifier: " + name);
                    }
                }
                exports.put(MODULE_DESCRIPTOR_EXPORTS.source(export), new Exports.Simple(MODULE_DESCRIPTOR_EXPORTS.targets(export), modifiers));
            }
            return exports;
        }

        /**
         * {@inheritDoc}
         */
        public Map<String, Opens> getOpens() {
            Map<String, Opens> opens = new LinkedHashMap<String, Opens>();
            for (Object open : MODULE_DESCRIPTOR.opens(MODULE.getDescriptor(module))) {
                int modifiers = 0;
                for (Object modifier : MODULE_DESCRIPTOR_OPENS.modifiers(open)) {
                    String name = ((Enum<?>) modifier).name();
                    if (name.equals("SYNTHETIC")) {
                        modifiers |= Opcodes.ACC_SYNTHETIC;
                    } else if (name.equals("MANDATED")) {
                        modifiers |= Opcodes.ACC_MANDATED;
                    } else {
                        throw new IllegalStateException("Unknown opens modifier: " + name);
                    }
                }
                opens.put(MODULE_DESCRIPTOR_OPENS.source(open), new Opens.Simple(MODULE_DESCRIPTOR_OPENS.targets(open), modifiers));
            }
            return opens;
        }

        /**
         * {@inheritDoc}
         */
        public Map<String, Requires> getRequires() {
            Map<String, Requires> requires = new LinkedHashMap<String, Requires>();
            for (Object require : MODULE_DESCRIPTOR.requires(MODULE.getDescriptor(module))) {
                int modifiers = 0;
                for (Object modifier : MODULE_DESCRIPTOR_REQUIRES.modifiers(require)) {
                    String name = ((Enum<?>) modifier).name();
                    if (name.equals("SYNTHETIC")) {
                        modifiers |= Opcodes.ACC_SYNTHETIC;
                    } else if (name.equals("MANDATED")) {
                        modifiers |= Opcodes.ACC_MANDATED;
                    } else if (name.equals("TRANSITIVE")) {
                        modifiers |= Opcodes.ACC_TRANSITIVE;
                    } else if (name.equals("STATIC")) {
                        modifiers |= Opcodes.ACC_STATIC_PHASE;
                    } else {
                        throw new IllegalStateException("Unknown requires modifier: " + name);
                    }
                }
                requires.put(MODULE_DESCRIPTOR_REQUIRES.name(require), new Requires.Simple(
                        (String) OPTIONAL.orElse(MODULE_DESCRIPTOR_REQUIRES.rawCompiledVersion(require), null),
                        modifiers));
            }
            return requires;
        }

        /**
         * {@inheritDoc}
         */
        public Map<String, Provides> getProvides() {
            Map<String, Provides> provides = new LinkedHashMap<String, Provides>();
            for (Object require : MODULE_DESCRIPTOR.provides(MODULE.getDescriptor(module))) {
                provides.put(MODULE_DESCRIPTOR_PROVIDES.service(require), new Provides.Simple(new LinkedHashSet<>(MODULE_DESCRIPTOR_PROVIDES.providers(require))));
            }
            return provides;
        }

        /**
         * {@inheritDoc}
         */
        public int getModifiers() {
            int modifiers = 0;
            for (Object modifier : MODULE_DESCRIPTOR.modifiers(module)) {
                String name = ((Enum<?>) modifier).name();
                if (name.equals("SYNTHETIC")) {
                    modifiers |= Opcodes.ACC_SYNTHETIC;
                } else if (name.equals("MANDATED")) {
                    modifiers |= Opcodes.ACC_MANDATED;
                } else if (name.equals("OPEN")) {
                    modifiers |= Opcodes.ACC_OPEN;
                } else {
                    throw new IllegalStateException("Unknown module modifier: " + name);
                }
            }
            return modifiers;
        }

        /**
         * {@inheritDoc}
         */
        public String getActualName() {
            return MODULE_DESCRIPTOR.name(MODULE.getDescriptor(module));
        }

        /**
         * {@inheritDoc}
         */
        public AnnotationList getDeclaredAnnotations() {
            return new AnnotationList.ForLoadedAnnotations(module.getDeclaredAnnotations());
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
             * Returns the module's exported packages.
             *
             * @param value The {@code java.lang.Module} to check for its packages.
             * @return The module's packages.
             */
            Set<String> getPackages(Object value);

            /**
             * Returns the module descriptor.
             * @param value The {@code java.lang.Module} to check for its descriptor.
             * @return The module's descriptor.
             */
            Object getDescriptor(Object value);
        }

        /**
         * A proxy for interacting with {@code java.lang.ModuleDescriptor}.
         */
        @JavaDispatcher.Proxied("java.lang.module.ModuleDescriptor")
        protected interface ModuleDescriptor {

            /**
             * Returns the module's name.
             *
             * @param value The {@code java.lang.ModuleDescriptor} instance.
             * @return The module's name.
             */
            String name(Object value);

            /**
             * Returns the module's modifiers.
             *
             * @param value The {@code java.lang.ModuleDescriptor} instance.
             * @return The module's modifiers.
             */
            Set<?> modifiers(Object value);

            /**
             * Returns {@code true} if this is an open module.
             *
             * @param value The {@code java.lang.ModuleDescriptor} instance.
             * @return {@code true} if this is an open module.
             */
            boolean isOpen(Object value);

            /**
             * Returns the module's requires declarations.
             *
             * @param value The {@code java.lang.ModuleDescriptor} instance.
             * @return The module's requires declarations.
             */
            Set<?> requires(Object value);

            /**
             * Returns the module's exports declarations.
             *
             * @param value The {@code java.lang.ModuleDescriptor} instance.
             * @return The module's exports declarations.
             */
            Set<?> exports(Object value);

            /**
             * Returns the module's opens declarations.
             *
             * @param value The {@code java.lang.ModuleDescriptor} instance.
             * @return The module's opens declarations.
             */
            Set<?> opens(Object value);

            /**
             * Returns the module's uses declarations.
             *
             * @param value The {@code java.lang.ModuleDescriptor} instance.
             * @return The module's uses declarations.
             */
            Set<String> uses(Object value);

            /**
             * Returns the module's provides declarations.
             *
             * @param value The {@code java.lang.ModuleDescriptor} instance.
             * @return The module's provides declarations.
             */
            Set<?> provides(Object value);

            /**
             * Returns the module's raw version.
             *
             * @param value The {@code java.lang.ModuleDescriptor} instance.
             * @return The module's raw version as an {@code Optional}.
             */
            Object rawVersion(Object value);

            /**
             * Returns the module's main class.
             *
             * @param value The {@code java.lang.ModuleDescriptor} instance.
             * @return The module's main class as an {@code Optional}.
             */
            Object mainClass(Object value);

            /**
             * Returns the module's packages.
             *
             * @param value The {@code java.lang.ModuleDescriptor} instance.
             * @return The module's packages.
             */
            Set<String> packages(Object value);

            /**
             * A proxy for interacting with {@code java.lang.ModuleDescriptor.Requires}.
             */
            @JavaDispatcher.Proxied("java.lang.module.ModuleDescriptor$Requires")
            interface Requires {

                /**
                 * Returns the name of the required module.
                 *
                 * @param value The {@code java.lang.ModuleDescriptor.Requires} instance.
                 * @return The name of the required module.
                 */
                String name(Object value);

                /**
                 * Returns the modifiers of the requires declaration.
                 *
                 * @param value The {@code java.lang.ModuleDescriptor.Requires} instance.
                 * @return The modifiers of the requires declaration.
                 */
                Set<?> modifiers(Object value);

                /**
                 * Returns the raw compiled version of the required module.
                 *
                 * @param value The {@code java.lang.ModuleDescriptor.Requires} instance.
                 * @return The raw compiled version as an {@code Optional}.
                 */
                Object rawCompiledVersion(Object value);
            }

            /**
             * A proxy for interacting with {@code java.lang.ModuleDescriptor.Exports}.
             */
            @JavaDispatcher.Proxied("java.lang.module.ModuleDescriptor$Exports")
            interface Exports {

                /**
                 * Returns the source package name for this export.
                 *
                 * @param value The {@code java.lang.ModuleDescriptor.Exports} instance.
                 * @return The source package name.
                 */
                String source(Object value);

                /**
                 * Returns the modifiers of the exports declaration.
                 *
                 * @param value The {@code java.lang.ModuleDescriptor.Exports} instance.
                 * @return The modifiers of the exports declaration.
                 */
                Set<?> modifiers(Object value);

                /**
                 * Returns the target modules for this export.
                 *
                 * @param value The {@code java.lang.ModuleDescriptor.Exports} instance.
                 * @return The target modules for this export.
                 */
                Set<String> targets(Object value);
            }

            /**
             * A proxy for interacting with {@code java.lang.ModuleDescriptor.Opens}.
             */
            @JavaDispatcher.Proxied("java.lang.module.ModuleDescriptor$Opens")
            interface Opens {

                /**
                 * Returns the source package name for this opens declaration.
                 *
                 * @param value The {@code java.lang.ModuleDescriptor.Opens} instance.
                 * @return The source package name.
                 */
                String source(Object value);

                /**
                 * Returns the modifiers of the opens declaration.
                 *
                 * @param value The {@code java.lang.ModuleDescriptor.Opens} instance.
                 * @return The modifiers of the opens declaration.
                 */
                Set<?> modifiers(Object value);

                /**
                 * Returns the target modules for this opens declaration.
                 *
                 * @param value The {@code java.lang.ModuleDescriptor.Opens} instance.
                 * @return The target modules for this opens declaration.
                 */
                Set<String> targets(Object value);
            }

            /**
             * A proxy for interacting with {@code java.lang.ModuleDescriptor.Provides}.
             */
            @JavaDispatcher.Proxied("java.lang.module.ModuleDescriptor$Provides")
            interface Provides {

                /**
                 * Returns the service interface name for this provides declaration.
                 *
                 * @param value The {@code java.lang.ModuleDescriptor.Provides} instance.
                 * @return The service interface name.
                 */
                String service(Object value);

                /**
                 * Returns the provider implementation class names for this provides declaration.
                 *
                 * @param value The {@code java.lang.ModuleDescriptor.Provides} instance.
                 * @return The provider implementation class names.
                 */
                List<String> providers(Object value);
            }
        }

        /**
         * A proxy for interacting with {@code java.util.Optional}.
         */
        @JavaDispatcher.Proxied("java.util.Optional")
        protected interface Optional {

            /**
             * Returns the value if present, otherwise returns the fallback value.
             *
             * @param value    The {@code java.util.Optional} instance.
             * @param fallback The fallback value to return if the optional is empty.
             * @return The value if present, otherwise the fallback value.
             */
            @MaybeNull
            Object orElse(Object value, @MaybeNull Object fallback);
        }
    }
}
