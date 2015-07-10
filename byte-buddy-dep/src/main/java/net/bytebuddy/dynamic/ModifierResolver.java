package net.bytebuddy.dynamic;

import net.bytebuddy.description.method.MethodDescription;
import org.objectweb.asm.Opcodes;

/**
 * A modifier resolver allows to define a modifier for a method. That modifier may derive from the original modifier.
 * It is the responsibility of the transformer to assure that a transformed modifier is legal.
 */
public interface ModifierResolver {

    /**
     * Resolves a modifier for a given method.
     *
     * @param methodDescription The method for which a modifier is to be resolved.
     * @param implemented       {@code true} if the method is implemented.
     * @return The modifier to be defined for this method. This modifier must be a method's actual modifier which might derive from the value that
     * is returned from the reflection API.
     * @see MethodDescription#getAdjustedModifiers(boolean)
     */
    int transform(MethodDescription methodDescription, boolean implemented);

    /**
     * Resolves a modifier as it is defined by the method itself.
     */
    enum Simple implements ModifierResolver {

        /**
         * The singleton instance.
         */
        INSTANCE;

        @Override
        public int transform(MethodDescription methodDescription, boolean implemented) {
            return methodDescription.getAdjustedModifiers(implemented);
        }

        @Override
        public String toString() {
            return "ModifierResolver.Simple." + name();
        }
    }

    /**
     *  Resolves a modifier as it is defined by the method itself but strips any {@code synchronized} modifier.
     */
    enum Desynchronizing implements ModifierResolver {

        /**
         * The singleton instance.
         */
        INSTANCE;

        @Override
        public int transform(MethodDescription methodDescription, boolean implemented) {
            return methodDescription.getAdjustedModifiers(implemented) & ~Opcodes.ACC_SYNCHRONIZED;
        }

        @Override
        public String toString() {
            return "ModifierResolver.Desynchronizing." + name();
        }
    }
}
