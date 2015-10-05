package net.bytebuddy.description;

import org.objectweb.asm.Opcodes;

import java.lang.reflect.Modifier;

/**
 * Implementations of this interface can be described in terms of a Java modifier.
 */
public interface ModifierReviewable {

    /**
     * Representation of the default modifier.
     */
    int EMPTY_MASK = 0;

    /**
     * Specifies if the modifier described by this object is {@code final}.
     *
     * @return {@code true} if the modifier described by this object is {@code final}.
     */
    boolean isFinal();

    /**
     * Specifies if the modifier described by this object is {@code static}.
     *
     * @return {@code true} if the modifier described by this object is {@code static}.
     */
    boolean isStatic();

    /**
     * Specifies if the modifier described by this object is {@code public}.
     *
     * @return {@code true} if the modifier described by this object is {@code public}.
     */
    boolean isPublic();

    /**
     * Specifies if the modifier described by this object is {@code protected}.
     *
     * @return {@code true} if the modifier described by this object is {@code protected}.
     */
    boolean isProtected();

    /**
     * Specifies if the modifier described by this object is package private.
     *
     * @return {@code true} if the modifier described by this object is package private.
     */
    boolean isPackagePrivate();

    /**
     * Specifies if the modifier described by this object is {@code private}.
     *
     * @return {@code true} if the modifier described by this object is {@code private}.
     */
    boolean isPrivate();

    /**
     * Specifies if the modifier described by this object is {@code abstract}.
     *
     * @return {@code true} if the modifier described by this object is {@code abstract}.
     */
    boolean isAbstract();

    /**
     * Specifies if the modifier described by this object is {@code native}.
     *
     * @return {@code true} if the modifier described by this object is {@code native}.
     */
    boolean isNative();

    /**
     * Specifies if the modifier described by this object is {@code synchronized}.
     *
     * @return {@code true} if the modifier described by this object is {@code synchronized}.
     */
    boolean isSynchronized();

    /**
     * Specifies if the modifier described by this object is {@code strictfp}.
     *
     * @return {@code true} if the modifier described by this object is {@code strictfp}.
     */
    boolean isStrict();

    /**
     * Specifies if the modifier described by this object is synthetic.
     *
     * @return {@code true} if the modifier described by this object is synthetic.
     */
    boolean isSynthetic();

    /**
     * CSpecifies if the modifier described by this object is mandated.
     *
     * @return {@code true} if the modifier described by this object is mandated.
     */
    boolean isMandated();

    /**
     * Specifies if the modifier described by this object reflects the type super flag.
     *
     * @return {@code true} if the modifier described by this object reflects the type super flag.
     */
    boolean isSuper();

    /**
     * Specifies if the modifier described by this object represents the bridge flag.
     *
     * @return {@code true} if the modifier described by this object represents the bridge flag
     */
    boolean isBridge();

    /**
     * Specifies if the modifier described by this object represents the deprecated flag.
     *
     * @return {@code true} if the modifier described by this object represents the deprecated flag.
     */
    boolean isDeprecated();

    /**
     * Specifies if the modifier described by this object represents the annotation flag.
     *
     * @return {@code true} if the modifier described by this object represents the annotation flag.
     */
    boolean isAnnotation();

    /**
     * Specifies if the modifier described by this object represents the enum flag.
     *
     * @return {@code true} if the modifier described by this object represents the enum flag.
     */
    boolean isEnum();

    /**
     * Specifies if the modifier described by this object represents the interface flag.
     *
     * @return {@code true} if the modifier described by this object represents the interface flag.
     */
    boolean isInterface();

    /**
     * Specifies if the modifier described by this object describes a non-interface and non-annotation type.
     *
     * @return {@code true} if the modifier described by this object represents a class, i.e. not an interface or an annotation.
     */
    boolean isClassType();

    /**
     * Specifies if the modifier described by this object represents the transient flag.
     *
     * @return {@code true} if the modifier described by this object represents the transient flag.
     */
    boolean isTransient();

    /**
     * Specifies if the modifier described by this object represents the volatile flag.
     *
     * @return {@code true} if the modifier described by this object represents the volatile flag.
     */
    boolean isVolatile();

    /**
     * Specifies if the modifier described by this object represents the var args flag.
     *
     * @return {@code true} if the modifier described by this object represents the var args flag.
     */
    boolean isVarArgs();

    /**
     * Returns the modifier that is described by this object.
     *
     * @return The modifier that is described by this object.
     */
    int getModifiers();

    /**
     * An abstract base implementation of a {@link ModifierReviewable} class.
     */
    abstract class AbstractBase implements ModifierReviewable {

        @Override
        public boolean isAbstract() {
            return matchesMask(Modifier.ABSTRACT);
        }

        @Override
        public boolean isFinal() {
            return matchesMask(Modifier.FINAL);
        }

        @Override
        public boolean isStatic() {
            return matchesMask(Modifier.STATIC);
        }

        @Override
        public boolean isPublic() {
            return matchesMask(Modifier.PUBLIC);
        }

        @Override
        public boolean isProtected() {
            return matchesMask(Modifier.PROTECTED);
        }

        @Override
        public boolean isPackagePrivate() {
            return !isPublic() && !isProtected() && !isPrivate();
        }

        @Override
        public boolean isPrivate() {
            return matchesMask(Modifier.PRIVATE);
        }

        @Override
        public boolean isNative() {
            return matchesMask(Modifier.NATIVE);
        }

        @Override
        public boolean isSynchronized() {
            return matchesMask(Modifier.SYNCHRONIZED);
        }

        @Override
        public boolean isStrict() {
            return matchesMask(Modifier.STRICT);
        }

        @Override
        public boolean isMandated() {
            return matchesMask(Opcodes.ACC_MANDATED);
        }

        @Override
        public boolean isSynthetic() {
            return matchesMask(Opcodes.ACC_SYNTHETIC);
        }

        @Override
        public boolean isSuper() {
            return matchesMask(Opcodes.ACC_SUPER);
        }

        @Override
        public boolean isBridge() {
            return matchesMask(Opcodes.ACC_BRIDGE);
        }

        @Override
        public boolean isDeprecated() {
            return matchesMask(Opcodes.ACC_DEPRECATED);
        }

        @Override
        public boolean isAnnotation() {
            return matchesMask(Opcodes.ACC_ANNOTATION);
        }

        @Override
        public boolean isEnum() {
            return matchesMask(Opcodes.ACC_ENUM);
        }

        @Override
        public boolean isInterface() {
            return matchesMask(Opcodes.ACC_INTERFACE);
        }

        @Override
        public boolean isClassType() {
            return !(isInterface() || isAnnotation());
        }

        @Override
        public boolean isTransient() {
            return matchesMask(Opcodes.ACC_TRANSIENT);
        }

        @Override
        public boolean isVolatile() {
            return matchesMask(Opcodes.ACC_VOLATILE);
        }

        @Override
        public boolean isVarArgs() {
            return matchesMask(Opcodes.ACC_VARARGS);
        }

        /**
         * Checks if a mask is matched by this instance.
         *
         * @param mask The mask to check.
         * @return {@code true} if the mask is matched.
         */
        private boolean matchesMask(int mask) {
            return (getModifiers() & mask) == mask;
        }
    }
}
