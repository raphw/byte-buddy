package net.bytebuddy.asm;

import org.objectweb.asm.ClassVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A class visitor wrapper is used in order to register an intermediate ASM {@link org.objectweb.asm.ClassVisitor} which
 * is applied to the main type created by a {@link net.bytebuddy.dynamic.DynamicType.Builder} but not
 * to any {@link net.bytebuddy.implementation.auxiliary.AuxiliaryType}s, if any.
 */
public interface ClassVisitorWrapper {

    /**
     * Applies a {@code ClassVisitorWrapper} to the creation of a {@link net.bytebuddy.dynamic.DynamicType}.
     *
     * @param classVisitor A {@code ClassVisitor} to become the new primary class visitor to which the created
     *                     {@link net.bytebuddy.dynamic.DynamicType} is written to.
     * @return A new {@code ClassVisitor} that usually delegates to the {@code ClassVisitor} delivered in the argument.
     */
    ClassVisitor wrap(ClassVisitor classVisitor);

    /**
     * An ordered, immutable chain of {@link net.bytebuddy.asm.ClassVisitorWrapper}s.
     */
    class Chain implements ClassVisitorWrapper {

        /**
         * The class visitor wrappers that are represented by this chain in their order. This list must not be mutated.
         */
        private final List<ClassVisitorWrapper> classVisitorWrappers;

        /**
         * Creates an immutable empty chain.
         */
        public Chain() {
            this.classVisitorWrappers = Collections.emptyList();
        }

        /**
         * Creates a new immutable chain based on an existing list of {@link net.bytebuddy.asm.ClassVisitorWrapper}s
         * where no copy of the received list is made.
         *
         * @param classVisitorWrappers A list of {@link net.bytebuddy.asm.ClassVisitorWrapper}s where elements
         *                             at the beginning of the list are applied first, i.e. will be at the bottom of the generated
         *                             {@link org.objectweb.asm.ClassVisitor}.
         */
        protected Chain(List<ClassVisitorWrapper> classVisitorWrappers) {
            this.classVisitorWrappers = classVisitorWrappers;
        }

        /**
         * Adds a {@code ClassVisitorWrapper} to the <b>beginning</b> of the chain such that the wrapped
         * ASM {@code ClassVisitor} will be applied before the other class visitors.
         *
         * @param classVisitorWrapper The {@code ClassVisitorWrapper} to add to the beginning of the chain.
         * @return A new chain incorporating the {@code ClassVisitorWrapper}.
         */
        public Chain prepend(ClassVisitorWrapper classVisitorWrapper) {
            List<ClassVisitorWrapper> appendedList = new ArrayList<ClassVisitorWrapper>(classVisitorWrappers.size() + 1);
            appendedList.add(classVisitorWrapper);
            appendedList.addAll(classVisitorWrappers);
            return new Chain(appendedList);
        }

        /**
         * Adds a {@code ClassVisitorWrapper} to the <b>end</b> of the chain such that the wrapped
         * ASM {@code ClassVisitor} will be applied after the other class visitors.
         *
         * @param classVisitorWrapper The {@code ClassVisitorWrapper} to add to the end of the chain.
         * @return A new chain incorporating the {@code ClassVisitorWrapper}.
         */
        public Chain append(ClassVisitorWrapper classVisitorWrapper) {
            List<ClassVisitorWrapper> appendedList = new ArrayList<ClassVisitorWrapper>(classVisitorWrappers.size() + 1);
            appendedList.addAll(classVisitorWrappers);
            appendedList.add(classVisitorWrapper);
            return new Chain(appendedList);
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
                    && classVisitorWrappers.equals(((Chain) other).classVisitorWrappers);
        }

        @Override
        public int hashCode() {
            return classVisitorWrappers.hashCode();
        }

        @Override
        public String toString() {
            return "ClassVisitorWrapper.Chain{classVisitorWrappers=" + classVisitorWrappers + '}';
        }
    }
}
