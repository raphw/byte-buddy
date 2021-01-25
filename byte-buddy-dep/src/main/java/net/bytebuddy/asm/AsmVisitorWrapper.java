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

/**
 * A class visitor wrapper is used in order to register an intermediate ASM {@link org.objectweb.asm.ClassVisitor} which
 * is applied to the main type created by a {@link net.bytebuddy.dynamic.DynamicType.Builder} but not
 * to any {@link net.bytebuddy.implementation.auxiliary.AuxiliaryType}s, if any. 类访问者包装器用于注册中间ASM {@link org.objectweb.asm.ClassVisitor} 应用于 {@link net.bytebuddy.dynamic.DynamicType.Builder} 但不是任何{@link net.bytebuddy.implementation.auxiliary.AuxiliaryType}, 如果有的话类访问者包装器用于注册中间ASM {@link org.objectweb.asm.ClassVisitor} 应用于 {@link net.bytebuddy.dynamic.DynamicType.Builder} 但不是任何{@link net.bytebuddy.implementation.auxiliary.AuxiliaryType}, 如果有的话
 */
public interface AsmVisitorWrapper {

    /**
     * Indicates that no flags should be set. 指示不应设置任何标志
     */
    int NO_FLAGS = 0;

    /**
     * Defines the flags that are provided to any {@code ClassWriter} when writing a class. Typically, this gives opportunity to instruct ASM
     * to compute stack map frames or the size of the local variables array and the operand stack. If no specific flags are required for
     * applying this wrapper, the given value is to be returned. 定义在编写类时提供给任何{@code ClassWriter}的标志。通常，这会让ASM有机会计算堆栈映射帧或局部变量数组和操作数堆栈的大小。如果应用此包装不需要特定的标志，则返回给定的值
     *
     * @param flags The currently set flags. This value should be combined (e.g. {@code flags | foo}) into the value that is returned by this wrapper.
     * @return The flags to be provided to the ASM {@code ClassWriter}.
     */
    int mergeWriter(int flags);

    /**
     * Defines the flags that are provided to any {@code ClassReader} when reading a class if applicable. Typically, this gives opportunity to
     * instruct ASM to expand or skip frames and to skip code and debug information. If no specific flags are required for applying this
     * wrapper, the given value is to be returned. 定义在读取类时提供给任何{@code ClassReader}的标志（如果适用）。通常，这样就有机会指示ASM展开或跳过帧，并跳过代码和调试信息。如果应用此包装不需要特定的标志，则返回给定的值
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
     * A class visitor wrapper that does not apply any changes. 不应用任何更改的类访问者包装
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
     * An abstract base implementation of an ASM visitor wrapper that does not set any flags. ASM访问者包装器的抽象基本实现，不设置任何标志
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
     * An ASM visitor wrapper that allows to wrap declared fields of the instrumented type with a {@link FieldVisitorWrapper}. ASM 访问者包装器，允许使用 {@link FieldVisitorWrapper} 包装已检测类型的声明字段
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
         * entries are applied before the given matcher is applied. 如果给定的字段匹配器匹配，则定义要应用的新字段访问者包装器。在应用给定的匹配器之前应用先前定义的条目
         *
         * @param matcher              The matcher to identify fields to be wrapped.
         * @param fieldVisitorWrappers The field visitor wrapper to be applied if the given matcher is matched.
         * @return A new ASM visitor wrapper that applied the given field visitor wrapper if the supplied matcher is matched. 如果提供的匹配器匹配，则应用给定字段访问器包装的新ASM访问器包装
         */
        public ForDeclaredFields field(ElementMatcher<? super FieldDescription.InDefinedShape> matcher, List<? extends FieldVisitorWrapper> fieldVisitorWrappers) {
            return new ForDeclaredFields(CompoundList.of(entries, new Entry(matcher, fieldVisitorWrappers)));
        }

        @Override
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
         * A field visitor wrapper that allows for wrapping a {@link FieldVisitor} defining a declared field. 一种字段访问者包装器，允许包装定义声明字段的{@link FieldVisitor}
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
         * An entry describing a field visitor wrapper paired with a matcher for fields to be wrapped. 描述字段访问者包装器的条目，与要包装的字段的匹配器配对
         */
        @HashCodeAndEqualsPlugin.Enhance
        protected static class Entry implements ElementMatcher<FieldDescription.InDefinedShape>, FieldVisitorWrapper {

            /**
             * The matcher to identify fields to be wrapped. 用于标识要包装的字段的匹配器
             */
            private final ElementMatcher<? super FieldDescription.InDefinedShape> matcher;

            /**
             * The field visitor wrapper to be applied if the given matcher is matched. 如果给定的匹配器匹配，则要应用的字段访问者包装器
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

            @Override
            public boolean matches(FieldDescription.InDefinedShape target) {
                return target != null && matcher.matches(target);
            }

            @Override
            public FieldVisitor wrap(TypeDescription instrumentedType, FieldDescription.InDefinedShape fieldDescription, FieldVisitor fieldVisitor) {
                for (FieldVisitorWrapper fieldVisitorWrapper : fieldVisitorWrappers) {
                    fieldVisitor = fieldVisitorWrapper.wrap(instrumentedType, fieldDescription, fieldVisitor);
                }
                return fieldVisitor;
            }
        }

        /**
         * A class visitor that applies the outer ASM visitor for identifying declared fields. 应用外部 ASM 访问器来标识声明字段的类访问器
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
                            fieldVisitor = entry.wrap(instrumentedType, fieldDescription, fieldVisitor); // 使用 entry.wrap 最终将 fieldVisitor 委托给 FieldVisitorWrapper 接口 所以 FieldVisitorWrapper 的子类只要实现 FieldVisitorWrapper 的 wrap 方法，在其中定义具体的修改逻辑
                        }
                    }
                }
                return fieldVisitor;
            }
        }
    }

    /**
     * <p>
     * An ASM visitor wrapper that allows to wrap <b>declared methods</b> of the instrumented type with a {@link MethodVisitorWrapper}. ASM访问者包装器，允许使用{@link MethodVisitorWrapper}包装已检测类型的已声明方法ASM访问者包装器，允许使用 {@link MethodVisitorWrapper} 包装已检测类型的已声明方法
     * </p>
     * <p>
     * Note: Inherited methods are <b>not</b> matched by this visitor, even if they are intercepted by a normal interception. 注意：此访问者不匹配继承的方法，即使它们被正常拦截拦截
     * </p>
     */
    @HashCodeAndEqualsPlugin.Enhance
    class ForDeclaredMethods implements AsmVisitorWrapper {

        /**
         * The list of entries that describe matched methods in their application order. 按应用程序顺序描述匹配方法的条目列表
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
         * entries are applied before the given matcher is applied. 如果给定的方法匹配器匹配，则定义要应用的新方法访问者包装器。在应用给定的匹配器之前应用先前定义的条目
         *
         * @param matcher              The matcher to identify methods to be wrapped.
         * @param methodVisitorWrapper The method visitor wrapper to be applied if the given matcher is matched.
         * @return A new ASM visitor wrapper that applied the given method visitor wrapper if the supplied matcher is matched.
         */
        public ForDeclaredMethods method(ElementMatcher<? super MethodDescription> matcher, MethodVisitorWrapper... methodVisitorWrapper) {
            return method(matcher, Arrays.asList(methodVisitorWrapper));
        }

        /**
         * Defines a new method visitor wrapper to be applied if the given method matcher is matched. Previously defined
         * entries are applied before the given matcher is applied. 如果给定的方法匹配器匹配，则定义要应用的新方法访问者包装器。在应用给定的匹配器之前应用先前定义的条目
         *
         * @param matcher               The matcher to identify methods to be wrapped.
         * @param methodVisitorWrappers The method visitor wrapper to be applied if the given matcher is matched.
         * @return A new ASM visitor wrapper that applied the given method visitor wrapper if the supplied matcher is matched.
         */
        public ForDeclaredMethods method(ElementMatcher<? super MethodDescription> matcher, List<? extends MethodVisitorWrapper> methodVisitorWrappers) {
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

        @Override
        public int mergeWriter(int flags) {
            return flags | writerFlags;
        }

        @Override
        public int mergeReader(int flags) {
            return flags | readerFlags;
        }

        @Override
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
         * An entry describing a method visitor wrapper paired with a matcher for fields to be wrapped. 描述方法访问者包装器的条目，与要包装的字段的匹配器配对
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

            @Override
            public boolean matches(MethodDescription target) {
                return target != null && matcher.matches(target);
            }

            @Override
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
         * A class visitor that applies the outer ASM visitor for identifying declared methods. 一个类访问者，它应用外部ASM访问者来标识声明的方法
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
             * A mapping of fields by their name. 按名称对字段进行映射
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
     * An ordered, immutable chain of {@link AsmVisitorWrapper}s. 有序不变的链式 {@link AsmVisitorWrapper}
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
