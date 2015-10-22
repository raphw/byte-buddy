package net.bytebuddy.asm;

import org.objectweb.asm.ClassVisitor;

import java.util.Arrays;
import java.util.List;

/**
 * A class visitor wrapper is used in order to register an intermediate ASM {@link org.objectweb.asm.ClassVisitor} which
 * is applied to the main type created by a {@link net.bytebuddy.dynamic.DynamicType.Builder} but not
 * to any {@link net.bytebuddy.implementation.auxiliary.AuxiliaryType}s, if any.
 */
public interface ClassVisitorWrapper {

    /**
     * Defines the flags that are provided to any {@code ClassWriter} when writing a class. Typically, this gives opportunity to instruct ASM
     * to compute stack map frames or the size of the local variables array and the operand stack. If no specific flags are required for
     * applying this wrapper, the given value is to be returned.
     *
     * @param flags The currently set flags. This value should be combined (e.g. {@code flags | foo}) into the value that is returned by this wrapper.
     * @return The flags to be provided to the ASM {@code ClassWriter}.
     */
    int mergeWriter(int flags);

    /**
     * Defines the flags that are provided to any {@code ClassReader} when reading a class if applicable. Typically, this gives opportunity to
     * instruct ASM to expand or skip frames and to skip code and debug information. If no specific flags are required for applying this
     * wrapper, the given value is to be returned.
     *
     * @param flags The currently set flags. This value should be combined (e.g. {@code flags | foo}) into the value that is returned by this wrapper.
     * @return The flags to be provided to the ASM {@code ClassReader}.
     */
    int mergeReader(int flags);

    /**
     * Applies a {@code ClassVisitorWrapper} to the creation of a {@link net.bytebuddy.dynamic.DynamicType}.
     *
     * @param classVisitor A {@code ClassVisitor} to become the new primary class visitor to which the created
     *                     {@link net.bytebuddy.dynamic.DynamicType} is written to.
     * @return A new {@code ClassVisitor} that usually delegates to the {@code ClassVisitor} delivered in the argument.
     */
    ClassVisitor wrap(ClassVisitor classVisitor);

    /**
     * A class visitor wrapper that does not apply any changes.
     */
    enum NoOp implements ClassVisitorWrapper {

        /**
         * The singleton instance.
         */
        INSTANCE;

        @Override
        public int mergeWriter(int flags) {
            return flags;
        }

        @Override
        public int mergeReader(int flags) {
            return flags;
        }

        @Override
        public ClassVisitor wrap(ClassVisitor classVisitor) {
            return classVisitor;
        }

        @Override
        public String toString() {
            return "ClassVisitorWrapper.NoOp." + name();
        }
    }

    /**
     * An ordered, immutable chain of {@link net.bytebuddy.asm.ClassVisitorWrapper}s.
     */
    class Compound implements ClassVisitorWrapper {

        /**
         * The class visitor wrappers that are represented by this chain in their order. This list must not be mutated.
         */
        private final List<? extends ClassVisitorWrapper> classVisitorWrappers;

        /**
         * Creates a new immutable chain based on an existing list of {@link net.bytebuddy.asm.ClassVisitorWrapper}s
         * where no copy of the received array is made.
         *
         * @param classVisitorWrapper An array of {@link net.bytebuddy.asm.ClassVisitorWrapper}s where elements
         *                            at the beginning of the list are applied first, i.e. will be at the bottom of the generated
         *                            {@link org.objectweb.asm.ClassVisitor}.
         */
        public Compound(ClassVisitorWrapper... classVisitorWrapper) {
            this(Arrays.asList(classVisitorWrapper));
        }

        /**
         * Creates a new immutable chain based on an existing list of {@link net.bytebuddy.asm.ClassVisitorWrapper}s
         * where no copy of the received list is made.
         *
         * @param classVisitorWrappers A list of {@link net.bytebuddy.asm.ClassVisitorWrapper}s where elements
         *                             at the beginning of the list are applied first, i.e. will be at the bottom of the generated
         *                             {@link org.objectweb.asm.ClassVisitor}.
         */
        public Compound(List<? extends ClassVisitorWrapper> classVisitorWrappers) {
            this.classVisitorWrappers = classVisitorWrappers;
        }

        @Override
        public int mergeWriter(int flags) {
            for (ClassVisitorWrapper classVisitorWrapper : classVisitorWrappers) {
                flags = classVisitorWrapper.mergeWriter(flags);
            }
            return flags;
        }

        @Override
        public int mergeReader(int flags) {
            for (ClassVisitorWrapper classVisitorWrapper : classVisitorWrappers) {
                flags = classVisitorWrapper.mergeReader(flags);
            }
            return flags;
        }

        @Override
        public ClassVisitor wrap(ClassVisitor classVisitor) {
            for (ClassVisitorWrapper classVisitorWrapper : classVisitorWrappers) {
                classVisitor = classVisitorWrapper.wrap(classVisitor);
            }
            return classVisitor;
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && classVisitorWrappers.equals(((Compound) other).classVisitorWrappers);
        }

        @Override
        public int hashCode() {
            return classVisitorWrappers.hashCode();
        }

        @Override
        public String toString() {
            return "ClassVisitorWrapper.Compound{classVisitorWrappers=" + classVisitorWrappers + '}';
        }
    }
}
