package net.bytebuddy.asm;

import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.utility.CompoundList;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.*;

/**
 * A class visitor wrapper is used in order to register an intermediate ASM {@link org.objectweb.asm.ClassVisitor} which
 * is applied to the main type created by a {@link net.bytebuddy.dynamic.DynamicType.Builder} but not
 * to any {@link net.bytebuddy.implementation.auxiliary.AuxiliaryType}s, if any.
 */
public interface AsmVisitorWrapper {

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
    ClassVisitor wrap(TypeDescription instrumentedType, ClassVisitor classVisitor);

    /**
     * A class visitor wrapper that does not apply any changes.
     */
    enum NoOp implements AsmVisitorWrapper {

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
        public ClassVisitor wrap(TypeDescription instrumentedType, ClassVisitor classVisitor) {
            return classVisitor;
        }

        @Override
        public String toString() {
            return "AsmVisitorWrapper.NoOp." + name();
        }
    }

    class FlagSetting implements AsmVisitorWrapper {

        private final int writerFlags;

        private final int readerFlags;

        public FlagSetting(int writerFlags, int readerFlags) {
            this.writerFlags = writerFlags;
            this.readerFlags = readerFlags;
        }

        @Override
        public int mergeWriter(int flags) {
            return flags | writerFlags;
        }

        @Override
        public int mergeReader(int flags) {
            return flags | readerFlags;
        }

        @Override
        public ClassVisitor wrap(TypeDescription instrumentedType, ClassVisitor classVisitor) {
            return classVisitor;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            FlagSetting that = (FlagSetting) other;
            return writerFlags == that.writerFlags && readerFlags == that.readerFlags;
        }

        @Override
        public int hashCode() {
            int result = writerFlags;
            result = 31 * result + readerFlags;
            return result;
        }

        @Override
        public String toString() {
            return "ClassVisitorWrapper.FlagSetting{" +
                    "writerFlags=" + writerFlags +
                    ", readerFlags=" + readerFlags +
                    '}';
        }
    }

    class ForDeclaredField implements AsmVisitorWrapper {

        private final List<Entry> entries;

        public ForDeclaredField() {
            this(Collections.<Entry>emptyList());
        }

        protected ForDeclaredField(List<Entry> entries) {
            this.entries = entries;
        }

        public ForDeclaredField match(ElementMatcher<? super FieldDescription> matcher, FieldVisitorWrapper fieldVisitorWrapper) {
            return new ForDeclaredField(CompoundList.of(entries, new Entry(matcher, fieldVisitorWrapper)));
        }

        @Override
        public int mergeWriter(int flags) {
            return flags;
        }

        @Override
        public int mergeReader(int flags) {
            return flags;
        }

        @Override
        public ClassVisitor wrap(TypeDescription instrumentedType, ClassVisitor classVisitor) {
            return new DispatchingVisitor(classVisitor, instrumentedType);
        }

        public interface FieldVisitorWrapper {

            FieldVisitor wrap(TypeDescription instrumentedType, FieldDescription.InDefinedShape fieldDescription, FieldVisitor fieldVisitor);
        }

        protected static class Entry implements ElementMatcher<FieldDescription>, FieldVisitorWrapper {

            private final ElementMatcher<? super FieldDescription> matcher;

            private final FieldVisitorWrapper fieldVisitorWrapper;

            protected Entry(ElementMatcher<? super FieldDescription> matcher, FieldVisitorWrapper fieldVisitorWrapper) {
                this.matcher = matcher;
                this.fieldVisitorWrapper = fieldVisitorWrapper;
            }

            @Override
            public boolean matches(FieldDescription target) {
                return target != null && matcher.matches(target);
            }

            @Override
            public FieldVisitor wrap(TypeDescription instrumentedType, FieldDescription.InDefinedShape fieldDescription, FieldVisitor fieldVisitor) {
                return fieldVisitorWrapper.wrap(instrumentedType, fieldDescription, fieldVisitor);
            }
        }

        protected class DispatchingVisitor extends ClassVisitor {

            private final TypeDescription instrumentedType;

            private final Map<String, FieldDescription.InDefinedShape> fieldsByName;

            protected DispatchingVisitor(ClassVisitor classVisitor, TypeDescription instrumentedType) {
                super(Opcodes.ASM5, classVisitor);
                this.instrumentedType = instrumentedType;
                fieldsByName = new HashMap<String, FieldDescription.InDefinedShape>();
                for (FieldDescription.InDefinedShape fieldDescription : instrumentedType.getDeclaredFields()) {
                    fieldsByName.put(fieldDescription.getInternalName(), fieldDescription);
                }
            }

            @Override
            public FieldVisitor visitField(int modifiers, String internalName, String descriptor, String signature, Object defaultValue) {
                FieldVisitor fieldVisitor = super.visitField(modifiers, internalName, descriptor, signature, defaultValue);
                FieldDescription.InDefinedShape fieldDescription = fieldsByName.get(internalName);
                for (Entry entry : entries) {
                    if (entry.matches(fieldDescription)) {
                        fieldVisitor = entry.wrap(instrumentedType, fieldDescription, fieldVisitor);
                    }
                }
                return fieldVisitor;
            }
        }
    }

    class ForDeclaredMethod implements AsmVisitorWrapper {

        private final List<Entry> entries;

        public ForDeclaredMethod() {
            this(Collections.<Entry>emptyList());
        }

        protected ForDeclaredMethod(List<Entry> entries) {
            this.entries = entries;
        }

        public ForDeclaredMethod match(ElementMatcher<? super MethodDescription> matcher, MethodVisitorWrapper methodVisitorWrapper) {
            return new ForDeclaredMethod(CompoundList.of(entries, new Entry(matcher, methodVisitorWrapper)));
        }

        @Override
        public int mergeWriter(int flags) {
            return flags;
        }

        @Override
        public int mergeReader(int flags) {
            return flags;
        }

        @Override
        public ClassVisitor wrap(TypeDescription instrumentedType, ClassVisitor classVisitor) {
            return new DispatchingVisitor(classVisitor, instrumentedType);
        }

        public interface MethodVisitorWrapper {

            MethodVisitor wrap(TypeDescription instrumentedType, MethodDescription.InDefinedShape methodDescription, MethodVisitor methodVisitor);
        }

        protected static class Entry implements ElementMatcher<MethodDescription>, MethodVisitorWrapper {

            private final ElementMatcher<? super MethodDescription> matcher;

            private final MethodVisitorWrapper methodVisitorWrapper;

            protected Entry(ElementMatcher<? super MethodDescription> matcher, MethodVisitorWrapper methodVisitorWrapper) {
                this.matcher = matcher;
                this.methodVisitorWrapper = methodVisitorWrapper;
            }

            @Override
            public boolean matches(MethodDescription target) {
                return target != null && matcher.matches(target);
            }

            @Override
            public MethodVisitor wrap(TypeDescription instrumentedType, MethodDescription.InDefinedShape methodDescription, MethodVisitor methodVisitor) {
                return methodVisitorWrapper.wrap(instrumentedType, methodDescription, methodVisitor);
            }
        }

        protected class DispatchingVisitor extends ClassVisitor {

            private final TypeDescription instrumentedType;

            private final Map<String, MethodDescription.InDefinedShape> methodsByName;

            protected DispatchingVisitor(ClassVisitor classVisitor, TypeDescription instrumentedType) {
                super(Opcodes.ASM5, classVisitor);
                this.instrumentedType = instrumentedType;
                methodsByName = new HashMap<String, MethodDescription.InDefinedShape>();
                for (MethodDescription.InDefinedShape methodDescription : instrumentedType.getDeclaredMethods()) {
                    methodsByName.put(methodDescription.getInternalName() + methodDescription.getDescriptor(), methodDescription);
                }
            }

            @Override
            public MethodVisitor visitMethod(int modifiers, String internalName, String descriptor, String signature, String[] exceptions) {
                MethodVisitor methodVisitor = super.visitMethod(modifiers, internalName, descriptor, signature, exceptions);
                MethodDescription.InDefinedShape methodDescription = methodsByName.get(internalName);
                for (Entry entry : entries) {
                    if (entry.matches(methodDescription)) {
                        methodVisitor = entry.wrap(instrumentedType, methodDescription, methodVisitor);
                    }
                }
                return methodVisitor;
            }
        }
    }

    /**
     * An ordered, immutable chain of {@link AsmVisitorWrapper}s.
     */
    class Compound implements AsmVisitorWrapper {

        /**
         * The class visitor wrappers that are represented by this chain in their order. This list must not be mutated.
         */
        private final List<? extends AsmVisitorWrapper> asmVisitorWrappers;

        /**
         * Creates a new immutable chain based on an existing list of {@link AsmVisitorWrapper}s
         * where no copy of the received array is made.
         *
         * @param asmVisitorWrapper An array of {@link AsmVisitorWrapper}s where elements
         *                            at the beginning of the list are applied first, i.e. will be at the bottom of the generated
         *                            {@link org.objectweb.asm.ClassVisitor}.
         */
        public Compound(AsmVisitorWrapper... asmVisitorWrapper) {
            this(Arrays.asList(asmVisitorWrapper));
        }

        /**
         * Creates a new immutable chain based on an existing list of {@link AsmVisitorWrapper}s
         * where no copy of the received list is made.
         *
         * @param asmVisitorWrappers A list of {@link AsmVisitorWrapper}s where elements
         *                             at the beginning of the list are applied first, i.e. will be at the bottom of the generated
         *                             {@link org.objectweb.asm.ClassVisitor}.
         */
        public Compound(List<? extends AsmVisitorWrapper> asmVisitorWrappers) {
            this.asmVisitorWrappers = asmVisitorWrappers;
        }

        @Override
        public int mergeWriter(int flags) {
            for (AsmVisitorWrapper asmVisitorWrapper : asmVisitorWrappers) {
                flags = asmVisitorWrapper.mergeWriter(flags);
            }
            return flags;
        }

        @Override
        public int mergeReader(int flags) {
            for (AsmVisitorWrapper asmVisitorWrapper : asmVisitorWrappers) {
                flags = asmVisitorWrapper.mergeReader(flags);
            }
            return flags;
        }

        @Override
        public ClassVisitor wrap(TypeDescription instrumentedType, ClassVisitor classVisitor) {
            for (AsmVisitorWrapper asmVisitorWrapper : asmVisitorWrappers) {
                classVisitor = asmVisitorWrapper.wrap(instrumentedType, classVisitor);
            }
            return classVisitor;
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && asmVisitorWrappers.equals(((Compound) other).asmVisitorWrappers);
        }

        @Override
        public int hashCode() {
            return asmVisitorWrappers.hashCode();
        }

        @Override
        public String toString() {
            return "AsmVisitorWrapper.Compound{asmVisitorWrappers=" + asmVisitorWrappers + '}';
        }
    }
}
