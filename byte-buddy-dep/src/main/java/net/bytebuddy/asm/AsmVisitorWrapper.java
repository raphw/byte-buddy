package net.bytebuddy.asm;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.utility.CompoundList;
import net.bytebuddy.utility.OpenedClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

import java.util.*;

import static net.bytebuddy.matcher.ElementMatchers.isConstructor;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;

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
     * @param instrumentedType      The instrumented type.
     * @param classVisitor          A {@code ClassVisitor} to become the new primary class visitor to which the created
     *                              {@link net.bytebuddy.dynamic.DynamicType} is written to.
     * @param implementationContext The implementation context of the current instrumentation.
     * @param typePool              The type pool that was provided for the class creation.
     * @param fields                The instrumented type's fields.
     * @param methods               The instrumented type's methods non-ignored declared and virtually inherited methods.
     * @param writerFlags           The ASM {@link org.objectweb.asm.ClassWriter} flags to consider.
     * @param readerFlags           The ASM {@link org.objectweb.asm.ClassReader} flags to consider.
     * @return A new {@code ClassVisitor} that usually delegates to the {@code ClassVisitor} delivered in the argument.
     */
    ClassVisitor wrap(TypeDescription instrumentedType,
                      ClassVisitor classVisitor,
                      Implementation.Context implementationContext,
                      TypePool typePool,
                      FieldList<FieldDescription.InDefinedShape> fields,
                      MethodList<?> methods,
                      int writerFlags,
                      int readerFlags);

    /**
     * A class visitor wrapper that does not apply any changes.
     */
    enum NoOp implements AsmVisitorWrapper {

        /**
         * The singleton instance.
         */
        INSTANCE;

        /**
         * {@inheritDoc}
         */
        public int mergeWriter(int flags) {
            return flags;
        }

        /**
         * {@inheritDoc}
         */
        public int mergeReader(int flags) {
            return flags;
        }

        /**
         * {@inheritDoc}
         */
        public ClassVisitor wrap(TypeDescription instrumentedType,
                                 ClassVisitor classVisitor,
                                 Implementation.Context implementationContext,
                                 TypePool typePool,
                                 FieldList<FieldDescription.InDefinedShape> fields,
                                 MethodList<?> methods,
                                 int writerFlags,
                                 int readerFlags) {
            return classVisitor;
        }
    }

    /**
     * An abstract base implementation of an ASM visitor wrapper that does not set any flags.
     */
    abstract class AbstractBase implements AsmVisitorWrapper {

        /**
         * {@inheritDoc}
         */
        public int mergeWriter(int flags) {
            return flags;
        }

        /**
         * {@inheritDoc}
         */
        public int mergeReader(int flags) {
            return flags;
        }
    }

    /**
     * An ASM visitor wrapper that allows to wrap declared fields of the instrumented type with a {@link FieldVisitorWrapper}.
     */
    @HashCodeAndEqualsPlugin.Enhance
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
        public ForDeclaredFields field(ElementMatcher<? super FieldDescription.InDefinedShape> matcher, FieldVisitorWrapper... fieldVisitorWrapper) {
            return field(matcher, Arrays.asList(fieldVisitorWrapper));
        }

        /**
         * Defines a new field visitor wrapper to be applied if the given field matcher is matched. Previously defined
         * entries are applied before the given matcher is applied.
         *
         * @param matcher              The matcher to identify fields to be wrapped.
         * @param fieldVisitorWrappers The field visitor wrapper to be applied if the given matcher is matched.
         * @return A new ASM visitor wrapper that applied the given field visitor wrapper if the supplied matcher is matched.
         */
        public ForDeclaredFields field(ElementMatcher<? super FieldDescription.InDefinedShape> matcher, List<? extends FieldVisitorWrapper> fieldVisitorWrappers) {
            return new ForDeclaredFields(CompoundList.of(entries, new Entry(matcher, fieldVisitorWrappers)));
        }

        /**
         * {@inheritDoc}
         */
        public ClassVisitor wrap(TypeDescription instrumentedType,
                                 ClassVisitor classVisitor,
                                 Implementation.Context implementationContext,
                                 TypePool typePool,
                                 FieldList<FieldDescription.InDefinedShape> fields,
                                 MethodList<?> methods,
                                 int writerFlags,
                                 int readerFlags) {
            Map<String, FieldDescription.InDefinedShape> mapped = new HashMap<String, FieldDescription.InDefinedShape>();
            for (FieldDescription.InDefinedShape fieldDescription : fields) {
                mapped.put(fieldDescription.getInternalName() + fieldDescription.getDescriptor(), fieldDescription);
            }
            return new DispatchingVisitor(classVisitor, instrumentedType, mapped);
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
        @HashCodeAndEqualsPlugin.Enhance
        protected static class Entry implements ElementMatcher<FieldDescription.InDefinedShape>, FieldVisitorWrapper {

            /**
             * The matcher to identify fields to be wrapped.
             */
            private final ElementMatcher<? super FieldDescription.InDefinedShape> matcher;

            /**
             * The field visitor wrapper to be applied if the given matcher is matched.
             */
            private final List<? extends FieldVisitorWrapper> fieldVisitorWrappers;

            /**
             * Creates a new entry.
             *
             * @param matcher              The matcher to identify fields to be wrapped.
             * @param fieldVisitorWrappers The field visitor wrapper to be applied if the given matcher is matched.
             */
            protected Entry(ElementMatcher<? super FieldDescription.InDefinedShape> matcher, List<? extends FieldVisitorWrapper> fieldVisitorWrappers) {
                this.matcher = matcher;
                this.fieldVisitorWrappers = fieldVisitorWrappers;
            }

            /**
             * {@inheritDoc}
             */
            public boolean matches(FieldDescription.InDefinedShape target) {
                return target != null && matcher.matches(target);
            }

            /**
             * {@inheritDoc}
             */
            public FieldVisitor wrap(TypeDescription instrumentedType, FieldDescription.InDefinedShape fieldDescription, FieldVisitor fieldVisitor) {
                for (FieldVisitorWrapper fieldVisitorWrapper : fieldVisitorWrappers) {
                    fieldVisitor = fieldVisitorWrapper.wrap(instrumentedType, fieldDescription, fieldVisitor);
                }
                return fieldVisitor;
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
             * A mapping of fields by their name and descriptor key-combination.
             */
            private final Map<String, FieldDescription.InDefinedShape> fields;

            /**
             * Creates a new dispatching visitor.
             *
             * @param classVisitor     The underlying class visitor.
             * @param instrumentedType The instrumented type.
             * @param fields           The instrumented type's declared fields.
             */
            protected DispatchingVisitor(ClassVisitor classVisitor, TypeDescription instrumentedType, Map<String, FieldDescription.InDefinedShape> fields) {
                super(OpenedClassReader.ASM_API, classVisitor);
                this.instrumentedType = instrumentedType;
                this.fields = fields;
            }

            @Override
            public FieldVisitor visitField(int modifiers, String internalName, String descriptor, String signature, Object defaultValue) {
                FieldVisitor fieldVisitor = super.visitField(modifiers, internalName, descriptor, signature, defaultValue);
                FieldDescription.InDefinedShape fieldDescription = fields.get(internalName + descriptor);
                if (fieldVisitor != null && fieldDescription != null) {
                    for (Entry entry : entries) {
                        if (entry.matches(fieldDescription)) {
                            fieldVisitor = entry.wrap(instrumentedType, fieldDescription, fieldVisitor);
                        }
                    }
                }
                return fieldVisitor;
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
    @HashCodeAndEqualsPlugin.Enhance
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
         * Defines a new method visitor wrapper to be applied on any method if the given method matcher is matched.
         * Previously defined entries are applied before the given matcher is applied.
         *
         * @param matcher              The matcher to identify methods to be wrapped.
         * @param methodVisitorWrapper The method visitor wrapper to be applied if the given matcher is matched.
         * @return A new ASM visitor wrapper that applied the given method visitor wrapper if the supplied matcher is matched.
         */
        public ForDeclaredMethods method(ElementMatcher<? super MethodDescription> matcher, MethodVisitorWrapper... methodVisitorWrapper) {
            return method(matcher, Arrays.asList(methodVisitorWrapper));
        }

        /**
         * Defines a new method visitor wrapper to be applied on any method if the given method matcher is matched.
         * Previously defined entries are applied before the given matcher is applied.
         *
         * @param matcher               The matcher to identify methods to be wrapped.
         * @param methodVisitorWrappers The method visitor wrapper to be applied if the given matcher is matched.
         * @return A new ASM visitor wrapper that applied the given method visitor wrapper if the supplied matcher is matched.
         */
        public ForDeclaredMethods method(ElementMatcher<? super MethodDescription> matcher, List<? extends MethodVisitorWrapper> methodVisitorWrappers) {
            return invokable(isMethod().and(matcher), methodVisitorWrappers);
        }

        /**
         * Defines a new method visitor wrapper to be applied on any constructor if the given method matcher is matched.
         * Previously defined entries are applied before the given matcher is applied.
         *
         * @param matcher              The matcher to identify constructors to be wrapped.
         * @param methodVisitorWrapper The method visitor wrapper to be applied if the given matcher is matched.
         * @return A new ASM visitor wrapper that applied the given method visitor wrapper if the supplied matcher is matched.
         */
        public ForDeclaredMethods constructor(ElementMatcher<? super MethodDescription> matcher, MethodVisitorWrapper... methodVisitorWrapper) {
            return constructor(matcher, Arrays.asList(methodVisitorWrapper));
        }

        /**
         * Defines a new method visitor wrapper to be applied on any constructor if the given method matcher is matched.
         * Previously defined entries are applied before the given matcher is applied.
         *
         * @param matcher               The matcher to identify constructors to be wrapped.
         * @param methodVisitorWrappers The method visitor wrapper to be applied if the given matcher is matched.
         * @return A new ASM visitor wrapper that applied the given method visitor wrapper if the supplied matcher is matched.
         */
        public ForDeclaredMethods constructor(ElementMatcher<? super MethodDescription> matcher, List<? extends MethodVisitorWrapper> methodVisitorWrappers) {
            return invokable(isConstructor().and(matcher), methodVisitorWrappers);
        }

        /**
         * Defines a new method visitor wrapper to be applied on any method or constructor if the given method matcher is matched.
         * Previously defined entries are applied before the given matcher is applied.
         *
         * @param matcher              The matcher to identify methods or constructors to be wrapped.
         * @param methodVisitorWrapper The method visitor wrapper to be applied if the given matcher is matched.
         * @return A new ASM visitor wrapper that applied the given method visitor wrapper if the supplied matcher is matched.
         */
        public ForDeclaredMethods invokable(ElementMatcher<? super MethodDescription> matcher, MethodVisitorWrapper... methodVisitorWrapper) {
            return invokable(matcher, Arrays.asList(methodVisitorWrapper));
        }

        /**
         * Defines a new method visitor wrapper to be applied on any method or constructor if the given method matcher is matched.
         * Previously defined entries are applied before the given matcher is applied.
         *
         * @param matcher               The matcher to identify methods or constructors to be wrapped.
         * @param methodVisitorWrappers The method visitor wrapper to be applied if the given matcher is matched.
         * @return A new ASM visitor wrapper that applied the given method visitor wrapper if the supplied matcher is matched.
         */
        public ForDeclaredMethods invokable(ElementMatcher<? super MethodDescription> matcher, List<? extends MethodVisitorWrapper> methodVisitorWrappers) {
            return new ForDeclaredMethods(CompoundList.of(entries, new Entry(matcher, methodVisitorWrappers)), writerFlags, readerFlags);
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

        /**
         * {@inheritDoc}
         */
        public int mergeWriter(int flags) {
            return flags | writerFlags;
        }

        /**
         * {@inheritDoc}
         */
        public int mergeReader(int flags) {
            return flags | readerFlags;
        }

        /**
         * {@inheritDoc}
         */
        public ClassVisitor wrap(TypeDescription instrumentedType,
                                 ClassVisitor classVisitor,
                                 Implementation.Context implementationContext,
                                 TypePool typePool,
                                 FieldList<FieldDescription.InDefinedShape> fields,
                                 MethodList<?> methods,
                                 int writerFlags,
                                 int readerFlags) {
            Map<String, MethodDescription> mapped = new HashMap<String, MethodDescription>();
            for (MethodDescription methodDescription : CompoundList.<MethodDescription>of(methods, new MethodDescription.Latent.TypeInitializer(instrumentedType))) {
                mapped.put(methodDescription.getInternalName() + methodDescription.getDescriptor(), methodDescription);
            }
            return new DispatchingVisitor(classVisitor,
                    instrumentedType,
                    implementationContext,
                    typePool,
                    mapped,
                    writerFlags,
                    readerFlags);
        }

        /**
         * A method visitor wrapper that allows for wrapping a {@link MethodVisitor} defining a declared method.
         */
        public interface MethodVisitorWrapper {

            /**
             * Wraps a method visitor.
             *
             * @param instrumentedType      The instrumented type.
             * @param instrumentedMethod    The method that is currently being defined.
             * @param methodVisitor         The original field visitor that defines the given method.
             * @param implementationContext The implementation context to use.
             * @param typePool              The type pool to use.
             * @param writerFlags           The ASM {@link org.objectweb.asm.ClassWriter} reader flags to consider.
             * @param readerFlags           The ASM {@link org.objectweb.asm.ClassReader} reader flags to consider.
             * @return The wrapped method visitor.
             */
            MethodVisitor wrap(TypeDescription instrumentedType,
                               MethodDescription instrumentedMethod,
                               MethodVisitor methodVisitor,
                               Implementation.Context implementationContext,
                               TypePool typePool,
                               int writerFlags,
                               int readerFlags);
        }

        /**
         * An entry describing a method visitor wrapper paired with a matcher for fields to be wrapped.
         */
        @HashCodeAndEqualsPlugin.Enhance
        protected static class Entry implements ElementMatcher<MethodDescription>, MethodVisitorWrapper {

            /**
             * The matcher to identify methods to be wrapped.
             */
            private final ElementMatcher<? super MethodDescription> matcher;

            /**
             * The method visitor wrapper to be applied if the given matcher is matched.
             */
            private final List<? extends MethodVisitorWrapper> methodVisitorWrappers;

            /**
             * Creates a new entry.
             *
             * @param matcher               The matcher to identify methods to be wrapped.
             * @param methodVisitorWrappers The method visitor wrapper to be applied if the given matcher is matched.
             */
            protected Entry(ElementMatcher<? super MethodDescription> matcher, List<? extends MethodVisitorWrapper> methodVisitorWrappers) {
                this.matcher = matcher;
                this.methodVisitorWrappers = methodVisitorWrappers;
            }

            /**
             * {@inheritDoc}
             */
            public boolean matches(MethodDescription target) {
                return target != null && matcher.matches(target);
            }

            /**
             * {@inheritDoc}
             */
            public MethodVisitor wrap(TypeDescription instrumentedType,
                                      MethodDescription instrumentedMethod,
                                      MethodVisitor methodVisitor,
                                      Implementation.Context implementationContext,
                                      TypePool typePool,
                                      int writerFlags,
                                      int readerFlags) {
                for (MethodVisitorWrapper methodVisitorWrapper : methodVisitorWrappers) {
                    methodVisitor = methodVisitorWrapper.wrap(instrumentedType,
                            instrumentedMethod,
                            methodVisitor,
                            implementationContext,
                            typePool,
                            writerFlags,
                            readerFlags);
                }
                return methodVisitor;
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
             * The implementation context to use.
             */
            private final Implementation.Context implementationContext;

            /**
             * The type pool to use.
             */
            private final TypePool typePool;

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
            private final Map<String, MethodDescription> methods;

            /**
             * Creates a new dispatching visitor.
             *
             * @param classVisitor          The underlying class visitor.
             * @param instrumentedType      The instrumented type.
             * @param implementationContext The implementation context to use.
             * @param typePool              The type pool to use.
             * @param methods               The methods that are declared by the instrumented type or virtually inherited.
             * @param writerFlags           The ASM {@link org.objectweb.asm.ClassWriter} flags to consider.
             * @param readerFlags           The ASM {@link org.objectweb.asm.ClassReader} flags to consider.
             */
            protected DispatchingVisitor(ClassVisitor classVisitor,
                                         TypeDescription instrumentedType,
                                         Implementation.Context implementationContext,
                                         TypePool typePool,
                                         Map<String, MethodDescription> methods,
                                         int writerFlags,
                                         int readerFlags) {
                super(OpenedClassReader.ASM_API, classVisitor);
                this.instrumentedType = instrumentedType;
                this.implementationContext = implementationContext;
                this.typePool = typePool;
                this.methods = methods;
                this.writerFlags = writerFlags;
                this.readerFlags = readerFlags;
            }

            @Override
            public MethodVisitor visitMethod(int modifiers, String internalName, String descriptor, String signature, String[] exceptions) {
                MethodVisitor methodVisitor = super.visitMethod(modifiers, internalName, descriptor, signature, exceptions);
                MethodDescription methodDescription = methods.get(internalName + descriptor);
                if (methodVisitor != null && methodDescription != null) {
                    for (Entry entry : entries) {
                        if (entry.matches(methodDescription)) {
                            methodVisitor = entry.wrap(instrumentedType,
                                    methodDescription,
                                    methodVisitor,
                                    implementationContext,
                                    typePool,
                                    writerFlags,
                                    readerFlags);
                        }
                    }
                }
                return methodVisitor;
            }
        }
    }

    /**
     * An ordered, immutable chain of {@link AsmVisitorWrapper}s.
     */
    @HashCodeAndEqualsPlugin.Enhance
    class Compound implements AsmVisitorWrapper {

        /**
         * The class visitor wrappers that are represented by this chain in their order. This list must not be mutated.
         */
        private final List<AsmVisitorWrapper> asmVisitorWrappers;

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
            this.asmVisitorWrappers = new ArrayList<AsmVisitorWrapper>();
            for (AsmVisitorWrapper asmVisitorWrapper : asmVisitorWrappers) {
                if (asmVisitorWrapper instanceof Compound) {
                    this.asmVisitorWrappers.addAll(((Compound) asmVisitorWrapper).asmVisitorWrappers);
                } else if (!(asmVisitorWrapper instanceof NoOp)) {
                    this.asmVisitorWrappers.add(asmVisitorWrapper);
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        public int mergeWriter(int flags) {
            for (AsmVisitorWrapper asmVisitorWrapper : asmVisitorWrappers) {
                flags = asmVisitorWrapper.mergeWriter(flags);
            }
            return flags;
        }

        /**
         * {@inheritDoc}
         */
        public int mergeReader(int flags) {
            for (AsmVisitorWrapper asmVisitorWrapper : asmVisitorWrappers) {
                flags = asmVisitorWrapper.mergeReader(flags);
            }
            return flags;
        }

        /**
         * {@inheritDoc}
         */
        public ClassVisitor wrap(TypeDescription instrumentedType,
                                 ClassVisitor classVisitor,
                                 Implementation.Context implementationContext,
                                 TypePool typePool,
                                 FieldList<FieldDescription.InDefinedShape> fields,
                                 MethodList<?> methods,
                                 int writerFlags,
                                 int readerFlags) {
            for (AsmVisitorWrapper asmVisitorWrapper : asmVisitorWrappers) {
                classVisitor = asmVisitorWrapper.wrap(instrumentedType,
                        classVisitor,
                        implementationContext,
                        typePool,
                        fields,
                        methods,
                        writerFlags,
                        readerFlags);
            }
            return classVisitor;
        }
    }
}
