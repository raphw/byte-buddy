package net.bytebuddy.description.module;

import net.bytebuddy.build.AccessControllerPlugin;
import net.bytebuddy.description.ModifierReviewable;
import net.bytebuddy.description.NamedElement;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.annotation.AnnotationSource;
import net.bytebuddy.utility.dispatcher.JavaDispatcher;
import net.bytebuddy.utility.nullability.MaybeNull;

import java.lang.reflect.AnnotatedElement;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public interface ModuleDescription extends NamedElement,
        ModifierReviewable,
        AnnotationSource {

    @MaybeNull
    String getVersion();

    @MaybeNull
    String getMainClass();

    boolean isOpen();

    Set<String> getPackages();

    Set<String> getUses();

    Map<String, Exports> getExports();

    Map<String, Opens> getOpens();

    Map<String, Requires> getRequires();

    Map<String, Provides> getProvides();

    interface Exports extends ModifierReviewable {

        Set<String> getTargets();

        boolean isQualified();

        abstract class AbstractBase extends ModifierReviewable.AbstractBase implements Exports {

            public boolean isQualified() {
                return !getTargets().isEmpty();
            }
        }

        class Simple extends AbstractBase {

            private final Set<String> targets;

            protected final int modifiers;

            public Simple(Set<String> targets, int modifiers) {
                this.targets = targets;
                this.modifiers = modifiers;
            }

            public Set<String> getTargets() {
                return targets;
            }

            public int getModifiers() {
                return modifiers;
            }

            // TODO: equals hash/code
        }
    }

    interface Opens extends ModifierReviewable {

        Set<String> getTargets();

        boolean isQualified();

        abstract class AbstractBase extends ModifierReviewable.AbstractBase implements Opens {

            public boolean isQualified() {
                return !getTargets().isEmpty();
            }
        }

        class Simple extends AbstractBase {

            private final Set<String> targets;

            protected final int modifiers;

            public Simple(Set<String> targets, int modifiers) {
                this.targets = targets;
                this.modifiers = modifiers;
            }

            public Set<String> getTargets() {
                return targets;
            }

            public boolean isQualified() {
                return !targets.isEmpty();
            }

            public int getModifiers() {
                return modifiers;
            }

            // TODO: equals hash/code
        }
    }

    interface Requires extends ModifierReviewable {

        @MaybeNull
        String getVersion();

        class Simple extends ModifierReviewable.AbstractBase implements Requires {

            @MaybeNull
            private final String version;

            protected final int modifiers;

            public Simple(@MaybeNull String version, int modifiers) {
                this.version = version;
                this.modifiers = modifiers;
            }

            @MaybeNull
            public String getVersion() {
                return version;
            }

            public int getModifiers() {
                return modifiers;
            }
        }
    }

    interface Provides {

        Set<String> getProviders();

        class Simple implements Provides {

            private final Set<String> providers;

            public Simple(Set<String> providers) {
                this.providers = providers;
            }

            public Set<String> getProviders() {
                return providers;
            }
        }
    }

    class ForLoadedModule extends ModifierReviewable.AbstractBase implements ModuleDescription {

        protected static final Module MODULE = doPrivileged(JavaDispatcher.of(Module.class));

        protected static final ModuleDescriptor MODULE_DESCRIPTOR = doPrivileged(JavaDispatcher.of(ModuleDescriptor.class));

        protected static final ModuleDescriptor.Exports MODULE_DESCRIPTOR_EXPORTS = doPrivileged(JavaDispatcher.of(ModuleDescriptor.Exports.class));

        protected static final ModuleDescriptor.Exports.Modifier MODULE_DESCRIPTOR_EXPORTS_MODIFIER = doPrivileged(JavaDispatcher.of(ModuleDescriptor.Exports.Modifier.class));

        protected static final ModuleDescriptor.Opens MODULE_DESCRIPTOR_OPENS = doPrivileged(JavaDispatcher.of(ModuleDescriptor.Opens.class));

        protected static final ModuleDescriptor.Opens.Modifier MODULE_DESCRIPTOR_OPENS_MODIFIER = doPrivileged(JavaDispatcher.of(ModuleDescriptor.Opens.Modifier.class));

        protected static final ModuleDescriptor.Requires MODULE_DESCRIPTOR_REQUIRES = doPrivileged(JavaDispatcher.of(ModuleDescriptor.Requires.class));

        protected static final ModuleDescriptor.Requires.Modifier MODULE_DESCRIPTOR_REQUIRES_MODIFIER = doPrivileged(JavaDispatcher.of(ModuleDescriptor.Requires.Modifier.class));

        protected static final ModuleDescriptor.Provides MODULE_DESCRIPTOR_PROVIDES = doPrivileged(JavaDispatcher.of(ModuleDescriptor.Provides.class));

        protected static final Optional OPTIONAL = doPrivileged(JavaDispatcher.of(Optional.class));

        private final AnnotatedElement module;

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

        protected ForLoadedModule(AnnotatedElement module) {
            this.module = module;
        }

        @MaybeNull
        public String getVersion() {
            return (String) OPTIONAL.orElse(MODULE_DESCRIPTOR.rawVersion(MODULE.getDescriptor(module)), null);
        }

        @MaybeNull
        public String getMainClass() {
            return (String) OPTIONAL.orElse(MODULE_DESCRIPTOR.mainClass(MODULE.getDescriptor(module)), null);
        }

        public boolean isOpen() {
            return MODULE_DESCRIPTOR.isOpen(MODULE.getDescriptor(module));
        }

        public Set<String> getPackages() {
            return MODULE_DESCRIPTOR.packages(MODULE.getDescriptor(module));
        }

        public Set<String> getUses() {
            return MODULE_DESCRIPTOR.uses(MODULE.getDescriptor(module));
        }

        public Map<String, Exports> getExports() {
            Map<String, Exports> exports = new LinkedHashMap<String, Exports>();
            for (Object export : MODULE_DESCRIPTOR.exports(MODULE.getDescriptor(module))) {
                int modifiers = 0;
                for (Object modifier : MODULE_DESCRIPTOR_EXPORTS.modifiers(export)) {
                    modifiers |= MODULE_DESCRIPTOR_EXPORTS_MODIFIER.getMask(modifier);
                }
                exports.put(MODULE_DESCRIPTOR_EXPORTS.source(export), new Exports.Simple(MODULE_DESCRIPTOR_EXPORTS.targets(export), modifiers));
            }
            return exports;
        }

        public Map<String, Opens> getOpens() {
            Map<String, Opens> opens = new LinkedHashMap<String, Opens>();
            for (Object open : MODULE_DESCRIPTOR.opens(MODULE.getDescriptor(module))) {
                int modifiers = 0;
                for (Object modifier : MODULE_DESCRIPTOR_OPENS.modifiers(open)) {
                    modifiers |= MODULE_DESCRIPTOR_OPENS_MODIFIER.getMask(modifier);
                }
                opens.put(MODULE_DESCRIPTOR_OPENS.source(open), new Opens.Simple(MODULE_DESCRIPTOR_OPENS.targets(open), modifiers));
            }
            return opens;
        }

        public Map<String, Requires> getRequires() {
            Map<String, Requires> requires = new LinkedHashMap<String, Requires>();
            for (Object require : MODULE_DESCRIPTOR.requires(MODULE.getDescriptor(module))) {
                int modifiers = 0;
                for (Object modifier : MODULE_DESCRIPTOR_REQUIRES.modifiers(require)) {
                    modifiers |= MODULE_DESCRIPTOR_REQUIRES_MODIFIER.getMask(modifier);
                }
                requires.put(MODULE_DESCRIPTOR_REQUIRES.name(require), new Requires.Simple(
                        (String) OPTIONAL.orElse(MODULE_DESCRIPTOR_REQUIRES.rawCompiledVersion(require), null),
                        modifiers));
            }
            return requires;
        }

        public Map<String, Provides> getProvides() {
            Map<String, Provides> provides = new LinkedHashMap<String, Provides>();
            for (Object require : MODULE_DESCRIPTOR.provides(MODULE.getDescriptor(module))) {
                provides.put(MODULE_DESCRIPTOR_PROVIDES.service(require), new Provides.Simple(MODULE_DESCRIPTOR_PROVIDES.provides(require)));
            }
            return provides;
        }

        public int getModifiers() {
            int modifiers = 0;
            for (Object modifier : MODULE_DESCRIPTOR.modifiers(module)) {
                modifiers |= MODULE_DESCRIPTOR_REQUIRES_MODIFIER.getMask(modifier);
            }
            return modifiers;
        }

        public String getActualName() {
            return MODULE_DESCRIPTOR.name(MODULE.getDescriptor(module));
        }

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
        @JavaDispatcher.Proxied("java.lang.ModuleDescriptor")
        protected interface ModuleDescriptor {

            String name(Object value);

            Set<?> modifiers(Object value);

            boolean isOpen(Object value);

            Set<?> requires(Object value);

            Set<?> exports(Object value);

            Set<?> opens(Object value);

            Set<String> uses(Object value);

            Set<?> provides(Object value);

            Object rawVersion(Object value);

            Object mainClass(Object value);

            Set<String> packages(Object value);

            @JavaDispatcher.Proxied("java.lang.ModuleDescriptor$Requires")
            interface Requires {

                String name(Object value);

                Set<?> modifiers(Object value);

                Object rawCompiledVersion(Object value);

                @JavaDispatcher.Proxied("java.lang.ModuleDescriptor$Requires$Modifier")
                interface Modifier {

                    int getMask(Object value);
                }
            }

            @JavaDispatcher.Proxied("java.lang.ModuleDescriptor$Exports")
            interface Exports {

                String source(Object value);

                Set<?> modifiers(Object value);

                Set<String> targets(Object value);

                @JavaDispatcher.Proxied("java.lang.ModuleDescriptor$Exports$Modifier")
                interface Modifier {

                    int getMask(Object value);
                }
            }

            @JavaDispatcher.Proxied("java.lang.ModuleDescriptor$Opens")
            interface Opens {

                String source(Object value);

                Set<?> modifiers(Object value);

                Set<String> targets(Object value);

                @JavaDispatcher.Proxied("java.lang.ModuleDescriptor$Opens$Modifier")
                interface Modifier {

                    int getMask(Object value);
                }
            }

            @JavaDispatcher.Proxied("java.lang.ModuleDescriptor$Provides")
            interface Provides {

                String service(Object value);

                Set<String> provides(Object value);
            }
        }

        @JavaDispatcher.Proxied("java.lang.Optional")
        protected interface Optional {

            @MaybeNull
            Object orElse(Object value, @MaybeNull Object fallback);
        }
    }
}
