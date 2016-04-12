package net.bytebuddy.asm;

import net.bytebuddy.ClassFileVersion;
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
     * Indicates that no flags should be set.
     */
    int NO_FLAGS = 0;

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
     * @param instrumentedType The instrumented type.
     * @param classVisitor     A {@code ClassVisitor} to become the new primary class visitor to which the created
     *                         {@link net.bytebuddy.dynamic.DynamicType} is written to.
     * @param writerFlags      The ASM {@link org.objectweb.asm.ClassWriter} flags to consider.
     * @param readerFlags      The ASM {@link org.objectweb.asm.ClassReader} flags to consider.
     * @return A new {@code ClassVisitor} that usually delegates to the {@code ClassVisitor} delivered in the argument.
     */
    ClassVisitor wrap(TypeDescription instrumentedType, ClassVisitor classVisitor, int writerFlags, int readerFlags);

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
        public ClassVisitor wrap(TypeDescription instrumentedType, ClassVisitor classVisitor, int writerFlags, int readerFlags) {
            return classVisitor;
        }

        @Override
        public String toString() {
            return "AsmVisitorWrapper.NoOp." + name();
        }
    }

    /**
     * An abstract base implementation of an ASM visitor wrapper that does not set any flags.
     */
    abstract class AbstractBase implements AsmVisitorWrapper {

        @Override
        public int mergeWriter(int flags) {
            return flags;
        }

        @Override
        public int mergeReader(int flags) {
            return flags;
        }
    }

    /**
     * An ASM visitor wrapper that allows to wrap declared fields of the instrumented type with a {@link FieldVisitorWrapper}.
     */
    class ForDeclaredFields extends AbstractBase {

        /**
         * The list of entries that describe matched fields in their application order.
         */
        private final List<Entry> entries;

        /**
         * Creates a new visitor wrapper for declared fields.
         */
        public ForDeclaredFields() {
            this(Collections.<Entry>emptyList());
        }

        /**
         * Creates a new visitor wrapper for declared fields.
         *
         * @param entries The list of entries that describe matched fields in their application order.
         */
        protected ForDeclaredFields(List<Entry> entries) {
            this.entries = entries;
        }

        /**
         * Defines a new field visitor wrapper to be applied if the given field matcher is matched. Previously defined
         * entries are applied before the given matcher is applied.
         *
         * @param matcher             The matcher to identify fields to be wrapped.
         * @param fieldVisitorWrapper The field visitor wrapper to be applied if the given matcher is matched.
         * @return A new ASM visitor wrapper that applied the given field visitor wrapper if the supplied matcher is matched.
         */
        public ForDeclaredFields field(ElementMatcher<? super FieldDescription.InDefinedShape> matcher, FieldVisitorWrapper fieldVisitorWrapper) {
            return new ForDeclaredFields(CompoundList.of(entries, new Entry(matcher, fieldVisitorWrapper)));
        }

        @Override
        public ClassVisitor wrap(TypeDescription instrumentedType, ClassVisitor classVisitor, int writerFlags, int readerFlags) {
            return new DispatchingVisitor(classVisitor, instrumentedType);
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && entries.equals(((ForDeclaredFields) other).entries);
        }

        @Override
        public int hashCode() {
            return entries.hashCode();
        }

        @Override
        public String toString() {
            return "AsmVisitorWrapper.ForDeclaredFields{" +
                    "entries=" + entries +
                    '}';
        }

        /**
         * A field visitor wrapper that allows for wrapping a {@link FieldVisitor} defining a declared field.
         */
        public interface FieldVisitorWrapper {

            /**
             * Wraps a field visitor.
             *
             * @param instrumentedType The instrumented type.
             * @param fieldDescription The field that is currently being defined.
             * @param fieldVisitor     The original field visitor that defines the given field.
             * @return The wrapped field visitor.
             */
            FieldVisitor wrap(TypeDescription instrumentedType, FieldDescription.InDefinedShape fieldDescription, FieldVisitor fieldVisitor);
        }

        /**
         * An entry describing a field visitor wrapper paired with a matcher for fields to be wrapped.
         */
        protected static class Entry implements ElementMatcher<FieldDescription.InDefinedShape>, FieldVisitorWrapper {

            /**
             * The matcher to identify fields to be wrapped.
             */
            private final ElementMatcher<? super FieldDescription.InDefinedShape> matcher;

            /**
             * The field visitor wrapper to be applied if the given matcher is matched.
             */
            private final FieldVisitorWrapper fieldVisitorWrapper;

            /**
             * Creates a new entry.
             *
             * @param matcher             The matcher to identify fields to be wrapped.
             * @param fieldVisitorWrapper The field visitor wrapper to be applied if the given matcher is matched.
             */
            protected Entry(ElementMatcher<? super FieldDescription.InDefinedShape> matcher, FieldVisitorWrapper fieldVisitorWrapper) {
                this.matcher = matcher;
                this.fieldVisitorWrapper = fieldVisitorWrapper;
            }

            @Override
            public boolean matches(FieldDescription.InDefinedShape target) {
                return target != null && matcher.matches(target);
            }

            @Override
            public FieldVisitor wrap(TypeDescription instrumentedType, FieldDescription.InDefinedShape fieldDescription, FieldVisitor fieldVisitor) {
                return fieldVisitorWrapper.wrap(instrumentedType, fieldDescription, fieldVisitor);
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) return true;
                if (other == null || getClass() != other.getClass()) return false;
                Entry entry = (Entry) other;
                return matcher.equals(entry.matcher)
                        && fieldVisitorWrapper.equals(entry.fieldVisitorWrapper);
            }

            @Override
            public int hashCode() {
                int result = matcher.hashCode();
                result = 31 * result + fieldVisitorWrapper.hashCode();
                return result;
            }

            @Override
            public String toString() {
                return "AsmVisitorWrapper.ForDeclaredFields.Entry{" +
                        "matcher=" + matcher +
                        ", fieldVisitorWrapper=" + fieldVisitorWrapper +
                        '}';
            }
        }

        /**
         * A class visitor that applies the outer ASM visitor for identifying declared fields.
         */
        protected class DispatchingVisitor extends ClassVisitor {

            /**
             * The instrumented type.
             */
            private final TypeDescription instrumentedType;

            /**
             * A mapping of fields by their name.
             */
            private final Map<String, FieldDescription.InDefinedShape> fieldsByName;

            /**
             * Creates a new dispatching visitor.
             *
             * @param classVisitor     The underlying class visitor.
             * @param instrumentedType The instrumented type.
             */
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

            /**
             * Returns the outer instance.
             *
             * @return The outer instance.
             */
            private ForDeclaredFields getOuter() {
                return ForDeclaredFields.this;
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) return true;
                if (other == null || getClass() != other.getClass()) return false;
                DispatchingVisitor that = ((DispatchingVisitor) other);
                return instrumentedType.equals(that.instrumentedType)
                        && cv.equals(that.cv)
                        && getOuter().equals(that.getOuter());
            }

            @Override
            public int hashCode() {
                int result = getOuter().hashCode();
                result = 31 * result + instrumentedType.hashCode();
                result = 31 * result + cv.hashCode();
                return result;
            }

            @Override
            public String toString() {
                return "AsmVisitorWrapper.ForDeclaredFields.DispatchingVisitor{" +
                        "outer=" + getOuter() +
                        ", instrumentedType=" + instrumentedType +
                        ", fieldsByName=" + fieldsByName +
                        '}';
            }
        }
    }

    /**
     * <p>
     * An ASM visitor wrapper that allows to wrap <b>declared methods</b> of the instrumented type with a {@link MethodVisitorWrapper}.
     * </p>
     * <p>
     * Note: Inherited methods are <b>not</b> matched by this visitor, even if they are intercepted by a normal interception.
     * </p>
     */
    class ForDeclaredMethods implements AsmVisitorWrapper {

        /**
         * The list of entries that describe matched methods in their application order.
         */
        private final List<Entry> entries;

        /**
         * The writer flags to set.
         */
        private final int writerFlags;

        /**
         * The reader flags to set.
         */
        private final int readerFlags;

        /**
         * Creates a new visitor wrapper for declared methods.
         */
        public ForDeclaredMethods() {
            this(Collections.<Entry>emptyList(), NO_FLAGS, NO_FLAGS);
        }

        /**
         * Creates a new visitor wrapper for declared methods.
         *
         * @param entries     The list of entries that describe matched methods in their application order.
         * @param readerFlags The reader flags to set.
         * @param writerFlags The writer flags to set.
         */
        protected ForDeclaredMethods(List<Entry> entries, int writerFlags, int readerFlags) {
            this.entries = entries;
            this.writerFlags = writerFlags;
            this.readerFlags = readerFlags;
        }

        /**
         * Defines a new method visitor wrapper to be applied if the given method matcher is matched. Previously defined
         * entries are applied before the given matcher is applied.
         *
         * @param matcher              The matcher to identify methods to be wrapped.
         * @param methodVisitorWrapper The method visitor wrapper to be applied if the given matcher is matched.
         * @return A new ASM visitor wrapper that applied the given method visitor wrapper if the supplied matcher is matched.
         */
        public ForDeclaredMethods method(ElementMatcher<? super MethodDescription.InDefinedShape> matcher, MethodVisitorWrapper methodVisitorWrapper) {
            return new ForDeclaredMethods(CompoundList.of(entries, new Entry(matcher, methodVisitorWrapper)), writerFlags, readerFlags);
        }

        /**
         * Sets flags for the {@link org.objectweb.asm.ClassWriter} this wrapper is applied to.
         *
         * @param flags The flags to set for the {@link org.objectweb.asm.ClassWriter}.
         * @return A new ASM visitor wrapper that sets the supplied writer flags.
         */
        public ForDeclaredMethods writerFlags(int flags) {
            return new ForDeclaredMethods(entries, writerFlags | flags, readerFlags);
        }

        /**
         * Sets flags for the {@link org.objectweb.asm.ClassReader} this wrapper is applied to.
         *
         * @param flags The flags to set for the {@link org.objectweb.asm.ClassReader}.
         * @return A new ASM visitor wrapper that sets the supplied reader flags.
         */
        public ForDeclaredMethods readerFlags(int flags) {
            return new ForDeclaredMethods(entries, writerFlags, readerFlags | flags);
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
        public ClassVisitor wrap(TypeDescription instrumentedType, ClassVisitor classVisitor, int writerFlags, int readerFlags) {
            return new DispatchingVisitor(classVisitor, instrumentedType, writerFlags, readerFlags);
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && writerFlags == ((ForDeclaredMethods) other).writerFlags
                    && readerFlags == ((ForDeclaredMethods) other).readerFlags
                    && entries.equals(((ForDeclaredMethods) other).entries);
        }

        @Override
        public int hashCode() {
            int result = entries.hashCode();
            result = 31 * result + writerFlags;
            result = 31 * result + readerFlags;
            return result;
        }

        @Override
        public String toString() {
            return "AsmVisitorWrapper.ForDeclaredMethods{" +
                    "entries=" + entries +
                    ", writerFlags=" + writerFlags +
                    ", readerFlags=" + readerFlags +
                    '}';
        }

        /**
         * A method visitor wrapper that allows for wrapping a {@link MethodVisitor} defining a declared method.
         */
        public interface MethodVisitorWrapper {

            /**
             * Wraps a method visitor.
             *
             * @param instrumentedType  The instrumented type.
             * @param methodDescription The method that is currently being defined.
             * @param methodVisitor     The original field visitor that defines the given method.
             * @param classFileVersion  The class file version of the visited class.
             * @param writerFlags       The ASM {@link org.objectweb.asm.ClassWriter} reader flags to consider.
             * @param readerFlags       The ASM {@link org.objectweb.asm.ClassReader} reader flags to consider.
             * @return The wrapped method visitor.
             */
            MethodVisitor wrap(TypeDescription instrumentedType,
                               MethodDescription.InDefinedShape methodDescription,
                               MethodVisitor methodVisitor,
                               ClassFileVersion classFileVersion,
                               int writerFlags,
                               int readerFlags);
        }

        /**
         * An entry describing a method visitor wrapper paired with a matcher for fields to be wrapped.
         */
        protected static class Entry implements ElementMatcher<MethodDescription.InDefinedShape>, MethodVisitorWrapper {

            /**
             * The matcher to identify methods to be wrapped.
             */
            private final ElementMatcher<? super MethodDescription.InDefinedShape> matcher;

            /**
             * The method visitor wrapper to be applied if the given matcher is matched.
             */
            private final MethodVisitorWrapper methodVisitorWrapper;

            /**
             * Creates a new entry.
             *
             * @param matcher              The matcher to identify methods to be wrapped.
             * @param methodVisitorWrapper The method visitor wrapper to be applied if the given matcher is matched.
             */
            protected Entry(ElementMatcher<? super MethodDescription.InDefinedShape> matcher, MethodVisitorWrapper methodVisitorWrapper) {
                this.matcher = matcher;
                this.methodVisitorWrapper = methodVisitorWrapper;
            }

            @Override
            public boolean matches(MethodDescription.InDefinedShape target) {
                return target != null && matcher.matches(target);
            }

            @Override
            public MethodVisitor wrap(TypeDescription instrumentedType,
                                      MethodDescription.InDefinedShape methodDescription,
                                      MethodVisitor methodVisitor,
                                      ClassFileVersion classFileVersion,
                                      int writerFlags,
                                      int readerFlags) {
                return methodVisitorWrapper.wrap(instrumentedType, methodDescription, methodVisitor, classFileVersion, writerFlags, readerFlags);
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) return true;
                if (other == null || getClass() != other.getClass()) return false;
                Entry entry = (Entry) other;
                return matcher.equals(entry.matcher)
                        && methodVisitorWrapper.equals(entry.methodVisitorWrapper);
            }

            @Override
            public int hashCode() {
                int result = matcher.hashCode();
                result = 31 * result + methodVisitorWrapper.hashCode();
                return result;
            }

            @Override
            public String toString() {
                return "AsmVisitorWrapper.ForDeclaredMethods.Entry{" +
                        "matcher=" + matcher +
                        ", methodVisitorWrapper=" + methodVisitorWrapper +
                        '}';
            }
        }

        /**
         * A class visitor that applies the outer ASM visitor for identifying declared methods.
         */
        protected class DispatchingVisitor extends ClassVisitor {

            /**
             * The instrumented type.
             */
            private final TypeDescription instrumentedType;

            /**
             * The ASM {@link org.objectweb.asm.ClassWriter} reader flags to consider.
             */
            private final int writerFlags;

            /**
             * The ASM {@link org.objectweb.asm.ClassReader} reader flags to consider.
             */
            private final int readerFlags;

            /**
             * A mapping of fields by their name.
             */
            private final Map<String, MethodDescription.InDefinedShape> methodsByName;

            /**
             * The class file version of the visited class.
             */
            private ClassFileVersion classFileVersion;

            /**
             * Creates a new dispatching visitor.
             *
             * @param classVisitor     The underlying class visitor.
             * @param instrumentedType The instrumented type.
             * @param writerFlags      The ASM {@link org.objectweb.asm.ClassWriter} flags to consider.
             * @param readerFlags      The ASM {@link org.objectweb.asm.ClassReader} flags to consider.
             */
            protected DispatchingVisitor(ClassVisitor classVisitor, TypeDescription instrumentedType, int writerFlags, int readerFlags) {
                super(Opcodes.ASM5, classVisitor);
                this.instrumentedType = instrumentedType;
                this.writerFlags = writerFlags;
                this.readerFlags = readerFlags;
                methodsByName = new HashMap<String, MethodDescription.InDefinedShape>();
                for (MethodDescription.InDefinedShape methodDescription : instrumentedType.getDeclaredMethods()) {
                    methodsByName.put(methodDescription.getInternalName() + methodDescription.getDescriptor(), methodDescription);
                }
            }

            @Override
            public void visit(int version, int modifiers, String name, String signature, String superTypeName, String[] interfaceName) {
                classFileVersion = ClassFileVersion.ofMinorMajor(version);
                super.visit(version, modifiers, name, signature, superTypeName, interfaceName);
            }

            @Override
            public MethodVisitor visitMethod(int modifiers, String internalName, String descriptor, String signature, String[] exceptions) {
                MethodVisitor methodVisitor = super.visitMethod(modifiers, internalName, descriptor, signature, exceptions);
                MethodDescription.InDefinedShape methodDescription = methodsByName.get(internalName + descriptor);
                for (Entry entry : entries) {
                    if (entry.matches(methodDescription)) {
                        methodVisitor = entry.wrap(instrumentedType, methodDescription, methodVisitor, classFileVersion, writerFlags, readerFlags);
                    }
                }
                return methodVisitor;
            }

            /**
             * Returns the outer instance.
             *
             * @return The outer instance.
             */
            private ForDeclaredMethods getOuter() {
                return ForDeclaredMethods.this;
            }

            @Override
            public String toString() {
                return "AsmVisitorWrapper.ForDeclaredMethods.DispatchingVisitor{" +
                        "outer=" + getOuter() +
                        ", instrumentedType=" + instrumentedType +
                        ", methodsByName=" + methodsByName +
                        ", classFileVersion=" + classFileVersion +
                        ", writerFlags=" + writerFlags +
                        ", readerFlags=" + readerFlags +
                        '}';
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
         *                          at the beginning of the list are applied first, i.e. will be at the bottom of the generated
         *                          {@link org.objectweb.asm.ClassVisitor}.
         */
        public Compound(AsmVisitorWrapper... asmVisitorWrapper) {
            this(Arrays.asList(asmVisitorWrapper));
        }

        /**
         * Creates a new immutable chain based on an existing list of {@link AsmVisitorWrapper}s
         * where no copy of the received list is made.
         *
         * @param asmVisitorWrappers A list of {@link AsmVisitorWrapper}s where elements
         *                           at the beginning of the list are applied first, i.e. will be at the bottom of the generated
         *                           {@link org.objectweb.asm.ClassVisitor}.
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
        public ClassVisitor wrap(TypeDescription instrumentedType, ClassVisitor classVisitor, int writerFlags, int readerFlags) {
            for (AsmVisitorWrapper asmVisitorWrapper : asmVisitorWrappers) {
                classVisitor = asmVisitorWrapper.wrap(instrumentedType, classVisitor, writerFlags, readerFlags);
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
