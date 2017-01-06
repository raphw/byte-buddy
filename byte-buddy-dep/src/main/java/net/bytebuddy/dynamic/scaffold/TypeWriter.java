package net.bytebuddy.dynamic.scaffold;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.EqualsAndHashCode;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.asm.AsmVisitorWrapper;
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
import net.bytebuddy.utility.privilege.GetSystemPropertyAction;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static net.bytebuddy.matcher.ElementMatchers.is;

/**
 * A type writer is a utility for writing an actual class file using the ASM library.
 *
 * @param <T> The best known loaded type for the dynamically created type.
 */
public interface TypeWriter<T> {

    /**
     * A system property that indicates a folder for Byte Buddy to dump class files of all types that it creates.
     * If this property is not set, Byte Buddy does not dump any class files. This property is only read a single
     * time which is why it must be set on application start-up.
     */
    String DUMP_PROPERTY = "net.bytebuddy.dump";

    /**
     * Creates the dynamic type that is described by this type writer.
     *
     * @param typeResolver The type resolution strategy to use.
     * @return An unloaded dynamic type that describes the created type.
     */
    DynamicType.Unloaded<T> make(TypeResolutionStrategy.Resolved typeResolver);

    /**
     * An field pool that allows a lookup for how to implement a field.
     */
    interface FieldPool {

        /**
         * Returns the field attribute appender that matches a given field description or a default field
         * attribute appender if no appender was registered for the given field.
         *
         * @param fieldDescription The field description of interest.
         * @return The registered field attribute appender for the given field or the default appender if no such
         * appender was found.
         */
        Record target(FieldDescription fieldDescription);

        /**
         * An entry of a field pool that describes how a field is implemented.
         *
         * @see net.bytebuddy.dynamic.scaffold.TypeWriter.FieldPool
         */
        interface Record {

            /**
             * Determines if this record is implicit, i.e is not defined by a {@link FieldPool}.
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
             * A record for a simple field without a default value where all of the field's declared annotations are appended.
             */
            @EqualsAndHashCode
            class ForImplicitField implements Record {

                /**
                 * The implemented field.
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
                    FieldAttributeAppender.ForInstrumentedField.INSTANCE.apply(fieldVisitor,
                            fieldDescription,
                            annotationValueFilterFactory.on(fieldDescription));
                    fieldVisitor.visitEnd();
                }

                @Override
                public void apply(FieldVisitor fieldVisitor, AnnotationValueFilter.Factory annotationValueFilterFactory) {
                    throw new IllegalStateException("An implicit field record is not intended for partial application: " + this);
                }
            }

            /**
             * A record for a rich field with attributes and a potential default value.
             */
            @EqualsAndHashCode
            class ForExplicitField implements Record {

                /**
                 * The attribute appender for the field.
                 */
                private final FieldAttributeAppender attributeAppender;

                /**
                 * The field's default value.
                 */
                private final Object defaultValue;

                /**
                 * The implemented field.
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
                    attributeAppender.apply(fieldVisitor, fieldDescription, annotationValueFilterFactory.on(fieldDescription));
                    fieldVisitor.visitEnd();
                }

                @Override
                public void apply(FieldVisitor fieldVisitor, AnnotationValueFilter.Factory annotationValueFilterFactory) {
                    attributeAppender.apply(fieldVisitor, fieldDescription, annotationValueFilterFactory.on(fieldDescription));
                }
            }
        }
    }

    /**
     * An method pool that allows a lookup for how to implement a method.
     */
    interface MethodPool {

        /**
         * Looks up a handler entry for a given method.
         *
         * @param methodDescription The method being processed.
         * @return A handler entry for the given method.
         */
        Record target(MethodDescription methodDescription);

        /**
         * An entry of a method pool that describes how a method is implemented.
         *
         * @see net.bytebuddy.dynamic.scaffold.TypeWriter.MethodPool
         */
        interface Record {

            /**
             * Returns the sort of this method instrumentation.
             *
             * @return The sort of this method instrumentation.
             */
            Sort getSort();

            /**
             * Returns the method that is implemented where the returned method resembles a potential transformation. An implemented
             * method is only defined if a method is not {@link Record.Sort#SKIPPED}.
             *
             * @return The implemented method.
             */
            MethodDescription getMethod();

            /**
             * The visibility to enforce for this method.
             *
             * @return The visibility to enforce for this method.
             */
            Visibility getVisibility();

            /**
             * Prepends the given method appender to this entry.
             *
             * @param byteCodeAppender The byte code appender to prepend.
             * @return This entry with the given code prepended.
             */
            Record prepend(ByteCodeAppender byteCodeAppender);

            /**
             * Applies this method entry. This method can always be called and might be a no-op.
             *
             * @param classVisitor                 The class visitor to which this entry should be applied.
             * @param implementationContext        The implementation context to which this entry should be applied.
             * @param annotationValueFilterFactory The annotation value filter factory to apply when writing annotations.
             */
            void apply(ClassVisitor classVisitor, Implementation.Context implementationContext, AnnotationValueFilter.Factory annotationValueFilterFactory);

            /**
             * Applies the head of this entry. Applying an entry is only possible if a method is defined, i.e. the sort of this entry is not
             * {@link Record.Sort#SKIPPED}.
             *
             * @param methodVisitor The method visitor to which this entry should be applied.
             */
            void applyHead(MethodVisitor methodVisitor);

            /**
             * Applies the body of this entry. Applying the body of an entry is only possible if a method is implemented, i.e. the sort of this
             * entry is {@link Record.Sort#IMPLEMENTED}.
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
             * The sort of an entry.
             */
            enum Sort {

                /**
                 * Describes a method that should not be implemented or retained in its original state.
                 */
                SKIPPED(false, false),

                /**
                 * Describes a method that should be defined but is abstract or native, i.e. does not define any byte code.
                 */
                DEFINED(true, false),

                /**
                 * Describes a method that is implemented in byte code.
                 */
                IMPLEMENTED(true, true);

                /**
                 * Indicates if this sort defines a method, with or without byte code.
                 */
                private final boolean define;

                /**
                 * Indicates if this sort defines byte code.
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
             * A canonical implementation of a method that is not declared but inherited by the instrumented type.
             */
            @EqualsAndHashCode
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
             * A base implementation of an abstract entry that defines a method.
             */
            abstract class ForDefinedMethod implements Record {

                @Override
                public void apply(ClassVisitor classVisitor, Implementation.Context implementationContext, AnnotationValueFilter.Factory annotationValueFilterFactory) {
                    MethodVisitor methodVisitor = classVisitor.visitMethod(getMethod().getActualModifiers(getSort().isImplemented(), getVisibility()),
                            getMethod().getInternalName(),
                            getMethod().getDescriptor(),
                            getMethod().getGenericSignature(),
                            getMethod().getExceptionTypes().asErasures().toInternalNames());
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

                /**
                 * Describes an entry that defines a method as byte code.
                 */
                @EqualsAndHashCode(callSuper = false)
                public static class WithBody extends ForDefinedMethod {

                    /**
                     * The implemented method.
                     */
                    private final MethodDescription methodDescription;

                    /**
                     * The byte code appender to apply.
                     */
                    private final ByteCodeAppender byteCodeAppender;

                    /**
                     * The method attribute appender to apply.
                     */
                    private final MethodAttributeAppender methodAttributeAppender;

                    /**
                     * The represented method's minimum visibility.
                     */
                    private final Visibility visibility;

                    /**
                     * Creates a new record for an implemented method without attributes or a modifier resolver.
                     *
                     * @param methodDescription The implemented method.
                     * @param byteCodeAppender  The byte code appender to apply.
                     */
                    public WithBody(MethodDescription methodDescription, ByteCodeAppender byteCodeAppender) {
                        this(methodDescription, byteCodeAppender, MethodAttributeAppender.NoOp.INSTANCE, methodDescription.getVisibility());
                    }

                    /**
                     * Creates a new entry for a method that defines a method as byte code.
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
                 * Describes an entry that defines a method but without byte code and without an annotation value.
                 */
                @EqualsAndHashCode(callSuper = false)
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
                 * Describes an entry that defines a method with a default annotation value.
                 */
                @EqualsAndHashCode(callSuper = false)
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
                @EqualsAndHashCode(callSuper = false)
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
                     * The super type of the instrumented type.
                     */
                    private final TypeDescription superClass;

                    /**
                     * The attribute appender to apply to the visibility bridge.
                     */
                    private final MethodAttributeAppender attributeAppender;

                    /**
                     * Creates a new record for a visibility bridge.
                     *
                     * @param visibilityBridge  The visibility bridge.
                     * @param bridgeTarget      The method the visibility bridge invokes.
                     * @param superClass        The super type of the instrumented type.
                     * @param attributeAppender The attribute appender to apply to the visibility bridge.
                     */
                    protected OfVisibilityBridge(MethodDescription visibilityBridge,
                                                 MethodDescription bridgeTarget,
                                                 TypeDescription superClass,
                                                 MethodAttributeAppender attributeAppender) {
                        this.visibilityBridge = visibilityBridge;
                        this.bridgeTarget = bridgeTarget;
                        this.superClass = superClass;
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
                        return new OfVisibilityBridge(new VisibilityBridge(instrumentedType, bridgeTarget),
                                bridgeTarget,
                                instrumentedType.getSuperClass().asErasure(),
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
                                MethodInvocation.invoke(bridgeTarget).special(superClass),
                                MethodReturn.of(instrumentedMethod.getReturnType())
                        ).apply(methodVisitor, implementationContext, instrumentedMethod);
                    }

                    /**
                     * A method describing a visibility bridge.
                     */
                    protected static class VisibilityBridge extends MethodDescription.InDefinedShape.AbstractBase {

                        /**
                         * The instrumented type.
                         */
                        private final TypeDescription instrumentedType;

                        /**
                         * The method that is the target of the bridge.
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

            /**
             * A wrapper that appends accessor bridges for a method's implementation. The bridges are only added if
             * {@link net.bytebuddy.dynamic.scaffold.TypeWriter.MethodPool.Record#apply(ClassVisitor, Implementation.Context, AnnotationValueFilter.Factory)}
             * is invoked such that bridges are not appended for methods that are rebased or redefined as such types already have bridge methods in place.
             */
            @EqualsAndHashCode
            class AccessBridgeWrapper implements Record {

                /**
                 * The delegate for implementing the bridge's target.
                 */
                private final Record delegate;

                /**
                 * The instrumented type that defines the bridge methods and the bridge target.
                 */
                private final TypeDescription instrumentedType;

                /**
                 * The target of the bridge method.
                 */
                private final MethodDescription bridgeTarget;

                /**
                 * A collection of all tokens representing all bridge methods.
                 */
                private final Set<MethodDescription.TypeToken> bridgeTypes;

                /**
                 * The attribute appender being applied for the bridge target.
                 */
                private final MethodAttributeAppender attributeAppender;

                /**
                 * Creates a wrapper for adding accessor bridges.
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
                 * Wraps the given record in an accessor bridge wrapper if necessary.
                 *
                 * @param delegate          The delegate for implementing the bridge's target.
                 * @param instrumentedType  The instrumented type that defines the bridge methods and the bridge target.
                 * @param bridgeTarget      The bridge methods' target methods.
                 * @param bridgeTypes       A collection of all tokens representing all bridge methods.
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
                    return bridgeTarget.getVisibility();
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
                        MethodVisitor methodVisitor = classVisitor.visitMethod(bridgeMethod.getActualModifiers(true),
                                bridgeMethod.getInternalName(),
                                bridgeMethod.getDescriptor(),
                                MethodDescription.NON_GENERIC_SIGNATURE,
                                bridgeMethod.getExceptionTypes().asErasures().toInternalNames());
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
     * A default implementation of a {@link net.bytebuddy.dynamic.scaffold.TypeWriter}.
     *
     * @param <S> The best known loaded type for the dynamically created type.
     */
    @EqualsAndHashCode
    abstract class Default<S> implements TypeWriter<S> {

        /**
         * A folder for dumping class files or {@code null} if no dump should be generated.
         */
        private static final String DUMP_FOLDER;

        /*
         * Reads the dumping property that is set at program start up. This might cause an error because of security constraints.
         */
        static {
            String dumpFolder;
            try {
                dumpFolder = AccessController.doPrivileged(new GetSystemPropertyAction(DUMP_PROPERTY));
            } catch (RuntimeException exception) {
                dumpFolder = null;
                Logger.getLogger("net.bytebuddy").log(Level.WARNING, "Could not enable dumping of class files", exception);
            }
            DUMP_FOLDER = dumpFolder;
        }

        /**
         * The instrumented type to be created.
         */
        protected final TypeDescription instrumentedType;

        /**
         * The class file specified by the user.
         */
        protected final ClassFileVersion classFileVersion;

        /**
         * The field pool to use.
         */
        protected final FieldPool fieldPool;

        /**
         * The explicit auxiliary types to add to the created type.
         */
        protected final List<? extends DynamicType> auxiliaryTypes;

        /**
         * The instrumented type's declared fields.
         */
        protected final FieldList<FieldDescription.InDefinedShape> fields;

        /**
         * The instrumented type's methods that are declared or inherited.
         */
        protected final MethodList<?> methods;

        /**
         * The instrumented methods relevant to this type creation.
         */
        protected final MethodList<?> instrumentedMethods;

        /**
         * The loaded type initializer to apply onto the created type after loading.
         */
        protected final LoadedTypeInitializer loadedTypeInitializer;

        /**
         * The type initializer to include in the created type's type initializer.
         */
        protected final TypeInitializer typeInitializer;

        /**
         * The type attribute appender to apply onto the instrumented type.
         */
        protected final TypeAttributeAppender typeAttributeAppender;

        /**
         * The ASM visitor wrapper to apply onto the class writer.
         */
        protected final AsmVisitorWrapper asmVisitorWrapper;

        /**
         * The annotation value filter factory to apply.
         */
        protected final AnnotationValueFilter.Factory annotationValueFilterFactory;

        /**
         * The annotation retention to apply.
         */
        protected final AnnotationRetention annotationRetention;

        /**
         * The naming strategy for auxiliary types to apply.
         */
        protected final AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy;

        /**
         * The implementation context factory to apply.
         */
        protected final Implementation.Context.Factory implementationContextFactory;

        /**
         * Determines if a type should be explicitly validated.
         */
        protected final TypeValidation typeValidation;

        /**
         * The type pool to use for computing stack map frames, if required.
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
         * @param methods                      The instrumented type's declared and virtually inhertied methods.
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
            this.typePool = typePool;
        }

        /**
         * Creates a type writer for creating a new type.
         *
         * @param methodRegistry               The compiled method registry to use.
         * @param fieldPool                    The field pool to use.
         * @param typeAttributeAppender        The type attribute appender to apply onto the instrumented type.
         * @param asmVisitorWrapper            The ASM visitor wrapper to apply onto the class writer.
         * @param classFileVersion             The class file version to use when no explicit class file version is applied.
         * @param annotationValueFilterFactory The annotation value filter factory to apply.
         * @param annotationRetention          The annotation retention to apply.
         * @param auxiliaryTypeNamingStrategy  The naming strategy for auxiliary types to apply.
         * @param implementationContextFactory The implementation context factory to apply.
         * @param typeValidation               Determines if a type should be explicitly validated.
         * @param typePool                     The type pool to use for computing stack map frames, if required.
         * @param <U>                          A loaded type that the instrumented type guarantees to subclass.
         * @return A suitable type writer.
         */
        public static <U> TypeWriter<U> forCreation(MethodRegistry.Compiled methodRegistry,
                                                    FieldPool fieldPool,
                                                    TypeAttributeAppender typeAttributeAppender,
                                                    AsmVisitorWrapper asmVisitorWrapper,
                                                    ClassFileVersion classFileVersion,
                                                    AnnotationValueFilter.Factory annotationValueFilterFactory,
                                                    AnnotationRetention annotationRetention,
                                                    AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                                                    Implementation.Context.Factory implementationContextFactory,
                                                    TypeValidation typeValidation,
                                                    TypePool typePool) {
            return new ForCreation<U>(methodRegistry.getInstrumentedType(),
                    classFileVersion,
                    fieldPool,
                    methodRegistry,
                    Collections.<DynamicType>emptyList(),
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
                    typePool);
        }

        /**
         * Creates a type writer for redefining a type.
         *
         * @param methodRegistry               The compiled method registry to use.
         * @param fieldPool                    The field pool to use.
         * @param typeAttributeAppender        The type attribute appender to apply onto the instrumented type.
         * @param asmVisitorWrapper            The ASM visitor wrapper to apply onto the class writer.
         * @param classFileVersion             The class file version to use when no explicit class file version is applied.
         * @param annotationValueFilterFactory The annotation value filter factory to apply.
         * @param annotationRetention          The annotation retention to apply.
         * @param auxiliaryTypeNamingStrategy  The naming strategy for auxiliary types to apply.
         * @param implementationContextFactory The implementation context factory to apply.
         * @param typeValidation               Determines if a type should be explicitly validated.
         * @param typePool                     The type pool to use for computing stack map frames, if required.
         * @param originalType                 The original type that is being redefined or rebased.
         * @param classFileLocator             The class file locator for locating the original type's class file.
         * @param <U>                          A loaded type that the instrumented type guarantees to subclass.
         * @return A suitable type writer.
         */
        public static <U> TypeWriter<U> forRedefinition(MethodRegistry.Prepared methodRegistry,
                                                        FieldPool fieldPool,
                                                        TypeAttributeAppender typeAttributeAppender,
                                                        AsmVisitorWrapper asmVisitorWrapper,
                                                        ClassFileVersion classFileVersion,
                                                        AnnotationValueFilter.Factory annotationValueFilterFactory,
                                                        AnnotationRetention annotationRetention,
                                                        AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                                                        Implementation.Context.Factory implementationContextFactory,
                                                        TypeValidation typeValidation,
                                                        TypePool typePool,
                                                        TypeDescription originalType,
                                                        ClassFileLocator classFileLocator) {
            return new ForInlining<U>(methodRegistry.getInstrumentedType(),
                    classFileVersion,
                    fieldPool,
                    methodRegistry,
                    SubclassImplementationTarget.Factory.LEVEL_TYPE,
                    Collections.<DynamicType>emptyList(),
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
                    typePool,
                    originalType,
                    classFileLocator,
                    MethodRebaseResolver.Disabled.INSTANCE);
        }

        /**
         * Creates a type writer for rebasing a type.
         *
         * @param methodRegistry               The compiled method registry to use.
         * @param fieldPool                    The field pool to use.
         * @param typeAttributeAppender        The type attribute appender to apply onto the instrumented type.
         * @param asmVisitorWrapper            The ASM visitor wrapper to apply onto the class writer.
         * @param classFileVersion             The class file version to use when no explicit class file version is applied.
         * @param annotationValueFilterFactory The annotation value filter factory to apply.
         * @param annotationRetention          The annotation retention to apply.
         * @param auxiliaryTypeNamingStrategy  The naming strategy for auxiliary types to apply.
         * @param implementationContextFactory The implementation context factory to apply.
         * @param typeValidation               Determines if a type should be explicitly validated.
         * @param typePool                     The type pool to use for computing stack map frames, if required.
         * @param originalType                 The original type that is being redefined or rebased.
         * @param classFileLocator             The class file locator for locating the original type's class file.
         * @param methodRebaseResolver         The method rebase resolver to use for rebasing names.
         * @param <U>                          A loaded type that the instrumented type guarantees to subclass.
         * @return A suitable type writer.
         */
        public static <U> TypeWriter<U> forRebasing(MethodRegistry.Prepared methodRegistry,
                                                    FieldPool fieldPool,
                                                    TypeAttributeAppender typeAttributeAppender,
                                                    AsmVisitorWrapper asmVisitorWrapper,
                                                    ClassFileVersion classFileVersion,
                                                    AnnotationValueFilter.Factory annotationValueFilterFactory,
                                                    AnnotationRetention annotationRetention,
                                                    AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                                                    Implementation.Context.Factory implementationContextFactory,
                                                    TypeValidation typeValidation,
                                                    TypePool typePool,
                                                    TypeDescription originalType,
                                                    ClassFileLocator classFileLocator,
                                                    MethodRebaseResolver methodRebaseResolver) {
            return new ForInlining<U>(methodRegistry.getInstrumentedType(),
                    classFileVersion,
                    fieldPool,
                    methodRegistry,
                    new RebaseImplementationTarget.Factory(methodRebaseResolver),
                    methodRebaseResolver.getAuxiliaryTypes(),
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
                    typePool,
                    originalType,
                    classFileLocator,
                    methodRebaseResolver);
        }

        @Override
        @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION", justification = "Setting a debugging property should never change the program outcome")
        public DynamicType.Unloaded<S> make(TypeResolutionStrategy.Resolved typeResolutionStrategy) {
            UnresolvedType unresolvedType = create(typeResolutionStrategy.injectedInto(typeInitializer));
            if (DUMP_FOLDER != null) {
                try {
                    AccessController.doPrivileged(new ClassDumpAction(DUMP_FOLDER, instrumentedType, unresolvedType.getBinaryRepresentation()));
                } catch (Exception exception) {
                    Logger.getLogger("net.bytebuddy").log(Level.WARNING, "Could not dump class file for " + instrumentedType, exception.getCause());
                }
            }
            return unresolvedType.toDynamicType(typeResolutionStrategy);
        }

        /**
         * Creates an unresolved version of the dynamic type.
         *
         * @param typeInitializer The type initializer to use.
         * @return An unresolved type.
         */
        protected abstract UnresolvedType create(TypeInitializer typeInitializer);

        /**
         * An unresolved type.
         */
        protected class UnresolvedType {

            /**
             * The type's binary representation.
             */
            private final byte[] binaryRepresentation;

            /**
             * A list of auxiliary types for this unresolved type.
             */
            private final List<? extends DynamicType> auxiliaryTypes;

            /**
             * Creates a new unresolved type.
             *
             * @param binaryRepresentation The type's binary representation.
             * @param auxiliaryTypes       A list of auxiliary types for this unresolved type.
             */
            protected UnresolvedType(byte[] binaryRepresentation, List<? extends DynamicType> auxiliaryTypes) {
                this.binaryRepresentation = binaryRepresentation;
                this.auxiliaryTypes = auxiliaryTypes;
            }

            /**
             * Resolves this type to a dynamic type.
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

            /**
             * Returns the outer instance.
             *
             * @return The outer instance.
             */
            private Default getOuter() {
                return Default.this;
            }

            @Override // HE: Remove when Lombok support for getOuter is added.
            @SuppressWarnings("unchecked")
            public boolean equals(Object object) {
                if (this == object) return true;
                if (object == null || getClass() != object.getClass()) return false;
                UnresolvedType that = (UnresolvedType) object; // Java 6 compilers cannot cast to a nested wildcard.
                return Arrays.equals(binaryRepresentation, that.binaryRepresentation)
                        && Default.this.equals(that.getOuter())
                        && auxiliaryTypes.equals(that.auxiliaryTypes);
            }

            @Override // HE: Remove when Lombok support for getOuter is added.
            public int hashCode() {
                int result = Arrays.hashCode(binaryRepresentation);
                result = 31 * result + auxiliaryTypes.hashCode();
                result = 31 * result + Default.this.hashCode();
                return result;
            }
        }

        /**
         * A class validator that validates that a class only defines members that are appropriate for the sort of the generated class.
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
             * The constraint to assert the members against. The constraint is first defined when the general class information is visited.
             */
            private Constraint constraint;

            /**
             * Creates a validating class visitor.
             *
             * @param classVisitor The class visitor to which any calls are delegated to.
             */
            protected ValidatingClassVisitor(ClassVisitor classVisitor) {
                super(Opcodes.ASM5, classVisitor);
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
                constraint.assertField(name, (modifiers & Opcodes.ACC_PUBLIC) != 0, (modifiers & Opcodes.ACC_STATIC) != 0, signature != null);
                return new ValidatingFieldVisitor(super.visitField(modifiers, name, descriptor, signature, defaultValue));
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
                return new ValidatingMethodVisitor(super.visitMethod(modifiers, name, descriptor, signature, exceptions), name);
            }

            /**
             * A constraint for members that are legal for a given type.
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
                 * @param isGeneric {@code true} if this field defines a generic signature.
                 */
                void assertField(String name, boolean isPublic, boolean isStatic, boolean isGeneric);

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
                 * Represents the constraint of a class type.
                 */
                enum ForClass implements Constraint {

                    /**
                     * Represents the constraints of a non-abstract class.
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
                    public void assertField(String name, boolean isPublic, boolean isStatic, boolean isGeneric) {
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
                    public void assertField(String name, boolean isPublic, boolean isStatic, boolean isGeneric) {
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
                }

                /**
                 * Represents the constraint of an interface type.
                 */
                enum ForInterface implements Constraint {

                    /**
                     * An interface type with the constrains for the Java versions 5 to 7.
                     */
                    CLASSIC(true),

                    /**
                     * An interface type with the constrains for the Java versions 8+.
                     */
                    JAVA_8(false);

                    /**
                     * {@code true} if this instance represents a classic interface type (pre Java 8).
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
                    public void assertField(String name, boolean isPublic, boolean isStatic, boolean isGeneric) {
                        if (!isStatic || !isPublic) {
                            throw new IllegalStateException("Cannot define non-static or non-public field '" + name + "' for interface type");
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
                }

                /**
                 * Represents the constraint of an annotation type.
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
                    public void assertField(String name, boolean isPublic, boolean isStatic, boolean isGeneric) {
                        if (!isStatic || !isPublic) {
                            throw new IllegalStateException("Cannot define non-static or non-public field '" + name + "' for annotation type");
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
                }

                /**
                 * Represents the constraint implied by a class file version.
                 */
                @EqualsAndHashCode
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
                    public ForClassFileVersion(ClassFileVersion classFileVersion) {
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
                    public void assertField(String name, boolean isPublic, boolean isStatic, boolean isGeneric) {
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
                        if (!classFileVersion.isAtLeast(ClassFileVersion.JAVA_V5)) {
                            throw new IllegalStateException("Cannot write annotations for class file version " + classFileVersion);
                        }
                    }

                    @Override
                    public void assertTypeAnnotation() {
                        if (!classFileVersion.isAtLeast(ClassFileVersion.JAVA_V5)) {
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
                        if (!classFileVersion.isAtLeast(ClassFileVersion.JAVA_V5)) {
                            throw new IllegalStateException("Cannot write type to constant pool for class file version " + classFileVersion);
                        }
                    }

                    @Override
                    public void assertMethodTypeInConstantPool() {
                        if (!classFileVersion.isAtLeast(ClassFileVersion.JAVA_V7)) {
                            throw new IllegalStateException("Cannot write method type to constant pool for class file version " + classFileVersion);
                        }
                    }

                    @Override
                    public void assertHandleInConstantPool() {
                        if (!classFileVersion.isAtLeast(ClassFileVersion.JAVA_V7)) {
                            throw new IllegalStateException("Cannot write method handle to constant pool for class file version " + classFileVersion);
                        }
                    }

                    @Override
                    public void assertInvokeDynamic() {
                        if (!classFileVersion.isAtLeast(ClassFileVersion.JAVA_V7)) {
                            throw new IllegalStateException("Cannot write invoke dynamic instruction for class file version " + classFileVersion);
                        }
                    }

                    @Override
                    public void assertSubRoutine() {
                        if (!classFileVersion.isLessThan(ClassFileVersion.JAVA_V6)) {
                            throw new IllegalStateException("Cannot write subroutine for class file version " + classFileVersion);
                        }
                    }
                }

                /**
                 * A constraint implementation that summarizes several constraints.
                 */
                @EqualsAndHashCode
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
                    public void assertField(String name, boolean isPublic, boolean isStatic, boolean isGeneric) {
                        for (Constraint constraint : constraints) {
                            constraint.assertField(name, isPublic, isStatic, isGeneric);
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
                    super(Opcodes.ASM5, fieldVisitor);
                }

                @Override
                public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                    constraint.assertAnnotation();
                    return super.visitAnnotation(desc, visible);
                }
            }

            /**
             * A method validator for checking default values.
             */
            protected class ValidatingMethodVisitor extends MethodVisitor {

                /**
                 * The name of the method being visited.
                 */
                private final String name;

                /**
                 * Creates a validating method visitor.
                 *
                 * @param methodVisitor The method visitor to which any calls are delegated to.
                 * @param name          The name of the method being visited.
                 */
                protected ValidatingMethodVisitor(MethodVisitor methodVisitor, String name) {
                    super(Opcodes.ASM5, methodVisitor);
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
                public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethod, Object... bootstrapArgument) {
                    constraint.assertInvokeDynamic();
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
         * A class writer that piggy-backs on Byte Buddy's {@link ClassFileLocator} to avoid class loading or look-up errors when redefining a class.
         * This is not available when creating a new class where automatic frame computation is however not normally a requirement.
         */
        protected static class FrameComputingClassWriter extends ClassWriter {

            /**
             * The type pool to use for computing stack map frames, if required.
             */
            private final TypePool typePool;

            /**
             * Creates a new frame computing class writer.
             *
             * @param flags    The flags to be handed to the writer.
             * @param typePool The type pool to use for computing stack map frames, if required.
             */
            protected FrameComputingClassWriter(int flags, TypePool typePool) {
                super(flags);
                this.typePool = typePool;
            }

            /**
             * Creates a new frame computing class writer.
             *
             * @param classReader The class reader from which the original class is read.
             * @param flags       The flags to be handed to the writer.
             * @param typePool    The type pool to use for computing stack map frames, if required.
             */
            protected FrameComputingClassWriter(ClassReader classReader, int flags, TypePool typePool) {
                super(classReader, flags);
                this.typePool = typePool;
            }

            @Override
            protected String getCommonSuperClass(String leftTypeName, String rightTypeName) {
                TypeDescription leftType = typePool.describe(leftTypeName.replace('/', '.')).resolve();
                TypeDescription rightType = typePool.describe(rightTypeName.replace('/', '.')).resolve();
                if (leftType.isAssignableFrom(rightType)) {
                    return leftType.getInternalName();
                } else if (leftType.isAssignableTo(rightType)) {
                    return rightType.getInternalName();
                } else if (leftType.isInterface() || rightType.isInterface()) {
                    return TypeDescription.OBJECT.getInternalName();
                } else {
                    do {
                        leftType = leftType.getSuperClass().asErasure();
                    } while (!leftType.isAssignableFrom(rightType));
                    return leftType.getInternalName();
                }
            }
        }

        /**
         * A type writer that inlines the created type into an existing class file.
         *
         * @param <U> The best known loaded type for the dynamically created type.
         */
        @EqualsAndHashCode(callSuper = true)
        public static class ForInlining<U> extends Default<U> {

            /**
             * Indicates that a type does not define a super type in its class file, i.e. the {@link Object} type.
             */
            private static final String NO_SUPER_TYPE = null;

            /**
             * Indicates that a method should be ignored.
             */
            private static final MethodVisitor IGNORE_METHOD = null;

            /**
             * Indicates that an annotation should be ignored.
             */
            private static final AnnotationVisitor IGNORE_ANNOTATION = null;

            /**
             * The method registry to use.
             */
            private final MethodRegistry.Prepared methodRegistry;

            /**
             * The implementation target factory to use.
             */
            private final Implementation.Target.Factory implementationTargetFactory;

            /**
             * The original type that is being redefined or rebased.
             */
            private final TypeDescription originalType;

            /**
             * The class file locator for locating the original type's class file.
             */
            private final ClassFileLocator classFileLocator;

            /**
             * The method rebase resolver to use for rebasing methods.
             */
            private final MethodRebaseResolver methodRebaseResolver;

            /**
             * Creates a new default type writer for creating a new type that is not based on an existing class file.
             *
             * @param instrumentedType             The instrumented type to be created.
             * @param classFileVersion             The class file version to define auxiliary types in.
             * @param fieldPool                    The field pool to use.
             * @param methodRegistry               The method registry to use.
             * @param implementationTargetFactory  The implementation target factory to use.
             * @param explicitAuxiliaryTypes       The explicit auxiliary types to add to the created type.
             * @param fields                       The instrumented type's declared fields.
             * @param methods                      The instrumented type's declared or virtually inherited methods.
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
             * @param typePool                     The type pool to use for computing stack map frames, if required.
             * @param originalType                 The original type that is being redefined or rebased.
             * @param classFileLocator             The class file locator for locating the original type's class file.
             * @param methodRebaseResolver         The method rebase resolver to use for rebasing methods.
             */
            protected ForInlining(TypeDescription instrumentedType,
                                  ClassFileVersion classFileVersion,
                                  FieldPool fieldPool,
                                  MethodRegistry.Prepared methodRegistry,
                                  Implementation.Target.Factory implementationTargetFactory,
                                  List<DynamicType> explicitAuxiliaryTypes,
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
                                  TypePool typePool,
                                  TypeDescription originalType,
                                  ClassFileLocator classFileLocator,
                                  MethodRebaseResolver methodRebaseResolver) {
                super(instrumentedType,
                        classFileVersion,
                        fieldPool,
                        explicitAuxiliaryTypes,
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
                        typePool);
                this.methodRegistry = methodRegistry;
                this.implementationTargetFactory = implementationTargetFactory;
                this.originalType = originalType;
                this.classFileLocator = classFileLocator;
                this.methodRebaseResolver = methodRebaseResolver;
            }

            @Override
            protected UnresolvedType create(TypeInitializer typeInitializer) {
                try {
                    int writerFlags = asmVisitorWrapper.mergeWriter(AsmVisitorWrapper.NO_FLAGS);
                    int readerFlags = asmVisitorWrapper.mergeReader(AsmVisitorWrapper.NO_FLAGS);
                    ClassReader classReader = new ClassReader(classFileLocator.locate(originalType.getName()).resolve());
                    ClassWriter classWriter = new FrameComputingClassWriter(classReader, writerFlags, typePool);
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
             * Creates a class visitor which weaves all changes and additions on the fly.
             *
             * @param classVisitor    The class visitor to which this entry is to be written to.
             * @param typeInitializer The type initializer to apply.
             * @param contextRegistry A context registry to register the lazily created implementation context to.
             * @param writerFlags     The writer flags being used.
             * @param readerFlags     The reader flags being used.
             * @return A class visitor which is capable of applying the changes.
             */
            private ClassVisitor writeTo(ClassVisitor classVisitor,
                                         TypeInitializer typeInitializer,
                                         ContextRegistry contextRegistry,
                                         int writerFlags,
                                         int readerFlags) {
                classVisitor = new RedefinitionClassVisitor(classVisitor, typeInitializer, contextRegistry, writerFlags, readerFlags);
                return originalType.getName().equals(instrumentedType.getName())
                        ? classVisitor
                        : new ClassRemapper(classVisitor, new SimpleRemapper(originalType.getInternalName(), instrumentedType.getInternalName()));
            }

            /**
             * An initialization handler is responsible for handling the creation of the type initializer.
             */
            protected interface InitializationHandler {

                /**
                 * Invoked upon completion of writing the instrumented type.
                 *
                 * @param classVisitor          The class visitor to write any methods to.
                 * @param implementationContext The implementation context to use.
                 */
                void complete(ClassVisitor classVisitor, Implementation.Context.ExtractableView implementationContext);

                /**
                 * An initialization handler that creates a new type initializer.
                 */
                class Creating extends TypeInitializer.Drain.Default implements InitializationHandler {

                    /**
                     * Creates a new creating initialization handler.
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
                 * An initialization handler that appends code to a c previously visited type initializer.
                 */
                abstract class Appending extends MethodVisitor implements InitializationHandler, TypeInitializer.Drain {

                    /**
                     * The instrumented type.
                     */
                    protected final TypeDescription instrumentedType;

                    /**
                     * The method pool record for the type initializer.
                     */
                    protected final MethodPool.Record record;

                    /**
                     * The used annotation value filter factory.
                     */
                    protected final AnnotationValueFilter.Factory annotationValueFilterFactory;

                    /**
                     * The frame writer to use.
                     */
                    protected final FrameWriter frameWriter;

                    /**
                     * A label marking the beginning of the appended code.
                     */
                    protected final Label appended;

                    /**
                     * A label marking the beginning og the original type initializer's code.
                     */
                    protected final Label original;

                    /**
                     * The currently recorded stack size.
                     */
                    protected int stackSize;

                    /**
                     * The currently recorded local variable length.
                     */
                    protected int localVariableLength;

                    /**
                     * Creates a new appending initialization handler.
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
                        super(Opcodes.ASM5, methodVisitor);
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
                        appended = new Label();
                        original = new Label();
                    }

                    /**
                     * Creates a new initialization handler that is appropriate for the supplied arguments.
                     *
                     * @param methodVisitor                The underlying method visitor.
                     * @param instrumentedType             The instrumented type.
                     * @param methodPool                   The method pool to use.
                     * @param annotationValueFilterFactory The used annotation value filter factory.
                     * @param requireFrames                {@code true} if the visitor is required to add frames.
                     * @param expandFrames                 {@code true} if the visitor is required to expand any added frame.
                     * @return An appropriate initialization handler which is required to also be a {@link MethodVisitor}.
                     */
                    protected static InitializationHandler of(MethodVisitor methodVisitor,
                                                              TypeDescription instrumentedType,
                                                              MethodPool methodPool,
                                                              AnnotationValueFilter.Factory annotationValueFilterFactory,
                                                              boolean requireFrames,
                                                              boolean expandFrames) {
                        MethodPool.Record record = methodPool.target(new MethodDescription.Latent.TypeInitializer(instrumentedType));
                        return record.getSort().isImplemented()
                                ? new WithActiveRecord(methodVisitor, instrumentedType, record, annotationValueFilterFactory, requireFrames, expandFrames)
                                : new WithoutActiveRecord(methodVisitor, instrumentedType, record, annotationValueFilterFactory, requireFrames, expandFrames);
                    }

                    @Override
                    public void visitCode() {
                        record.applyAttributes(mv, annotationValueFilterFactory);
                        super.visitCode();
                        mv.visitJumpInsn(Opcodes.GOTO, appended);
                        mv.visitLabel(original);
                        frameWriter.emitFrame(mv);
                    }

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
                        mv.visitLabel(appended);
                        frameWriter.emitFrame(mv);
                    }

                    @Override
                    public void apply(ClassVisitor classVisitor, TypeInitializer typeInitializer, Implementation.Context implementationContext) {
                        ByteCodeAppender.Size size = typeInitializer.apply(mv, implementationContext, new MethodDescription.Latent.TypeInitializer(instrumentedType));
                        stackSize = Math.max(stackSize, size.getOperandStackSize());
                        localVariableLength = Math.max(localVariableLength, size.getLocalVariableSize());
                        mv.visitJumpInsn(Opcodes.GOTO, original);
                        onComplete(implementationContext);
                    }

                    @Override
                    public void complete(ClassVisitor classVisitor, Implementation.Context.ExtractableView implementationContext) {
                        implementationContext.drain(this, classVisitor, annotationValueFilterFactory);
                        mv.visitMaxs(stackSize, localVariableLength);
                        mv.visitEnd();
                    }

                    /**
                     * Invoked upon completion of writing the type initializer.
                     *
                     * @param implementationContext The implementation context to use.
                     */
                    protected abstract void onComplete(Implementation.Context implementationContext);

                    /**
                     * A frame writer is responsible for adding empty frames on jumo instructions.
                     */
                    protected interface FrameWriter {

                        /**
                         * An empty array.
                         */
                        Object[] EMPTY = new Object[0];

                        /**
                         * Informs this frame writer of an observed frame.
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
                         * A non-operational frame writer.
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
                         * A frame writer that creates an expanded frame.
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
                         * An active frame writer that creates the most efficient frame.
                         */
                        class Active implements FrameWriter {

                            /**
                             * The current length of the current local variable array.
                             */
                            private int currentLocalVariableLength;

                            @Override
                            public void onFrame(int type, int localVariableLength) {
                                switch (type) {
                                    case Opcodes.F_SAME:
                                    case Opcodes.F_SAME1:
                                        currentLocalVariableLength = 0;
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
                     * A code appending initialization handler that does not apply an explicit record.
                     */
                    protected static class WithoutActiveRecord extends Appending {

                        /**
                         * Creates a new appending initialization handler without an active record.
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
                        protected void onComplete(Implementation.Context implementationContext) {
                            /* do nothing */
                        }
                    }

                    /**
                     * A code appending initialization handler that applies an explicit record.
                     */
                    protected static class WithActiveRecord extends Appending {

                        /**
                         * A label indicating the beginning of the record's code.
                         */
                        private final Label label;

                        /**
                         * Creates a new appending initialization handler with an active record.
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
            }

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
             * A class visitor which is capable of applying a redefinition of an existing class file.
             */
            @SuppressFBWarnings(value = "UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", justification = "Field access order is implied by ASM")
            protected class RedefinitionClassVisitor extends ClassVisitor {

                /**
                 * The type initializer to apply.
                 */
                private final TypeInitializer typeInitializer;

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
                 * A mapping of fields to write by their names.
                 */
                private final LinkedHashMap<String, FieldDescription> declarableFields;

                /**
                 * A mapping of methods to write by a concatenation of internal name and descriptor.
                 */
                private final LinkedHashMap<String, MethodDescription> declarableMethods;

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
                 * Creates a class visitor which is capable of redefining an existent class on the fly.
                 *
                 * @param classVisitor    The underlying class visitor to which writes are delegated.
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
                    super(Opcodes.ASM5, classVisitor);
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
                            ForInlining.this.classFileVersion);
                    contextRegistry.setImplementationContext(implementationContext);
                    cv = asmVisitorWrapper.wrap(instrumentedType,
                            cv,
                            implementationContext,
                            typePool,
                            fields,
                            methods,
                            writerFlags,
                            readerFlags);
                    super.visit(classFileVersionNumber,
                            instrumentedType.getActualModifiers((modifiers & Opcodes.ACC_SUPER) != 0 && !instrumentedType.isInterface())
                                    // Anonymous types might not preserve their class file's final modifier via their inner class modifier.
                                    | (((modifiers & Opcodes.ACC_FINAL) != 0 && instrumentedType.isAnonymousClass()) ? Opcodes.ACC_FINAL : 0),
                            instrumentedType.getInternalName(),
                            instrumentedType.getGenericSignature(),
                            instrumentedType.getSuperClass() == null
                                    ? (instrumentedType.isInterface() ? TypeDescription.OBJECT.getInternalName() : NO_SUPER_TYPE)
                                    : instrumentedType.getSuperClass().asErasure().getInternalName(),
                            instrumentedType.getInterfaces().asErasures().toInternalNames());
                    typeAttributeAppender.apply(cv, instrumentedType, annotationValueFilterFactory.on(instrumentedType));
                }

                @Override
                public void visitInnerClass(String internalName, String outerName, String innerName, int modifiers) {
                    if (internalName.equals(instrumentedType.getInternalName())) {
                        modifiers = instrumentedType.getModifiers();
                    }
                    super.visitInnerClass(internalName, outerName, innerName, modifiers);
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
                public FieldVisitor visitField(int modifiers,
                                               String internalName,
                                               String descriptor,
                                               String genericSignature,
                                               Object defaultValue) {
                    FieldDescription fieldDescription = declarableFields.remove(internalName + descriptor);
                    if (fieldDescription != null) {
                        FieldPool.Record record = fieldPool.target(fieldDescription);
                        if (!record.isImplicit()) {
                            return redefine(record, defaultValue);
                        }
                    }
                    return super.visitField(modifiers, internalName, descriptor, genericSignature, defaultValue);
                }

                /**
                 * Redefines a field using the given explicit field pool record and default value.
                 *
                 * @param record       The field pool value to apply during visitation of the existing field.
                 * @param defaultValue The default value to write onto the field which might be {@code null}.
                 * @return A field visitor for visiting the existing field definition.
                 */
                protected FieldVisitor redefine(FieldPool.Record record, Object defaultValue) {
                    FieldDescription instrumentedField = record.getField();
                    return new AttributeObtainingFieldVisitor(super.visitField(instrumentedField.getActualModifiers(),
                            instrumentedField.getInternalName(),
                            instrumentedField.getDescriptor(),
                            instrumentedField.getGenericSignature(),
                            record.resolveDefault(defaultValue)), record);
                }

                @Override
                public MethodVisitor visitMethod(int modifiers,
                                                 String internalName,
                                                 String descriptor,
                                                 String genericSignature,
                                                 String[] exceptionName) {
                    if (internalName.equals(MethodDescription.TYPE_INITIALIZER_INTERNAL_NAME)) {
                        return (MethodVisitor) (initializationHandler = InitializationHandler.Appending.of(super.visitMethod(modifiers, internalName, descriptor, genericSignature, exceptionName),
                                instrumentedType,
                                methodPool,
                                annotationValueFilterFactory,
                                (writerFlags & ClassWriter.COMPUTE_FRAMES) == 0 && implementationContext.getClassFileVersion().isAtLeast(ClassFileVersion.JAVA_V6),
                                (readerFlags & ClassReader.EXPAND_FRAMES) != 0));
                    } else {
                        MethodDescription methodDescription = declarableMethods.remove(internalName + descriptor);
                        return methodDescription == null
                                ? super.visitMethod(modifiers, internalName, descriptor, genericSignature, exceptionName)
                                : redefine(methodDescription, (modifiers & Opcodes.ACC_ABSTRACT) != 0);
                    }
                }

                /**
                 * Redefines a given method if this is required by looking up a potential implementation from the
                 * {@link net.bytebuddy.dynamic.scaffold.TypeWriter.MethodPool}.
                 *
                 * @param methodDescription The method being considered for redefinition.
                 * @param abstractOrigin    {@code true} if the original method is abstract, i.e. there is no implementation
                 *                          to preserve.
                 * @return A method visitor which is capable of consuming the original method.
                 */
                protected MethodVisitor redefine(MethodDescription methodDescription, boolean abstractOrigin) {
                    MethodPool.Record record = methodPool.target(methodDescription);
                    if (!record.getSort().isDefined()) {
                        return super.visitMethod(methodDescription.getActualModifiers(),
                                methodDescription.getInternalName(),
                                methodDescription.getDescriptor(),
                                methodDescription.getGenericSignature(),
                                methodDescription.getExceptionTypes().asErasures().toInternalNames());
                    }
                    MethodDescription implementedMethod = record.getMethod();
                    MethodVisitor methodVisitor = super.visitMethod(ModifierContributor.Resolver.of(Collections.singleton(record.getVisibility()))
                                    .resolve(implementedMethod.getActualModifiers(record.getSort().isImplemented())),
                            implementedMethod.getInternalName(),
                            implementedMethod.getDescriptor(),
                            implementedMethod.getGenericSignature(),
                            implementedMethod.getExceptionTypes().asErasures().toInternalNames());
                    return abstractOrigin
                            ? new AttributeObtainingMethodVisitor(methodVisitor, record)
                            : new CodePreservingMethodVisitor(methodVisitor, record, methodRebaseResolver.resolve(implementedMethod.asDefined()));
                }

                @Override
                public void visitEnd() {
                    for (FieldDescription fieldDescription : declarableFields.values()) {
                        fieldPool.target(fieldDescription).apply(cv, annotationValueFilterFactory);
                    }
                    for (MethodDescription methodDescription : declarableMethods.values()) {
                        methodPool.target(methodDescription).apply(cv, implementationContext, annotationValueFilterFactory);
                    }
                    initializationHandler.complete(cv, implementationContext);
                    super.visitEnd();
                }

                /**
                 * A field visitor that obtains all attributes and annotations of a field that is found in the
                 * class file but that discards all code.
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
                        super(Opcodes.ASM5, fieldVisitor);
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
                 * method while copying all attributes and annotations to the actual method.
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
                        super(Opcodes.ASM5, actualMethodVisitor);
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
                 * class file but that discards all code.
                 */
                protected class AttributeObtainingMethodVisitor extends MethodVisitor {

                    /**
                     * The method visitor to which the actual method is to be written to.
                     */
                    private final MethodVisitor actualMethodVisitor;

                    /**
                     * The method pool entry to apply.
                     */
                    private final MethodPool.Record record;

                    /**
                     * Creates a new attribute obtaining method visitor.
                     *
                     * @param actualMethodVisitor The method visitor of the actual method.
                     * @param record              The method pool entry to apply.
                     */
                    protected AttributeObtainingMethodVisitor(MethodVisitor actualMethodVisitor, MethodPool.Record record) {
                        super(Opcodes.ASM5, actualMethodVisitor);
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
         * A type writer that creates a class file that is not based upon another, existing class.
         *
         * @param <U> The best known loaded type for the dynamically created type.
         */
        @EqualsAndHashCode(callSuper = true)
        public static class ForCreation<U> extends Default<U> {

            /**
             * The method pool to use.
             */
            private final MethodPool methodPool;

            /**
             * Creates a new default type writer for creating a new type that is not based on an existing class file.
             *
             * @param instrumentedType             The instrumented type to be created.
             * @param classFileVersion             The class file version to write the instrumented type in and to apply when creating auxiliary types.
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
                        typePool);
                this.methodPool = methodPool;
            }

            @Override
            protected UnresolvedType create(TypeInitializer typeInitializer) {
                int writerFlags = asmVisitorWrapper.mergeWriter(AsmVisitorWrapper.NO_FLAGS);
                ClassWriter classWriter = new FrameComputingClassWriter(writerFlags, typePool);
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
                classVisitor.visitEnd();
                return new UnresolvedType(classWriter.toByteArray(), implementationContext.getAuxiliaryTypes());
            }
        }

        /**
         * An action to write a class file to the dumping location.
         */
        @EqualsAndHashCode
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
             * The type's binary representation.
             */
            private final byte[] binaryRepresentation;

            /**
             * Creates a new class dump action.
             *
             * @param target               The target folder for writing the class file to.
             * @param instrumentedType     The instrumented type.
             * @param binaryRepresentation The type's binary representation.
             */
            protected ClassDumpAction(String target, TypeDescription instrumentedType, byte[] binaryRepresentation) {
                this.target = target;
                this.instrumentedType = instrumentedType;
                this.binaryRepresentation = binaryRepresentation;
            }

            @Override
            public Void run() throws Exception {
                OutputStream outputStream = new FileOutputStream(new File(target, instrumentedType.getName() + "." + System.currentTimeMillis()));
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
