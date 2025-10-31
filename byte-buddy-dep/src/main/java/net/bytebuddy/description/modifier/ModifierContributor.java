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
package net.bytebuddy.description.modifier;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import org.objectweb.asm.Opcodes;

import java.util.Arrays;
import java.util.Collection;

/**
 * An element that describes a type modifier as described in the
 * <a href="http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html">JVMS</a>.
 * <p>&nbsp;</p>
 * This allows for a more expressive and type safe alternative of defining a type's or type member's modifiers.
 * However, note that modifier's that apply competing modifiers (such as {@code private} and {@code protected}
 * should not be combined and will result in invalid types. An exception is thrown when built-in modifiers that
 * cannot be combined are used together.
 */
public interface ModifierContributor {

    /**
     * The empty modifier.
     */
    int EMPTY_MASK = 0;

    /**
     * Returns the mask of this modifier.
     *
     * @return The modifier mask that is to be applied to the target type or type member.
     */
    int getMask();

    /**
     * Returns the entire range of modifiers that address this contributor's property.
     *
     * @return The range of this contributor's property.
     */
    int getRange();

    /**
     * Determines if this is the default modifier.
     *
     * @return {@code true} if this contributor represents the default modifier.
     */
    boolean isDefault();

    /**
     * A marker interface for modifiers that can be applied to types.
     */
    interface ForType extends ModifierContributor {

        /**
         * A mask for all legal modifiers of a Java type.
         */
        int MASK = Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED | Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC
                | Opcodes.ACC_ABSTRACT | Opcodes.ACC_INTERFACE | Opcodes.ACC_ANNOTATION | Opcodes.ACC_DEPRECATED
                | Opcodes.ACC_ENUM | Opcodes.ACC_FINAL | Opcodes.ACC_STATIC | Opcodes.ACC_STRICT;
    }

    /**
     * A marker interface for modifiers that can be applied to modules.
     */
    interface ForModule extends ModifierContributor {

        /**
         * A mask for all legal modifiers of a Java module.
         */
        int MASK = Opcodes.ACC_OPEN | Opcodes.ACC_MANDATED | Opcodes.ACC_SYNTHETIC;

        /**
         * A marker interface for modifiers that can be applied to module requirement.
         */
        interface OfRequire extends ModifierContributor {

            /**
             * A mask for all legal modifiers of a Java module requirement.
             */
            int MASK = Opcodes.ACC_TRANSITIVE | Opcodes.ACC_MANDATED | Opcodes.ACC_SYNTHETIC;
        }

        /**
         * A marker interface for modifiers that can be applied to module exports.
         */
        interface OfExport extends ModifierContributor {

            /**
             * A mask for all legal modifiers of a Java module export.
             */
            int MASK = Opcodes.ACC_STATIC_PHASE | Opcodes.ACC_MANDATED | Opcodes.ACC_SYNTHETIC;
        }

        /**
         * A marker interface for modifiers that can be applied to module opening.
         */
        interface OfOpen extends ModifierContributor {

            /**
             * A mask for all legal modifiers of a Java module opening.
             */
            int MASK = Opcodes.ACC_STATIC_PHASE | Opcodes.ACC_MANDATED | Opcodes.ACC_SYNTHETIC;
        }
    }

    /**
     * A marker interface for modifiers that can be applied to fields.
     */
    interface ForField extends ModifierContributor {

        /**
         * A mask for all legal modifiers of a Java field.
         */
        int MASK = Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED | Opcodes.ACC_PRIVATE
                | Opcodes.ACC_DEPRECATED | Opcodes.ACC_ENUM | Opcodes.ACC_FINAL | Opcodes.ACC_STATIC
                | Opcodes.ACC_SYNTHETIC | Opcodes.ACC_TRANSIENT | Opcodes.ACC_VOLATILE;
    }

    /**
     * A marker interface for modifiers that can be applied to methods.
     */
    interface ForMethod extends ModifierContributor {

        /**
         * A mask for all legal modifiers of a Java method.
         */
        int MASK = Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED | Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC
                | Opcodes.ACC_BRIDGE | Opcodes.ACC_FINAL | Opcodes.ACC_NATIVE | Opcodes.ACC_ABSTRACT
                | Opcodes.ACC_STATIC | Opcodes.ACC_STRICT | Opcodes.ACC_SYNCHRONIZED
                | Opcodes.ACC_VARARGS;
    }

    /**
     * A marker interface for modifiers that can be applied to method parameters.
     */
    interface ForParameter extends ModifierContributor {

        /**
         * A mask for all legal modifiers of a Java parameter.
         */
        int MASK = Opcodes.ACC_MANDATED | Opcodes.ACC_FINAL | Opcodes.ACC_SYNTHETIC;

    }

    /**
     * A resolver for Java modifiers represented by {@link ModifierContributor}s.
     *
     * @param <T> The type of the {@link ModifierContributor}s being resolved.
     */
    @HashCodeAndEqualsPlugin.Enhance
    class Resolver<T extends ModifierContributor> {

        /**
         * The modifier contributors to resolve.
         */
        private final Collection<? extends T> modifierContributors;

        /**
         * Creates a new resolver.
         *
         * @param modifierContributors The modifier contributors to resolve.
         */
        protected Resolver(Collection<? extends T> modifierContributors) {
            this.modifierContributors = modifierContributors;
        }

        /**
         * Creates a new resolver for modifier contributors to a type.
         *
         * @param modifierContributor The modifier contributors to resolve.
         * @return A resolver for the provided modifier contributors.
         */
        public static Resolver<ForType> of(ForType... modifierContributor) {
            return of(Arrays.asList(modifierContributor));
        }

        /**
         * Creates a new resolver for modifier contributors to a module.
         *
         * @param modifierContributor The modifier contributors to resolve.
         * @return A resolver for the provided modifier contributors.
         */
        public static Resolver<ForModule> of(ForModule... modifierContributor) {
            return of(Arrays.asList(modifierContributor));
        }

        /**
         * Creates a new resolver for modifier contributors to a module requirement.
         *
         * @param modifierContributor The modifier contributors to resolve.
         * @return A resolver for the provided modifier contributors.
         */
        public static Resolver<ForModule.OfRequire> of(ForModule.OfRequire... modifierContributor) {
            return of(Arrays.asList(modifierContributor));
        }

        /**
         * Creates a new resolver for modifier contributors to a module export.
         *
         * @param modifierContributor The modifier contributors to resolve.
         * @return A resolver for the provided modifier contributors.
         */
        public static Resolver<ForModule.OfExport> of(ForModule.OfExport... modifierContributor) {
            return of(Arrays.asList(modifierContributor));
        }

        /**
         * Creates a new resolver for modifier contributors to a module opening.
         *
         * @param modifierContributor The modifier contributors to resolve.
         * @return A resolver for the provided modifier contributors.
         */
        public static Resolver<ForModule.OfOpen> of(ForModule.OfOpen... modifierContributor) {
            return of(Arrays.asList(modifierContributor));
        }

        /**
         * Creates a new resolver for modifier contributors to a field.
         *
         * @param modifierContributor The modifier contributors to resolve.
         * @return A resolver for the provided modifier contributors.
         */
        public static Resolver<ForField> of(ForField... modifierContributor) {
            return of(Arrays.asList(modifierContributor));
        }

        /**
         * Creates a new resolver for modifier contributors to a method.
         *
         * @param modifierContributor The modifier contributors to resolve.
         * @return A resolver for the provided modifier contributors.
         */
        public static Resolver<ForMethod> of(ForMethod... modifierContributor) {
            return of(Arrays.asList(modifierContributor));
        }

        /**
         * Creates a new resolver for modifier contributors to a parameter.
         *
         * @param modifierContributor The modifier contributors to resolve.
         * @return A resolver for the provided modifier contributors.
         */
        public static Resolver<ForParameter> of(ForParameter... modifierContributor) {
            return Resolver.of(Arrays.asList(modifierContributor));
        }

        /**
         * Creates a new resolver for any modifier contributor of a given type.
         *
         * @param modifierContributors The modifier contributors to resolve.
         * @param <S>                  The modifier contributors type.
         * @return A resolver for the provided modifier contributors.
         */
        public static <S extends ModifierContributor> Resolver<S> of(Collection<? extends S> modifierContributors) {
            return new Resolver<S>(modifierContributors);
        }

        /**
         * Resolves the modifier contributors based on a zero modifier.
         *
         * @return The resolved modifiers.
         */
        public int resolve() {
            return resolve(EMPTY_MASK);
        }

        /**
         * Resolves the modifier contributors based on a given modifier.
         *
         * @param modifiers The base modifiers.
         * @return The resolved modifiers.
         */
        public int resolve(int modifiers) {
            for (T modifierContributor : modifierContributors) {
                modifiers = (modifiers & ~modifierContributor.getRange()) | modifierContributor.getMask();
            }
            return modifiers;
        }
    }
}
