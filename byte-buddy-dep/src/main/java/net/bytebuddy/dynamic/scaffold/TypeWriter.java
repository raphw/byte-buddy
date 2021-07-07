package net.bytebuddy.dynamic.scaffold;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.annotation.AnnotationValue;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.modifier.ModifierContributor;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.PackageDescription;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.TypeResolutionStrategy;
import net.bytebuddy.dynamic.scaffold.inline.MethodRebaseResolver;
import net.bytebuddy.dynamic.scaffold.inline.RebaseImplementationTarget;
import net.bytebuddy.dynamic.scaffold.subclass.SubclassImplementationTarget;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.LoadedTypeInitializer;
import net.bytebuddy.implementation.attribute.*;
import net.bytebuddy.implementation.auxiliary.AuxiliaryType;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.assign.TypeCasting;
import net.bytebuddy.implementation.bytecode.constant.DefaultValue;
import net.bytebuddy.implementation.bytecode.member.MethodInvocation;
import net.bytebuddy.implementation.bytecode.member.MethodReturn;
import net.bytebuddy.implementation.bytecode.member.MethodVariableAccess;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.utility.CompoundList;
import net.bytebuddy.utility.OpenedClassReader;
import net.bytebuddy.utility.privilege.GetSystemPropertyAction;
import net.bytebuddy.utility.visitor.MetadataAwareClassVisitor;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.SimpleRemapper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.*;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * A type writer is a utility for writing an actual class file using the ASM library. 类型编写器是使用 ASM 库编写实际类文件的实用工具
 *
 * @param <T> The best known loaded type for the dynamically created type. 动态创建的类型的最著名的加载类型
 */
public interface TypeWriter<T> {

    /**
     * A system property that indicates a folder for Byte Buddy to dump class files of all types that it creates.
     * If this property is not set, Byte Buddy does not dump any class files. This property is only read a single
     * time which is why it must be set on application start-up. 一种系统属性，表示字节伙伴用来转储其创建的所有类型的类文件的文件夹。如果未设置此属性，则 ByteBuddy 不会转储任何类文件。此属性只能读取一次，因此必须在应用程序启动时设置
     */
    String DUMP_PROPERTY = "net.bytebuddy.dump";

    /**
     * Creates the dynamic type that is described by this type writer. 创建此类型编写器描述的动态类型
     *
     * @param typeResolver The type resolution strategy to use.
     * @return An unloaded dynamic type that describes the created type.
     */
    DynamicType.Unloaded<T> make(TypeResolutionStrategy.Resolved typeResolver);

    /**
     * An field pool that allows a lookup for how to implement a field. 允许查找如何实现字段的字段池
     */
    interface FieldPool {

        /**
         * Returns the field attribute appender that matches a given field description or a default field
         * attribute appender if no appender was registered for the given field. 返回与给定字段描述匹配的字段属性appender，如果没有为给定字段注册appender，则返回默认字段属性appender
         *
         * @param fieldDescription The field description of interest.
         * @return The registered field attribute appender for the given field or the default appender if no such
         * appender was found.
         */
        Record target(FieldDescription fieldDescription);

        /**
         * An entry of a field pool that describes how a field is implemented. 字段池的一个条目，描述字段是如何实现的
         *
         * @see net.bytebuddy.dynamic.scaffold.TypeWriter.FieldPool
         */
        interface Record {

            /**
             * Determines if this record is implicit, i.e is not defined by a {@link FieldPool}. 确定此条目是否是隐式的，即不是由{@link FieldPool}定义的。
             *
             * @return {@code true} if this record is implicit.
             */
            boolean isImplicit();

            /**
             * Returns the field that this record represents.
             *
             * @return The field that this record represents.
             */
            FieldDescription getField();

            /**
             * Returns the field attribute appender for a given field.
             *
             * @return The attribute appender to be applied on the given field.
             */
            FieldAttributeAppender getFieldAppender();

            /**
             * Resolves the default value that this record represents. This is not possible for implicit records.
             *
             * @param defaultValue The default value that was defined previously or {@code null} if no default value is defined.
             * @return The default value for the represented field or {@code null} if no default value is to be defined.
             */
            Object resolveDefault(Object defaultValue);

            /**
             * Writes this entry to a given class visitor.
             *
             * @param classVisitor                 The class visitor to which this entry is to be written to.
             * @param annotationValueFilterFactory The annotation value filter factory to apply when writing annotations.
             */
            void apply(ClassVisitor classVisitor, AnnotationValueFilter.Factory annotationValueFilterFactory);

            /**
             * Applies this record to a field visitor. This is not possible for implicit records.
             *
             * @param fieldVisitor                 The field visitor onto which this record is to be applied.
             * @param annotationValueFilterFactory The annotation value filter factory to use for annotations.
             */
            void apply(FieldVisitor fieldVisitor, AnnotationValueFilter.Factory annotationValueFilterFactory);

            /**
             * A record for a simple field without a default value where all of the field's declared annotations are appended. 一个没有默认值的简单字段的条目，其中附加了该字段所有声明的注解
             */
            @HashCodeAndEqualsPlugin.Enhance
            class ForImplicitField implements Record {

                /**
                 * The implemented field. 实现的字段
                 */
                private final FieldDescription fieldDescription;

                /**
                 * Creates a new record for a simple field.
                 *
                 * @param fieldDescription The described field.
                 */
                public ForImplicitField(FieldDescription fieldDescription) {
                    this.fieldDescription = fieldDescription;
                }

                @Override
                public boolean isImplicit() {
                    return true;
                }

                @Override
                public FieldDescription getField() {
                    return fieldDescription;
                }

                @Override
                public FieldAttributeAppender getFieldAppender() {
                    throw new IllegalStateException("An implicit field record does not expose a field appender: " + this);
                }

                @Override
                public Object resolveDefault(Object defaultValue) {
                    throw new IllegalStateException("An implicit field record does not expose a default value: " + this);
                }

                @Override
                public void apply(ClassVisitor classVisitor, AnnotationValueFilter.Factory annotationValueFilterFactory) {
                    FieldVisitor fieldVisitor = classVisitor.visitField(fieldDescription.getActualModifiers(),
                            fieldDescription.getInternalName(),
                            fieldDescription.getDescriptor(),
                            fieldDescription.getGenericSignature(),
                            FieldDescription.NO_DEFAULT_VALUE);
                    if (fieldVisitor != null) {
                        FieldAttributeAppender.ForInstrumentedField.INSTANCE.apply(fieldVisitor,
                                fieldDescription,
                                annotationValueFilterFactory.on(fieldDescription));
                        fieldVisitor.visitEnd();
                    }
                }

                @Override
                public void apply(FieldVisitor fieldVisitor, AnnotationValueFilter.Factory annotationValueFilterFactory) {
                    throw new IllegalStateException("An implicit field record is not intended for partial application: " + this);
                }
            }

            /**
             * A record for a rich field with attributes and a potential default value. 具有属性和潜在默认值的丰富字段条目
             */
            @HashCodeAndEqualsPlugin.Enhance
            class ForExplicitField implements Record {

                /**
                 * The attribute appender for the field. 字段的属性追加器
                 */
                private final FieldAttributeAppender attributeAppender;

                /**
                 * The field's default value. 字段默认值
                 */
                private final Object defaultValue;

                /**
                 * The implemented field. 实现字段
                 */
                private final FieldDescription fieldDescription;

                /**
                 * Creates a record for a rich field.
                 *
                 * @param attributeAppender The attribute appender for the field.
                 * @param defaultValue      The field's default value.
                 * @param fieldDescription  The implemented field.
                 */
                public ForExplicitField(FieldAttributeAppender attributeAppender, Object defaultValue, FieldDescription fieldDescription) {
                    this.attributeAppender = attributeAppender;
                    this.defaultValue = defaultValue;
                    this.fieldDescription = fieldDescription;
                }

                @Override
                public boolean isImplicit() {
                    return false;
                }

                @Override
                public FieldDescription getField() {
                    return fieldDescription;
                }

                @Override
                public FieldAttributeAppender getFieldAppender() {
                    return attributeAppender;
                }

                @Override
                public Object resolveDefault(Object defaultValue) {
                    return this.defaultValue == null
                            ? defaultValue
                            : this.defaultValue;
                }

                @Override
                public void apply(ClassVisitor classVisitor, AnnotationValueFilter.Factory annotationValueFilterFactory) {
                    FieldVisitor fieldVisitor = classVisitor.visitField(fieldDescription.getActualModifiers(),
                            fieldDescription.getInternalName(),
                            fieldDescription.getDescriptor(),
                            fieldDescription.getGenericSignature(),
                            resolveDefault(FieldDescription.NO_DEFAULT_VALUE));
                    if (fieldVisitor != null) {
                        attributeAppender.apply(fieldVisitor, fieldDescription, annotationValueFilterFactory.on(fieldDescription));
                        fieldVisitor.visitEnd();
                    }
                }

                @Override
                public void apply(FieldVisitor fieldVisitor, AnnotationValueFilter.Factory annotationValueFilterFactory) {
                    attributeAppender.apply(fieldVisitor, fieldDescription, annotationValueFilterFactory.on(fieldDescription));
                }
            }
        }

        /**
         * A field pool that does not allow any look ups.
         */
        enum Disabled implements FieldPool {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public Record target(FieldDescription fieldDescription) {
                throw new IllegalStateException("Cannot look up field from disabld pool");
            }
        }
    }

    /**
     * An method pool that allows a lookup for how to implement a method. 允许查找如何实现方法的方法池
     */
    interface MethodPool {

        /**
         * Looks up a handler entry for a given method. 查找给定方法的处理程序条目
         *
         * @param methodDescription The method being processed. 正在处理的方法
         * @return A handler entry for the given method. 给定方法的处理程序项
         */
        Record target(MethodDescription methodDescription);

        /**
         * An entry of a method pool that describes how a method is implemented. 方法池的一个条目，描述如何实现一个方法
         *
         * @see net.bytebuddy.dynamic.scaffold.TypeWriter.MethodPool
         */
        interface Record {

            /**
             * Returns the sort of this method instrumentation. 返回此方法的类型
             *
             * @return The sort of this method instrumentation. 这种方法的类型
             */
            Sort getSort();

            /**
             * Returns the method that is implemented where the returned method resembles a potential transformation. An implemented
             * method is only defined if a method is not {@link Record.Sort#SKIPPED}. 返回在返回的方法类似于潜在转换的情况下实现的方法。只有在方法没有 {@link Record.Sort#SKIPPED} 时才定义实现的方法
             *
             * @return The implemented method.
             */
            MethodDescription getMethod();

            /**
             * The visibility to enforce for this method. 此方法要强制的可见性
             *
             * @return The visibility to enforce for this method. 此方法要强制的可见性
             */
            Visibility getVisibility();

            /**
             * Prepends the given method appender to this entry. 将给定的方法追加器前置到此项，根据积累的字节码附加器完成对应记录的字节码的写入
             *
             * @param byteCodeAppender The byte code appender to prepend. 要前置的字节码追加器
             * @return This entry with the given code prepended. 此条目前面带有给定代码
             */
            Record prepend(ByteCodeAppender byteCodeAppender);

            /**
             * Applies this method entry. This method can always be called and might be a no-op. 应用此方法项。此方法总是可以调用的，并且可能是禁止操作的
             *
             * @param classVisitor                 The class visitor to which this entry should be applied.
             * @param implementationContext        The implementation context to which this entry should be applied.
             * @param annotationValueFilterFactory The annotation value filter factory to apply when writing annotations.
             */
            void apply(ClassVisitor classVisitor, Implementation.Context implementationContext, AnnotationValueFilter.Factory annotationValueFilterFactory);

            /**
             * Applies the head of this entry. Applying an entry is only possible if a method is defined, i.e. the sort of this entry is not
             * {@link Record.Sort#SKIPPED}. 应用此条目的标题。只有在定义了方法的情况下才能应用条目，即该条目的类型不是 {@link Record.Sort#SKIPPED}
             *
             * @param methodVisitor The method visitor to which this entry should be applied.
             */
            void applyHead(MethodVisitor methodVisitor);

            /**
             * Applies the body of this entry. Applying the body of an entry is only possible if a method is implemented, i.e. the sort of this
             * entry is {@link Record.Sort#IMPLEMENTED}. 应用此条目的正文。只有在实现了一个方法的情况下才能应用条目的主体，也就是说，这个条目的类型是{@link Record.Sort#IMPLEMENTED}
             *
             * @param methodVisitor                The method visitor to which this entry should be applied.
             * @param implementationContext        The implementation context to which this entry should be applied.
             * @param annotationValueFilterFactory The annotation value filter factory to apply when writing annotations.
             */
            void applyBody(MethodVisitor methodVisitor, Implementation.Context implementationContext, AnnotationValueFilter.Factory annotationValueFilterFactory);

            /**
             * Applies the attributes of this entry. Applying the body of an entry is only possible if a method is implemented, i.e. the sort of this
             * entry is {@link Record.Sort#DEFINED}.
             *
             * @param methodVisitor                The method visitor to which this entry should be applied.
             * @param annotationValueFilterFactory The annotation value filter factory to apply when writing annotations.
             */
            void applyAttributes(MethodVisitor methodVisitor, AnnotationValueFilter.Factory annotationValueFilterFactory);

            /**
             * Applies the code of this entry. Applying the body of an entry is only possible if a method is implemented, i.e. the sort of this
             * entry is {@link Record.Sort#IMPLEMENTED}.
             *
             * @param methodVisitor         The method visitor to which this entry should be applied.
             * @param implementationContext The implementation context to which this entry should be applied.
             * @return The size requirements of the implemented code.
             */
            ByteCodeAppender.Size applyCode(MethodVisitor methodVisitor, Implementation.Context implementationContext);

            /**
             * The sort of an entry. 条目的类型
             */
            enum Sort {

                /**
                 * Describes a method that should not be implemented or retained in its original state. 描述不应在其原始状态下实现或保留的方法
                 */
                SKIPPED(false, false),

                /**
                 * Describes a method that should be defined but is abstract or native, i.e. does not define any byte code. 描述一个应该定义但抽象或本机的方法，即不定义任何字节码
                 */
                DEFINED(true, false),

                /**
                 * Describes a method that is implemented in byte code. 描述用字节码实现的方法
                 */
                IMPLEMENTED(true, true);

                /**
                 * Indicates if this sort defines a method, with or without byte code. 指示此类型是否定义方法（带或不带字节码）
                 */
                private final boolean define;

                /**
                 * Indicates if this sort defines byte code. 指示此类型是否定义字节码
                 */
                private final boolean implement;

                /**
                 * Creates a new sort.
                 *
                 * @param define    Indicates if this sort defines a method, with or without byte code.
                 * @param implement Indicates if this sort defines byte code.
                 */
                Sort(boolean define, boolean implement) {
                    this.define = define;
                    this.implement = implement;
                }

                /**
                 * Indicates if this sort defines a method, with or without byte code.
                 *
                 * @return {@code true} if this sort defines a method, with or without byte code.
                 */
                public boolean isDefined() {
                    return define;
                }

                /**
                 * Indicates if this sort defines byte code.
                 *
                 * @return {@code true} if this sort defines byte code.
                 */
                public boolean isImplemented() {
                    return implement;
                }
            }

            /**
             * A canonical implementation of a method that is not declared but inherited by the instrumented type. 方法的规范实现，该方法未声明，但由插桩类继承
             */
            @HashCodeAndEqualsPlugin.Enhance
            class ForNonImplementedMethod implements Record {

                /**
                 * The undefined method.
                 */
                private final MethodDescription methodDescription;

                /**
                 * Creates a new undefined record.
                 *
                 * @param methodDescription The undefined method.
                 */
                public ForNonImplementedMethod(MethodDescription methodDescription) {
                    this.methodDescription = methodDescription;
                }

                @Override
                public void apply(ClassVisitor classVisitor, Implementation.Context implementationContext, AnnotationValueFilter.Factory annotationValueFilterFactory) {
                    /* do nothing */
                }

                @Override
                public void applyBody(MethodVisitor methodVisitor, Implementation.Context implementationContext, AnnotationValueFilter.Factory annotationValueFilterFactory) {
                    throw new IllegalStateException("Cannot apply body for non-implemented method on " + methodDescription);
                }

                @Override
                public void applyAttributes(MethodVisitor methodVisitor, AnnotationValueFilter.Factory annotationValueFilterFactory) {
                    /* do nothing */
                }

                @Override
                public ByteCodeAppender.Size applyCode(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
                    throw new IllegalStateException("Cannot apply code for non-implemented method on " + methodDescription);
                }

                @Override
                public void applyHead(MethodVisitor methodVisitor) {
                    throw new IllegalStateException("Cannot apply head for non-implemented method on " + methodDescription);
                }

                @Override
                public MethodDescription getMethod() {
                    return methodDescription;
                }

                @Override
                public Visibility getVisibility() {
                    return methodDescription.getVisibility();
                }

                @Override
                public Sort getSort() {
                    return Sort.SKIPPED;
                }

                @Override
                public Record prepend(ByteCodeAppender byteCodeAppender) {
                    return new ForDefinedMethod.WithBody(methodDescription, new ByteCodeAppender.Compound(byteCodeAppender,
                            new ByteCodeAppender.Simple(DefaultValue.of(methodDescription.getReturnType()), MethodReturn.of(methodDescription.getReturnType()))));
                }
            }

            /**
             * A base implementation of an abstract entry that defines a method. 定义方法的抽象项的基实现
             */
            abstract class ForDefinedMethod implements Record {

                @Override
                public void apply(ClassVisitor classVisitor, Implementation.Context implementationContext, AnnotationValueFilter.Factory annotationValueFilterFactory) {
                    MethodVisitor methodVisitor = classVisitor.visitMethod(getMethod().getActualModifiers(getSort().isImplemented(), getVisibility()),
                            getMethod().getInternalName(),
                            getMethod().getDescriptor(),
                            getMethod().getGenericSignature(),
                            getMethod().getExceptionTypes().asErasures().toInternalNames());
                    if (methodVisitor != null) {
                        ParameterList<?> parameterList = getMethod().getParameters();
                        if (parameterList.hasExplicitMetaData()) {
                            for (ParameterDescription parameterDescription : parameterList) {
                                methodVisitor.visitParameter(parameterDescription.getName(), parameterDescription.getModifiers());
                            }
                        }
                        applyHead(methodVisitor);
                        applyBody(methodVisitor, implementationContext, annotationValueFilterFactory);
                        methodVisitor.visitEnd();
                    }
                }

                /**
                 * Describes an entry that defines a method as byte code. 描述将方法定义为字节码的条目
                 */
                @HashCodeAndEqualsPlugin.Enhance
                public static class WithBody extends ForDefinedMethod {

                    /**
                     * The implemented method. 要实现的方法
                     */
                    private final MethodDescription methodDescription;

                    /**
                     * The byte code appender to apply.  要应用的字节码追加器
                     */
                    private final ByteCodeAppender byteCodeAppender;

                    /**
                     * The method attribute appender to apply. 要应用的方法属性附加器
                     */
                    private final MethodAttributeAppender methodAttributeAppender;

                    /**
                     * The represented method's minimum visibility. 表示方法的最小可见性
                     */
                    private final Visibility visibility;

                    /**
                     * Creates a new record for an implemented method without attributes or a modifier resolver. 为没有属性或修饰符解析器的已实现方法创建新条目
                     *
                     * @param methodDescription The implemented method.
                     * @param byteCodeAppender  The byte code appender to apply.
                     */
                    public WithBody(MethodDescription methodDescription, ByteCodeAppender byteCodeAppender) {
                        this(methodDescription, byteCodeAppender, MethodAttributeAppender.NoOp.INSTANCE, methodDescription.getVisibility());
                    }

                    /**
                     * Creates a new entry for a method that defines a method as byte code. 为将方法定义为字节码的方法创建新条目
                     *
                     * @param methodDescription       The implemented method.
                     * @param byteCodeAppender        The byte code appender to apply.
                     * @param methodAttributeAppender The method attribute appender to apply.
                     * @param visibility              The represented method's minimum visibility.
                     */
                    public WithBody(MethodDescription methodDescription,
                                    ByteCodeAppender byteCodeAppender,
                                    MethodAttributeAppender methodAttributeAppender,
                                    Visibility visibility) {
                        this.methodDescription = methodDescription;
                        this.byteCodeAppender = byteCodeAppender;
                        this.methodAttributeAppender = methodAttributeAppender;
                        this.visibility = visibility;
                    }

                    @Override
                    public MethodDescription getMethod() {
                        return methodDescription;
                    }

                    @Override
                    public Sort getSort() {
                        return Sort.IMPLEMENTED;
                    }

                    @Override
                    public Visibility getVisibility() {
                        return visibility;
                    }

                    @Override
                    public void applyHead(MethodVisitor methodVisitor) {
                        /* do nothing */
                    }

                    @Override
                    public void applyBody(MethodVisitor methodVisitor, Implementation.Context implementationContext, AnnotationValueFilter.Factory annotationValueFilterFactory) {
                        applyAttributes(methodVisitor, annotationValueFilterFactory);
                        methodVisitor.visitCode();
                        ByteCodeAppender.Size size = applyCode(methodVisitor, implementationContext);
                        methodVisitor.visitMaxs(size.getOperandStackSize(), size.getLocalVariableSize());
                    }

                    @Override
                    public void applyAttributes(MethodVisitor methodVisitor, AnnotationValueFilter.Factory annotationValueFilterFactory) {
                        methodAttributeAppender.apply(methodVisitor, methodDescription, annotationValueFilterFactory.on(methodDescription));
                    }

                    @Override
                    public ByteCodeAppender.Size applyCode(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
                        return byteCodeAppender.apply(methodVisitor, implementationContext, methodDescription);
                    }

                    @Override
                    public Record prepend(ByteCodeAppender byteCodeAppender) {
                        return new WithBody(methodDescription,
                                new ByteCodeAppender.Compound(byteCodeAppender, this.byteCodeAppender),
                                methodAttributeAppender,
                                visibility);
                    }
                }

                /**
                 * Describes an entry that defines a method but without byte code and without an annotation value. 描述定义方法但没有字节码和注释值的项
                 */
                @HashCodeAndEqualsPlugin.Enhance
                public static class WithoutBody extends ForDefinedMethod {

                    /**
                     * The implemented method.
                     */
                    private final MethodDescription methodDescription;

                    /**
                     * The method attribute appender to apply.
                     */
                    private final MethodAttributeAppender methodAttributeAppender;

                    /**
                     * The represented method's minimum visibility.
                     */
                    private final Visibility visibility;

                    /**
                     * Creates a new entry for a method that is defines but does not append byte code, i.e. is native or abstract.
                     *
                     * @param methodDescription       The implemented method.
                     * @param methodAttributeAppender The method attribute appender to apply.
                     * @param visibility              The represented method's minimum visibility.
                     */
                    public WithoutBody(MethodDescription methodDescription, MethodAttributeAppender methodAttributeAppender, Visibility visibility) {
                        this.methodDescription = methodDescription;
                        this.methodAttributeAppender = methodAttributeAppender;
                        this.visibility = visibility;
                    }

                    @Override
                    public MethodDescription getMethod() {
                        return methodDescription;
                    }

                    @Override
                    public Sort getSort() {
                        return Sort.DEFINED;
                    }

                    @Override
                    public Visibility getVisibility() {
                        return visibility;
                    }

                    @Override
                    public void applyHead(MethodVisitor methodVisitor) {
                        /* do nothing */
                    }

                    @Override
                    public void applyBody(MethodVisitor methodVisitor, Implementation.Context implementationContext, AnnotationValueFilter.Factory annotationValueFilterFactory) {
                        applyAttributes(methodVisitor, annotationValueFilterFactory);
                    }

                    @Override
                    public void applyAttributes(MethodVisitor methodVisitor, AnnotationValueFilter.Factory annotationValueFilterFactory) {
                        methodAttributeAppender.apply(methodVisitor, methodDescription, annotationValueFilterFactory.on(methodDescription));
                    }

                    @Override
                    public ByteCodeAppender.Size applyCode(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
                        throw new IllegalStateException("Cannot apply code for abstract method on " + methodDescription);
                    }

                    @Override
                    public Record prepend(ByteCodeAppender byteCodeAppender) {
                        throw new IllegalStateException("Cannot prepend code for abstract method on " + methodDescription);
                    }
                }

                /**
                 * Describes an entry that defines a method with a default annotation value. 描述用默认注释值定义方法的条目
                 */
                @HashCodeAndEqualsPlugin.Enhance
                public static class WithAnnotationDefaultValue extends ForDefinedMethod {

                    /**
                     * The implemented method.
                     */
                    private final MethodDescription methodDescription;

                    /**
                     * The annotation value to define.
                     */
                    private final AnnotationValue<?, ?> annotationValue;

                    /**
                     * The method attribute appender to apply.
                     */
                    private final MethodAttributeAppender methodAttributeAppender;

                    /**
                     * Creates a new entry for defining a method with a default annotation value.
                     *
                     * @param methodDescription       The implemented method.
                     * @param annotationValue         The annotation value to define.
                     * @param methodAttributeAppender The method attribute appender to apply.
                     */
                    public WithAnnotationDefaultValue(MethodDescription methodDescription,
                                                      AnnotationValue<?, ?> annotationValue,
                                                      MethodAttributeAppender methodAttributeAppender) {
                        this.methodDescription = methodDescription;
                        this.annotationValue = annotationValue;
                        this.methodAttributeAppender = methodAttributeAppender;
                    }

                    @Override
                    public MethodDescription getMethod() {
                        return methodDescription;
                    }

                    @Override
                    public Sort getSort() {
                        return Sort.DEFINED;
                    }

                    @Override
                    public Visibility getVisibility() {
                        return methodDescription.getVisibility();
                    }

                    @Override
                    public void applyHead(MethodVisitor methodVisitor) {
                        if (!methodDescription.isDefaultValue(annotationValue)) {
                            throw new IllegalStateException("Cannot set " + annotationValue + " as default for " + methodDescription);
                        }
                        AnnotationVisitor annotationVisitor = methodVisitor.visitAnnotationDefault();
                        AnnotationAppender.Default.apply(annotationVisitor,
                                methodDescription.getReturnType().asErasure(),
                                AnnotationAppender.NO_NAME,
                                annotationValue.resolve());
                        annotationVisitor.visitEnd();
                    }

                    @Override
                    public void applyBody(MethodVisitor methodVisitor, Implementation.Context implementationContext, AnnotationValueFilter.Factory annotationValueFilterFactory) {
                        methodAttributeAppender.apply(methodVisitor, methodDescription, annotationValueFilterFactory.on(methodDescription));
                    }

                    @Override
                    public void applyAttributes(MethodVisitor methodVisitor, AnnotationValueFilter.Factory annotationValueFilterFactory) {
                        throw new IllegalStateException("Cannot apply attributes for default value on " + methodDescription);
                    }

                    @Override
                    public ByteCodeAppender.Size applyCode(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
                        throw new IllegalStateException("Cannot apply code for default value on " + methodDescription);
                    }

                    @Override
                    public Record prepend(ByteCodeAppender byteCodeAppender) {
                        throw new IllegalStateException("Cannot prepend code for default value on " + methodDescription);
                    }
                }

                /**
                 * A record for a visibility bridge.
                 */
                @HashCodeAndEqualsPlugin.Enhance
                public static class OfVisibilityBridge extends ForDefinedMethod implements ByteCodeAppender {

                    /**
                     * The visibility bridge.
                     */
                    private final MethodDescription visibilityBridge;

                    /**
                     * The method the visibility bridge invokes.
                     */
                    private final MethodDescription bridgeTarget;

                    /**
                     * The type on which the bridge method is invoked.
                     */
                    private final TypeDescription bridgeType;

                    /**
                     * The attribute appender to apply to the visibility bridge.
                     */
                    private final MethodAttributeAppender attributeAppender;

                    /**
                     * Creates a new record for a visibility bridge.
                     *
                     * @param visibilityBridge  The visibility bridge.
                     * @param bridgeTarget      The method the visibility bridge invokes.
                     * @param bridgeType        The type of the instrumented type.
                     * @param attributeAppender The attribute appender to apply to the visibility bridge.
                     */
                    protected OfVisibilityBridge(MethodDescription visibilityBridge,
                                                 MethodDescription bridgeTarget,
                                                 TypeDescription bridgeType,
                                                 MethodAttributeAppender attributeAppender) {
                        this.visibilityBridge = visibilityBridge;
                        this.bridgeTarget = bridgeTarget;
                        this.bridgeType = bridgeType;
                        this.attributeAppender = attributeAppender;
                    }

                    /**
                     * Creates a record for a visibility bridge.
                     *
                     * @param instrumentedType  The instrumented type.
                     * @param bridgeTarget      The target method of the visibility bridge.
                     * @param attributeAppender The attribute appender to apply to the visibility bridge.
                     * @return A record describing the visibility bridge.
                     */
                    public static Record of(TypeDescription instrumentedType, MethodDescription bridgeTarget, MethodAttributeAppender attributeAppender) {
                        // Default method bridges must be dispatched on an implemented interface type, not considering the declaring type.
                        TypeDefinition bridgeType = null;
                        if (bridgeTarget.isDefaultMethod()) {
                            TypeDescription declaringType = bridgeTarget.getDeclaringType().asErasure();
                            for (TypeDescription interfaceType : instrumentedType.getInterfaces().asErasures().filter(isSubTypeOf(declaringType))) {
                                if (bridgeType == null || declaringType.isAssignableTo(bridgeType.asErasure())) {
                                    bridgeType = interfaceType;
                                }
                            }
                        }
                        // Non-default method or default method that is inherited by a super class.
                        if (bridgeType == null) {
                            bridgeType = instrumentedType.getSuperClass();
                        }
                        return new OfVisibilityBridge(new VisibilityBridge(instrumentedType, bridgeTarget),
                                bridgeTarget,
                                bridgeType.asErasure(),
                                attributeAppender);
                    }

                    @Override
                    public MethodDescription getMethod() {
                        return visibilityBridge;
                    }

                    @Override
                    public Sort getSort() {
                        return Sort.IMPLEMENTED;
                    }

                    @Override
                    public Visibility getVisibility() {
                        return bridgeTarget.getVisibility();
                    }

                    @Override
                    public Record prepend(ByteCodeAppender byteCodeAppender) {
                        return new ForDefinedMethod.WithBody(visibilityBridge,
                                new ByteCodeAppender.Compound(this, byteCodeAppender),
                                attributeAppender,
                                bridgeTarget.getVisibility());
                    }

                    @Override
                    public void applyHead(MethodVisitor methodVisitor) {
                        /* do nothing */
                    }

                    @Override
                    public void applyBody(MethodVisitor methodVisitor, Implementation.Context implementationContext, AnnotationValueFilter.Factory annotationValueFilterFactory) {
                        applyAttributes(methodVisitor, annotationValueFilterFactory);
                        methodVisitor.visitCode();
                        ByteCodeAppender.Size size = applyCode(methodVisitor, implementationContext);
                        methodVisitor.visitMaxs(size.getOperandStackSize(), size.getLocalVariableSize());
                    }

                    @Override
                    public void applyAttributes(MethodVisitor methodVisitor, AnnotationValueFilter.Factory annotationValueFilterFactory) {
                        attributeAppender.apply(methodVisitor, visibilityBridge, annotationValueFilterFactory.on(visibilityBridge));
                    }

                    @Override
                    public Size applyCode(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
                        return apply(methodVisitor, implementationContext, visibilityBridge);
                    }

                    @Override
                    public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext, MethodDescription instrumentedMethod) {
                        return new ByteCodeAppender.Simple(
                                MethodVariableAccess.allArgumentsOf(instrumentedMethod).prependThisReference(),
                                MethodInvocation.invoke(bridgeTarget).special(bridgeType),
                                MethodReturn.of(instrumentedMethod.getReturnType())
                        ).apply(methodVisitor, implementationContext, instrumentedMethod);
                    }

                    /**
                     * A method describing a visibility bridge. 一种描述可视桥的方法
                     */
                    protected static class VisibilityBridge extends MethodDescription.InDefinedShape.AbstractBase {

                        /**
                         * The instrumented type.
                         */
                        private final TypeDescription instrumentedType;

                        /**
                         * The method that is the target of the bridge. 桥梁的目标方法
                         */
                        private final MethodDescription bridgeTarget;

                        /**
                         * Creates a new visibility bridge.
                         *
                         * @param instrumentedType The instrumented type.
                         * @param bridgeTarget     The method that is the target of the bridge.
                         */
                        protected VisibilityBridge(TypeDescription instrumentedType, MethodDescription bridgeTarget) {
                            this.instrumentedType = instrumentedType;
                            this.bridgeTarget = bridgeTarget;
                        }

                        @Override
                        public TypeDescription getDeclaringType() {
                            return instrumentedType;
                        }

                        @Override
                        public ParameterList<ParameterDescription.InDefinedShape> getParameters() {
                            return new ParameterList.Explicit.ForTypes(this, bridgeTarget.getParameters().asTypeList().asRawTypes());
                        }

                        @Override
                        public TypeDescription.Generic getReturnType() {
                            return bridgeTarget.getReturnType().asRawType();
                        }

                        @Override
                        public TypeList.Generic getExceptionTypes() {
                            return bridgeTarget.getExceptionTypes().asRawTypes();
                        }

                        @Override
                        public AnnotationValue<?, ?> getDefaultValue() {
                            return AnnotationValue.UNDEFINED;
                        }

                        @Override
                        public TypeList.Generic getTypeVariables() {
                            return new TypeList.Generic.Empty();
                        }

                        @Override
                        public AnnotationList getDeclaredAnnotations() {
                            return bridgeTarget.getDeclaredAnnotations();
                        }

                        @Override
                        public int getModifiers() {
                            return (bridgeTarget.getModifiers() | Opcodes.ACC_SYNTHETIC | Opcodes.ACC_BRIDGE) & ~Opcodes.ACC_NATIVE;
                        }

                        @Override
                        public String getInternalName() {
                            return bridgeTarget.getName();
                        }
                    }
                }
            }

            /** 附加方法实现的访问器桥的包装器
             * A wrapper that appends accessor bridges for a method's implementation. The bridges are only added if
             * {@link net.bytebuddy.dynamic.scaffold.TypeWriter.MethodPool.Record#apply(ClassVisitor, Implementation.Context, AnnotationValueFilter.Factory)}
             * is invoked such that bridges are not appended for methods that are rebased or redefined as such types already have bridge methods in place. 只有当 {@link net.bytebuddy.dynamic.scaffold.TypeWriter.MethodPool.Record#apply(ClassVisitor, Implementation.Context, AnnotationValueFilter.Factory)} 被调用时，桥才会被添加，这样桥就不会被附加到重基或重定义的方法上，因为这些类型已经有了桥方法
             */
            @HashCodeAndEqualsPlugin.Enhance
            class AccessBridgeWrapper implements Record {

                /**
                 * The delegate for implementing the bridge's target. 执行桥目标的代表
                 */
                private final Record delegate;

                /**
                 * The instrumented type that defines the bridge methods and the bridge target. 定义桥方法和桥目标的插桩类型
                 */
                private final TypeDescription instrumentedType;

                /**
                 * The target of the bridge method. 桥方法的目标
                 */
                private final MethodDescription bridgeTarget;

                /**
                 * A collection of all tokens representing all bridge methods. 表示所有桥方法的所有标记集合
                 */
                private final Set<MethodDescription.TypeToken> bridgeTypes;

                /**
                 * The attribute appender being applied for the bridge target. 应用于网桥目标的属性附加器
                 */
                private final MethodAttributeAppender attributeAppender;

                /**
                 * Creates a wrapper for adding accessor bridges. 创建用于添加访问器桥的包装器
                 *
                 * @param delegate          The delegate for implementing the bridge's target.
                 * @param instrumentedType  The instrumented type that defines the bridge methods and the bridge target.
                 * @param bridgeTarget      The target of the bridge method.
                 * @param bridgeTypes       A collection of all tokens representing all bridge methods.
                 * @param attributeAppender The attribute appender being applied for the bridge target.
                 */
                protected AccessBridgeWrapper(Record delegate,
                                              TypeDescription instrumentedType,
                                              MethodDescription bridgeTarget,
                                              Set<MethodDescription.TypeToken> bridgeTypes,
                                              MethodAttributeAppender attributeAppender) {
                    this.delegate = delegate;
                    this.instrumentedType = instrumentedType;
                    this.bridgeTarget = bridgeTarget;
                    this.bridgeTypes = bridgeTypes;
                    this.attributeAppender = attributeAppender;
                }

                /**
                 * Wraps the given record in an accessor bridge wrapper if necessary. 如有必要，将给定记录包装在访问桥包装器中
                 *
                 * @param delegate          The delegate for implementing the bridge's target.
                 * @param instrumentedType  The instrumented type that defines the bridge methods and the bridge target.
                 * @param bridgeTarget      The bridge methods' target methods.
                 * @param bridgeTypes       A collection of all tokens representing all bridge methods. 表示所有桥接方法的所有标记的集合
                 * @param attributeAppender The attribute appender being applied for the bridge target.
                 * @return The given record wrapped by a bridge method wrapper if necessary.
                 */
                public static Record of(Record delegate,
                                        TypeDescription instrumentedType,
                                        MethodDescription bridgeTarget,
                                        Set<MethodDescription.TypeToken> bridgeTypes,
                                        MethodAttributeAppender attributeAppender) {
                    Set<MethodDescription.TypeToken> compatibleBridgeTypes = new HashSet<MethodDescription.TypeToken>();
                    for (MethodDescription.TypeToken bridgeType : bridgeTypes) {
                        if (bridgeTarget.isBridgeCompatible(bridgeType)) {
                            compatibleBridgeTypes.add(bridgeType);
                        }
                    }
                    return compatibleBridgeTypes.isEmpty() || (instrumentedType.isInterface() && !delegate.getSort().isImplemented())
                            ? delegate
                            : new AccessBridgeWrapper(delegate, instrumentedType, bridgeTarget, compatibleBridgeTypes, attributeAppender);
                }

                @Override
                public Sort getSort() {
                    return delegate.getSort();
                }

                @Override
                public MethodDescription getMethod() {
                    return bridgeTarget;
                }

                @Override
                public Visibility getVisibility() {
                    return delegate.getVisibility();
                }

                @Override
                public Record prepend(ByteCodeAppender byteCodeAppender) {
                    return new AccessBridgeWrapper(delegate.prepend(byteCodeAppender), instrumentedType, bridgeTarget, bridgeTypes, attributeAppender);
                }

                @Override
                public void apply(ClassVisitor classVisitor,
                                  Implementation.Context implementationContext,
                                  AnnotationValueFilter.Factory annotationValueFilterFactory) {
                    delegate.apply(classVisitor, implementationContext, annotationValueFilterFactory);
                    for (MethodDescription.TypeToken bridgeType : bridgeTypes) {
                        MethodDescription.InDefinedShape bridgeMethod = new AccessorBridge(bridgeTarget, bridgeType, instrumentedType);
                        MethodDescription.InDefinedShape bridgeTarget = new BridgeTarget(this.bridgeTarget, instrumentedType);
                        MethodVisitor methodVisitor = classVisitor.visitMethod(bridgeMethod.getActualModifiers(true, getVisibility()),
                                bridgeMethod.getInternalName(),
                                bridgeMethod.getDescriptor(),
                                MethodDescription.NON_GENERIC_SIGNATURE,
                                bridgeMethod.getExceptionTypes().asErasures().toInternalNames());
                        if (methodVisitor != null) {
                            attributeAppender.apply(methodVisitor, bridgeMethod, annotationValueFilterFactory.on(instrumentedType));
                            methodVisitor.visitCode();
                            ByteCodeAppender.Size size = new ByteCodeAppender.Simple(
                                    MethodVariableAccess.allArgumentsOf(bridgeMethod).asBridgeOf(bridgeTarget).prependThisReference(),
                                    MethodInvocation.invoke(bridgeTarget).virtual(instrumentedType),
                                    bridgeTarget.getReturnType().asErasure().isAssignableTo(bridgeMethod.getReturnType().asErasure())
                                            ? StackManipulation.Trivial.INSTANCE
                                            : TypeCasting.to(bridgeMethod.getReturnType().asErasure()),
                                    MethodReturn.of(bridgeMethod.getReturnType())
                            ).apply(methodVisitor, implementationContext, bridgeMethod);
                            methodVisitor.visitMaxs(size.getOperandStackSize(), size.getLocalVariableSize());
                            methodVisitor.visitEnd();
                        }
                    }
                }

                @Override
                public void applyHead(MethodVisitor methodVisitor) {
                    delegate.applyHead(methodVisitor);
                }

                @Override
                public void applyBody(MethodVisitor methodVisitor,
                                      Implementation.Context implementationContext,
                                      AnnotationValueFilter.Factory annotationValueFilterFactory) {
                    delegate.applyBody(methodVisitor, implementationContext, annotationValueFilterFactory);
                }

                @Override
                public void applyAttributes(MethodVisitor methodVisitor, AnnotationValueFilter.Factory annotationValueFilterFactory) {
                    delegate.applyAttributes(methodVisitor, annotationValueFilterFactory);
                }

                @Override
                public ByteCodeAppender.Size applyCode(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
                    return delegate.applyCode(methodVisitor, implementationContext);
                }

                /**
                 * A method representing an accessor bridge method.
                 */
                protected static class AccessorBridge extends MethodDescription.InDefinedShape.AbstractBase {

                    /**
                     * The target method of the bridge.
                     */
                    private final MethodDescription bridgeTarget;

                    /**
                     * The bridge's type token.
                     */
                    private final MethodDescription.TypeToken bridgeType;

                    /**
                     * The instrumented type defining the bridge target.
                     */
                    private final TypeDescription instrumentedType;

                    /**
                     * Creates a new accessor bridge method.
                     *
                     * @param bridgeTarget     The target method of the bridge.
                     * @param bridgeType       The bridge's type token.
                     * @param instrumentedType The instrumented type defining the bridge target.
                     */
                    protected AccessorBridge(MethodDescription bridgeTarget, TypeToken bridgeType, TypeDescription instrumentedType) {
                        this.bridgeTarget = bridgeTarget;
                        this.bridgeType = bridgeType;
                        this.instrumentedType = instrumentedType;
                    }

                    @Override
                    public TypeDescription getDeclaringType() {
                        return instrumentedType;
                    }

                    @Override
                    public ParameterList<ParameterDescription.InDefinedShape> getParameters() {
                        return new ParameterList.Explicit.ForTypes(this, bridgeType.getParameterTypes());
                    }

                    @Override
                    public TypeDescription.Generic getReturnType() {
                        return bridgeType.getReturnType().asGenericType();
                    }

                    @Override
                    public TypeList.Generic getExceptionTypes() {
                        return bridgeTarget.getExceptionTypes().accept(TypeDescription.Generic.Visitor.TypeErasing.INSTANCE);
                    }

                    @Override
                    public AnnotationValue<?, ?> getDefaultValue() {
                        return AnnotationValue.UNDEFINED;
                    }

                    @Override
                    public TypeList.Generic getTypeVariables() {
                        return new TypeList.Generic.Empty();
                    }

                    @Override
                    public AnnotationList getDeclaredAnnotations() {
                        return new AnnotationList.Empty();
                    }

                    @Override
                    public int getModifiers() {
                        return (bridgeTarget.getModifiers() | Opcodes.ACC_BRIDGE | Opcodes.ACC_SYNTHETIC) & ~(Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE);
                    }

                    @Override
                    public String getInternalName() {
                        return bridgeTarget.getInternalName();
                    }
                }

                /**
                 * A method representing a bridge's target method in its defined shape.
                 */
                protected static class BridgeTarget extends MethodDescription.InDefinedShape.AbstractBase {

                    /**
                     * The target method of the bridge.
                     */
                    private final MethodDescription bridgeTarget;

                    /**
                     * The instrumented type defining the bridge target.
                     */
                    private final TypeDescription instrumentedType;

                    /**
                     * Creates a new bridge target.
                     *
                     * @param bridgeTarget     The target method of the bridge.
                     * @param instrumentedType The instrumented type defining the bridge target.
                     */
                    protected BridgeTarget(MethodDescription bridgeTarget, TypeDescription instrumentedType) {
                        this.bridgeTarget = bridgeTarget;
                        this.instrumentedType = instrumentedType;
                    }

                    @Override
                    public TypeDescription getDeclaringType() {
                        return instrumentedType;
                    }

                    @Override
                    public ParameterList<ParameterDescription.InDefinedShape> getParameters() {
                        return new ParameterList.ForTokens(this, bridgeTarget.getParameters().asTokenList(is(instrumentedType)));
                    }

                    @Override
                    public TypeDescription.Generic getReturnType() {
                        return bridgeTarget.getReturnType();
                    }

                    @Override
                    public TypeList.Generic getExceptionTypes() {
                        return bridgeTarget.getExceptionTypes();
                    }

                    @Override
                    public AnnotationValue<?, ?> getDefaultValue() {
                        return bridgeTarget.getDefaultValue();
                    }

                    @Override
                    public TypeList.Generic getTypeVariables() {
                        return bridgeTarget.getTypeVariables();
                    }

                    @Override
                    public AnnotationList getDeclaredAnnotations() {
                        return bridgeTarget.getDeclaredAnnotations();
                    }

                    @Override
                    public int getModifiers() {
                        return bridgeTarget.getModifiers();
                    }

                    @Override
                    public String getInternalName() {
                        return bridgeTarget.getInternalName();
                    }
                }
            }
        }
    }

    /**
     * A default implementation of a {@link net.bytebuddy.dynamic.scaffold.TypeWriter}. {@link net.bytebuddy.dynamic.scaffold.TypeWriter} 的默认实现
     *
     * @param <S> The best known loaded type for the dynamically created type. 动态创建类型的最著名加载类型
     */
    @HashCodeAndEqualsPlugin.Enhance
    abstract class Default<S> implements TypeWriter<S> {

        /**
         * Indicates an empty reference in a class file which is expressed by {@code null}. 指示由{@code null}表示的类文件中的空引用
         */
        private static final String NO_REFERENCE = null;

        /**
         * A folder for dumping class files or {@code null} if no dump should be generated. 用于转储类文件的文件夹，如果不应生成转储，则使用{@code null}
         */
        protected static final String DUMP_FOLDER;

        /*
         * Reads the dumping property that is set at program start up. This might cause an error because of security constraints. 读取程序启动时设置的转储属性。由于安全限制，这可能会导致错误
         */
        static {
            String dumpFolder;
            try {
                dumpFolder = AccessController.doPrivileged(new GetSystemPropertyAction(DUMP_PROPERTY));
            } catch (RuntimeException exception) {
                dumpFolder = null;
            }
            DUMP_FOLDER = dumpFolder;
        }

        /**
         * The instrumented type to be created. 要创建的插桩类型
         */
        protected final TypeDescription instrumentedType;

        /**
         * The class file specified by the user. 用户指定的类文件
         */
        protected final ClassFileVersion classFileVersion;

        /**
         * The field pool to use. 要使用的字段池
         */
        protected final FieldPool fieldPool;

        /**
         * The explicit auxiliary types to add to the created type. 要添加到已创建类型的显式辅助类型
         */
        protected final List<? extends DynamicType> auxiliaryTypes;

        /**
         * The instrumented type's declared fields. 插桩类的声明字段
         */
        protected final FieldList<FieldDescription.InDefinedShape> fields;

        /**
         * The instrumented type's methods that are declared or inherited. 声明或继承得到的插桩类方法
         */
        protected final MethodList<?> methods;

        /**
         * The instrumented methods relevant to this type creation. 与此类型创建相关的插桩方法
         */
        protected final MethodList<?> instrumentedMethods;

        /**
         * The loaded type initializer to apply onto the created type after loading. 加载后要应用于已创建类型的已加载类型初始值设定项
         */
        protected final LoadedTypeInitializer loadedTypeInitializer;

        /**
         * The type initializer to include in the created type's type initializer. 要包含在已创建类型的类型初始值设定项中的类型初始值设定项
         */
        protected final TypeInitializer typeInitializer;

        /**
         * The type attribute appender to apply onto the instrumented type. 要应用于插入指令的类型的类型属性 appender
         */
        protected final TypeAttributeAppender typeAttributeAppender;

        /**
         * The ASM visitor wrapper to apply onto the class writer. 要应用于类编写器的 ASM 访问者包装器
         */
        protected final AsmVisitorWrapper asmVisitorWrapper;

        /**
         * The annotation value filter factory to apply. 要应用的注释值筛选器工厂
         */
        protected final AnnotationValueFilter.Factory annotationValueFilterFactory;

        /**
         * The annotation retention to apply. 要应用的注解保留
         */
        protected final AnnotationRetention annotationRetention;

        /**
         * The naming strategy for auxiliary types to apply. 要应用的辅助类型的命名策略
         */
        protected final AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy;

        /**
         * The implementation context factory to apply. 要应用的实现上下文工厂
         */
        protected final Implementation.Context.Factory implementationContextFactory;

        /**
         * Determines if a type should be explicitly validated. 确定是否应显式验证类型
         */
        protected final TypeValidation typeValidation;

        /**
         * The class writer strategy to use. 要使用的类编写器策略
         */
        protected final ClassWriterStrategy classWriterStrategy;

        /**
         * The type pool to use for computing stack map frames, if required. 用于计算堆栈映射帧的类型池（如果需要）
         */
        protected final TypePool typePool;

        /**
         * Creates a new default type writer.
         *
         * @param instrumentedType             The instrumented type to be created.
         * @param classFileVersion             The class file specified by the user.
         * @param fieldPool                    The field pool to use.
         * @param auxiliaryTypes               The explicit auxiliary types to add to the created type.
         * @param fields                       The instrumented type's declared fields.
         * @param methods                      The instrumented type's declared and virtually inherited methods.
         * @param instrumentedMethods          The instrumented methods relevant to this type creation.
         * @param loadedTypeInitializer        The loaded type initializer to apply onto the created type after loading.
         * @param typeInitializer              The type initializer to include in the created type's type initializer.
         * @param typeAttributeAppender        The type attribute appender to apply onto the instrumented type.
         * @param asmVisitorWrapper            The ASM visitor wrapper to apply onto the class writer.
         * @param annotationValueFilterFactory The annotation value filter factory to apply.
         * @param annotationRetention          The annotation retention to apply.
         * @param auxiliaryTypeNamingStrategy  The naming strategy for auxiliary types to apply.
         * @param implementationContextFactory The implementation context factory to apply.
         * @param typeValidation               Determines if a type should be explicitly validated.
         * @param classWriterStrategy          The class writer strategy to use.
         * @param typePool                     The type pool to use for computing stack map frames, if required.
         */
        protected Default(TypeDescription instrumentedType,
                          ClassFileVersion classFileVersion,
                          FieldPool fieldPool,
                          List<? extends DynamicType> auxiliaryTypes,
                          FieldList<FieldDescription.InDefinedShape> fields,
                          MethodList<?> methods,
                          MethodList<?> instrumentedMethods,
                          LoadedTypeInitializer loadedTypeInitializer,
                          TypeInitializer typeInitializer,
                          TypeAttributeAppender typeAttributeAppender,
                          AsmVisitorWrapper asmVisitorWrapper,
                          AnnotationValueFilter.Factory annotationValueFilterFactory,
                          AnnotationRetention annotationRetention,
                          AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                          Implementation.Context.Factory implementationContextFactory,
                          TypeValidation typeValidation,
                          ClassWriterStrategy classWriterStrategy,
                          TypePool typePool) {
            this.instrumentedType = instrumentedType;
            this.classFileVersion = classFileVersion;
            this.fieldPool = fieldPool;
            this.auxiliaryTypes = auxiliaryTypes;
            this.fields = fields;
            this.methods = methods;
            this.instrumentedMethods = instrumentedMethods;
            this.loadedTypeInitializer = loadedTypeInitializer;
            this.typeInitializer = typeInitializer;
            this.typeAttributeAppender = typeAttributeAppender;
            this.asmVisitorWrapper = asmVisitorWrapper;
            this.auxiliaryTypeNamingStrategy = auxiliaryTypeNamingStrategy;
            this.annotationValueFilterFactory = annotationValueFilterFactory;
            this.annotationRetention = annotationRetention;
            this.implementationContextFactory = implementationContextFactory;
            this.typeValidation = typeValidation;
            this.classWriterStrategy = classWriterStrategy;
            this.typePool = typePool;
        }

        /**
         * Creates a type writer for creating a new type.
         *
         * @param methodRegistry               The compiled method registry to use.
         * @param auxiliaryTypes               A list of explicitly required auxiliary types.
         * @param fieldPool                    The field pool to use.
         * @param typeAttributeAppender        The type attribute appender to apply onto the instrumented type.
         * @param asmVisitorWrapper            The ASM visitor wrapper to apply onto the class writer.
         * @param classFileVersion             The class file version to use when no explicit class file version is applied.
         * @param annotationValueFilterFactory The annotation value filter factory to apply.
         * @param annotationRetention          The annotation retention to apply.
         * @param auxiliaryTypeNamingStrategy  The naming strategy for auxiliary types to apply.
         * @param implementationContextFactory The implementation context factory to apply.
         * @param typeValidation               Determines if a type should be explicitly validated.
         * @param classWriterStrategy          The class writer strategy to use.
         * @param typePool                     The type pool to use for computing stack map frames, if required.
         * @param <U>                          A loaded type that the instrumented type guarantees to subclass.
         * @return A suitable type writer.
         */
        public static <U> TypeWriter<U> forCreation(MethodRegistry.Compiled methodRegistry,
                                                    List<? extends DynamicType> auxiliaryTypes,
                                                    FieldPool fieldPool,
                                                    TypeAttributeAppender typeAttributeAppender,
                                                    AsmVisitorWrapper asmVisitorWrapper,
                                                    ClassFileVersion classFileVersion,
                                                    AnnotationValueFilter.Factory annotationValueFilterFactory,
                                                    AnnotationRetention annotationRetention,
                                                    AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                                                    Implementation.Context.Factory implementationContextFactory,
                                                    TypeValidation typeValidation,
                                                    ClassWriterStrategy classWriterStrategy,
                                                    TypePool typePool) {
            return new ForCreation<U>(methodRegistry.getInstrumentedType(),
                    classFileVersion,
                    fieldPool,
                    methodRegistry,
                    auxiliaryTypes,
                    methodRegistry.getInstrumentedType().getDeclaredFields(),
                    methodRegistry.getMethods(),
                    methodRegistry.getInstrumentedMethods(),
                    methodRegistry.getLoadedTypeInitializer(),
                    methodRegistry.getTypeInitializer(),
                    typeAttributeAppender,
                    asmVisitorWrapper,
                    annotationValueFilterFactory,
                    annotationRetention,
                    auxiliaryTypeNamingStrategy,
                    implementationContextFactory,
                    typeValidation,
                    classWriterStrategy,
                    typePool);
        }

        /**
         * Creates a type writer for redefining a type.
         *
         * @param methodRegistry               The compiled method registry to use.
         * @param auxiliaryTypes               A list of explicitly required auxiliary types.
         * @param fieldPool                    The field pool to use.
         * @param typeAttributeAppender        The type attribute appender to apply onto the instrumented type.
         * @param asmVisitorWrapper            The ASM visitor wrapper to apply onto the class writer.
         * @param classFileVersion             The class file version to use when no explicit class file version is applied.
         * @param annotationValueFilterFactory The annotation value filter factory to apply.
         * @param annotationRetention          The annotation retention to apply.
         * @param auxiliaryTypeNamingStrategy  The naming strategy for auxiliary types to apply.
         * @param implementationContextFactory The implementation context factory to apply.
         * @param typeValidation               Determines if a type should be explicitly validated.
         * @param classWriterStrategy          The class writer strategy to use.
         * @param typePool                     The type pool to use for computing stack map frames, if required.
         * @param originalType                 The original type that is being redefined or rebased.
         * @param classFileLocator             The class file locator for locating the original type's class file.
         * @param <U>                          A loaded type that the instrumented type guarantees to subclass.
         * @return A suitable type writer.
         */
        public static <U> TypeWriter<U> forRedefinition(MethodRegistry.Prepared methodRegistry,
                                                        List<? extends DynamicType> auxiliaryTypes,
                                                        FieldPool fieldPool,
                                                        TypeAttributeAppender typeAttributeAppender,
                                                        AsmVisitorWrapper asmVisitorWrapper,
                                                        ClassFileVersion classFileVersion,
                                                        AnnotationValueFilter.Factory annotationValueFilterFactory,
                                                        AnnotationRetention annotationRetention,
                                                        AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                                                        Implementation.Context.Factory implementationContextFactory,
                                                        TypeValidation typeValidation,
                                                        ClassWriterStrategy classWriterStrategy,
                                                        TypePool typePool,
                                                        TypeDescription originalType,
                                                        ClassFileLocator classFileLocator) {
            return new ForInlining.WithFullProcessing<U>(methodRegistry.getInstrumentedType(),
                    classFileVersion,
                    fieldPool,
                    auxiliaryTypes,
                    methodRegistry.getInstrumentedType().getDeclaredFields(),
                    methodRegistry.getMethods(),
                    methodRegistry.getInstrumentedMethods(),
                    methodRegistry.getLoadedTypeInitializer(),
                    methodRegistry.getTypeInitializer(),
                    typeAttributeAppender,
                    asmVisitorWrapper,
                    annotationValueFilterFactory,
                    annotationRetention,
                    auxiliaryTypeNamingStrategy,
                    implementationContextFactory,
                    typeValidation,
                    classWriterStrategy,
                    typePool,
                    originalType,
                    classFileLocator,
                    methodRegistry,
                    SubclassImplementationTarget.Factory.LEVEL_TYPE,
                    MethodRebaseResolver.Disabled.INSTANCE);
        }

        /**
         * Creates a type writer for rebasing a type.
         *
         * @param methodRegistry               The compiled method registry to use.
         * @param auxiliaryTypes               A list of explicitly required auxiliary types.
         * @param fieldPool                    The field pool to use.
         * @param typeAttributeAppender        The type attribute appender to apply onto the instrumented type.
         * @param asmVisitorWrapper            The ASM visitor wrapper to apply onto the class writer.
         * @param classFileVersion             The class file version to use when no explicit class file version is applied.
         * @param annotationValueFilterFactory The annotation value filter factory to apply.
         * @param annotationRetention          The annotation retention to apply.
         * @param auxiliaryTypeNamingStrategy  The naming strategy for auxiliary types to apply.
         * @param implementationContextFactory The implementation context factory to apply.
         * @param typeValidation               Determines if a type should be explicitly validated.
         * @param classWriterStrategy          The class writer strategy to use.
         * @param typePool                     The type pool to use for computing stack map frames, if required.
         * @param originalType                 The original type that is being redefined or rebased.               正在重新定义或重设基准的原始类型
         * @param classFileLocator             The class file locator for locating the original type's class file. 用于定位原始类型的类文件的类文件定位器
         * @param methodRebaseResolver         The method rebase resolver to use for rebasing names.               用于重新设置名称的方法重新设置解析器的基础
         * @param <U>                          A loaded type that the instrumented type guarantees to subclass.    插桩类型保证为子类的已加载类型
         * @return A suitable type writer.
         */
        public static <U> TypeWriter<U> forRebasing(MethodRegistry.Prepared methodRegistry,
                                                    List<? extends DynamicType> auxiliaryTypes,
                                                    FieldPool fieldPool,
                                                    TypeAttributeAppender typeAttributeAppender,
                                                    AsmVisitorWrapper asmVisitorWrapper,
                                                    ClassFileVersion classFileVersion,
                                                    AnnotationValueFilter.Factory annotationValueFilterFactory,
                                                    AnnotationRetention annotationRetention,
                                                    AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                                                    Implementation.Context.Factory implementationContextFactory,
                                                    TypeValidation typeValidation,
                                                    ClassWriterStrategy classWriterStrategy,
                                                    TypePool typePool,
                                                    TypeDescription originalType,
                                                    ClassFileLocator classFileLocator,
                                                    MethodRebaseResolver methodRebaseResolver) {
            return new ForInlining.WithFullProcessing<U>(methodRegistry.getInstrumentedType(),
                    classFileVersion,
                    fieldPool,
                    CompoundList.of(auxiliaryTypes, methodRebaseResolver.getAuxiliaryTypes()),
                    methodRegistry.getInstrumentedType().getDeclaredFields(),
                    methodRegistry.getMethods(),
                    methodRegistry.getInstrumentedMethods(),
                    methodRegistry.getLoadedTypeInitializer(),
                    methodRegistry.getTypeInitializer(),
                    typeAttributeAppender,
                    asmVisitorWrapper,
                    annotationValueFilterFactory,
                    annotationRetention,
                    auxiliaryTypeNamingStrategy,
                    implementationContextFactory,
                    typeValidation,
                    classWriterStrategy,
                    typePool,
                    originalType,
                    classFileLocator,
                    methodRegistry,
                    new RebaseImplementationTarget.Factory(methodRebaseResolver),
                    methodRebaseResolver);
        }

        /**
         * Creates a type writer for decorating a type. 创建用于装饰类型的类型编写器
         *
         * @param instrumentedType             The instrumented type. 插桩类型
         * @param classFileVersion             The class file version to use when no explicit class file version is applied. 没有应用显式类文件版本时要使用的类文件版本
         * @param auxiliaryTypes               A list of explicitly required auxiliary types. 明确要求的辅助类型的列表
         * @param methods                      The methods to instrument. 插桩方法
         * @param typeAttributeAppender        The type attribute appender to apply onto the instrumented type. 应用于插桩类型的属性附加器
         * @param asmVisitorWrapper            The ASM visitor wrapper to apply onto the class writer.    要应用于类编写器的ASM访问者包装器
         * @param annotationValueFilterFactory The annotation value filter factory to apply.  要应用的注解值筛选器工厂
         * @param annotationRetention          The annotation retention to apply.             要应用的注解保留
         * @param auxiliaryTypeNamingStrategy  The naming strategy for auxiliary types to apply.    要应用的辅助类型的命名策略
         * @param implementationContextFactory The implementation context factory to apply.
         * @param typeValidation               Determines if a type should be explicitly validated.
         * @param classWriterStrategy          The class writer strategy to use.
         * @param typePool                     The type pool to use for computing stack map frames, if required.
         * @param classFileLocator             The class file locator for locating the original type's class file.
         * @param <U>                          A loaded type that the instrumented type guarantees to subclass.
         * @return A suitable type writer.
         */
        public static <U> TypeWriter<U> forDecoration(TypeDescription instrumentedType,
                                                      ClassFileVersion classFileVersion,
                                                      List<? extends DynamicType> auxiliaryTypes,
                                                      List<? extends MethodDescription> methods,
                                                      TypeAttributeAppender typeAttributeAppender,
                                                      AsmVisitorWrapper asmVisitorWrapper,
                                                      AnnotationValueFilter.Factory annotationValueFilterFactory,
                                                      AnnotationRetention annotationRetention,
                                                      AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                                                      Implementation.Context.Factory implementationContextFactory,
                                                      TypeValidation typeValidation,
                                                      ClassWriterStrategy classWriterStrategy,
                                                      TypePool typePool,
                                                      ClassFileLocator classFileLocator) {
            return new ForInlining.WithDecorationOnly<U>(instrumentedType,
                    classFileVersion,
                    auxiliaryTypes,
                    new MethodList.Explicit<MethodDescription>(methods),
                    typeAttributeAppender,
                    asmVisitorWrapper,
                    annotationValueFilterFactory,
                    annotationRetention,
                    auxiliaryTypeNamingStrategy,
                    implementationContextFactory,
                    typeValidation,
                    classWriterStrategy,
                    typePool,
                    classFileLocator);
        }

        @Override
        @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION", justification = "Setting a debugging property should never change the program outcome")
        public DynamicType.Unloaded<S> make(TypeResolutionStrategy.Resolved typeResolutionStrategy) {
            UnresolvedType unresolvedType = create(typeResolutionStrategy.injectedInto(typeInitializer));
            ClassDumpAction.dump(DUMP_FOLDER, instrumentedType, false, unresolvedType.getBinaryRepresentation());
            return unresolvedType.toDynamicType(typeResolutionStrategy);
        }

        /**
         * Creates an unresolved version of the dynamic type. 创建动态类型的未解析版本
         *
         * @param typeInitializer The type initializer to use.
         * @return An unresolved type.
         */
        protected abstract UnresolvedType create(TypeInitializer typeInitializer);

        /**
         * An unresolved type.
         */
        @HashCodeAndEqualsPlugin.Enhance(includeSyntheticFields = true)
        protected class UnresolvedType {

            /**
             * The type's binary representation. 类型的二进制表示
             */
            private final byte[] binaryRepresentation;

            /**
             * A list of auxiliary types for this unresolved type. 此未解析类型的辅助类型列表
             */
            private final List<? extends DynamicType> auxiliaryTypes;

            /**
             * Creates a new unresolved type.
             *
             * @param binaryRepresentation The type's binary representation.
             * @param auxiliaryTypes       A list of auxiliary types for this unresolved type. 此未解析类型的辅助类型列表
             */
            protected UnresolvedType(byte[] binaryRepresentation, List<? extends DynamicType> auxiliaryTypes) {
                this.binaryRepresentation = binaryRepresentation;
                this.auxiliaryTypes = auxiliaryTypes;
            }

            /**
             * Resolves this type to a dynamic type. 将此类型解析为动态类型
             *
             * @param typeResolutionStrategy The type resolution strategy to apply.
             * @return A dynamic type representing the inlined type.
             */
            protected DynamicType.Unloaded<S> toDynamicType(TypeResolutionStrategy.Resolved typeResolutionStrategy) {
                return new DynamicType.Default.Unloaded<S>(instrumentedType,
                        binaryRepresentation,
                        loadedTypeInitializer,
                        CompoundList.of(Default.this.auxiliaryTypes, auxiliaryTypes),
                        typeResolutionStrategy);
            }

            /**
             * Returns the binary representation of this unresolved type.
             *
             * @return The binary representation of this unresolved type.
             */
            protected byte[] getBinaryRepresentation() {
                return binaryRepresentation;
            }
        }

        /**
         * A class validator that validates that a class only defines members that are appropriate for the sort of the generated class. 验证类的类验证器仅定义适合于所生成类的成员
         */
        protected static class ValidatingClassVisitor extends ClassVisitor {

            /**
             * Indicates that a method has no method parameters.
             */
            private static final String NO_PARAMETERS = "()";

            /**
             * Indicates that a method returns void.
             */
            private static final String RETURNS_VOID = "V";

            /**
             * The descriptor of the {@link String} type.
             */
            private static final String STRING_DESCRIPTOR = "Ljava/lang/String;";

            /**
             * Indicates that a field is ignored.
             */
            private static final FieldVisitor IGNORE_FIELD = null;

            /**
             * Indicates that a method is ignored.
             */
            private static final MethodVisitor IGNORE_METHOD = null;

            /**
             * The constraint to assert the members against. The constraint is first defined when the general class information is visited.
             */
            private Constraint constraint;

            /**
             * Creates a validating class visitor.
             *
             * @param classVisitor The class visitor to which any calls are delegated to.
             */
            protected ValidatingClassVisitor(ClassVisitor classVisitor) {
                super(OpenedClassReader.ASM_API, classVisitor);
            }

            /**
             * Adds a validating visitor if type validation is enabled.
             *
             * @param classVisitor   The original class visitor.
             * @param typeValidation The type validation state.
             * @return A class visitor that applies type validation if this is required.
             */
            protected static ClassVisitor of(ClassVisitor classVisitor, TypeValidation typeValidation) {
                return typeValidation.isEnabled()
                        ? new ValidatingClassVisitor(classVisitor)
                        : classVisitor;
            }

            @Override
            public void visit(int version, int modifiers, String name, String signature, String superName, String[] interfaces) {
                ClassFileVersion classFileVersion = ClassFileVersion.ofMinorMajor(version);
                List<Constraint> constraints = new ArrayList<Constraint>();
                constraints.add(new Constraint.ForClassFileVersion(classFileVersion));
                if (name.endsWith('/' + PackageDescription.PACKAGE_CLASS_NAME)) {
                    constraints.add(Constraint.ForPackageType.INSTANCE);
                } else if ((modifiers & Opcodes.ACC_ANNOTATION) != 0) {
                    if (!classFileVersion.isAtLeast(ClassFileVersion.JAVA_V5)) {
                        throw new IllegalStateException("Cannot define an annotation type for class file version " + classFileVersion);
                    }
                    constraints.add(classFileVersion.isAtLeast(ClassFileVersion.JAVA_V8)
                            ? Constraint.ForAnnotation.JAVA_8
                            : Constraint.ForAnnotation.CLASSIC);
                } else if ((modifiers & Opcodes.ACC_INTERFACE) != 0) {
                    constraints.add(classFileVersion.isAtLeast(ClassFileVersion.JAVA_V8)
                            ? Constraint.ForInterface.JAVA_8
                            : Constraint.ForInterface.CLASSIC);
                } else if ((modifiers & Opcodes.ACC_ABSTRACT) != 0) {
                    constraints.add(Constraint.ForClass.ABSTRACT);
                } else {
                    constraints.add(Constraint.ForClass.MANIFEST);
                }
                constraint = new Constraint.Compound(constraints);
                constraint.assertType(modifiers, interfaces != null, signature != null);
                super.visit(version, modifiers, name, signature, superName, interfaces);
            }

            @Override
            public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                constraint.assertAnnotation();
                return super.visitAnnotation(descriptor, visible);
            }

            @Override
            public AnnotationVisitor visitTypeAnnotation(int typeReference, TypePath typePath, String descriptor, boolean visible) {
                constraint.assertTypeAnnotation();
                return super.visitTypeAnnotation(typeReference, typePath, descriptor, visible);
            }

            @Override
            @SuppressWarnings("deprecation")
            public void visitNestHostExperimental(String nestHost) {
                constraint.assertNestMate();
                super.visitNestHostExperimental(nestHost);
            }

            @Override
            @SuppressWarnings("deprecation")
            public void visitNestMemberExperimental(String nestMember) {
                constraint.assertNestMate();
                super.visitNestMemberExperimental(nestMember);
            }

            @Override
            public FieldVisitor visitField(int modifiers, String name, String descriptor, String signature, Object defaultValue) {
                if (defaultValue != null) {
                    Class<?> type;
                    switch (descriptor.charAt(0)) {
                        case 'Z':
                        case 'B':
                        case 'C':
                        case 'S':
                        case 'I':
                            type = Integer.class;
                            break;
                        case 'J':
                            type = Long.class;
                            break;
                        case 'F':
                            type = Float.class;
                            break;
                        case 'D':
                            type = Double.class;
                            break;
                        default:
                            if (!descriptor.equals(STRING_DESCRIPTOR)) {
                                throw new IllegalStateException("Cannot define a default value for type of field " + name);
                            }
                            type = String.class;
                    }
                    if (!type.isInstance(defaultValue)) {
                        throw new IllegalStateException("Field " + name + " defines an incompatible default value " + defaultValue);
                    } else if (type == Integer.class) {
                        int minimum, maximum;
                        switch (descriptor.charAt(0)) {
                            case 'Z':
                                minimum = 0;
                                maximum = 1;
                                break;
                            case 'B':
                                minimum = Byte.MIN_VALUE;
                                maximum = Byte.MAX_VALUE;
                                break;
                            case 'C':
                                minimum = Character.MIN_VALUE;
                                maximum = Character.MAX_VALUE;
                                break;
                            case 'S':
                                minimum = Short.MIN_VALUE;
                                maximum = Short.MAX_VALUE;
                                break;
                            default:
                                minimum = Integer.MIN_VALUE;
                                maximum = Integer.MAX_VALUE;
                        }
                        int value = (Integer) defaultValue;
                        if (value < minimum || value > maximum) {
                            throw new IllegalStateException("Field " + name + " defines an incompatible default value " + defaultValue);
                        }
                    }
                }
                constraint.assertField(name,
                        (modifiers & Opcodes.ACC_PUBLIC) != 0,
                        (modifiers & Opcodes.ACC_STATIC) != 0,
                        (modifiers & Opcodes.ACC_FINAL) != 0,
                        signature != null);
                FieldVisitor fieldVisitor = super.visitField(modifiers, name, descriptor, signature, defaultValue);
                return fieldVisitor == null
                        ? IGNORE_FIELD
                        : new ValidatingFieldVisitor(fieldVisitor);
            }

            @Override
            public MethodVisitor visitMethod(int modifiers, String name, String descriptor, String signature, String[] exceptions) {
                constraint.assertMethod(name,
                        (modifiers & Opcodes.ACC_ABSTRACT) != 0,
                        (modifiers & Opcodes.ACC_PUBLIC) != 0,
                        (modifiers & Opcodes.ACC_PRIVATE) != 0,
                        (modifiers & Opcodes.ACC_STATIC) != 0,
                        !name.equals(MethodDescription.CONSTRUCTOR_INTERNAL_NAME)
                                && !name.equals(MethodDescription.TYPE_INITIALIZER_INTERNAL_NAME)
                                && (modifiers & (Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC)) == 0,
                        name.equals(MethodDescription.CONSTRUCTOR_INTERNAL_NAME),
                        !descriptor.startsWith(NO_PARAMETERS) || descriptor.endsWith(RETURNS_VOID),
                        signature != null);
                MethodVisitor methodVisitor = super.visitMethod(modifiers, name, descriptor, signature, exceptions);
                return methodVisitor == null
                        ? IGNORE_METHOD
                        : new ValidatingMethodVisitor(methodVisitor, name);
            }

            /**
             * A constraint for members that are legal for a given type. 对给定类型合法成员的约束
             */
            protected interface Constraint {

                /**
                 * Asserts if the type can legally represent a package description.
                 *
                 * @param modifier          The modifier that is to be written to the type.
                 * @param definesInterfaces {@code true} if this type implements at least one interface.
                 * @param isGeneric         {@code true} if this type defines a generic type signature.
                 */
                void assertType(int modifier, boolean definesInterfaces, boolean isGeneric);

                /**
                 * Asserts a field for being valid.
                 *
                 * @param name      The name of the field.
                 * @param isPublic  {@code true} if this field is public.
                 * @param isStatic  {@code true} if this field is static.
                 * @param isFinal   {@code true} if this field is final.
                 * @param isGeneric {@code true} if this field defines a generic signature.
                 */
                void assertField(String name, boolean isPublic, boolean isStatic, boolean isFinal, boolean isGeneric);

                /**
                 * Asserts a method for being valid.
                 *
                 * @param name                       The name of the method.
                 * @param isAbstract                 {@code true} if the method is abstract.
                 * @param isPublic                   {@code true} if this method is public.
                 * @param isPrivate                  {@code true} if this method is private.
                 * @param isStatic                   {@code true} if this method is static.
                 * @param isVirtual                  {@code true} if this method is virtual.
                 * @param isConstructor              {@code true} if this method is a constructor.
                 * @param isDefaultValueIncompatible {@code true} if a method's signature cannot describe an annotation property method.
                 * @param isGeneric                  {@code true} if this method defines a generic signature.
                 */
                void assertMethod(String name,
                                  boolean isAbstract,
                                  boolean isPublic,
                                  boolean isPrivate,
                                  boolean isStatic,
                                  boolean isVirtual,
                                  boolean isConstructor,
                                  boolean isDefaultValueIncompatible,
                                  boolean isGeneric);

                /**
                 * Asserts the legitimacy of an annotation for the instrumented type.
                 */
                void assertAnnotation();

                /**
                 * Asserts the legitimacy of a type annotation for the instrumented type.
                 */
                void assertTypeAnnotation();

                /**
                 * Asserts if a default value is legal for a method.
                 *
                 * @param name The name of the method.
                 */
                void assertDefaultValue(String name);

                /**
                 * Asserts if it is legal to invoke a default method from a type.
                 */
                void assertDefaultMethodCall();

                /**
                 * Asserts the capability to store a type constant in the class's constant pool.
                 */
                void assertTypeInConstantPool();

                /**
                 * Asserts the capability to store a method type constant in the class's constant pool.
                 */
                void assertMethodTypeInConstantPool();

                /**
                 * Asserts the capability to store a method handle in the class's constant pool.
                 */
                void assertHandleInConstantPool();

                /**
                 * Asserts the capability to invoke a method dynamically.
                 */
                void assertInvokeDynamic();

                /**
                 * Asserts the capability of executing a subroutine.
                 */
                void assertSubRoutine();

                /**
                 * Asserts the capability of storing a dynamic value in the constant pool.
                 */
                void assertDynamicValueInConstantPool();

                /**
                 * Asserts the capability of storing nest mate information.
                 */
                void assertNestMate();

                /**
                 * Represents the constraint of a class type.
                 */
                enum ForClass implements Constraint {

                    /**
                     * Represents the constraints of a non-abstract class. 表示非抽象类的约束
                     */
                    MANIFEST(true),

                    /**
                     * Represents the constraints of an abstract class.
                     */
                    ABSTRACT(false);

                    /**
                     * {@code true} if this instance represents the constraints a non-abstract class.
                     */
                    private final boolean manifestType;

                    /**
                     * Creates a new constraint for a class.
                     *
                     * @param manifestType {@code true} if this instance represents a non-abstract class.
                     */
                    ForClass(boolean manifestType) {
                        this.manifestType = manifestType;
                    }

                    @Override
                    public void assertType(int modifier, boolean definesInterfaces, boolean isGeneric) {
                        /* do nothing */
                    }

                    @Override
                    public void assertField(String name, boolean isPublic, boolean isStatic, boolean isFinal, boolean isGeneric) {
                        /* do nothing */
                    }

                    @Override
                    public void assertMethod(String name,
                                             boolean isAbstract,
                                             boolean isPublic,
                                             boolean isPrivate,
                                             boolean isStatic,
                                             boolean isVirtual,
                                             boolean isConstructor,
                                             boolean isDefaultValueIncompatible,
                                             boolean isGeneric) {
                        if (isAbstract && manifestType) {
                            throw new IllegalStateException("Cannot define abstract method '" + name + "' for non-abstract class");
                        }
                    }

                    @Override
                    public void assertAnnotation() {
                        /* do nothing */
                    }

                    @Override
                    public void assertTypeAnnotation() {
                        /* do nothing */
                    }

                    @Override
                    public void assertDefaultValue(String name) {
                        throw new IllegalStateException("Cannot define default value for '" + name + "' for non-annotation type");
                    }

                    @Override
                    public void assertDefaultMethodCall() {
                        /* do nothing */
                    }

                    @Override
                    public void assertTypeInConstantPool() {
                        /* do nothing */
                    }

                    @Override
                    public void assertMethodTypeInConstantPool() {
                        /* do nothing */
                    }

                    @Override
                    public void assertHandleInConstantPool() {
                        /* do nothing */
                    }

                    @Override
                    public void assertInvokeDynamic() {
                        /* do nothing */
                    }

                    @Override
                    public void assertSubRoutine() {
                        /* do nothing */
                    }

                    @Override
                    public void assertDynamicValueInConstantPool() {
                        /* do nothing */
                    }

                    @Override
                    public void assertNestMate() {
                        /* do nothing */
                    }
                }

                /**
                 * Represents the constraint of a package type.
                 */
                enum ForPackageType implements Constraint {

                    /**
                     * The singleton instance.
                     */
                    INSTANCE;

                    @Override
                    public void assertField(String name, boolean isPublic, boolean isStatic, boolean isFinal, boolean isGeneric) {
                        throw new IllegalStateException("Cannot define a field for a package description type");
                    }

                    @Override
                    public void assertMethod(String name,
                                             boolean isAbstract,
                                             boolean isPublic,
                                             boolean isPrivate,
                                             boolean isStatic,
                                             boolean isVirtual,
                                             boolean isConstructor,
                                             boolean isNoDefaultValue,
                                             boolean isGeneric) {
                        throw new IllegalStateException("Cannot define a method for a package description type");
                    }

                    @Override
                    public void assertAnnotation() {
                        /* do nothing */
                    }

                    @Override
                    public void assertTypeAnnotation() {
                        /* do nothing */
                    }

                    @Override
                    public void assertDefaultValue(String name) {
                        /* do nothing, implicit by forbidding methods */
                    }

                    @Override
                    public void assertDefaultMethodCall() {
                        /* do nothing */
                    }

                    @Override
                    public void assertTypeInConstantPool() {
                        /* do nothing */
                    }

                    @Override
                    public void assertMethodTypeInConstantPool() {
                        /* do nothing */
                    }

                    @Override
                    public void assertHandleInConstantPool() {
                        /* do nothing */
                    }

                    @Override
                    public void assertInvokeDynamic() {
                        /* do nothing */
                    }

                    @Override
                    public void assertSubRoutine() {
                        /* do nothing */
                    }

                    @Override
                    public void assertType(int modifier, boolean definesInterfaces, boolean isGeneric) {
                        if (modifier != PackageDescription.PACKAGE_MODIFIERS) {
                            throw new IllegalStateException("A package description type must define " + PackageDescription.PACKAGE_MODIFIERS + " as modifier");
                        } else if (definesInterfaces) {
                            throw new IllegalStateException("Cannot implement interface for package type");
                        }
                    }

                    @Override
                    public void assertDynamicValueInConstantPool() {
                        /* do nothing */
                    }

                    @Override
                    public void assertNestMate() {
                        /* do nothing */
                    }
                }

                /**
                 * Represents the constraint of an interface type. 表示接口类型的约束
                 */
                enum ForInterface implements Constraint {

                    /**
                     * An interface type with the constrains for the Java versions 5 to 7. 具有Java版本5到7的约束的接口类型
                     */
                    CLASSIC(true),

                    /**
                     * An interface type with the constrains for the Java versions 8+. 具有Java版本8+的约束的接口类型
                     */
                    JAVA_8(false);

                    /**
                     * {@code true} if this instance represents a classic interface type (pre Java 8). {@code true}如果此实例表示经典接口类型(Java8之前)
                     */
                    private final boolean classic;

                    /**
                     * Creates a constraint for an interface type.
                     *
                     * @param classic {@code true} if this instance represents a classic interface (pre Java 8).
                     */
                    ForInterface(boolean classic) {
                        this.classic = classic;
                    }

                    @Override
                    public void assertField(String name, boolean isPublic, boolean isStatic, boolean isFinal, boolean isGeneric) {
                        if (!isStatic || !isPublic || !isFinal) {
                            throw new IllegalStateException("Cannot only define public, static, final field '" + name + "' for interface type");
                        }
                    }

                    @Override
                    public void assertMethod(String name,
                                             boolean isAbstract,
                                             boolean isPublic,
                                             boolean isPrivate,
                                             boolean isStatic,
                                             boolean isVirtual,
                                             boolean isConstructor,
                                             boolean isDefaultValueIncompatible,
                                             boolean isGeneric) {
                        if (!name.equals(MethodDescription.TYPE_INITIALIZER_INTERNAL_NAME)) {
                            if (isConstructor) {
                                throw new IllegalStateException("Cannot define constructor for interface type");
                            } else if (classic && !isPublic) {
                                throw new IllegalStateException("Cannot define non-public method '" + name + "' for interface type");
                            } else if (classic && !isVirtual) {
                                throw new IllegalStateException("Cannot define non-virtual method '" + name + "' for a pre-Java 8 interface type");
                            } else if (classic && !isAbstract) {
                                throw new IllegalStateException("Cannot define default method '" + name + "' for pre-Java 8 interface type");
                            }
                        }
                    }

                    @Override
                    public void assertAnnotation() {
                        /* do nothing */
                    }

                    @Override
                    public void assertTypeAnnotation() {
                        /* do nothing */
                    }

                    @Override
                    public void assertDefaultValue(String name) {
                        throw new IllegalStateException("Cannot define default value for '" + name + "' for non-annotation type");
                    }

                    @Override
                    public void assertDefaultMethodCall() {
                        /* do nothing */
                    }

                    @Override
                    public void assertType(int modifier, boolean definesInterfaces, boolean isGeneric) {
                        /* do nothing */
                    }

                    @Override
                    public void assertTypeInConstantPool() {
                        /* do nothing */
                    }

                    @Override
                    public void assertMethodTypeInConstantPool() {
                        /* do nothing */
                    }

                    @Override
                    public void assertHandleInConstantPool() {
                        /* do nothing */
                    }

                    @Override
                    public void assertInvokeDynamic() {
                        /* do nothing */
                    }

                    @Override
                    public void assertSubRoutine() {
                        /* do nothing */
                    }

                    @Override
                    public void assertDynamicValueInConstantPool() {
                        /* do nothing */
                    }

                    @Override
                    public void assertNestMate() {
                        /* do nothing */
                    }
                }

                /**
                 * Represents the constraint of an annotation type. 表示注解类型的约束
                 */
                enum ForAnnotation implements Constraint {

                    /**
                     * An annotation type with the constrains for the Java versions 5 to 7.
                     */
                    CLASSIC(true),

                    /**
                     * An annotation type with the constrains for the Java versions 8+.
                     */
                    JAVA_8(false);

                    /**
                     * {@code true} if this instance represents a classic annotation type (pre Java 8).
                     */
                    private final boolean classic;

                    /**
                     * Creates a constraint for an annotation type.
                     *
                     * @param classic {@code true} if this instance represents a classic annotation type (pre Java 8).
                     */
                    ForAnnotation(boolean classic) {
                        this.classic = classic;
                    }

                    @Override
                    public void assertField(String name, boolean isPublic, boolean isStatic, boolean isFinal, boolean isGeneric) {
                        if (!isStatic || !isPublic || !isFinal) {
                            throw new IllegalStateException("Cannot only define public, static, final field '" + name + "' for interface type");
                        }
                    }

                    @Override
                    public void assertMethod(String name,
                                             boolean isAbstract,
                                             boolean isPublic,
                                             boolean isPrivate,
                                             boolean isStatic,
                                             boolean isVirtual,
                                             boolean isConstructor,
                                             boolean isDefaultValueIncompatible,
                                             boolean isGeneric) {
                        if (!name.equals(MethodDescription.TYPE_INITIALIZER_INTERNAL_NAME)) {
                            if (isConstructor) {
                                throw new IllegalStateException("Cannot define constructor for interface type");
                            } else if (classic && !isVirtual) {
                                throw new IllegalStateException("Cannot define non-virtual method '" + name + "' for a pre-Java 8 annotation type");
                            } else if (!isStatic && isDefaultValueIncompatible) {
                                throw new IllegalStateException("Cannot define method '" + name + "' with the given signature as an annotation type method");
                            }
                        }
                    }

                    @Override
                    public void assertAnnotation() {
                        /* do nothing */
                    }

                    @Override
                    public void assertTypeAnnotation() {
                        /* do nothing */
                    }

                    @Override
                    public void assertDefaultValue(String name) {
                        /* do nothing */
                    }

                    @Override
                    public void assertDefaultMethodCall() {
                        /* do nothing */
                    }

                    @Override
                    public void assertType(int modifier, boolean definesInterfaces, boolean isGeneric) {
                        if ((modifier & Opcodes.ACC_INTERFACE) == 0) {
                            throw new IllegalStateException("Cannot define annotation type without interface modifier");
                        }
                    }

                    @Override
                    public void assertTypeInConstantPool() {
                        /* do nothing */
                    }

                    @Override
                    public void assertMethodTypeInConstantPool() {
                        /* do nothing */
                    }

                    @Override
                    public void assertHandleInConstantPool() {
                        /* do nothing */
                    }

                    @Override
                    public void assertInvokeDynamic() {
                        /* do nothing */
                    }

                    @Override
                    public void assertSubRoutine() {
                        /* do nothing */
                    }

                    @Override
                    public void assertDynamicValueInConstantPool() {
                        /* do nothing */
                    }

                    @Override
                    public void assertNestMate() {
                        /* do nothing */
                    }
                }

                /**
                 * Represents the constraint implied by a class file version. 表示类文件版本隐含的约束
                 */
                @HashCodeAndEqualsPlugin.Enhance
                class ForClassFileVersion implements Constraint {

                    /**
                     * The enforced class file version.
                     */
                    private final ClassFileVersion classFileVersion;

                    /**
                     * Creates a new constraint for the given class file version.
                     *
                     * @param classFileVersion The enforced class file version.
                     */
                    protected ForClassFileVersion(ClassFileVersion classFileVersion) {
                        this.classFileVersion = classFileVersion;
                    }

                    @Override
                    public void assertType(int modifiers, boolean definesInterfaces, boolean isGeneric) {
                        if ((modifiers & Opcodes.ACC_ANNOTATION) != 0 && !classFileVersion.isAtLeast(ClassFileVersion.JAVA_V5)) {
                            throw new IllegalStateException("Cannot define annotation type for class file version " + classFileVersion);
                        } else if (isGeneric && !classFileVersion.isAtLeast(ClassFileVersion.JAVA_V5)) {
                            throw new IllegalStateException("Cannot define a generic type for class file version " + classFileVersion);
                        }
                    }

                    @Override
                    public void assertField(String name, boolean isPublic, boolean isStatic, boolean isFinal, boolean isGeneric) {
                        if (isGeneric && !classFileVersion.isAtLeast(ClassFileVersion.JAVA_V5)) {
                            throw new IllegalStateException("Cannot define generic field '" + name + "' for class file version " + classFileVersion);
                        }
                    }

                    @Override
                    public void assertMethod(String name,
                                             boolean isAbstract,
                                             boolean isPublic,
                                             boolean isPrivate,
                                             boolean isStatic,
                                             boolean isVirtual,
                                             boolean isConstructor,
                                             boolean isDefaultValueIncompatible,
                                             boolean isGeneric) {
                        if (isGeneric && !classFileVersion.isAtLeast(ClassFileVersion.JAVA_V5)) {
                            throw new IllegalStateException("Cannot define generic method '" + name + "' for class file version " + classFileVersion);
                        } else if (!isVirtual && isAbstract) {
                            throw new IllegalStateException("Cannot define static or non-virtual method '" + name + "' to be abstract");
                        }
                    }

                    @Override
                    public void assertAnnotation() {
                        if (classFileVersion.isLessThan(ClassFileVersion.JAVA_V5)) {
                            throw new IllegalStateException("Cannot write annotations for class file version " + classFileVersion);
                        }
                    }

                    @Override
                    public void assertTypeAnnotation() {
                        if (classFileVersion.isLessThan(ClassFileVersion.JAVA_V5)) {
                            throw new IllegalStateException("Cannot write type annotations for class file version " + classFileVersion);
                        }
                    }

                    @Override
                    public void assertDefaultValue(String name) {
                        /* do nothing, implicitly checked by type assertion */
                    }

                    @Override
                    public void assertDefaultMethodCall() {
                        if (classFileVersion.isLessThan(ClassFileVersion.JAVA_V8)) {
                            throw new IllegalStateException("Cannot invoke default method for class file version " + classFileVersion);
                        }
                    }

                    @Override
                    public void assertTypeInConstantPool() {
                        if (classFileVersion.isLessThan(ClassFileVersion.JAVA_V5)) {
                            throw new IllegalStateException("Cannot write type to constant pool for class file version " + classFileVersion);
                        }
                    }

                    @Override
                    public void assertMethodTypeInConstantPool() {
                        if (classFileVersion.isLessThan(ClassFileVersion.JAVA_V7)) {
                            throw new IllegalStateException("Cannot write method type to constant pool for class file version " + classFileVersion);
                        }
                    }

                    @Override
                    public void assertHandleInConstantPool() {
                        if (classFileVersion.isLessThan(ClassFileVersion.JAVA_V7)) {
                            throw new IllegalStateException("Cannot write method handle to constant pool for class file version " + classFileVersion);
                        }
                    }

                    @Override
                    public void assertInvokeDynamic() {
                        if (classFileVersion.isLessThan(ClassFileVersion.JAVA_V7)) {
                            throw new IllegalStateException("Cannot write invoke dynamic instruction for class file version " + classFileVersion);
                        }
                    }

                    @Override
                    public void assertSubRoutine() {
                        if (classFileVersion.isGreaterThan(ClassFileVersion.JAVA_V5)) {
                            throw new IllegalStateException("Cannot write subroutine for class file version " + classFileVersion);
                        }
                    }

                    @Override
                    public void assertDynamicValueInConstantPool() {
                        if (classFileVersion.isLessThan(ClassFileVersion.JAVA_V11)) {
                            throw new IllegalStateException("Cannot write dynamic constant for class file version " + classFileVersion);
                        }
                    }

                    @Override
                    public void assertNestMate() {
                        if (classFileVersion.isLessThan(ClassFileVersion.JAVA_V11)) {
                            throw new IllegalStateException("Cannot define nest mate for class file version " + classFileVersion);
                        }
                    }
                }

                /**
                 * A constraint implementation that summarizes several constraints. 总结了几个约束的约束实现
                 */
                @HashCodeAndEqualsPlugin.Enhance
                class Compound implements Constraint {

                    /**
                     * A list of constraints that is enforced in the given order.
                     */
                    private final List<Constraint> constraints;

                    /**
                     * Creates a new compound constraint.
                     *
                     * @param constraints A list of constraints that is enforced in the given order.
                     */
                    public Compound(List<? extends Constraint> constraints) {
                        this.constraints = new ArrayList<Constraint>();
                        for (Constraint constraint : constraints) {
                            if (constraint instanceof Compound) {
                                this.constraints.addAll(((Compound) constraint).constraints);
                            } else {
                                this.constraints.add(constraint);
                            }
                        }
                    }

                    @Override
                    public void assertType(int modifier, boolean definesInterfaces, boolean isGeneric) {
                        for (Constraint constraint : constraints) {
                            constraint.assertType(modifier, definesInterfaces, isGeneric);
                        }
                    }

                    @Override
                    public void assertField(String name, boolean isPublic, boolean isStatic, boolean isFinal, boolean isGeneric) {
                        for (Constraint constraint : constraints) {
                            constraint.assertField(name, isPublic, isStatic, isFinal, isGeneric);
                        }
                    }

                    @Override
                    public void assertMethod(String name,
                                             boolean isAbstract,
                                             boolean isPublic,
                                             boolean isPrivate,
                                             boolean isStatic,
                                             boolean isVirtual,
                                             boolean isConstructor,
                                             boolean isDefaultValueIncompatible,
                                             boolean isGeneric) {
                        for (Constraint constraint : constraints) {
                            constraint.assertMethod(name,
                                    isAbstract,
                                    isPublic,
                                    isPrivate,
                                    isStatic,
                                    isVirtual,
                                    isConstructor,
                                    isDefaultValueIncompatible,
                                    isGeneric);
                        }
                    }

                    @Override
                    public void assertDefaultValue(String name) {
                        for (Constraint constraint : constraints) {
                            constraint.assertDefaultValue(name);
                        }
                    }

                    @Override
                    public void assertDefaultMethodCall() {
                        for (Constraint constraint : constraints) {
                            constraint.assertDefaultMethodCall();
                        }
                    }

                    @Override
                    public void assertAnnotation() {
                        for (Constraint constraint : constraints) {
                            constraint.assertAnnotation();
                        }
                    }

                    @Override
                    public void assertTypeAnnotation() {
                        for (Constraint constraint : constraints) {
                            constraint.assertTypeAnnotation();
                        }
                    }

                    @Override
                    public void assertTypeInConstantPool() {
                        for (Constraint constraint : constraints) {
                            constraint.assertTypeInConstantPool();
                        }
                    }

                    @Override
                    public void assertMethodTypeInConstantPool() {
                        for (Constraint constraint : constraints) {
                            constraint.assertMethodTypeInConstantPool();
                        }
                    }

                    @Override
                    public void assertHandleInConstantPool() {
                        for (Constraint constraint : constraints) {
                            constraint.assertHandleInConstantPool();
                        }
                    }

                    @Override
                    public void assertInvokeDynamic() {
                        for (Constraint constraint : constraints) {
                            constraint.assertInvokeDynamic();
                        }
                    }

                    @Override
                    public void assertSubRoutine() {
                        for (Constraint constraint : constraints) {
                            constraint.assertSubRoutine();
                        }
                    }

                    @Override
                    public void assertDynamicValueInConstantPool() {
                        for (Constraint constraint : constraints) {
                            constraint.assertDynamicValueInConstantPool();
                        }
                    }

                    @Override
                    public void assertNestMate() {
                        for (Constraint constraint : constraints) {
                            constraint.assertNestMate();
                        }
                    }
                }
            }

            /**
             * A field validator for checking default values.
             */
            protected class ValidatingFieldVisitor extends FieldVisitor {

                /**
                 * Creates a validating field visitor.
                 *
                 * @param fieldVisitor The field visitor to which any calls are delegated to.
                 */
                protected ValidatingFieldVisitor(FieldVisitor fieldVisitor) {
                    super(OpenedClassReader.ASM_API, fieldVisitor);
                }

                @Override
                public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                    constraint.assertAnnotation();
                    return super.visitAnnotation(desc, visible);
                }
            }

            /**
             * A method validator for checking default values. 用于检查默认值的方法验证程序
             */
            protected class ValidatingMethodVisitor extends MethodVisitor {

                /**
                 * The name of the method being visited. 正在访问的方法的名称
                 */
                private final String name;

                /**
                 * Creates a validating method visitor. 创建验证方法访问者
                 *
                 * @param methodVisitor The method visitor to which any calls are delegated to. 对其委派任何调用的方法访问者
                 * @param name          The name of the method being visited.
                 */
                protected ValidatingMethodVisitor(MethodVisitor methodVisitor, String name) {
                    super(OpenedClassReader.ASM_API, methodVisitor);
                    this.name = name;
                }

                @Override
                public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                    constraint.assertAnnotation();
                    return super.visitAnnotation(desc, visible);
                }

                @Override
                public AnnotationVisitor visitAnnotationDefault() {
                    constraint.assertDefaultValue(name);
                    return super.visitAnnotationDefault();
                }

                @Override
                @SuppressFBWarnings(value = "SF_SWITCH_NO_DEFAULT", justification = "Fall through to default case is intentional")
                @SuppressWarnings("deprecation")
                public void visitLdcInsn(Object constant) {
                    if (constant instanceof Type) {
                        Type type = (Type) constant;
                        switch (type.getSort()) {
                            case Type.OBJECT:
                            case Type.ARRAY:
                                constraint.assertTypeInConstantPool();
                                break;
                            case Type.METHOD:
                                constraint.assertMethodTypeInConstantPool();
                                break;
                        }
                    } else if (constant instanceof Handle) {
                        constraint.assertHandleInConstantPool();
                    } else if (constant instanceof ConstantDynamic) {
                        constraint.assertDynamicValueInConstantPool();
                    }
                    super.visitLdcInsn(constant);
                }

                @Override
                public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                    if (isInterface && opcode == Opcodes.INVOKESPECIAL) {
                        constraint.assertDefaultMethodCall();
                    }
                    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                }

                @Override
                @SuppressWarnings("deprecation")
                public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethod, Object[] bootstrapArgument) {
                    constraint.assertInvokeDynamic();
                    for (Object constant : bootstrapArgument) {
                        if (constant instanceof ConstantDynamic) {
                            constraint.assertDynamicValueInConstantPool();
                        }
                    }
                    super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethod, bootstrapArgument);
                }

                @Override
                public void visitJumpInsn(int opcode, Label label) {
                    if (opcode == Opcodes.JSR) {
                        constraint.assertSubRoutine();
                    }
                    super.visitJumpInsn(opcode, label);
                }
            }
        }

        /**
         * A type writer that inlines the created type into an existing class file. 将创建的类型内联到现有类文件中的类型编写器
         *
         * @param <U> The best known loaded type for the dynamically created type.
         */
        @HashCodeAndEqualsPlugin.Enhance
        public abstract static class ForInlining<U> extends Default<U> {

            /**
             * Indicates that a field should be ignored.
             */
            private static final FieldVisitor IGNORE_FIELD = null;

            /**
             * Indicates that a method should be ignored.
             */
            private static final MethodVisitor IGNORE_METHOD = null;

            /**
             * Indicates that an annotation should be ignored. 指示应忽略注解
             */
            private static final AnnotationVisitor IGNORE_ANNOTATION = null;

            /**
             * The original type's description. 原始类型的描述
             */
            protected final TypeDescription originalType;

            /**
             * The class file locator for locating the original type's class file. 用于定位原始类型的类文件的类文件定位器
             */
            protected final ClassFileLocator classFileLocator;

            /**
             * Creates a new inlining type writer. 创建新的内联类型编写器
             *
             * @param instrumentedType             The instrumented type to be created.
             * @param classFileVersion             The class file specified by the user.
             * @param fieldPool                    The field pool to use.
             * @param auxiliaryTypes               The explicit auxiliary types to add to the created type.
             * @param fields                       The instrumented type's declared fields.
             * @param methods                      The instrumented type's declared and virtually inherited methods.
             * @param instrumentedMethods          The instrumented methods relevant to this type creation.
             * @param loadedTypeInitializer        The loaded type initializer to apply onto the created type after loading.
             * @param typeInitializer              The type initializer to include in the created type's type initializer.
             * @param typeAttributeAppender        The type attribute appender to apply onto the instrumented type.
             * @param asmVisitorWrapper            The ASM visitor wrapper to apply onto the class writer.
             * @param annotationValueFilterFactory The annotation value filter factory to apply.
             * @param annotationRetention          The annotation retention to apply.
             * @param auxiliaryTypeNamingStrategy  The naming strategy for auxiliary types to apply.
             * @param implementationContextFactory The implementation context factory to apply.
             * @param typeValidation               Determines if a type should be explicitly validated.
             * @param classWriterStrategy          The class writer strategy to use.
             * @param typePool                     The type pool to use for computing stack map frames, if required.
             * @param originalType                 The original type's description.
             * @param classFileLocator             The class file locator for locating the original type's class file.
             */
            protected ForInlining(TypeDescription instrumentedType,
                                  ClassFileVersion classFileVersion,
                                  FieldPool fieldPool,
                                  List<? extends DynamicType> auxiliaryTypes,
                                  FieldList<FieldDescription.InDefinedShape> fields,
                                  MethodList<?> methods,
                                  MethodList<?> instrumentedMethods,
                                  LoadedTypeInitializer loadedTypeInitializer,
                                  TypeInitializer typeInitializer,
                                  TypeAttributeAppender typeAttributeAppender,
                                  AsmVisitorWrapper asmVisitorWrapper,
                                  AnnotationValueFilter.Factory annotationValueFilterFactory,
                                  AnnotationRetention annotationRetention,
                                  AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                                  Implementation.Context.Factory implementationContextFactory,
                                  TypeValidation typeValidation,
                                  ClassWriterStrategy classWriterStrategy,
                                  TypePool typePool,
                                  TypeDescription originalType,
                                  ClassFileLocator classFileLocator) {
                super(instrumentedType,
                        classFileVersion,
                        fieldPool,
                        auxiliaryTypes,
                        fields,
                        methods,
                        instrumentedMethods,
                        loadedTypeInitializer,
                        typeInitializer,
                        typeAttributeAppender,
                        asmVisitorWrapper,
                        annotationValueFilterFactory,
                        annotationRetention,
                        auxiliaryTypeNamingStrategy,
                        implementationContextFactory,
                        typeValidation,
                        classWriterStrategy,
                        typePool);
                this.originalType = originalType;
                this.classFileLocator = classFileLocator;
            }

            @Override
            protected UnresolvedType create(TypeInitializer typeInitializer) {
                try {
                    int writerFlags = asmVisitorWrapper.mergeWriter(AsmVisitorWrapper.NO_FLAGS);
                    int readerFlags = asmVisitorWrapper.mergeReader(AsmVisitorWrapper.NO_FLAGS);
                    byte[] binaryRepresentation = classFileLocator.locate(originalType.getName()).resolve();
                    ClassDumpAction.dump(DUMP_FOLDER, instrumentedType, true, binaryRepresentation);
                    ClassReader classReader = OpenedClassReader.of(binaryRepresentation);
                    ClassWriter classWriter = classWriterStrategy.resolve(writerFlags, typePool, classReader);
                    ContextRegistry contextRegistry = new ContextRegistry();
                    classReader.accept(writeTo(ValidatingClassVisitor.of(classWriter, typeValidation),
                            typeInitializer,
                            contextRegistry,
                            writerFlags,
                            readerFlags), readerFlags);
                    return new UnresolvedType(classWriter.toByteArray(), contextRegistry.getAuxiliaryTypes());
                } catch (IOException exception) {
                    throw new RuntimeException("The class file could not be written", exception);
                }
            }

            /**
             * Creates a class visitor which weaves all changes and additions on the fly. 创建一个类访问者，动态地组织所有更改和添加
             *
             * @param classVisitor    The class visitor to which this entry is to be written to.
             * @param typeInitializer The type initializer to apply.
             * @param contextRegistry A context registry to register the lazily created implementation context to.
             * @param writerFlags     The writer flags being used.
             * @param readerFlags     The reader flags being used.
             * @return A class visitor which is capable of applying the changes.
             */
            protected abstract ClassVisitor writeTo(ClassVisitor classVisitor,
                                                    TypeInitializer typeInitializer,
                                                    ContextRegistry contextRegistry,
                                                    int writerFlags,
                                                    int readerFlags);

            /**
             * A context registry allows to extract auxiliary types from a lazily created implementation context.
             */
            protected static class ContextRegistry {

                /**
                 * The implementation context that is used for creating a class or {@code null} if it was not registered.
                 */
                private Implementation.Context.ExtractableView implementationContext;

                /**
                 * Registers the implementation context.
                 *
                 * @param implementationContext The implementation context.
                 */
                public void setImplementationContext(Implementation.Context.ExtractableView implementationContext) {
                    this.implementationContext = implementationContext;
                }

                /**
                 * Returns the auxiliary types that were registered during class creation. This method must only be called after
                 * a class was created.
                 *
                 * @return The auxiliary types that were registered during class creation
                 */
                @SuppressFBWarnings(value = "UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", justification = "Lazy value definition is intended")
                public List<DynamicType> getAuxiliaryTypes() {
                    return implementationContext.getAuxiliaryTypes();
                }
            }

            /**
             * A default type writer that reprocesses a type completely.
             *
             * @param <V> The best known loaded type for the dynamically created type.
             */
            @HashCodeAndEqualsPlugin.Enhance
            protected static class WithFullProcessing<V> extends ForInlining<V> {

                /**
                 * The method registry to use.
                 */
                private final MethodRegistry.Prepared methodRegistry;

                /**
                 * The implementation target factory to use.
                 */
                private final Implementation.Target.Factory implementationTargetFactory;

                /**
                 * The method rebase resolver to use for rebasing methods.
                 */
                private final MethodRebaseResolver methodRebaseResolver;

                /**
                 * Creates a new inlining type writer that fully reprocesses a type.
                 *
                 * @param instrumentedType             The instrumented type to be created.
                 * @param classFileVersion             The class file specified by the user.
                 * @param fieldPool                    The field pool to use.
                 * @param auxiliaryTypes               The explicit auxiliary types to add to the created type.
                 * @param fields                       The instrumented type's declared fields.
                 * @param methods                      The instrumented type's declared and virtually inherited methods.
                 * @param instrumentedMethods          The instrumented methods relevant to this type creation.
                 * @param loadedTypeInitializer        The loaded type initializer to apply onto the created type after loading.
                 * @param typeInitializer              The type initializer to include in the created type's type initializer.
                 * @param typeAttributeAppender        The type attribute appender to apply onto the instrumented type.
                 * @param asmVisitorWrapper            The ASM visitor wrapper to apply onto the class writer.
                 * @param annotationValueFilterFactory The annotation value filter factory to apply.
                 * @param annotationRetention          The annotation retention to apply.
                 * @param auxiliaryTypeNamingStrategy  The naming strategy for auxiliary types to apply.
                 * @param implementationContextFactory The implementation context factory to apply.
                 * @param typeValidation               Determines if a type should be explicitly validated.
                 * @param classWriterStrategy          The class writer strategy to use.
                 * @param typePool                     The type pool to use for computing stack map frames, if required.
                 * @param originalType                 The original type's description.
                 * @param classFileLocator             The class file locator for locating the original type's class file.
                 * @param methodRegistry               The method registry to use.
                 * @param implementationTargetFactory  The implementation target factory to use.
                 * @param methodRebaseResolver         The method rebase resolver to use for rebasing methods.
                 */
                protected WithFullProcessing(TypeDescription instrumentedType,
                                             ClassFileVersion classFileVersion,
                                             FieldPool fieldPool,
                                             List<? extends DynamicType> auxiliaryTypes,
                                             FieldList<FieldDescription.InDefinedShape> fields,
                                             MethodList<?> methods, MethodList<?> instrumentedMethods,
                                             LoadedTypeInitializer loadedTypeInitializer,
                                             TypeInitializer typeInitializer,
                                             TypeAttributeAppender typeAttributeAppender,
                                             AsmVisitorWrapper asmVisitorWrapper,
                                             AnnotationValueFilter.Factory annotationValueFilterFactory,
                                             AnnotationRetention annotationRetention,
                                             AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                                             Implementation.Context.Factory implementationContextFactory,
                                             TypeValidation typeValidation,
                                             ClassWriterStrategy classWriterStrategy,
                                             TypePool typePool,
                                             TypeDescription originalType,
                                             ClassFileLocator classFileLocator,
                                             MethodRegistry.Prepared methodRegistry,
                                             Implementation.Target.Factory implementationTargetFactory,
                                             MethodRebaseResolver methodRebaseResolver) {
                    super(instrumentedType,
                            classFileVersion,
                            fieldPool,
                            auxiliaryTypes,
                            fields,
                            methods,
                            instrumentedMethods,
                            loadedTypeInitializer,
                            typeInitializer,
                            typeAttributeAppender,
                            asmVisitorWrapper,
                            annotationValueFilterFactory,
                            annotationRetention,
                            auxiliaryTypeNamingStrategy,
                            implementationContextFactory,
                            typeValidation,
                            classWriterStrategy,
                            typePool,
                            originalType,
                            classFileLocator);
                    this.methodRegistry = methodRegistry;
                    this.implementationTargetFactory = implementationTargetFactory;
                    this.methodRebaseResolver = methodRebaseResolver;
                }

                @Override
                protected ClassVisitor writeTo(ClassVisitor classVisitor, TypeInitializer typeInitializer, ContextRegistry contextRegistry, int writerFlags, int readerFlags) {
                    classVisitor = new RedefinitionClassVisitor(classVisitor, typeInitializer, contextRegistry, writerFlags, readerFlags);
                    return originalType.getName().equals(instrumentedType.getName())
                            ? classVisitor
                            : new OpenedClassRemapper(classVisitor, new SimpleRemapper(originalType.getInternalName(), instrumentedType.getInternalName()));
                }

                /**
                 * A {@link ClassRemapper} that uses the Byte Buddy-defined API version.
                 */
                protected static class OpenedClassRemapper extends ClassRemapper {

                    /**
                     * Creates a new opened class remapper.
                     *
                     * @param classVisitor The class visitor to wrap
                     * @param remapper     The remapper to apply.
                     */
                    protected OpenedClassRemapper(ClassVisitor classVisitor, Remapper remapper) {
                        super(OpenedClassReader.ASM_API, classVisitor, remapper);
                    }
                }

                /**
                 * An initialization handler is responsible for handling the creation of the type initializer. 初始化处理程序负责处理类型初始值设定项的创建
                 */
                protected interface InitializationHandler {

                    /**
                     * Invoked upon completion of writing the instrumented type. 在完成编写插入指令的类型时调用
                     *
                     * @param classVisitor          The class visitor to write any methods to. 类访问者来编写任何方法
                     * @param implementationContext The implementation context to use.
                     */
                    void complete(ClassVisitor classVisitor, Implementation.Context.ExtractableView implementationContext);

                    /**
                     * An initialization handler that creates a new type initializer. 创建新类型初始值设定项的初始化处理程序
                     */
                    class Creating extends TypeInitializer.Drain.Default implements InitializationHandler {

                        /**
                         * Creates a new creating initialization handler. 创建新的创建初始化处理程序
                         *
                         * @param instrumentedType             The instrumented type.
                         * @param methodPool                   The method pool to use.
                         * @param annotationValueFilterFactory The annotation value filter factory to use.
                         */
                        protected Creating(TypeDescription instrumentedType,
                                           MethodPool methodPool,
                                           AnnotationValueFilter.Factory annotationValueFilterFactory) {
                            super(instrumentedType, methodPool, annotationValueFilterFactory);
                        }

                        @Override
                        public void complete(ClassVisitor classVisitor, Implementation.Context.ExtractableView implementationContext) {
                            implementationContext.drain(this, classVisitor, annotationValueFilterFactory);
                        }
                    }

                    /**
                     * An initialization handler that appends code to a previously visited type initializer. 将代码附加到以前访问的类型初始值设定项的初始化处理程序
                     */
                    abstract class Appending extends MethodVisitor implements InitializationHandler, TypeInitializer.Drain {

                        /**
                         * The instrumented type.
                         */
                        protected final TypeDescription instrumentedType;

                        /**
                         * The method pool record for the type initializer. 类型初始值设定项的方法池记录
                         */
                        protected final MethodPool.Record record;

                        /**
                         * The used annotation value filter factory. 使用的注释值筛选器工厂
                         */
                        protected final AnnotationValueFilter.Factory annotationValueFilterFactory;

                        /**
                         * The frame writer to use.
                         */
                        protected final FrameWriter frameWriter;

                        /**
                         * The currently recorded stack size.
                         */
                        protected int stackSize;

                        /**
                         * The currently recorded local variable length. 当前记录的局部变量长度
                         */
                        protected int localVariableLength;

                        /**
                         * Creates a new appending initialization handler. 创建新的附加初始化处理程序
                         *
                         * @param methodVisitor                The underlying method visitor.
                         * @param instrumentedType             The instrumented type.
                         * @param record                       The method pool record for the type initializer.
                         * @param annotationValueFilterFactory The used annotation value filter factory.
                         * @param requireFrames                {@code true} if the visitor is required to add frames.
                         * @param expandFrames                 {@code true} if the visitor is required to expand any added frame.
                         */
                        protected Appending(MethodVisitor methodVisitor,
                                            TypeDescription instrumentedType,
                                            MethodPool.Record record,
                                            AnnotationValueFilter.Factory annotationValueFilterFactory,
                                            boolean requireFrames,
                                            boolean expandFrames) {
                            super(OpenedClassReader.ASM_API, methodVisitor);
                            this.instrumentedType = instrumentedType;
                            this.record = record;
                            this.annotationValueFilterFactory = annotationValueFilterFactory;
                            if (!requireFrames) {
                                frameWriter = FrameWriter.NoOp.INSTANCE;
                            } else if (expandFrames) {
                                frameWriter = FrameWriter.Expanding.INSTANCE;
                            } else {
                                frameWriter = new FrameWriter.Active();
                            }
                        }

                        /**
                         * Resolves an initialization handler.
                         *
                         * @param enabled                      {@code true} if the implementation context is enabled, i.e. any {@link TypeInitializer} might be active.
                         * @param methodVisitor                The delegation method visitor.
                         * @param instrumentedType             The instrumented type.
                         * @param methodPool                   The method pool to use.
                         * @param annotationValueFilterFactory The annotation value filter factory to use.
                         * @param requireFrames                {@code true} if frames must be computed.
                         * @param expandFrames                 {@code true} if frames must be expanded.
                         * @return An initialization handler which is also guaranteed to be a {@link MethodVisitor}.
                         */
                        protected static InitializationHandler of(boolean enabled,
                                                                  MethodVisitor methodVisitor,
                                                                  TypeDescription instrumentedType,
                                                                  MethodPool methodPool,
                                                                  AnnotationValueFilter.Factory annotationValueFilterFactory,
                                                                  boolean requireFrames,
                                                                  boolean expandFrames) {
                            return enabled
                                    ? withDrain(methodVisitor, instrumentedType, methodPool, annotationValueFilterFactory, requireFrames, expandFrames)
                                    : withoutDrain(methodVisitor, instrumentedType, methodPool, annotationValueFilterFactory, requireFrames, expandFrames);
                        }

                        /**
                         * Resolves an initialization handler with a drain.
                         *
                         * @param methodVisitor                The delegation method visitor.
                         * @param instrumentedType             The instrumented type.
                         * @param methodPool                   The method pool to use.
                         * @param annotationValueFilterFactory The annotation value filter factory to use.
                         * @param requireFrames                {@code true} if frames must be computed.
                         * @param expandFrames                 {@code true} if frames must be expanded.
                         * @return An initialization handler which is also guaranteed to be a {@link MethodVisitor}.
                         */
                        private static WithDrain withDrain(MethodVisitor methodVisitor,
                                                           TypeDescription instrumentedType,
                                                           MethodPool methodPool,
                                                           AnnotationValueFilter.Factory annotationValueFilterFactory,
                                                           boolean requireFrames,
                                                           boolean expandFrames) {
                            MethodPool.Record record = methodPool.target(new MethodDescription.Latent.TypeInitializer(instrumentedType));
                            return record.getSort().isImplemented()
                                    ? new WithDrain.WithActiveRecord(methodVisitor, instrumentedType, record, annotationValueFilterFactory, requireFrames, expandFrames)
                                    : new WithDrain.WithoutActiveRecord(methodVisitor, instrumentedType, record, annotationValueFilterFactory, requireFrames, expandFrames);
                        }

                        /**
                         * Resolves an initialization handler without a drain.
                         *
                         * @param methodVisitor                The delegation method visitor.
                         * @param instrumentedType             The instrumented type.
                         * @param methodPool                   The method pool to use.
                         * @param annotationValueFilterFactory The annotation value filter factory to use.
                         * @param requireFrames                {@code true} if frames must be computed.
                         * @param expandFrames                 {@code true} if frames must be expanded.
                         * @return An initialization handler which is also guaranteed to be a {@link MethodVisitor}.
                         */
                        private static WithoutDrain withoutDrain(MethodVisitor methodVisitor,
                                                                 TypeDescription instrumentedType,
                                                                 MethodPool methodPool,
                                                                 AnnotationValueFilter.Factory annotationValueFilterFactory,
                                                                 boolean requireFrames,
                                                                 boolean expandFrames) {
                            MethodPool.Record record = methodPool.target(new MethodDescription.Latent.TypeInitializer(instrumentedType));
                            return record.getSort().isImplemented()
                                    ? new WithoutDrain.WithActiveRecord(methodVisitor, instrumentedType, record, annotationValueFilterFactory, requireFrames, expandFrames)
                                    : new WithoutDrain.WithoutActiveRecord(methodVisitor, instrumentedType, record, annotationValueFilterFactory);
                        }

                        @Override
                        public void visitCode() {
                            record.applyAttributes(mv, annotationValueFilterFactory);
                            super.visitCode();
                            onStart();
                        }

                        /**
                         * Invoked after the user code was visited. 在访问用户代码后调用
                         */
                        protected abstract void onStart();

                        @Override
                        public void visitFrame(int type, int localVariableLength, Object[] localVariable, int stackSize, Object[] stack) {
                            super.visitFrame(type, localVariableLength, localVariable, stackSize, stack);
                            frameWriter.onFrame(type, localVariableLength);
                        }

                        @Override
                        public void visitMaxs(int stackSize, int localVariableLength) {
                            this.stackSize = stackSize;
                            this.localVariableLength = localVariableLength;
                        }

                        @Override
                        public void visitEnd() {
                            onEnd();
                        }

                        /**
                         * Invoked after the user code was completed. 在用户代码完成后调用
                         */
                        protected abstract void onEnd();

                        @Override
                        public void apply(ClassVisitor classVisitor, TypeInitializer typeInitializer, Implementation.Context implementationContext) {
                            ByteCodeAppender.Size size = typeInitializer.apply(mv, implementationContext, new MethodDescription.Latent.TypeInitializer(instrumentedType));
                            stackSize = Math.max(stackSize, size.getOperandStackSize());
                            localVariableLength = Math.max(localVariableLength, size.getLocalVariableSize());
                            onComplete(implementationContext);
                        }

                        /**
                         * Invoked upon completion of writing the type initializer.
                         *
                         * @param implementationContext The implementation context to use.
                         */
                        protected abstract void onComplete(Implementation.Context implementationContext);

                        @Override
                        public void complete(ClassVisitor classVisitor, Implementation.Context.ExtractableView implementationContext) {
                            implementationContext.drain(this, classVisitor, annotationValueFilterFactory);
                            mv.visitMaxs(stackSize, localVariableLength);
                            mv.visitEnd();
                        }

                        /**
                         * A frame writer is responsible for adding empty frames on jump instructions. 帧编写器负责在跳转指令上添加空帧
                         */
                        protected interface FrameWriter {

                            /**
                             * An empty array.
                             */
                            Object[] EMPTY = new Object[0];

                            /**
                             * Informs this frame writer of an observed frame. 将观察到的帧通知此帧编写器
                             *
                             * @param type                The frame type.
                             * @param localVariableLength The length of the local variables array.
                             */
                            void onFrame(int type, int localVariableLength);

                            /**
                             * Emits an empty frame.
                             *
                             * @param methodVisitor The method visitor to write the frame to.
                             */
                            void emitFrame(MethodVisitor methodVisitor);

                            /**
                             * A non-operational frame writer. 一种不可操作的框架编写器
                             */
                            enum NoOp implements FrameWriter {

                                /**
                                 * The singleton instance.
                                 */
                                INSTANCE;

                                @Override
                                public void onFrame(int type, int localVariableLength) {
                                    /* do nothing */
                                }

                                @Override
                                public void emitFrame(MethodVisitor methodVisitor) {
                                    /* do nothing */
                                }
                            }

                            /**
                             * A frame writer that creates an expanded frame. 创建扩展帧的帧编写器
                             */
                            enum Expanding implements FrameWriter {

                                /**
                                 * The singleton instance.
                                 */
                                INSTANCE;

                                @Override
                                public void onFrame(int type, int localVariableLength) {
                                    /* do nothing */
                                }

                                @Override
                                public void emitFrame(MethodVisitor methodVisitor) {
                                    methodVisitor.visitFrame(Opcodes.F_NEW, EMPTY.length, EMPTY, EMPTY.length, EMPTY);
                                }
                            }

                            /**
                             * An active frame writer that creates the most efficient frame. 创建最有效帧的活动帧编写器
                             */
                            class Active implements FrameWriter {

                                /**
                                 * The current length of the current local variable array. 当前局部变量数组的当前长度
                                 */
                                private int currentLocalVariableLength;

                                @Override
                                public void onFrame(int type, int localVariableLength) {
                                    switch (type) {
                                        case Opcodes.F_SAME:
                                        case Opcodes.F_SAME1:
                                            break;
                                        case Opcodes.F_APPEND:
                                            currentLocalVariableLength += localVariableLength;
                                            break;
                                        case Opcodes.F_CHOP:
                                            currentLocalVariableLength -= localVariableLength;
                                            break;
                                        case Opcodes.F_NEW:
                                        case Opcodes.F_FULL:
                                            currentLocalVariableLength = localVariableLength;
                                            break;
                                        default:
                                            throw new IllegalStateException("Unexpected frame type: " + type);
                                    }
                                }

                                @Override
                                public void emitFrame(MethodVisitor methodVisitor) {
                                    if (currentLocalVariableLength == 0) {
                                        methodVisitor.visitFrame(Opcodes.F_SAME, EMPTY.length, EMPTY, EMPTY.length, EMPTY);
                                    } else if (currentLocalVariableLength > 3) {
                                        methodVisitor.visitFrame(Opcodes.F_FULL, EMPTY.length, EMPTY, EMPTY.length, EMPTY);
                                    } else {
                                        methodVisitor.visitFrame(Opcodes.F_CHOP, currentLocalVariableLength, EMPTY, EMPTY.length, EMPTY);
                                    }
                                    currentLocalVariableLength = 0;
                                }
                            }
                        }

                        /**
                         * An initialization handler that appends code to a previously visited type initializer without allowing active
                         * {@link TypeInitializer} registrations. 一个初始化处理程序，它将代码附加到以前访问过的类型初始值设定项，而不允许活动的{@link TypeInitializer}注册
                         */
                        protected abstract static class WithoutDrain extends Appending {

                            /**
                             * Creates a new appending initialization handler without a drain.
                             *
                             * @param methodVisitor                The underlying method visitor.
                             * @param instrumentedType             The instrumented type.
                             * @param record                       The method pool record for the type initializer.
                             * @param annotationValueFilterFactory The used annotation value filter factory.
                             * @param requireFrames                {@code true} if the visitor is required to add frames.
                             * @param expandFrames                 {@code true} if the visitor is required to expand any added frame.
                             */
                            protected WithoutDrain(MethodVisitor methodVisitor,
                                                   TypeDescription instrumentedType,
                                                   MethodPool.Record record,
                                                   AnnotationValueFilter.Factory annotationValueFilterFactory,
                                                   boolean requireFrames,
                                                   boolean expandFrames) {
                                super(methodVisitor, instrumentedType, record, annotationValueFilterFactory, requireFrames, expandFrames);
                            }

                            @Override
                            protected void onStart() {
                                /* do nothing */
                            }

                            @Override
                            protected void onEnd() {
                                /* do nothing */
                            }

                            /**
                             * An initialization handler that appends code to a previously visited type initializer without allowing active
                             * {@link TypeInitializer} registrations and without an active record. 一种初始化处理程序，在不允许活动 {@link TypeInitializer} 注册且没有活动记录的情况下，将代码附加到以前访问过的类型初始值设定项
                             */
                            protected static class WithoutActiveRecord extends WithoutDrain {

                                /**
                                 * Creates a new appending initialization handler without a drain and without an active record. 创建一个新的附加初始化处理程序，该处理程序不带drain和活动记录
                                 *
                                 * @param methodVisitor                The underlying method visitor.
                                 * @param instrumentedType             The instrumented type.
                                 * @param record                       The method pool record for the type initializer.
                                 * @param annotationValueFilterFactory The used annotation value filter factory.
                                 */
                                protected WithoutActiveRecord(MethodVisitor methodVisitor,
                                                              TypeDescription instrumentedType,
                                                              MethodPool.Record record,
                                                              AnnotationValueFilter.Factory annotationValueFilterFactory) {
                                    super(methodVisitor, instrumentedType, record, annotationValueFilterFactory, false, false);
                                }

                                @Override
                                protected void onComplete(Implementation.Context implementationContext) {
                                    /* do nothing */
                                }
                            }

                            /**
                             * An initialization handler that appends code to a previously visited type initializer without allowing active
                             * {@link TypeInitializer} registrations and with an active record. 一种初始化处理程序，它将代码附加到以前访问过的类型初始值设定项中，而不允许活动{@link TypeInitializer}注册，并且具有活动记录
                             */
                            protected static class WithActiveRecord extends WithoutDrain {

                                /**
                                 * The label that indicates the beginning of the active record.
                                 */
                                private final Label label;

                                /**
                                 * Creates a new appending initialization handler without a drain and with an active record.
                                 *
                                 * @param methodVisitor                The underlying method visitor.
                                 * @param instrumentedType             The instrumented type.
                                 * @param record                       The method pool record for the type initializer.
                                 * @param annotationValueFilterFactory The used annotation value filter factory.
                                 * @param requireFrames                {@code true} if the visitor is required to add frames.
                                 * @param expandFrames                 {@code true} if the visitor is required to expand any added frame.
                                 */
                                protected WithActiveRecord(MethodVisitor methodVisitor,
                                                           TypeDescription instrumentedType,
                                                           MethodPool.Record record,
                                                           AnnotationValueFilter.Factory annotationValueFilterFactory,
                                                           boolean requireFrames,
                                                           boolean expandFrames) {
                                    super(methodVisitor, instrumentedType, record, annotationValueFilterFactory, requireFrames, expandFrames);
                                    label = new Label();
                                }

                                @Override
                                public void visitInsn(int opcode) {
                                    if (opcode == Opcodes.RETURN) {
                                        mv.visitJumpInsn(Opcodes.GOTO, label);
                                    } else {
                                        super.visitInsn(opcode);
                                    }
                                }

                                @Override
                                protected void onComplete(Implementation.Context implementationContext) {
                                    mv.visitLabel(label);
                                    frameWriter.emitFrame(mv);
                                    ByteCodeAppender.Size size = record.applyCode(mv, implementationContext);
                                    stackSize = Math.max(stackSize, size.getOperandStackSize());
                                    localVariableLength = Math.max(localVariableLength, size.getLocalVariableSize());
                                }

                            }
                        }

                        /**
                         * An initialization handler that appends code to a previously visited type initializer with allowing active
                         * {@link TypeInitializer} registrations. 一个初始化处理程序，它将代码附加到以前访问过的类型初始值设定项，并允许活动的{@link TypeInitializer}注册
                         */
                        protected abstract static class WithDrain extends Appending {

                            /**
                             * A label marking the beginning of the appended code. 标记附加代码开头的标签
                             */
                            protected final Label appended;

                            /**
                             * A label marking the beginning og the original type initializer's code. 标记原始类型初始值设定项代码开头的标签
                             */
                            protected final Label original;

                            /**
                             * Creates a new appending initialization handler with a drain.
                             *
                             * @param methodVisitor                The underlying method visitor.
                             * @param instrumentedType             The instrumented type.
                             * @param record                       The method pool record for the type initializer.
                             * @param annotationValueFilterFactory The used annotation value filter factory.
                             * @param requireFrames                {@code true} if the visitor is required to add frames.
                             * @param expandFrames                 {@code true} if the visitor is required to expand any added frame.
                             */
                            protected WithDrain(MethodVisitor methodVisitor,
                                                TypeDescription instrumentedType,
                                                MethodPool.Record record,
                                                AnnotationValueFilter.Factory annotationValueFilterFactory,
                                                boolean requireFrames,
                                                boolean expandFrames) {
                                super(methodVisitor, instrumentedType, record, annotationValueFilterFactory, requireFrames, expandFrames);
                                appended = new Label();
                                original = new Label();
                            }

                            @Override
                            protected void onStart() {
                                mv.visitJumpInsn(Opcodes.GOTO, appended);
                                mv.visitLabel(original);
                                frameWriter.emitFrame(mv);
                            }

                            @Override
                            protected void onEnd() {
                                mv.visitLabel(appended);
                                frameWriter.emitFrame(mv);
                            }

                            @Override
                            protected void onComplete(Implementation.Context implementationContext) {
                                mv.visitJumpInsn(Opcodes.GOTO, original);
                                afterComplete(implementationContext);
                            }

                            /**
                             * Invoked after completion of writing the type initializer. 在完成类型初始值设定项的写入后调用
                             *
                             * @param implementationContext The implementation context to use.
                             */
                            protected abstract void afterComplete(Implementation.Context implementationContext);

                            /**
                             * A code appending initialization handler with a drain that does not apply an explicit record. 用不应用显式记录的排出来附加初始化处理程序的代码
                             */
                            protected static class WithoutActiveRecord extends WithDrain {

                                /**
                                 * Creates a new appending initialization handler with a drain and without an active record. 创建一个新的附加初始化处理程序，其中包含一个drain，但没有活动记录
                                 *
                                 * @param methodVisitor                The underlying method visitor.
                                 * @param instrumentedType             The instrumented type.
                                 * @param record                       The method pool record for the type initializer.
                                 * @param annotationValueFilterFactory The used annotation value filter factory.
                                 * @param requireFrames                {@code true} if the visitor is required to add frames.
                                 * @param expandFrames                 {@code true} if the visitor is required to expand any added frame.
                                 */
                                protected WithoutActiveRecord(MethodVisitor methodVisitor,
                                                              TypeDescription instrumentedType,
                                                              MethodPool.Record record,
                                                              AnnotationValueFilter.Factory annotationValueFilterFactory,
                                                              boolean requireFrames,
                                                              boolean expandFrames) {
                                    super(methodVisitor, instrumentedType, record, annotationValueFilterFactory, requireFrames, expandFrames);
                                }

                                @Override
                                protected void afterComplete(Implementation.Context implementationContext) {
                                    /* do nothing */
                                }
                            }

                            /**
                             * A code appending initialization handler with a drain that applies an explicit record. 用应用显式记录的漏极附加初始化处理程序的代码
                             */
                            protected static class WithActiveRecord extends WithDrain {

                                /**
                                 * A label indicating the beginning of the record's code. 表示记录代码开头的标签
                                 */
                                private final Label label;

                                /**
                                 * Creates a new appending initialization handler with a drain and with an active record.
                                 *
                                 * @param methodVisitor                The underlying method visitor.
                                 * @param instrumentedType             The instrumented type.
                                 * @param record                       The method pool record for the type initializer.
                                 * @param annotationValueFilterFactory The used annotation value filter factory.
                                 * @param requireFrames                {@code true} if the visitor is required to add frames.
                                 * @param expandFrames                 {@code true} if the visitor is required to expand any added frame.
                                 */
                                protected WithActiveRecord(MethodVisitor methodVisitor,
                                                           TypeDescription instrumentedType,
                                                           MethodPool.Record record,
                                                           AnnotationValueFilter.Factory annotationValueFilterFactory,
                                                           boolean requireFrames,
                                                           boolean expandFrames) {
                                    super(methodVisitor, instrumentedType, record, annotationValueFilterFactory, requireFrames, expandFrames);
                                    label = new Label();
                                }

                                @Override
                                public void visitInsn(int opcode) {
                                    if (opcode == Opcodes.RETURN) {
                                        mv.visitJumpInsn(Opcodes.GOTO, label);
                                    } else {
                                        super.visitInsn(opcode);
                                    }
                                }

                                @Override
                                protected void afterComplete(Implementation.Context implementationContext) {
                                    mv.visitLabel(label);
                                    frameWriter.emitFrame(mv);
                                    ByteCodeAppender.Size size = record.applyCode(mv, implementationContext);
                                    stackSize = Math.max(stackSize, size.getOperandStackSize());
                                    localVariableLength = Math.max(localVariableLength, size.getLocalVariableSize());
                                }
                            }
                        }
                    }
                }

                /**
                 * A class visitor which is capable of applying a redefinition of an existing class file. 能够应用现有类文件的重新定义的类访问者
                 */
                @SuppressFBWarnings(value = "UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", justification = "Field access order is implied by ASM")
                protected class RedefinitionClassVisitor extends MetadataAwareClassVisitor {

                    /**
                     * The type initializer to apply.
                     */
                    private final TypeInitializer typeInitializer;

                    /**
                     * A context registry to register the lazily created implementation context to. 将延迟创建的实现上下文注册到的上下文注册表
                     */
                    private final ContextRegistry contextRegistry;

                    /**
                     * The writer flags being used.
                     */
                    private final int writerFlags;

                    /**
                     * The reader flags being used.
                     */
                    private final int readerFlags;

                    /**
                     * A mapping of fields to write by their names.
                     */
                    private final LinkedHashMap<String, FieldDescription> declarableFields;

                    /**
                     * A mapping of methods to write by a concatenation of internal name and descriptor. 通过连接内部名称和描述符来编写的方法的映射
                     */
                    private final LinkedHashMap<String, MethodDescription> declarableMethods;

                    /**
                     * A set of internal names of all nest members not yet defined by this type. If this type is not a nest host, this set is empty. 尚未由该类型定义的所有嵌套成员的一组内部名称。如果此类型不是嵌套主机，则此集合为空
                     */
                    private final Set<String> nestMembers;

                    /**
                     * A mapping of the internal names of all declared types to their description.
                     */
                    private final LinkedHashMap<String, TypeDescription> declaredTypes;

                    /**
                     * The method pool to use or {@code null} if the pool was not yet initialized.
                     */
                    private MethodPool methodPool;

                    /**
                     * The initialization handler to use or {@code null} if the handler was not yet initialized.
                     */
                    private InitializationHandler initializationHandler;

                    /**
                     * The implementation context for this class creation or {@code null} if it was not yet created.
                     */
                    private Implementation.Context.ExtractableView implementationContext;

                    /**
                     * {@code true} if the modifiers for deprecation should be retained.
                     */
                    private boolean retainDeprecationModifiers;

                    /**
                     * Creates a class visitor which is capable of redefining an existent class on the fly. 创建一个能够动态重新定义现有类的类访问者
                     *
                     * @param classVisitor    The underlying class visitor to which writes are delegated. 委托写入的底层类访问者
                     * @param typeInitializer The type initializer to apply.
                     * @param contextRegistry A context registry to register the lazily created implementation context to.
                     * @param writerFlags     The writer flags being used.
                     * @param readerFlags     The reader flags being used.
                     */
                    protected RedefinitionClassVisitor(ClassVisitor classVisitor,
                                                       TypeInitializer typeInitializer,
                                                       ContextRegistry contextRegistry,
                                                       int writerFlags,
                                                       int readerFlags) {
                        super(OpenedClassReader.ASM_API, classVisitor);
                        this.typeInitializer = typeInitializer;
                        this.contextRegistry = contextRegistry;
                        this.writerFlags = writerFlags;
                        this.readerFlags = readerFlags;
                        declarableFields = new LinkedHashMap<String, FieldDescription>();
                        for (FieldDescription fieldDescription : fields) {
                            declarableFields.put(fieldDescription.getInternalName() + fieldDescription.getDescriptor(), fieldDescription);
                        }
                        declarableMethods = new LinkedHashMap<String, MethodDescription>();
                        for (MethodDescription methodDescription : instrumentedMethods) {
                            declarableMethods.put(methodDescription.getInternalName() + methodDescription.getDescriptor(), methodDescription);
                        }
                        if (instrumentedType.isNestHost()) {
                            nestMembers = new LinkedHashSet<String>();
                            for (TypeDescription typeDescription : instrumentedType.getNestMembers().filter(not(is(instrumentedType)))) {
                                nestMembers.add(typeDescription.getInternalName());
                            }
                        } else {
                            nestMembers = Collections.emptySet();
                        }
                        declaredTypes = new LinkedHashMap<String, TypeDescription>();
                        for (TypeDescription typeDescription : instrumentedType.getDeclaredTypes()) {
                            declaredTypes.put(typeDescription.getInternalName(), typeDescription);
                        }
                    }

                    @Override
                    public void visit(int classFileVersionNumber,
                                      int modifiers,
                                      String internalName,
                                      String genericSignature,
                                      String superClassInternalName,
                                      String[] interfaceTypeInternalName) {
                        ClassFileVersion classFileVersion = ClassFileVersion.ofMinorMajor(classFileVersionNumber);
                        methodPool = methodRegistry.compile(implementationTargetFactory, classFileVersion);
                        initializationHandler = new InitializationHandler.Creating(instrumentedType, methodPool, annotationValueFilterFactory);
                        implementationContext = implementationContextFactory.make(instrumentedType,
                                auxiliaryTypeNamingStrategy,
                                typeInitializer,
                                classFileVersion,
                                WithFullProcessing.this.classFileVersion);
                        retainDeprecationModifiers = classFileVersion.isLessThan(ClassFileVersion.JAVA_V5);
                        contextRegistry.setImplementationContext(implementationContext);
                        cv = asmVisitorWrapper.wrap(instrumentedType,
                                cv,
                                implementationContext,
                                typePool,
                                fields,
                                methods,
                                writerFlags,
                                readerFlags);
                        cv.visit(classFileVersionNumber,
                                instrumentedType.getActualModifiers((modifiers & Opcodes.ACC_SUPER) != 0 && !instrumentedType.isInterface())
                                        | resolveDeprecationModifiers(modifiers)
                                        // Anonymous types might not preserve their class file's final modifier via their inner class modifier.
                                        | (((modifiers & Opcodes.ACC_FINAL) != 0 && instrumentedType.isAnonymousClass()) ? Opcodes.ACC_FINAL : 0),
                                instrumentedType.getInternalName(),
                                TypeDescription.AbstractBase.RAW_TYPES
                                        ? genericSignature
                                        : instrumentedType.getGenericSignature(),
                                instrumentedType.getSuperClass() == null
                                        ? (instrumentedType.isInterface() ? TypeDescription.OBJECT.getInternalName() : NO_REFERENCE)
                                        : instrumentedType.getSuperClass().asErasure().getInternalName(),
                                instrumentedType.getInterfaces().asErasures().toInternalNames());
                    }

                    @Override
                    protected void onVisitNestHost(String nestHost) {
                        onNestHost();
                    }

                    @Override
                    @SuppressWarnings("deprecation")
                    protected void onNestHost() {
                        if (!instrumentedType.isNestHost()) {
                            cv.visitNestHostExperimental(instrumentedType.getNestHost().getInternalName());
                        }
                    }

                    @Override
                    protected void onVisitOuterClass(String owner, String name, String descriptor) {
                        onOuterType();
                    }

                    @Override
                    protected void onOuterType() {
                        MethodDescription.InDefinedShape enclosingMethod = instrumentedType.getEnclosingMethod();
                        if (enclosingMethod != null) {
                            cv.visitOuterClass(enclosingMethod.getDeclaringType().getInternalName(),
                                    enclosingMethod.getInternalName(),
                                    enclosingMethod.getDescriptor());
                        } else {
                            TypeDescription enclosingType = instrumentedType.getEnclosingType();
                            if (enclosingType != null) {
                                cv.visitOuterClass(enclosingType.getInternalName(), NO_REFERENCE, NO_REFERENCE);
                            }
                        }
                    }

                    @Override
                    protected void onAfterAttributes() {
                        typeAttributeAppender.apply(cv, instrumentedType, annotationValueFilterFactory.on(instrumentedType));
                    }

                    @Override
                    protected AnnotationVisitor onVisitTypeAnnotation(int typeReference, TypePath typePath, String descriptor, boolean visible) {
                        return annotationRetention.isEnabled()
                                ? cv.visitTypeAnnotation(typeReference, typePath, descriptor, visible)
                                : IGNORE_ANNOTATION;
                    }

                    @Override
                    protected AnnotationVisitor onVisitAnnotation(String descriptor, boolean visible) {
                        return annotationRetention.isEnabled()
                                ? cv.visitAnnotation(descriptor, visible)
                                : IGNORE_ANNOTATION;
                    }

                    @Override
                    protected FieldVisitor onVisitField(int modifiers,
                                                   String internalName,
                                                   String descriptor,
                                                   String genericSignature,
                                                   Object defaultValue) {
                        FieldDescription fieldDescription = declarableFields.remove(internalName + descriptor);
                        if (fieldDescription != null) {
                            FieldPool.Record record = fieldPool.target(fieldDescription);
                            if (!record.isImplicit()) {
                                return redefine(record, defaultValue, modifiers, genericSignature);
                            }
                        }
                        return cv.visitField(modifiers, internalName, descriptor, genericSignature, defaultValue);
                    }

                    /**
                     * Redefines a field using the given explicit field pool record and default value. 使用给定的显式字段池记录和默认值重新定义字段
                     *
                     * @param record           The field pool value to apply during visitation of the existing field.
                     * @param defaultValue     The default value to write onto the field which might be {@code null}.
                     * @param modifiers        The original modifiers of the transformed field.
                     * @param genericSignature The field's original generic signature which can be {@code null}.
                     * @return A field visitor for visiting the existing field definition.
                     */
                    protected FieldVisitor redefine(FieldPool.Record record, Object defaultValue, int modifiers, String genericSignature) {
                        FieldDescription instrumentedField = record.getField();
                        FieldVisitor fieldVisitor = cv.visitField(instrumentedField.getActualModifiers() | resolveDeprecationModifiers(modifiers),
                                instrumentedField.getInternalName(),
                                instrumentedField.getDescriptor(),
                                TypeDescription.AbstractBase.RAW_TYPES
                                        ? genericSignature
                                        : instrumentedField.getGenericSignature(),
                                record.resolveDefault(defaultValue));
                        return fieldVisitor == null
                                ? IGNORE_FIELD
                                : new AttributeObtainingFieldVisitor(fieldVisitor, record);
                    }

                    @Override
                    protected MethodVisitor onVisitMethod(int modifiers,
                                                     String internalName,
                                                     String descriptor,
                                                     String genericSignature,
                                                     String[] exceptionName) {
                        if (internalName.equals(MethodDescription.TYPE_INITIALIZER_INTERNAL_NAME)) {
                            MethodVisitor methodVisitor = cv.visitMethod(modifiers, internalName, descriptor, genericSignature, exceptionName);
                            return methodVisitor == null
                                    ? IGNORE_METHOD
                                    : (MethodVisitor) (initializationHandler = InitializationHandler.Appending.of(implementationContext.isEnabled(),
                                    methodVisitor,
                                    instrumentedType,
                                    methodPool,
                                    annotationValueFilterFactory,
                                    (writerFlags & ClassWriter.COMPUTE_FRAMES) == 0 && implementationContext.getClassFileVersion().isAtLeast(ClassFileVersion.JAVA_V6),
                                    (readerFlags & ClassReader.EXPAND_FRAMES) != 0));
                        } else {
                            MethodDescription methodDescription = declarableMethods.remove(internalName + descriptor);
                            return methodDescription == null
                                    ? cv.visitMethod(modifiers, internalName, descriptor, genericSignature, exceptionName)
                                    : redefine(methodDescription, (modifiers & Opcodes.ACC_ABSTRACT) != 0, modifiers, genericSignature);
                        }
                    }

                    /**
                     * Redefines a given method if this is required by looking up a potential implementation from the
                     * {@link net.bytebuddy.dynamic.scaffold.TypeWriter.MethodPool}.
                     *
                     * @param methodDescription The method being considered for redefinition.
                     * @param abstractOrigin    {@code true} if the original method is abstract, i.e. there is no implementation to preserve.
                     * @param modifiers         The original modifiers of the transformed method.
                     * @param genericSignature  The method's original generic signature which can be {@code null}.
                     * @return A method visitor which is capable of consuming the original method.
                     */
                    protected MethodVisitor redefine(MethodDescription methodDescription, boolean abstractOrigin, int modifiers, String genericSignature) {
                        MethodPool.Record record = methodPool.target(methodDescription);
                        if (!record.getSort().isDefined()) {
                            return cv.visitMethod(methodDescription.getActualModifiers() | resolveDeprecationModifiers(modifiers),
                                    methodDescription.getInternalName(),
                                    methodDescription.getDescriptor(),
                                    TypeDescription.AbstractBase.RAW_TYPES
                                            ? genericSignature
                                            : methodDescription.getGenericSignature(),
                                    methodDescription.getExceptionTypes().asErasures().toInternalNames());
                        }
                        MethodDescription implementedMethod = record.getMethod();
                        MethodVisitor methodVisitor = cv.visitMethod(ModifierContributor.Resolver
                                        .of(Collections.singleton(record.getVisibility()))
                                        .resolve(implementedMethod.getActualModifiers(record.getSort().isImplemented())) | resolveDeprecationModifiers(modifiers),
                                implementedMethod.getInternalName(),
                                implementedMethod.getDescriptor(),
                                TypeDescription.AbstractBase.RAW_TYPES
                                        ? genericSignature
                                        : implementedMethod.getGenericSignature(),
                                implementedMethod.getExceptionTypes().asErasures().toInternalNames());
                        if (methodVisitor == null) {
                            return IGNORE_METHOD;
                        } else if (abstractOrigin) {
                            return new AttributeObtainingMethodVisitor(methodVisitor, record);
                        } else if (methodDescription.isNative()) {
                            MethodRebaseResolver.Resolution resolution = methodRebaseResolver.resolve(implementedMethod.asDefined());
                            if (resolution.isRebased()) {
                                MethodVisitor rebasedMethodVisitor = super.visitMethod(resolution.getResolvedMethod().getActualModifiers()
                                                | resolveDeprecationModifiers(modifiers),
                                        resolution.getResolvedMethod().getInternalName(),
                                        resolution.getResolvedMethod().getDescriptor(),
                                        TypeDescription.AbstractBase.RAW_TYPES
                                                ? genericSignature
                                                : implementedMethod.getGenericSignature(),
                                        resolution.getResolvedMethod().getExceptionTypes().asErasures().toInternalNames());
                                if (rebasedMethodVisitor != null) {
                                    rebasedMethodVisitor.visitEnd();
                                }
                            }
                            return new AttributeObtainingMethodVisitor(methodVisitor, record);
                        } else {
                            return new CodePreservingMethodVisitor(methodVisitor, record, methodRebaseResolver.resolve(implementedMethod.asDefined()));
                        }
                    }

                    @Override
                    protected void onVisitInnerClass(String internalName, String outerName, String innerName, int modifiers) {
                        if (!internalName.equals(instrumentedType.getInternalName()) && (declaredTypes.remove(internalName) != null || innerName == null)) {
                            cv.visitInnerClass(internalName, outerName, innerName, modifiers);
                        }
                    }

                    @Override
                    @SuppressWarnings("deprecation")
                    protected void onVisitNestMember(String nestMember) {
                        if (instrumentedType.isNestHost() && nestMembers.remove(nestMember)) {
                            cv.visitNestMemberExperimental(nestMember);
                        }
                    }

                    @Override
                    protected void onVisitEnd() {
                        for (FieldDescription fieldDescription : declarableFields.values()) {
                            fieldPool.target(fieldDescription).apply(cv, annotationValueFilterFactory);
                        }
                        for (MethodDescription methodDescription : declarableMethods.values()) {
                            methodPool.target(methodDescription).apply(cv, implementationContext, annotationValueFilterFactory);
                        }
                        initializationHandler.complete(cv, implementationContext);
                        for (TypeDescription typeDescription : declaredTypes.values()) {
                            cv.visitInnerClass(typeDescription.getInternalName(),
                                    instrumentedType.getInternalName(),
                                    typeDescription.getSimpleName(),
                                    typeDescription.getModifiers());
                        }
                        TypeDescription declaringType = instrumentedType.getDeclaringType();
                        if (declaringType != null) {
                            cv.visitInnerClass(instrumentedType.getInternalName(),
                                    declaringType.getInternalName(),
                                    instrumentedType.isAnonymousClass()
                                            ? NO_REFERENCE
                                            : instrumentedType.getSimpleName(),
                                    instrumentedType.getModifiers());
                        }
                        cv.visitEnd();
                    }

                    /**
                     * Returns {@link Opcodes#ACC_DEPRECATED} if the current class file version only represents deprecated methods using modifiers
                     * that are not exposed in the type description API what is true for class files before Java 5 and if the supplied modifiers indicate
                     * deprecation.
                     *
                     * @param modifiers The original modifiers.
                     * @return {@link Opcodes#ACC_DEPRECATED} if the supplied modifiers imply deprecation.
                     */
                    private int resolveDeprecationModifiers(int modifiers) {
                        return retainDeprecationModifiers && (modifiers & Opcodes.ACC_DEPRECATED) != 0
                                ? Opcodes.ACC_DEPRECATED
                                : ModifierContributor.EMPTY_MASK;
                    }

                    /**
                     * A field visitor that obtains all attributes and annotations of a field that is found in the
                     * class file but that discards all code. 一种字段访问器，它获取在类文件中找到的字段的所有属性和注释，但丢弃所有代码
                     */
                    protected class AttributeObtainingFieldVisitor extends FieldVisitor {

                        /**
                         * The field pool record to apply onto the field visitor.
                         */
                        private final FieldPool.Record record;

                        /**
                         * Creates a new attribute obtaining field visitor.
                         *
                         * @param fieldVisitor The field visitor to delegate to.
                         * @param record       The field pool record to apply onto the field visitor.
                         */
                        protected AttributeObtainingFieldVisitor(FieldVisitor fieldVisitor, FieldPool.Record record) {
                            super(OpenedClassReader.ASM_API, fieldVisitor);
                            this.record = record;
                        }

                        @Override
                        public AnnotationVisitor visitTypeAnnotation(int typeReference, TypePath typePath, String descriptor, boolean visible) {
                            return annotationRetention.isEnabled()
                                    ? super.visitTypeAnnotation(typeReference, typePath, descriptor, visible)
                                    : IGNORE_ANNOTATION;
                        }

                        @Override
                        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                            return annotationRetention.isEnabled()
                                    ? super.visitAnnotation(descriptor, visible)
                                    : IGNORE_ANNOTATION;
                        }

                        @Override
                        public void visitEnd() {
                            record.apply(fv, annotationValueFilterFactory);
                            super.visitEnd();
                        }
                    }

                    /**
                     * A method visitor that preserves the code of a method in the class file by copying it into a rebased
                     * method while copying all attributes and annotations to the actual method. 一种方法访问者，通过将类文件中某个方法的代码复制到一个重基方法中，同时将所有属性和注释复制到实际方法中，从而在类文件中保留该方法的代码
                     */
                    protected class CodePreservingMethodVisitor extends MethodVisitor {

                        /**
                         * The method visitor of the actual method.
                         */
                        private final MethodVisitor actualMethodVisitor;

                        /**
                         * The method pool entry to apply.
                         */
                        private final MethodPool.Record record;

                        /**
                         * The resolution of a potential rebased method.
                         */
                        private final MethodRebaseResolver.Resolution resolution;

                        /**
                         * Creates a new code preserving method visitor.
                         *
                         * @param actualMethodVisitor The method visitor of the actual method.
                         * @param record              The method pool entry to apply.
                         * @param resolution          The resolution of the method rebase resolver in use.
                         */
                        protected CodePreservingMethodVisitor(MethodVisitor actualMethodVisitor,
                                                              MethodPool.Record record,
                                                              MethodRebaseResolver.Resolution resolution) {
                            super(OpenedClassReader.ASM_API, actualMethodVisitor);
                            this.actualMethodVisitor = actualMethodVisitor;
                            this.record = record;
                            this.resolution = resolution;
                            record.applyHead(actualMethodVisitor);
                        }

                        @Override
                        public AnnotationVisitor visitAnnotationDefault() {
                            return IGNORE_ANNOTATION; // Annotation types can never be rebased.
                        }

                        @Override
                        public AnnotationVisitor visitTypeAnnotation(int typeReference, TypePath typePath, String descriptor, boolean visible) {
                            return annotationRetention.isEnabled()
                                    ? super.visitTypeAnnotation(typeReference, typePath, descriptor, visible)
                                    : IGNORE_ANNOTATION;
                        }

                        @Override
                        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                            return annotationRetention.isEnabled()
                                    ? super.visitAnnotation(descriptor, visible)
                                    : IGNORE_ANNOTATION;
                        }

                        @Override
                        public void visitAnnotableParameterCount(int count, boolean visible) {
                            if (annotationRetention.isEnabled()) {
                                super.visitAnnotableParameterCount(count, visible);
                            }
                        }

                        @Override
                        public AnnotationVisitor visitParameterAnnotation(int index, String descriptor, boolean visible) {
                            return annotationRetention.isEnabled()
                                    ? super.visitParameterAnnotation(index, descriptor, visible)
                                    : IGNORE_ANNOTATION;
                        }

                        @Override
                        public void visitCode() {
                            record.applyBody(actualMethodVisitor, implementationContext, annotationValueFilterFactory);
                            actualMethodVisitor.visitEnd();
                            mv = resolution.isRebased()
                                    ? cv.visitMethod(resolution.getResolvedMethod().getActualModifiers(),
                                    resolution.getResolvedMethod().getInternalName(),
                                    resolution.getResolvedMethod().getDescriptor(),
                                    resolution.getResolvedMethod().getGenericSignature(),
                                    resolution.getResolvedMethod().getExceptionTypes().asErasures().toInternalNames())
                                    : IGNORE_METHOD;
                            super.visitCode();
                        }

                        @Override
                        public void visitMaxs(int stackSize, int localVariableLength) {
                            super.visitMaxs(stackSize, Math.max(localVariableLength, resolution.getResolvedMethod().getStackSize()));
                        }
                    }

                    /**
                     * A method visitor that obtains all attributes and annotations of a method that is found in the
                     * class file but that discards all code. 一种方法访问程序，它获取在类文件中找到的方法的所有属性和注释，但丢弃所有代码
                     */
                    protected class AttributeObtainingMethodVisitor extends MethodVisitor {

                        /**
                         * The method visitor to which the actual method is to be written to. 要写入实际方法的方法访问器
                         */
                        private final MethodVisitor actualMethodVisitor;

                        /**
                         * The method pool entry to apply. 要应用的方法池条目
                         */
                        private final MethodPool.Record record;

                        /**
                         * Creates a new attribute obtaining method visitor. 创建一个新的属性获取方法
                         *
                         * @param actualMethodVisitor The method visitor of the actual method. 实际方法的方法访问者
                         * @param record              The method pool entry to apply.
                         */
                        protected AttributeObtainingMethodVisitor(MethodVisitor actualMethodVisitor, MethodPool.Record record) {
                            super(OpenedClassReader.ASM_API, actualMethodVisitor);
                            this.actualMethodVisitor = actualMethodVisitor;
                            this.record = record;
                            record.applyHead(actualMethodVisitor);
                        }

                        @Override
                        public AnnotationVisitor visitAnnotationDefault() {
                            return IGNORE_ANNOTATION;
                        }

                        @Override
                        public AnnotationVisitor visitTypeAnnotation(int typeReference, TypePath typePath, String descriptor, boolean visible) {
                            return annotationRetention.isEnabled()
                                    ? super.visitTypeAnnotation(typeReference, typePath, descriptor, visible)
                                    : IGNORE_ANNOTATION;
                        }

                        @Override
                        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                            return annotationRetention.isEnabled()
                                    ? super.visitAnnotation(descriptor, visible)
                                    : IGNORE_ANNOTATION;
                        }

                        @Override
                        public void visitAnnotableParameterCount(int count, boolean visible) {
                            if (annotationRetention.isEnabled()) {
                                super.visitAnnotableParameterCount(count, visible);
                            }
                        }

                        @Override
                        public AnnotationVisitor visitParameterAnnotation(int index, String descriptor, boolean visible) {
                            return annotationRetention.isEnabled()
                                    ? super.visitParameterAnnotation(index, descriptor, visible)
                                    : IGNORE_ANNOTATION;
                        }

                        @Override
                        public void visitCode() {
                            mv = IGNORE_METHOD;
                        }

                        @Override
                        public void visitEnd() {
                            record.applyBody(actualMethodVisitor, implementationContext, annotationValueFilterFactory);
                            actualMethodVisitor.visitEnd();
                        }
                    }
                }
            }

            /**
             * A default type writer that only applies a type decoration. 只应用类型修饰的默认类型编写器
             *
             * @param <V> The best known loaded type for the dynamically created type. 动态创建的类型的最著名加载类型
             */
            protected static class WithDecorationOnly<V> extends ForInlining<V> {

                /**
                 * Creates a new inlining type writer that only applies a decoration.
                 *
                 * @param instrumentedType             The instrumented type to be created.
                 * @param classFileVersion             The class file specified by the user.
                 * @param auxiliaryTypes               The explicit auxiliary types to add to the created type.
                 * @param methods                      The instrumented type's declared and virtually inherited methods.
                 * @param typeAttributeAppender        The type attribute appender to apply onto the instrumented type.
                 * @param asmVisitorWrapper            The ASM visitor wrapper to apply onto the class writer.
                 * @param annotationValueFilterFactory The annotation value filter factory to apply.
                 * @param annotationRetention          The annotation retention to apply.
                 * @param auxiliaryTypeNamingStrategy  The naming strategy for auxiliary types to apply.
                 * @param implementationContextFactory The implementation context factory to apply.
                 * @param typeValidation               Determines if a type should be explicitly validated.
                 * @param classWriterStrategy          The class writer strategy to use.
                 * @param typePool                     The type pool to use for computing stack map frames, if required.
                 * @param classFileLocator             The class file locator for locating the original type's class file.
                 */
                protected WithDecorationOnly(TypeDescription instrumentedType,
                                             ClassFileVersion classFileVersion,
                                             List<? extends DynamicType> auxiliaryTypes,
                                             MethodList<?> methods,
                                             TypeAttributeAppender typeAttributeAppender,
                                             AsmVisitorWrapper asmVisitorWrapper,
                                             AnnotationValueFilter.Factory annotationValueFilterFactory,
                                             AnnotationRetention annotationRetention,
                                             AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                                             Implementation.Context.Factory implementationContextFactory,
                                             TypeValidation typeValidation,
                                             ClassWriterStrategy classWriterStrategy,
                                             TypePool typePool,
                                             ClassFileLocator classFileLocator) {
                    super(instrumentedType,
                            classFileVersion,
                            FieldPool.Disabled.INSTANCE,
                            auxiliaryTypes,
                            new LazyFieldList(instrumentedType),
                            methods,
                            new MethodList.Empty<MethodDescription>(),
                            LoadedTypeInitializer.NoOp.INSTANCE,
                            TypeInitializer.None.INSTANCE,
                            typeAttributeAppender,
                            asmVisitorWrapper,
                            annotationValueFilterFactory,
                            annotationRetention,
                            auxiliaryTypeNamingStrategy,
                            implementationContextFactory,
                            typeValidation,
                            classWriterStrategy,
                            typePool,
                            instrumentedType,
                            classFileLocator);
                }

                @Override
                protected ClassVisitor writeTo(ClassVisitor classVisitor,
                                               TypeInitializer typeInitializer,
                                               ContextRegistry contextRegistry,
                                               int writerFlags,
                                               int readerFlags) {
                    if (typeInitializer.isDefined()) {
                        throw new UnsupportedOperationException("Cannot apply a type initializer for a decoration");
                    }
                    return new DecorationClassVisitor(classVisitor, contextRegistry, writerFlags, readerFlags);
                }

                /**
                 * A field list that only reads fields lazy to avoid an eager lookup since fields are often not required. 一种只读取字段的字段列表，由于字段通常不是必需的，所以它可以避免快速查找
                 */
                protected static class LazyFieldList extends FieldList.AbstractBase<FieldDescription.InDefinedShape> {

                    /**
                     * The instrumented type.
                     */
                    private final TypeDescription instrumentedType;

                    /**
                     * Creates a lazy field list.
                     *
                     * @param instrumentedType The instrumented type.
                     */
                    protected LazyFieldList(TypeDescription instrumentedType) {
                        this.instrumentedType = instrumentedType;
                    }

                    @Override
                    public FieldDescription.InDefinedShape get(int index) {
                        return instrumentedType.getDeclaredFields().get(index);
                    }

                    @Override
                    public int size() {
                        return instrumentedType.getDeclaredFields().size();
                    }
                }

                /**
                 * A class visitor that decorates an existing type. 装饰现有类型的类访问者
                 */
                @SuppressFBWarnings(value = "UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", justification = "Field access order is implied by ASM")
                protected class DecorationClassVisitor extends MetadataAwareClassVisitor implements TypeInitializer.Drain {

                    /**
                     * A context registry to register the lazily created implementation context to.
                     */
                    private final ContextRegistry contextRegistry;

                    /**
                     * The writer flags being used.
                     */
                    private final int writerFlags;

                    /**
                     * The reader flags being used.
                     */
                    private final int readerFlags;

                    /**
                     * The implementation context to use or {@code null} if the context is not yet initialized.
                     */
                    private Implementation.Context.ExtractableView implementationContext;

                    /**
                     * Creates a class visitor which is capable of decorating an existent class on the fly.
                     *
                     * @param classVisitor    The underlying class visitor to which writes are delegated.
                     * @param contextRegistry A context registry to register the lazily created implementation context to.
                     * @param writerFlags     The writer flags being used.
                     * @param readerFlags     The reader flags being used.
                     */
                    protected DecorationClassVisitor(ClassVisitor classVisitor, ContextRegistry contextRegistry, int writerFlags, int readerFlags) {
                        super(OpenedClassReader.ASM_API, classVisitor);
                        this.contextRegistry = contextRegistry;
                        this.writerFlags = writerFlags;
                        this.readerFlags = readerFlags;
                    }

                    @Override
                    public void visit(int classFileVersionNumber,
                                      int modifiers,
                                      String internalName,
                                      String genericSignature,
                                      String superClassInternalName,
                                      String[] interfaceTypeInternalName) {
                        ClassFileVersion classFileVersion = ClassFileVersion.ofMinorMajor(classFileVersionNumber);
                        implementationContext = implementationContextFactory.make(instrumentedType,
                                auxiliaryTypeNamingStrategy,
                                typeInitializer,
                                classFileVersion,
                                WithDecorationOnly.this.classFileVersion);
                        contextRegistry.setImplementationContext(implementationContext);
                        cv = asmVisitorWrapper.wrap(instrumentedType,
                                cv,
                                implementationContext,
                                typePool,
                                fields,
                                methods,
                                writerFlags,
                                readerFlags);
                        cv.visit(classFileVersionNumber, modifiers, internalName, genericSignature, superClassInternalName, interfaceTypeInternalName);
                    }

                    @Override
                    protected void onNestHost() {
                        /* do nothing */
                    }

                    @Override
                    protected void onOuterType() {
                        /* do nothing */
                    }

                    @Override
                    protected AnnotationVisitor onVisitTypeAnnotation(int typeReference, TypePath typePath, String descriptor, boolean visible) {
                        return annotationRetention.isEnabled()
                                ? cv.visitTypeAnnotation(typeReference, typePath, descriptor, visible)
                                : IGNORE_ANNOTATION;
                    }

                    @Override
                    protected AnnotationVisitor onVisitAnnotation(String descriptor, boolean visible) {
                        return annotationRetention.isEnabled()
                                ? cv.visitAnnotation(descriptor, visible)
                                : IGNORE_ANNOTATION;
                    }

                    @Override
                    protected void onAfterAttributes() {
                        typeAttributeAppender.apply(cv, instrumentedType, annotationValueFilterFactory.on(instrumentedType));
                    }

                    @Override
                    protected void onVisitEnd() {
                        implementationContext.drain(this, cv, annotationValueFilterFactory);
                        cv.visitEnd();
                    }

                    @Override
                    public void apply(ClassVisitor classVisitor, TypeInitializer typeInitializer, Implementation.Context implementationContext) {
                        /* do nothing */
                    }
                }
            }
        }

        /**
         * A type writer that creates a class file that is not based upon another, existing class. 一种类型编写器，用于创建一个不基于另一个现有类的类文件
         *
         * @param <U> The best known loaded type for the dynamically created type.
         */
        @HashCodeAndEqualsPlugin.Enhance
        public static class ForCreation<U> extends Default<U> {

            /**
             * The method pool to use.
             */
            private final MethodPool methodPool;

            /**
             * Creates a new default type writer for creating a new type that is not based on an existing class file. 创建新的默认类型编写器，用于创建不基于现有类文件的新类型
             *
             * @param instrumentedType             The instrumented type to be created. 被创建的插桩类型
             * @param classFileVersion             The class file version to write the instrumented type in and to apply when creating auxiliary types. 将插桩类型写入并在创建辅助类型时应用的类文件版本
             * @param fieldPool                    The field pool to use.
             * @param methodPool                   The method pool to use.
             * @param auxiliaryTypes               A list of auxiliary types to add to the created type.
             * @param fields                       The instrumented type's declared fields.
             * @param methods                      The instrumented type's declared and virtually inherited methods.
             * @param instrumentedMethods          The instrumented methods relevant to this type creation.
             * @param loadedTypeInitializer        The loaded type initializer to apply onto the created type after loading.
             * @param typeInitializer              The type initializer to include in the created type's type initializer.
             * @param typeAttributeAppender        The type attribute appender to apply onto the instrumented type.
             * @param asmVisitorWrapper            The ASM visitor wrapper to apply onto the class writer.
             * @param annotationValueFilterFactory The annotation value filter factory to apply.
             * @param annotationRetention          The annotation retention to apply.
             * @param auxiliaryTypeNamingStrategy  The naming strategy for auxiliary types to apply.
             * @param implementationContextFactory The implementation context factory to apply.
             * @param typeValidation               Determines if a type should be explicitly validated.
             * @param classWriterStrategy          The class writer strategy to use.
             * @param typePool                     The type pool to use for computing stack map frames, if required.
             */
            protected ForCreation(TypeDescription instrumentedType,
                                  ClassFileVersion classFileVersion,
                                  FieldPool fieldPool,
                                  MethodPool methodPool,
                                  List<? extends DynamicType> auxiliaryTypes,
                                  FieldList<FieldDescription.InDefinedShape> fields,
                                  MethodList<?> methods,
                                  MethodList<?> instrumentedMethods,
                                  LoadedTypeInitializer loadedTypeInitializer,
                                  TypeInitializer typeInitializer,
                                  TypeAttributeAppender typeAttributeAppender,
                                  AsmVisitorWrapper asmVisitorWrapper,
                                  AnnotationValueFilter.Factory annotationValueFilterFactory,
                                  AnnotationRetention annotationRetention,
                                  AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                                  Implementation.Context.Factory implementationContextFactory,
                                  TypeValidation typeValidation,
                                  ClassWriterStrategy classWriterStrategy,
                                  TypePool typePool) {
                super(instrumentedType,
                        classFileVersion,
                        fieldPool,
                        auxiliaryTypes,
                        fields,
                        methods,
                        instrumentedMethods,
                        loadedTypeInitializer,
                        typeInitializer,
                        typeAttributeAppender,
                        asmVisitorWrapper,
                        annotationValueFilterFactory,
                        annotationRetention,
                        auxiliaryTypeNamingStrategy,
                        implementationContextFactory,
                        typeValidation,
                        classWriterStrategy,
                        typePool);
                this.methodPool = methodPool;
            }

            @Override
            @SuppressWarnings("deprecation")
            protected UnresolvedType create(TypeInitializer typeInitializer) {
                int writerFlags = asmVisitorWrapper.mergeWriter(AsmVisitorWrapper.NO_FLAGS);
                ClassWriter classWriter = classWriterStrategy.resolve(writerFlags, typePool);
                Implementation.Context.ExtractableView implementationContext = implementationContextFactory.make(instrumentedType,
                        auxiliaryTypeNamingStrategy,
                        typeInitializer,
                        classFileVersion,
                        classFileVersion);
                ClassVisitor classVisitor = asmVisitorWrapper.wrap(instrumentedType,
                        ValidatingClassVisitor.of(classWriter, typeValidation),
                        implementationContext,
                        typePool,
                        fields,
                        methods,
                        writerFlags,
                        asmVisitorWrapper.mergeReader(AsmVisitorWrapper.NO_FLAGS));
                classVisitor.visit(classFileVersion.getMinorMajorVersion(),
                        instrumentedType.getActualModifiers(!instrumentedType.isInterface()),
                        instrumentedType.getInternalName(),
                        instrumentedType.getGenericSignature(),
                        (instrumentedType.getSuperClass() == null
                                ? TypeDescription.OBJECT
                                : instrumentedType.getSuperClass().asErasure()).getInternalName(),
                        instrumentedType.getInterfaces().asErasures().toInternalNames());
                if (!instrumentedType.isNestHost()) {
                    classVisitor.visitNestHostExperimental(instrumentedType.getNestHost().getInternalName());
                }
                MethodDescription.InDefinedShape enclosingMethod = instrumentedType.getEnclosingMethod();
                if (enclosingMethod != null) {
                    classVisitor.visitOuterClass(enclosingMethod.getDeclaringType().getInternalName(),
                            enclosingMethod.getInternalName(),
                            enclosingMethod.getDescriptor());
                } else {
                    TypeDescription enclosingType = instrumentedType.getEnclosingType();
                    if (enclosingType != null) {
                        classVisitor.visitOuterClass(enclosingType.getInternalName(), NO_REFERENCE, NO_REFERENCE);
                    }
                }
                typeAttributeAppender.apply(classVisitor, instrumentedType, annotationValueFilterFactory.on(instrumentedType));
                for (FieldDescription fieldDescription : fields) {
                    fieldPool.target(fieldDescription).apply(classVisitor, annotationValueFilterFactory);
                }
                for (MethodDescription methodDescription : instrumentedMethods) {
                    methodPool.target(methodDescription).apply(classVisitor, implementationContext, annotationValueFilterFactory);
                }
                implementationContext.drain(new TypeInitializer.Drain.Default(instrumentedType,
                        methodPool,
                        annotationValueFilterFactory), classVisitor, annotationValueFilterFactory);
                if (instrumentedType.isNestHost()) {
                    for (TypeDescription typeDescription : instrumentedType.getNestMembers().filter(not(is(instrumentedType)))) {
                        classVisitor.visitNestMemberExperimental(typeDescription.getInternalName());
                    }
                }
                for (TypeDescription typeDescription : instrumentedType.getDeclaredTypes()) {
                    classVisitor.visitInnerClass(typeDescription.getInternalName(),
                            instrumentedType.getInternalName(),
                            typeDescription.getSimpleName(),
                            typeDescription.getModifiers());
                }
                TypeDescription declaringType = instrumentedType.getDeclaringType();
                if (declaringType != null) {
                    classVisitor.visitInnerClass(instrumentedType.getInternalName(),
                            declaringType.getInternalName(),
                            instrumentedType.isAnonymousClass()
                                    ? NO_REFERENCE
                                    : instrumentedType.getSimpleName(),
                            instrumentedType.getModifiers());
                }
                classVisitor.visitEnd();
                return new UnresolvedType(classWriter.toByteArray(), implementationContext.getAuxiliaryTypes());
            }
        }

        /**
         * An action to write a class file to the dumping location. 将类文件写入转储位置的操作
         */
        @HashCodeAndEqualsPlugin.Enhance
        protected static class ClassDumpAction implements PrivilegedExceptionAction<Void> {

            /**
             * Indicates that nothing is returned from this action.
             */
            private static final Void NOTHING = null;

            /**
             * The target folder for writing the class file to.
             */
            private final String target;

            /**
             * The instrumented type.
             */
            private final TypeDescription instrumentedType;

            /**
             * {@code true} if the dumped class file is an input to a class transformation.
             */
            private final boolean original;

            /**
             * The type's binary representation.
             */
            private final byte[] binaryRepresentation;

            /**
             * Creates a new class dump action.
             *
             * @param target               The target folder for writing the class file to.
             * @param instrumentedType     The instrumented type.
             * @param original             {@code true} if the dumped class file is an input to a class transformation.
             * @param binaryRepresentation The type's binary representation.
             */
            protected ClassDumpAction(String target, TypeDescription instrumentedType, boolean original, byte[] binaryRepresentation) {
                this.target = target;
                this.instrumentedType = instrumentedType;
                this.original = original;
                this.binaryRepresentation = binaryRepresentation;
            }

            /**
             * Dumps the instrumented type if a {@link TypeWriter.Default#DUMP_FOLDER} is configured.
             *
             * @param dumpFolder           The dump folder.
             * @param instrumentedType     The instrumented type.
             * @param original             {@code true} if the dumped class file is an input to a class transformation.
             * @param binaryRepresentation The binary representation.
             */
            protected static void dump(String dumpFolder, TypeDescription instrumentedType, boolean original, byte[] binaryRepresentation) {
                if (dumpFolder != null) {
                    try {
                        AccessController.doPrivileged(new ClassDumpAction(dumpFolder, instrumentedType, original, binaryRepresentation));
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                }
            }

            @Override
            public Void run() throws Exception {
                OutputStream outputStream = new FileOutputStream(new File(target, instrumentedType.getName()
                        + (original ? "-original." : ".")
                        + System.currentTimeMillis()));
                try {
                    outputStream.write(binaryRepresentation);
                    return NOTHING;
                } finally {
                    outputStream.close();
                }
            }
        }
    }
}
