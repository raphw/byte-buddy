package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.asm.ClassVisitorWrapper;
import net.bytebuddy.description.ModifierReviewable;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.PackageDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeList;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.inline.MethodRebaseResolver;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.LoadedTypeInitializer;
import net.bytebuddy.implementation.attribute.AnnotationAppender;
import net.bytebuddy.implementation.attribute.FieldAttributeAppender;
import net.bytebuddy.implementation.attribute.MethodAttributeAppender;
import net.bytebuddy.implementation.attribute.TypeAttributeAppender;
import net.bytebuddy.implementation.auxiliary.AuxiliaryType;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.implementation.bytecode.member.MethodInvocation;
import net.bytebuddy.implementation.bytecode.member.MethodReturn;
import net.bytebuddy.implementation.bytecode.member.MethodVariableAccess;
import net.bytebuddy.utility.RandomString;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.RemappingClassAdapter;
import org.objectweb.asm.commons.RemappingMethodAdapter;
import org.objectweb.asm.commons.SimpleRemapper;

import java.io.IOException;
import java.util.*;

import static net.bytebuddy.utility.ByteBuddyCommons.join;

/**
 * A type writer is a utility for writing an actual class file using the ASM library.
 *
 * @param <T> The best known loaded type for the dynamically created type.
 */
public interface TypeWriter<T> {

    /**
     * Creates the dynamic type that is described by this type writer.
     *
     * @return An unloaded dynamic type that describes the created type.
     */
    DynamicType.Unloaded<T> make();

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
             * Returns the field attribute appender for a given field.
             *
             * @return The attribute appender to be applied on the given field.
             */
            FieldAttributeAppender getFieldAppender();

            /**
             * Returns the default value for the field that is represented by this entry. This value might be
             * {@code null} if no such value is set.
             *
             * @return The default value for the field that is represented by this entry.
             */
            Object getDefaultValue();

            /**
             * Writes this entry to a given class visitor.
             *
             * @param classVisitor The class visitor to which this entry is to be written to.
             */
            void apply(ClassVisitor classVisitor);

            /**
             * A record for a simple field without attributes or a default value.
             */
            class ForSimpleField implements Record {

                /**
                 * The implemented field.
                 */
                private final FieldDescription fieldDescription;

                /**
                 * Creates a new record for a simple field.
                 *
                 * @param fieldDescription The described field.
                 */
                public ForSimpleField(FieldDescription fieldDescription) {
                    this.fieldDescription = fieldDescription;
                }

                @Override
                public FieldAttributeAppender getFieldAppender() {
                    return FieldAttributeAppender.NoOp.INSTANCE;
                }

                @Override
                public Object getDefaultValue() {
                    return FieldDescription.NO_DEFAULT_VALUE;
                }

                @Override
                public void apply(ClassVisitor classVisitor) {
                    classVisitor.visitField(fieldDescription.getModifiers(),
                            fieldDescription.getInternalName(),
                            fieldDescription.getDescriptor(),
                            fieldDescription.getGenericSignature(),
                            FieldDescription.NO_DEFAULT_VALUE).visitEnd();
                }

                @Override
                public boolean equals(Object other) {
                    if (this == other) return true;
                    if (other == null || getClass() != other.getClass()) return false;
                    ForSimpleField that = (ForSimpleField) other;
                    return fieldDescription.equals(that.fieldDescription);
                }

                @Override
                public int hashCode() {
                    return fieldDescription.hashCode();
                }

                @Override
                public String toString() {
                    return "TypeWriter.FieldPool.Record.ForSimpleField{" +
                            "fieldDescription=" + fieldDescription +
                            '}';
                }
            }

            /**
             * A record for a rich field with attributes and a potential default value.
             */
            class ForRichField implements Record {

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
                public ForRichField(FieldAttributeAppender attributeAppender, Object defaultValue, FieldDescription fieldDescription) {
                    this.attributeAppender = attributeAppender;
                    this.defaultValue = defaultValue;
                    this.fieldDescription = fieldDescription;
                }

                @Override
                public FieldAttributeAppender getFieldAppender() {
                    return attributeAppender;
                }

                @Override
                public Object getDefaultValue() {
                    return defaultValue;
                }

                @Override
                public void apply(ClassVisitor classVisitor) {
                    FieldVisitor fieldVisitor = classVisitor.visitField(fieldDescription.getModifiers(),
                            fieldDescription.getInternalName(),
                            fieldDescription.getDescriptor(),
                            fieldDescription.getGenericSignature(),
                            getDefaultValue());
                    attributeAppender.apply(fieldVisitor, fieldDescription);
                    fieldVisitor.visitEnd();
                }

                @Override
                public boolean equals(Object other) {
                    if (this == other) return true;
                    if (other == null || getClass() != other.getClass()) return false;
                    ForRichField that = (ForRichField) other;
                    return attributeAppender.equals(that.attributeAppender)
                            && !(defaultValue != null ? !defaultValue.equals(that.defaultValue) : that.defaultValue != null)
                            && fieldDescription.equals(that.fieldDescription);
                }

                @Override
                public int hashCode() {
                    int result = attributeAppender.hashCode();
                    result = 31 * result + (defaultValue != null ? defaultValue.hashCode() : 0);
                    result = 31 * result + fieldDescription.hashCode();
                    return result;
                }

                @Override
                public String toString() {
                    return "TypeWriter.FieldPool.Record.ForRichField{" +
                            "attributeAppender=" + attributeAppender +
                            ", defaultValue=" + defaultValue +
                            ", fieldDescription=" + fieldDescription +
                            '}';
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
             * Returns the method that is implemented where the returned method ressembles a potential transformation. An implemented
             * method is only defined if a method is not {@link Record.Sort#SKIPPED}.
             *
             * @return The implemented method.
             */
            MethodDescription getImplementedMethod();

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
             * @param classVisitor          The class visitor to which this entry should be applied.
             * @param implementationContext The implementation context to which this entry should be applied.
             */
            void apply(ClassVisitor classVisitor, Implementation.Context implementationContext);

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
             * @param methodVisitor         The method visitor to which this entry should be applied.
             * @param implementationContext The implementation context to which this entry should be applied.
             */
            void applyBody(MethodVisitor methodVisitor, Implementation.Context implementationContext);

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

                @Override
                public String toString() {
                    return "TypeWriter.MethodPool.Entry.Sort." + name();
                }
            }

            /**
             * A canonical implementation of a method that is not declared but inherited by the instrumented type.
             */
            enum ForNonDefinedMethod implements Record {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                @Override
                public void apply(ClassVisitor classVisitor, Implementation.Context implementationContext) {
                    /* do nothing */
                }

                @Override
                public void applyBody(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
                    throw new IllegalStateException("Cannot apply headless implementation for method that should be skipped");
                }

                @Override
                public void applyHead(MethodVisitor methodVisitor) {
                    throw new IllegalStateException("Cannot apply headless implementation for method that should be skipped");
                }

                @Override
                public MethodDescription getImplementedMethod() {
                    throw new IllegalStateException("A method that is not defined cannot be extracted");
                }

                @Override
                public Sort getSort() {
                    return Sort.SKIPPED;
                }

                @Override
                public Record prepend(ByteCodeAppender byteCodeAppender) {
                    throw new IllegalStateException("Cannot prepend code to non-implemented method");
                }

                @Override
                public String toString() {
                    return "TypeWriter.MethodPool.Record.ForNonDefinedMethod." + name();
                }
            }

            /**
             * A base implementation of an abstract entry that defines a method.
             */
            abstract class ForDefinedMethod implements Record {

                @Override
                public void apply(ClassVisitor classVisitor, Implementation.Context implementationContext) {
                    MethodVisitor methodVisitor = classVisitor.visitMethod(getImplementedMethod().getAdjustedModifiers(getSort().isImplemented()),
                            getImplementedMethod().getInternalName(),
                            getImplementedMethod().getDescriptor(),
                            getImplementedMethod().getGenericSignature(),
                            getImplementedMethod().getExceptionTypes().asRawTypes().toInternalNames());
                    ParameterList<?> parameterList = getImplementedMethod().getParameters();
                    if (parameterList.hasExplicitMetaData()) {
                        for (ParameterDescription parameterDescription : parameterList) {
                            methodVisitor.visitParameter(parameterDescription.getName(), parameterDescription.getModifiers());
                        }
                    }
                    applyHead(methodVisitor);
                    applyBody(methodVisitor, implementationContext);
                    methodVisitor.visitEnd();
                }

                /**
                 * Describes an entry that defines a method as byte code.
                 */
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
                     * Creates a new record for an implemented method without attributes or a modifier resolver.
                     *
                     * @param methodDescription The implemented method.
                     * @param byteCodeAppender  The byte code appender to apply.
                     */
                    public WithBody(MethodDescription methodDescription, ByteCodeAppender byteCodeAppender) {
                        this(methodDescription, byteCodeAppender, MethodAttributeAppender.NoOp.INSTANCE);
                    }

                    /**
                     * Creates a new entry for a method that defines a method as byte code.
                     *
                     * @param methodDescription       The implemented method.
                     * @param byteCodeAppender        The byte code appender to apply.
                     * @param methodAttributeAppender The method attribute appender to apply.
                     */
                    public WithBody(MethodDescription methodDescription, ByteCodeAppender byteCodeAppender, MethodAttributeAppender methodAttributeAppender) {
                        this.methodDescription = methodDescription;
                        this.byteCodeAppender = byteCodeAppender;
                        this.methodAttributeAppender = methodAttributeAppender;
                    }

                    @Override
                    public MethodDescription getImplementedMethod() {
                        return methodDescription;
                    }

                    @Override
                    public Sort getSort() {
                        return Sort.IMPLEMENTED;
                    }

                    @Override
                    public void applyHead(MethodVisitor methodVisitor) {
                        /* do nothing */
                    }

                    @Override
                    public void applyBody(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
                        methodAttributeAppender.apply(methodVisitor, methodDescription);
                        methodVisitor.visitCode();
                        ByteCodeAppender.Size size = byteCodeAppender.apply(methodVisitor, implementationContext, methodDescription);
                        methodVisitor.visitMaxs(size.getOperandStackSize(), size.getLocalVariableSize());
                    }

                    @Override
                    public Record prepend(ByteCodeAppender byteCodeAppender) {
                        return new WithBody(methodDescription, new ByteCodeAppender.Compound(byteCodeAppender, this.byteCodeAppender), methodAttributeAppender);
                    }

                    @Override
                    public boolean equals(Object other) {
                        if (this == other) return true;
                        if (other == null || getClass() != other.getClass()) return false;
                        WithBody withBody = (WithBody) other;
                        return methodDescription.equals(withBody.methodDescription)
                                && byteCodeAppender.equals(withBody.byteCodeAppender)
                                && methodAttributeAppender.equals(withBody.methodAttributeAppender);
                    }

                    @Override
                    public int hashCode() {
                        int result = methodDescription.hashCode();
                        result = 31 * result + byteCodeAppender.hashCode();
                        result = 31 * result + methodAttributeAppender.hashCode();
                        return result;
                    }

                    @Override
                    public String toString() {
                        return "TypeWriter.MethodPool.Record.ForDefinedMethod.WithBody{" +
                                "methodDescription=" + methodDescription +
                                ", byteCodeAppender=" + byteCodeAppender +
                                ", methodAttributeAppender=" + methodAttributeAppender +
                                '}';
                    }
                }

                /**
                 * Describes an entry that defines a method but without byte code and without an annotation value.
                 */
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
                     * Creates a new entry for a method that is defines but does not append byte code, i.e. is native or abstract.
                     *
                     * @param methodDescription       The implemented method.
                     * @param methodAttributeAppender The method attribute appender to apply.
                     */
                    public WithoutBody(MethodDescription methodDescription, MethodAttributeAppender methodAttributeAppender) {
                        this.methodDescription = methodDescription;
                        this.methodAttributeAppender = methodAttributeAppender;
                    }

                    @Override
                    public MethodDescription getImplementedMethod() {
                        return methodDescription;
                    }

                    @Override
                    public Sort getSort() {
                        return Sort.DEFINED;
                    }

                    @Override
                    public void applyHead(MethodVisitor methodVisitor) {
                        /* do nothing */
                    }

                    @Override
                    public void applyBody(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
                        methodAttributeAppender.apply(methodVisitor, methodDescription);
                    }

                    @Override
                    public Record prepend(ByteCodeAppender byteCodeAppender) {
                        throw new IllegalStateException("Cannot prepend code to abstract method");
                    }

                    @Override
                    public boolean equals(Object other) {
                        if (this == other) return true;
                        if (other == null || getClass() != other.getClass()) return false;
                        WithoutBody that = (WithoutBody) other;
                        return methodDescription.equals(that.methodDescription)
                                && methodAttributeAppender.equals(that.methodAttributeAppender);
                    }

                    @Override
                    public int hashCode() {
                        int result = methodDescription.hashCode();
                        result = 31 * result + methodAttributeAppender.hashCode();
                        return result;
                    }

                    @Override
                    public String toString() {
                        return "TypeWriter.MethodPool.Record.ForDefinedMethod.WithoutBody{" +
                                "methodDescription=" + methodDescription +
                                ", methodAttributeAppender=" + methodAttributeAppender +
                                '}';
                    }
                }

                /**
                 * Describes an entry that defines a method with a default annotation value.
                 */
                public static class WithAnnotationDefaultValue extends ForDefinedMethod {

                    /**
                     * The implemented method.
                     */
                    private final MethodDescription methodDescription;

                    /**
                     * The annotation value to define.
                     */
                    private final Object annotationValue;

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
                                                      Object annotationValue,
                                                      MethodAttributeAppender methodAttributeAppender) {
                        this.methodDescription = methodDescription;
                        this.annotationValue = annotationValue;
                        this.methodAttributeAppender = methodAttributeAppender;
                    }

                    @Override
                    public MethodDescription getImplementedMethod() {
                        return methodDescription;
                    }

                    @Override
                    public Sort getSort() {
                        return Sort.DEFINED;
                    }

                    @Override
                    public void applyHead(MethodVisitor methodVisitor) {
                        if (!methodDescription.isDefaultValue(annotationValue)) {
                            throw new IllegalStateException("Cannot set " + annotationValue + " as default for " + methodDescription);
                        }
                        AnnotationVisitor annotationVisitor = methodVisitor.visitAnnotationDefault();
                        AnnotationAppender.Default.apply(annotationVisitor,
                                methodDescription.getReturnType().asRawType(),
                                AnnotationAppender.NO_NAME,
                                annotationValue);
                        annotationVisitor.visitEnd();
                    }

                    @Override
                    public void applyBody(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
                        methodAttributeAppender.apply(methodVisitor, methodDescription);
                    }

                    @Override
                    public Record prepend(ByteCodeAppender byteCodeAppender) {
                        throw new IllegalStateException("Cannot prepend code to method that defines a default annotation value");
                    }

                    @Override
                    public boolean equals(Object other) {
                        if (this == other) return true;
                        if (other == null || getClass() != other.getClass()) return false;
                        WithAnnotationDefaultValue that = (WithAnnotationDefaultValue) other;
                        return methodDescription.equals(that.methodDescription)
                                && annotationValue.equals(that.annotationValue)
                                && methodAttributeAppender.equals(that.methodAttributeAppender);
                    }

                    @Override
                    public int hashCode() {
                        int result = methodDescription.hashCode();
                        result = 31 * result + annotationValue.hashCode();
                        result = 31 * result + methodAttributeAppender.hashCode();
                        return result;
                    }

                    @Override
                    public String toString() {
                        return "TypeWriter.MethodPool.Record.ForDefinedMethod.WithAnnotationDefaultValue{" +
                                "methodDescription=" + methodDescription +
                                ", annotationValue=" + annotationValue +
                                ", methodAttributeAppender=" + methodAttributeAppender +
                                '}';
                    }
                }

                /**
                 * A record for a visibility bridge.
                 */
                public static class OfVisibilityBridge extends ForDefinedMethod implements ByteCodeAppender {

                    /**
                     * Creates a record for a visibility bridge.
                     *
                     * @param instrumentedType  The instrumented type.
                     * @param bridgeTarget      The target method of the visibility bridge.
                     * @param attributeAppender The attribute appender to apply to the visibility bridge.
                     * @return A record describing the visibility bridge.
                     */
                    public static Record of(TypeDescription instrumentedType, MethodDescription bridgeTarget, MethodAttributeAppender attributeAppender) {
                        return new OfVisibilityBridge(VisibilityBridge.of(instrumentedType, bridgeTarget),
                                bridgeTarget,
                                instrumentedType.getSuperType().asRawType(),
                                attributeAppender);
                    }

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
                    private final TypeDescription superType;

                    /**
                     * The attribute appender to apply to the visibility bridge.
                     */
                    private final MethodAttributeAppender attributeAppender;

                    /**
                     * Creates a new record for a visibility bridge.
                     *
                     * @param visibilityBridge  The visibility bridge.
                     * @param bridgeTarget      The method the visibility bridge invokes.
                     * @param superType         The super type of the instrumented type.
                     * @param attributeAppender The attribute appender to apply to the visibility bridge.
                     */
                    protected OfVisibilityBridge(MethodDescription visibilityBridge,
                                                 MethodDescription bridgeTarget,
                                                 TypeDescription superType,
                                                 MethodAttributeAppender attributeAppender) {
                        this.visibilityBridge = visibilityBridge;
                        this.bridgeTarget = bridgeTarget;
                        this.superType = superType;
                        this.attributeAppender = attributeAppender;
                    }

                    @Override
                    public MethodDescription getImplementedMethod() {
                        return visibilityBridge;
                    }

                    @Override
                    public Sort getSort() {
                        return Sort.IMPLEMENTED;
                    }

                    @Override
                    public Record prepend(ByteCodeAppender byteCodeAppender) {
                        return new ForDefinedMethod.WithBody(visibilityBridge, new ByteCodeAppender.Compound(this, byteCodeAppender), attributeAppender);
                    }

                    @Override
                    public void applyHead(MethodVisitor methodVisitor) {
                        /* do nothing */
                    }

                    @Override
                    public void applyBody(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
                        attributeAppender.apply(methodVisitor, visibilityBridge);
                        methodVisitor.visitCode();
                        ByteCodeAppender.Size size = apply(methodVisitor, implementationContext, visibilityBridge);
                        methodVisitor.visitMaxs(size.getOperandStackSize(), size.getLocalVariableSize());
                    }

                    @Override
                    public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext, MethodDescription instrumentedMethod) {
                        return new ByteCodeAppender.Simple(
                                MethodVariableAccess.allArgumentsOf(instrumentedMethod).prependThisReference(),
                                MethodInvocation.invoke(bridgeTarget).special(superType),
                                MethodReturn.returning(instrumentedMethod.getReturnType().asRawType())
                        ).apply(methodVisitor, implementationContext, instrumentedMethod);
                    }

                    @Override
                    public boolean equals(Object other) {
                        if (this == other) return true;
                        if (other == null || getClass() != other.getClass()) return false;
                        OfVisibilityBridge that = (OfVisibilityBridge) other;
                        return visibilityBridge.equals(that.visibilityBridge)
                                && bridgeTarget.equals(that.bridgeTarget)
                                && superType.equals(that.superType)
                                && attributeAppender.equals(that.attributeAppender);
                    }

                    @Override
                    public int hashCode() {
                        int result = visibilityBridge.hashCode();
                        result = 31 * result + bridgeTarget.hashCode();
                        result = 31 * result + superType.hashCode();
                        result = 31 * result + attributeAppender.hashCode();
                        return result;
                    }

                    @Override
                    public String toString() {
                        return "TypeWriter.MethodPool.Record.ForDefinedMethod.OfVisibilityBridge{" +
                                "visibilityBridge=" + visibilityBridge +
                                ", bridgeTarget=" + bridgeTarget +
                                ", superType=" + superType +
                                ", attributeAppender=" + attributeAppender +
                                '}';
                    }

                    /**
                     * A method describing a visibility bridge.
                     */
                    protected static class VisibilityBridge extends MethodDescription.InDefinedShape.AbstractBase {

                        /**
                         * Creates a visibility bridge.
                         *
                         * @param instrumentedType The instrumented type.
                         * @param bridgeTarget     The target method of the visibility bridge.
                         * @return A method description of the visibility bridge.
                         */
                        protected static MethodDescription of(TypeDescription instrumentedType, MethodDescription bridgeTarget) {
                            return new VisibilityBridge(instrumentedType, bridgeTarget.asToken().accept(GenericTypeDescription.Visitor.TypeErasing.INSTANCE));
                        }

                        /**
                         * The instrumented type.
                         */
                        private final TypeDescription instrumentedType;

                        /**
                         * A token describing the bridge's raw target.
                         */
                        private final MethodDescription.Token bridgeTarget;

                        /**
                         * Creates a new visibility bridge method.
                         *
                         * @param instrumentedType The instrumented type.
                         * @param bridgeTarget     A token describing the bridge's target.
                         */
                        protected VisibilityBridge(TypeDescription instrumentedType, Token bridgeTarget) {
                            this.instrumentedType = instrumentedType;
                            this.bridgeTarget = bridgeTarget;
                        }

                        @Override
                        public TypeDescription getDeclaringType() {
                            return instrumentedType;
                        }

                        @Override
                        public ParameterList<ParameterDescription.InDefinedShape> getParameters() {
                            return new ParameterList.ForTokens(this, bridgeTarget.getParameterTokens());
                        }

                        @Override
                        public GenericTypeDescription getReturnType() {
                            return bridgeTarget.getReturnType();
                        }

                        @Override
                        public GenericTypeList getExceptionTypes() {
                            return bridgeTarget.getExceptionTypes();
                        }

                        @Override
                        public Object getDefaultValue() {
                            return MethodDescription.NO_DEFAULT_VALUE;
                        }

                        @Override
                        public GenericTypeList getTypeVariables() {
                            return new GenericTypeList.Empty();
                        }

                        @Override
                        public AnnotationList getDeclaredAnnotations() {
                            return bridgeTarget.getAnnotations();
                        }

                        @Override
                        public int getModifiers() {
                            return (bridgeTarget.getModifiers() | Opcodes.ACC_SYNTHETIC | Opcodes.ACC_BRIDGE) & ~Opcodes.ACC_NATIVE;
                        }

                        @Override
                        public String getInternalName() {
                            return bridgeTarget.getInternalName();
                        }
                    }
                }
            }

            /**
             * A wrapper that appends accessor bridges for a method's implementation. The bridges are only added if
             * {@link net.bytebuddy.dynamic.scaffold.TypeWriter.MethodPool.Record#apply(ClassVisitor, Implementation.Context)} is invoked such
             * that bridges are not appended for methods that are rebased or redefined as such types already have bridge methods in place.
             */
            class AccessBridgeWrapper implements Record {

                /**
                 * Wrapps the given record in an accessor bridge wrapper if necessary.
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
                    return bridgeTypes.isEmpty() || (instrumentedType.isInterface() && !delegate.getSort().isImplemented())
                            ? delegate
                            : new AccessBridgeWrapper(delegate, instrumentedType, bridgeTarget, bridgeTypes, attributeAppender);
                }

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

                @Override
                public Sort getSort() {
                    return delegate.getSort();
                }

                @Override
                public MethodDescription getImplementedMethod() {
                    return bridgeTarget;
                }

                @Override
                public Record prepend(ByteCodeAppender byteCodeAppender) {
                    return new AccessBridgeWrapper(delegate.prepend(byteCodeAppender), instrumentedType, bridgeTarget, bridgeTypes, attributeAppender);
                }

                @Override
                public void apply(ClassVisitor classVisitor, Implementation.Context implementationContext) {
                    delegate.apply(classVisitor, implementationContext);
                    for (MethodDescription.TypeToken bridgeType : bridgeTypes) {
                        MethodDescription bridgeMethod = new AccessorBridge(bridgeTarget, bridgeType, instrumentedType);
                        MethodDescription bridgeTarget = new BridgeTarget(this.bridgeTarget, instrumentedType);
                        MethodVisitor methodVisitor = classVisitor.visitMethod(bridgeMethod.getAdjustedModifiers(true),
                                bridgeMethod.getInternalName(),
                                bridgeMethod.getDescriptor(),
                                MethodDescription.NON_GENERIC_SIGNATURE,
                                bridgeMethod.getExceptionTypes().asRawTypes().toInternalNames());
                        attributeAppender.apply(methodVisitor, bridgeMethod);
                        methodVisitor.visitCode();
                        ByteCodeAppender.Size size = new ByteCodeAppender.Simple(
                                MethodVariableAccess.allArgumentsOf(bridgeMethod).asBridgeOf(bridgeTarget).prependThisReference(),
                                MethodInvocation.invoke(bridgeTarget).virtual(instrumentedType),
                                MethodReturn.returning(bridgeTarget.getReturnType().asRawType())
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
                public void applyBody(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
                    delegate.applyBody(methodVisitor, implementationContext);
                }

                @Override
                public boolean equals(Object other) {
                    if (this == other) return true;
                    if (other == null || getClass() != other.getClass()) return false;
                    AccessBridgeWrapper that = (AccessBridgeWrapper) other;
                    return delegate.equals(that.delegate)
                            && instrumentedType.equals(that.instrumentedType)
                            && bridgeTarget.equals(that.bridgeTarget)
                            && bridgeTypes.equals(that.bridgeTypes)
                            && attributeAppender.equals(that.attributeAppender);
                }

                @Override
                public int hashCode() {
                    int result = delegate.hashCode();
                    result = 31 * result + instrumentedType.hashCode();
                    result = 31 * result + bridgeTarget.hashCode();
                    result = 31 * result + bridgeTypes.hashCode();
                    result = 31 * result + attributeAppender.hashCode();
                    return result;
                }

                @Override
                public String toString() {
                    return "TypeWriter.MethodPool.Record.AccessBridgeWrapper{" +
                            "delegate=" + delegate +
                            ", instrumentedType=" + instrumentedType +
                            ", bridgeTarget=" + bridgeTarget +
                            ", bridgeTypes=" + bridgeTypes +
                            ", attributeAppender=" + attributeAppender +
                            '}';
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
                    public GenericTypeDescription getReturnType() {
                        return bridgeType.getReturnType();
                    }

                    @Override
                    public GenericTypeList getExceptionTypes() {
                        return bridgeTarget.getExceptionTypes().accept(GenericTypeDescription.Visitor.TypeErasing.INSTANCE);
                    }

                    @Override
                    public Object getDefaultValue() {
                        return MethodDescription.NO_DEFAULT_VALUE;
                    }

                    @Override
                    public GenericTypeList getTypeVariables() {
                        return new GenericTypeList.Empty();
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
                        return new ParameterList.ForTokens(this, bridgeTarget.getParameters().asTokenList());
                    }

                    @Override
                    public GenericTypeDescription getReturnType() {
                        return bridgeTarget.getReturnType();
                    }

                    @Override
                    public GenericTypeList getExceptionTypes() {
                        return bridgeTarget.getExceptionTypes();
                    }

                    @Override
                    public Object getDefaultValue() {
                        return bridgeTarget.getDefaultValue();
                    }

                    @Override
                    public GenericTypeList getTypeVariables() {
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
    abstract class Default<S> implements TypeWriter<S> {

        /**
         * A flag for ASM not to automatically compute any information such as operand stack sizes and stack map frames.
         */
        protected static final int ASM_MANUAL_FLAG = 0;

        /**
         * The ASM API version to use.
         */
        protected static final int ASM_API_VERSION = Opcodes.ASM5;

        /**
         * The instrumented type that is to be written.
         */
        protected final TypeDescription instrumentedType;

        /**
         * The loaded type initializer of the instrumented type.
         */
        protected final LoadedTypeInitializer loadedTypeInitializer;

        /**
         * The type initializer of the instrumented type.
         */
        protected final InstrumentedType.TypeInitializer typeInitializer;

        /**
         * A list of explicit auxiliary types that are to be added to the created dynamic type.
         */
        protected final List<DynamicType> explicitAuxiliaryTypes;

        /**
         * The class file version of the written type.
         */
        protected final ClassFileVersion classFileVersion;

        /**
         * A naming strategy that is used for naming auxiliary types.
         */
        protected final AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy;

        /**
         * A class visitor wrapper to apply during instrumentation.
         */
        protected final ClassVisitorWrapper classVisitorWrapper;

        /**
         * The type attribute appender to apply.
         */
        protected final TypeAttributeAppender attributeAppender;

        /**
         * The field pool to be used for instrumenting fields.
         */
        protected final FieldPool fieldPool;

        /**
         * The method pool to be used for instrumenting methods.
         */
        protected final MethodPool methodPool;

        /**
         * A list of all instrumented methods.
         */
        protected final MethodList<?> instrumentedMethods;

        /**
         * Creates a new default type writer.
         *
         * @param instrumentedType            The instrumented type that is to be written.
         * @param loadedTypeInitializer       The loaded type initializer of the instrumented type.
         * @param typeInitializer             The type initializer of the instrumented type.
         * @param explicitAuxiliaryTypes      A list of explicit auxiliary types that are to be added to the created dynamic type.
         * @param classFileVersion            The class file version of the written type.
         * @param auxiliaryTypeNamingStrategy A naming strategy that is used for naming auxiliary types.
         * @param classVisitorWrapper         A class visitor wrapper to apply during instrumentation.
         * @param attributeAppender           The type attribute appender to apply.
         * @param fieldPool                   The field pool to be used for instrumenting fields.
         * @param methodPool                  The method pool to be used for instrumenting methods.
         * @param instrumentedMethods         A list of all instrumented methods.
         */
        protected Default(TypeDescription instrumentedType,
                          LoadedTypeInitializer loadedTypeInitializer,
                          InstrumentedType.TypeInitializer typeInitializer,
                          List<DynamicType> explicitAuxiliaryTypes,
                          ClassFileVersion classFileVersion,
                          AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                          ClassVisitorWrapper classVisitorWrapper,
                          TypeAttributeAppender attributeAppender,
                          FieldPool fieldPool,
                          MethodPool methodPool,
                          MethodList<?> instrumentedMethods) {
            this.instrumentedType = instrumentedType;
            this.loadedTypeInitializer = loadedTypeInitializer;
            this.typeInitializer = typeInitializer;
            this.explicitAuxiliaryTypes = explicitAuxiliaryTypes;
            this.classFileVersion = classFileVersion;
            this.auxiliaryTypeNamingStrategy = auxiliaryTypeNamingStrategy;
            this.classVisitorWrapper = classVisitorWrapper;
            this.attributeAppender = attributeAppender;
            this.fieldPool = fieldPool;
            this.methodPool = methodPool;
            this.instrumentedMethods = instrumentedMethods;
        }

        /**
         * Creates a type writer for creating a new type.
         *
         * @param methodRegistry              The method registry to use for creating the type.
         * @param fieldPool                   The field pool to use.
         * @param auxiliaryTypeNamingStrategy A naming strategy for naming auxiliary types.
         * @param classVisitorWrapper         The class visitor wrapper to apply when creating the type.
         * @param attributeAppender           The attribute appender to use.
         * @param classFileVersion            The class file version of the created type.
         * @param <U>                         The best known loaded type for the dynamically created type.
         * @return An appropriate type writer.
         */
        public static <U> TypeWriter<U> forCreation(MethodRegistry.Compiled methodRegistry,
                                                    FieldPool fieldPool,
                                                    AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                                                    ClassVisitorWrapper classVisitorWrapper,
                                                    TypeAttributeAppender attributeAppender,
                                                    ClassFileVersion classFileVersion) {
            return new ForCreation<U>(methodRegistry.getInstrumentedType(),
                    methodRegistry.getLoadedTypeInitializer(),
                    methodRegistry.getTypeInitializer(),
                    Collections.<DynamicType>emptyList(),
                    classFileVersion,
                    auxiliaryTypeNamingStrategy,
                    classVisitorWrapper,
                    attributeAppender,
                    fieldPool,
                    methodRegistry,
                    methodRegistry.getInstrumentedMethods());
        }

        /**
         * Creates a type writer for creating a new type.
         *
         * @param methodRegistry              The method registry to use for creating the type.
         * @param fieldPool                   The field pool to use.
         * @param auxiliaryTypeNamingStrategy A naming strategy for naming auxiliary types.
         * @param classVisitorWrapper         The class visitor wrapper to apply when creating the type.
         * @param attributeAppender           The attribute appender to use.
         * @param classFileVersion            The minimum class file version of the created type.
         * @param classFileLocator            The class file locator to use.
         * @param methodRebaseResolver        The method rebase resolver to use.
         * @param targetType                  The target type that is to be rebased.
         * @param <U>                         The best known loaded type for the dynamically created type.
         * @return An appropriate type writer.
         */
        public static <U> TypeWriter<U> forRebasing(MethodRegistry.Compiled methodRegistry,
                                                    FieldPool fieldPool,
                                                    AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                                                    ClassVisitorWrapper classVisitorWrapper,
                                                    TypeAttributeAppender attributeAppender,
                                                    ClassFileVersion classFileVersion,
                                                    ClassFileLocator classFileLocator,
                                                    TypeDescription targetType,
                                                    MethodRebaseResolver methodRebaseResolver) {
            return new ForInlining<U>(methodRegistry.getInstrumentedType(),
                    methodRegistry.getLoadedTypeInitializer(),
                    methodRegistry.getTypeInitializer(),
                    methodRebaseResolver.getAuxiliaryTypes(),
                    classFileVersion,
                    auxiliaryTypeNamingStrategy,
                    classVisitorWrapper,
                    attributeAppender,
                    fieldPool,
                    methodRegistry,
                    methodRegistry.getInstrumentedMethods(),
                    classFileLocator,
                    targetType,
                    methodRebaseResolver);
        }

        /**
         * Creates a type writer for creating a new type.
         *
         * @param methodRegistry              The method registry to use for creating the type.
         * @param fieldPool                   The field pool to use.
         * @param auxiliaryTypeNamingStrategy A naming strategy for naming auxiliary types.
         * @param classVisitorWrapper         The class visitor wrapper to apply when creating the type.
         * @param attributeAppender           The attribute appender to use.
         * @param classFileVersion            The minimum class file version of the created type.
         * @param classFileLocator            The class file locator to use.
         * @param targetType                  The target type that is to be rebased.
         * @param <U>                         The best known loaded type for the dynamically created type.
         * @return An appropriate type writer.
         */
        public static <U> TypeWriter<U> forRedefinition(MethodRegistry.Compiled methodRegistry,
                                                        FieldPool fieldPool,
                                                        AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                                                        ClassVisitorWrapper classVisitorWrapper,
                                                        TypeAttributeAppender attributeAppender,
                                                        ClassFileVersion classFileVersion,
                                                        ClassFileLocator classFileLocator,
                                                        TypeDescription targetType) {
            return new ForInlining<U>(methodRegistry.getInstrumentedType(),
                    methodRegistry.getLoadedTypeInitializer(),
                    methodRegistry.getTypeInitializer(),
                    Collections.<DynamicType>emptyList(),
                    classFileVersion,
                    auxiliaryTypeNamingStrategy,
                    classVisitorWrapper,
                    attributeAppender,
                    fieldPool,
                    methodRegistry,
                    methodRegistry.getInstrumentedMethods(),
                    classFileLocator,
                    targetType,
                    MethodRebaseResolver.Disabled.INSTANCE);
        }

        @Override
        public DynamicType.Unloaded<S> make() {
            Implementation.Context.ExtractableView implementationContext = new Implementation.Context.Default(instrumentedType,
                    auxiliaryTypeNamingStrategy,
                    typeInitializer,
                    classFileVersion);
            return new DynamicType.Default.Unloaded<S>(instrumentedType,
                    create(implementationContext),
                    loadedTypeInitializer,
                    join(explicitAuxiliaryTypes, implementationContext.getRegisteredAuxiliaryTypes()));
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            Default<?> aDefault = (Default<?>) other;
            return instrumentedType.equals(aDefault.instrumentedType)
                    && loadedTypeInitializer.equals(aDefault.loadedTypeInitializer)
                    && typeInitializer.equals(aDefault.typeInitializer)
                    && explicitAuxiliaryTypes.equals(aDefault.explicitAuxiliaryTypes)
                    && classFileVersion.equals(aDefault.classFileVersion)
                    && auxiliaryTypeNamingStrategy.equals(aDefault.auxiliaryTypeNamingStrategy)
                    && classVisitorWrapper.equals(aDefault.classVisitorWrapper)
                    && attributeAppender.equals(aDefault.attributeAppender)
                    && fieldPool.equals(aDefault.fieldPool)
                    && methodPool.equals(aDefault.methodPool)
                    && instrumentedMethods.equals(aDefault.instrumentedMethods);
        }

        @Override
        public int hashCode() {
            int result = instrumentedType.hashCode();
            result = 31 * result + loadedTypeInitializer.hashCode();
            result = 31 * result + typeInitializer.hashCode();
            result = 31 * result + explicitAuxiliaryTypes.hashCode();
            result = 31 * result + classFileVersion.hashCode();
            result = 31 * result + auxiliaryTypeNamingStrategy.hashCode();
            result = 31 * result + classVisitorWrapper.hashCode();
            result = 31 * result + attributeAppender.hashCode();
            result = 31 * result + fieldPool.hashCode();
            result = 31 * result + methodPool.hashCode();
            result = 31 * result + instrumentedMethods.hashCode();
            return result;
        }

        /**
         * Creates the instrumented type.
         *
         * @param implementationContext The implementation context to use.
         * @return A byte array that is represented by the instrumented type.
         */
        protected abstract byte[] create(Implementation.Context.ExtractableView implementationContext);

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
             * The constraint to assert the members against. The constraint is first defined when the general class information is visited.
             */
            private Constraint constraint;

            /**
             * Creates a validating class visitor.
             *
             * @param classVisitor The class visitor to which any calls are delegated to.
             */
            protected ValidatingClassVisitor(ClassVisitor classVisitor) {
                super(ASM_API_VERSION, classVisitor);
            }

            @Override
            public void visit(int version, int modifier, String name, String signature, String superName, String[] interfaces) {
                ClassFileVersion classFileVersion = new ClassFileVersion(version);
                if (name.endsWith("/" + PackageDescription.PACKAGE_CLASS_NAME)) {
                    constraint = Constraint.PACKAGE_CLASS;
                } else if ((modifier & Opcodes.ACC_ANNOTATION) != ModifierReviewable.EMPTY_MASK) {
                    constraint = classFileVersion.isSupportsDefaultMethods()
                            ? Constraint.JAVA8_ANNOTATION
                            : Constraint.ANNOTATION;
                } else if ((modifier & Opcodes.ACC_INTERFACE) != ModifierReviewable.EMPTY_MASK) {
                    constraint = classFileVersion.isSupportsDefaultMethods()
                            ? Constraint.JAVA8_INTERFACE
                            : Constraint.INTERFACE;
                } else if ((modifier & Opcodes.ACC_ABSTRACT) != ModifierReviewable.EMPTY_MASK) {
                    constraint = Constraint.ABSTRACT_CLASS;
                } else {
                    constraint = Constraint.MANIFEST_CLASS;
                }
                constraint.assertPackage(modifier, interfaces);
                super.visit(version, modifier, name, signature, superName, interfaces);
            }

            @Override
            public FieldVisitor visitField(int modifiers, String name, String desc, String signature, Object defaultValue) {
                constraint.assertField(name, (modifiers & Opcodes.ACC_PUBLIC) != 0, (modifiers & Opcodes.ACC_STATIC) != 0);
                return super.visitField(modifiers, name, desc, signature, defaultValue);
            }

            @Override
            public MethodVisitor visitMethod(int modifiers, String name, String descriptor, String signature, String[] exceptions) {
                constraint.assertMethod(name,
                        (modifiers & Opcodes.ACC_ABSTRACT) != 0,
                        (modifiers & Opcodes.ACC_PUBLIC) != 0,
                        (modifiers & Opcodes.ACC_STATIC) != 0,
                        !descriptor.startsWith(NO_PARAMETERS) || descriptor.endsWith(RETURNS_VOID));
                return new ValidatingMethodVisitor(super.visitMethod(modifiers, name, descriptor, signature, exceptions), name);
            }

            @Override
            public String toString() {
                return "TypeWriter.Default.ValidatingClassVisitor{" +
                        "constraint=" + constraint +
                        "}";
            }

            /**
             * A constraint for members that are legal for a given type.
             */
            protected enum Constraint {

                /**
                 * Constraints for a non-abstract class.
                 */
                MANIFEST_CLASS("non-abstract class", true, true, true, true, false, true, false, true),

                /**
                 * Constraints for an abstract class.
                 */
                ABSTRACT_CLASS("abstract class", true, true, true, true, true, true, false, true),

                /**
                 * Constrains for an interface type before Java 8.
                 */
                INTERFACE("interface (Java 7-)", false, false, false, false, true, false, false, true),

                /**
                 * Constrains for an interface type since Java 8.
                 */
                JAVA8_INTERFACE("interface (Java 8+)", false, false, false, true, true, true, false, true),

                /**
                 * Constrains for an annotation type before Java 8.
                 */
                ANNOTATION("annotation (Java 7-)", false, false, false, false, true, false, true, true),

                /**
                 * Constrains for an annotation type since Java 8.
                 */
                JAVA8_ANNOTATION("annotation (Java 8+)", false, false, false, true, true, true, true, true),

                /**
                 * Constraints for a package type, i.e. a class named {@code package-info}.
                 */
                PACKAGE_CLASS("package definition", false, false, false, false, false, false, false, false);

                /**
                 * A name to represent the type being validated within an error message.
                 */
                private final String sortName;

                /**
                 * Determines if a sort allows constructors.
                 */
                private final boolean allowsConstructor;

                /**
                 * Determines if a sort allows non-public members.
                 */
                private final boolean allowsNonPublic;

                /**
                 * Determines if a sort allows non-static fields.
                 */
                private final boolean allowsNonStaticFields;

                /**
                 * Determines if a sort allows static methods.
                 */
                private final boolean allowsStaticMethods;

                /**
                 * Determines if a sort allows abstract methods.
                 */
                private final boolean allowsAbstract;

                /**
                 * Determines if a sort allows non-abstract methods.
                 */
                private final boolean allowsNonAbstract;

                /**
                 * Determines if a sort allows the definition of annotation default values.
                 */
                private final boolean allowsDefaultValue;

                /**
                 * Determines if a sort allows the type a shape that does not resemble a package description.
                 */
                private final boolean allowsNonPackage;

                /**
                 * Creates a new constraint.
                 *
                 * @param sortName              A name to represent the type being validated within an error message.
                 * @param allowsConstructor     Determines if a sort allows constructors.
                 * @param allowsNonPublic       Determines if a sort allows non-public members.
                 * @param allowsNonStaticFields Determines if a sort allows non-static fields.
                 * @param allowsStaticMethods   Determines if a sort allows static methods.
                 * @param allowsAbstract        Determines if a sort allows abstract methods.
                 * @param allowsNonAbstract     Determines if a sort allows non-abstract methods.
                 * @param allowsDefaultValue    Determines if a sort allows the definition of annotation default values.
                 * @param allowsNonPackage      Determines if a sort allows the type a shape that does not resemble a package description.
                 */
                Constraint(String sortName,
                           boolean allowsConstructor,
                           boolean allowsNonPublic,
                           boolean allowsNonStaticFields,
                           boolean allowsStaticMethods,
                           boolean allowsAbstract,
                           boolean allowsNonAbstract,
                           boolean allowsDefaultValue,
                           boolean allowsNonPackage) {
                    this.sortName = sortName;
                    this.allowsConstructor = allowsConstructor;
                    this.allowsNonPublic = allowsNonPublic;
                    this.allowsNonStaticFields = allowsNonStaticFields;
                    this.allowsStaticMethods = allowsStaticMethods;
                    this.allowsAbstract = allowsAbstract;
                    this.allowsNonAbstract = allowsNonAbstract;
                    this.allowsDefaultValue = allowsDefaultValue;
                    this.allowsNonPackage = allowsNonPackage;
                }

                /**
                 * Asserts a field for being valid.
                 *
                 * @param name     The name of the field.
                 * @param isPublic {@code true} if this field is public.
                 * @param isStatic {@code true} if this field is static.
                 */
                protected void assertField(String name, boolean isPublic, boolean isStatic) {
                    if (!isPublic && !allowsNonPublic) {
                        throw new IllegalStateException("Cannot define non-public field " + name + " for " + sortName);
                    } else if (!isStatic && !allowsNonStaticFields) {
                        throw new IllegalStateException("Cannot define for non-static field " + name + " for " + sortName);
                    }
                }

                /**
                 * Asserts a method for being valid.
                 *
                 * @param name                  The name of the method.
                 * @param isAbstract            {@code true} if the method is abstract.
                 * @param isPublic              {@code true} if this method is public.
                 * @param isStatic              {@code true} if this method is static.
                 * @param isDefaultIncompatible {@code true} if a method's signature cannot describe an annotation property method.
                 */
                protected void assertMethod(String name, boolean isAbstract, boolean isPublic, boolean isStatic, boolean isDefaultIncompatible) {
                    if (!allowsConstructor && name.equals(MethodDescription.CONSTRUCTOR_INTERNAL_NAME)) {
                        throw new IllegalStateException("Cannot define constructor for " + sortName);
                    } else if (isStatic && isAbstract) {
                        throw new IllegalStateException("Cannot define static method " + name + " to be abstract");
                    } else if (isAbstract && name.equals(MethodDescription.CONSTRUCTOR_INTERNAL_NAME)) {
                        throw new IllegalStateException("Cannot define abstract constructor " + name);
                    } else if (!isPublic && !allowsNonPublic) {
                        throw new IllegalStateException("Cannot define non-public method " + name + " for " + sortName);
                    } else if (isStatic && !allowsStaticMethods) {
                        throw new IllegalStateException("Cannot define static method " + name + " for " + sortName);
                    } else if (!isStatic && isAbstract && !allowsAbstract) {
                        throw new IllegalStateException("Cannot define abstract method " + name + " for " + sortName);
                    } else if (!isAbstract && !allowsNonAbstract) {
                        throw new IllegalStateException("Cannot define non-abstract method " + name + " for " + sortName);
                    } else if (!isStatic && isDefaultIncompatible && allowsDefaultValue) {
                        throw new IllegalStateException("The signature of " + name + " is not compatible for a property of " + sortName);
                    }
                }

                /**
                 * Asserts if a default value is legal for a method.
                 *
                 * @param name The name of the method.
                 */
                protected void assertDefault(String name) {
                    if (!allowsDefaultValue) {
                        throw new IllegalStateException("Cannot define define default value on " + name + " for " + sortName);
                    }
                }

                /**
                 * Asserts if the type can legally represent a package description.
                 *
                 * @param modifier   The modifier that is to be written to the type.
                 * @param interfaces The interfaces that are to be appended to the type.
                 */
                protected void assertPackage(int modifier, String[] interfaces) {
                    if (!allowsNonPackage && modifier != PackageDescription.PACKAGE_MODIFIERS) {
                        throw new IllegalStateException("Cannot alter modifier for " + sortName);
                    } else if (!allowsNonPackage && interfaces != null) {
                        throw new IllegalStateException("Cannot implement interface for " + sortName);
                    }
                }

                @Override
                public String toString() {
                    return "TypeWriter.Default.ValidatingClassVisitor.Constraint." + name();
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
                    super(ASM_API_VERSION, methodVisitor);
                    this.name = name;
                }

                @Override
                public AnnotationVisitor visitAnnotationDefault() {
                    constraint.assertDefault(name);
                    return super.visitAnnotationDefault();
                }

                @Override
                public String toString() {
                    return "TypeWriter.Default.ValidatingClassVisitor.ValidatingMethodVisitor{" +
                            "classVisitor=" + ValidatingClassVisitor.this +
                            ", name='" + name + '\'' +
                            '}';
                }
            }
        }

        /**
         * A type writer that inlines the created type into an existing class file.
         *
         * @param <U> The best known loaded type for the dynamically created type.
         */
        public static class ForInlining<U> extends Default<U> {

            /**
             * Indicates that a class does not define an explicit super class.
             */
            private static final TypeDescription NO_SUPER_CLASS = null;

            /**
             * Indicates that a method should be retained.
             */
            private static final MethodDescription RETAIN_METHOD = null;

            /**
             * Indicates that a method should be ignored.
             */
            private static final MethodVisitor IGNORE_METHOD = null;

            /**
             * Indicates that an annotation should be ignored.
             */
            private static final AnnotationVisitor IGNORE_ANNOTATION = null;

            /**
             * The class file locator to use.
             */
            private final ClassFileLocator classFileLocator;

            /**
             * The target type that is to be redefined via inlining.
             */
            private final TypeDescription targetType;

            /**
             * The method rebase resolver to use.
             */
            private final MethodRebaseResolver methodRebaseResolver;

            /**
             * Creates a new type writer for inling a type into an existing type description.
             *
             * @param instrumentedType            The instrumented type that is to be written.
             * @param loadedTypeInitializer       The loaded type initializer of the instrumented type.
             * @param typeInitializer             The type initializer of the instrumented type.
             * @param explicitAuxiliaryTypes      A list of explicit auxiliary types that are to be added to the created dynamic type.
             * @param classFileVersion            The class file version of the written type.
             * @param auxiliaryTypeNamingStrategy A naming strategy that is used for naming auxiliary types.
             * @param classVisitorWrapper         A class visitor wrapper to apply during instrumentation.
             * @param attributeAppender           The type attribute appender to apply.
             * @param fieldPool                   The field pool to be used for instrumenting fields.
             * @param methodPool                  The method pool to be used for instrumenting methods.
             * @param instrumentedMethods         A list of all instrumented methods.
             * @param classFileLocator            The class file locator to use.
             * @param targetType                  The target type that is to be redefined via inlining.
             * @param methodRebaseResolver        The method rebase resolver to use.
             */
            protected ForInlining(TypeDescription instrumentedType,
                                  LoadedTypeInitializer loadedTypeInitializer,
                                  InstrumentedType.TypeInitializer typeInitializer,
                                  List<DynamicType> explicitAuxiliaryTypes,
                                  ClassFileVersion classFileVersion,
                                  AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                                  ClassVisitorWrapper classVisitorWrapper,
                                  TypeAttributeAppender attributeAppender,
                                  FieldPool fieldPool,
                                  MethodPool methodPool,
                                  MethodList instrumentedMethods,
                                  ClassFileLocator classFileLocator,
                                  TypeDescription targetType,
                                  MethodRebaseResolver methodRebaseResolver) {
                super(instrumentedType,
                        loadedTypeInitializer,
                        typeInitializer,
                        explicitAuxiliaryTypes,
                        classFileVersion,
                        auxiliaryTypeNamingStrategy,
                        classVisitorWrapper,
                        attributeAppender,
                        fieldPool,
                        methodPool,
                        instrumentedMethods);
                this.classFileLocator = classFileLocator;
                this.targetType = targetType;
                this.methodRebaseResolver = methodRebaseResolver;
            }

            @Override
            public byte[] create(Implementation.Context.ExtractableView implementationContext) {
                try {
                    ClassFileLocator.Resolution resolution = classFileLocator.locate(targetType.getName());
                    if (!resolution.isResolved()) {
                        throw new IllegalArgumentException("Cannot locate the class file for " + targetType + " using " + classFileLocator);
                    }
                    return doCreate(implementationContext, resolution.resolve());
                } catch (IOException e) {
                    throw new RuntimeException("The class file could not be written", e);
                }
            }

            /**
             * Performs the actual creation of a class file.
             *
             * @param implementationContext The implementation context to use for implementing the class file.
             * @param binaryRepresentation  The binary representation of the class file.
             * @return The byte array representing the created class.
             */
            private byte[] doCreate(Implementation.Context.ExtractableView implementationContext, byte[] binaryRepresentation) {
                ClassReader classReader = new ClassReader(binaryRepresentation);
                ClassWriter classWriter = new ClassWriter(classReader, ASM_MANUAL_FLAG);
                classReader.accept(writeTo(classVisitorWrapper.wrap(new ValidatingClassVisitor(classWriter)), implementationContext), ASM_MANUAL_FLAG);
                return classWriter.toByteArray();
            }

            /**
             * Creates a class visitor which weaves all changes and additions on the fly.
             *
             * @param classVisitor          The class visitor to which this entry is to be written to.
             * @param implementationContext The implementation context to use for implementing the class file.
             * @return A class visitor which is capable of applying the changes.
             */
            private ClassVisitor writeTo(ClassVisitor classVisitor, Implementation.Context.ExtractableView implementationContext) {
                return FramePreservingRemapper.of(targetType.getInternalName(),
                        instrumentedType.getInternalName(),
                        new RedefinitionClassVisitor(classVisitor, implementationContext));
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) return true;
                if (other == null || getClass() != other.getClass()) return false;
                if (!super.equals(other)) return false;
                ForInlining<?> that = (ForInlining<?>) other;
                return classFileLocator.equals(that.classFileLocator)
                        && targetType.equals(that.targetType)
                        && methodRebaseResolver.equals(that.methodRebaseResolver);
            }

            @Override
            public int hashCode() {
                int result = super.hashCode();
                result = 31 * result + classFileLocator.hashCode();
                result = 31 * result + targetType.hashCode();
                result = 31 * result + methodRebaseResolver.hashCode();
                return result;
            }

            @Override
            public String toString() {
                return "TypeWriter.Default.ForInlining{" +
                        "instrumentedType=" + instrumentedType +
                        ", loadedTypeInitializer=" + loadedTypeInitializer +
                        ", typeInitializer=" + typeInitializer +
                        ", explicitAuxiliaryTypes=" + explicitAuxiliaryTypes +
                        ", classFileVersion=" + classFileVersion +
                        ", auxiliaryTypeNamingStrategy=" + auxiliaryTypeNamingStrategy +
                        ", classVisitorWrapper=" + classVisitorWrapper +
                        ", attributeAppender=" + attributeAppender +
                        ", fieldPool=" + fieldPool +
                        ", methodPool=" + methodPool +
                        ", instrumentedMethods=" + instrumentedMethods +
                        ", classFileLocator=" + classFileLocator +
                        ", targetType=" + targetType +
                        ", methodRebaseResolver=" + methodRebaseResolver +
                        '}';
            }

            /**
             * A class visitor which is capable of applying a redefinition of an existing class file.
             */
            protected class RedefinitionClassVisitor extends ClassVisitor {

                /**
                 * The implementation context for this class creation.
                 */
                private final Implementation.Context.ExtractableView implementationContext;

                /**
                 * A mapping of fields to write by their names.
                 */
                private final Map<String, FieldDescription> declaredFields;

                /**
                 * A mapping of methods to write by a concatenation of internal name and descriptor.
                 */
                private final Map<String, MethodDescription> declarableMethods;

                /**
                 * A mutable reference for code that is to be injected into the actual type initializer, if any.
                 * Usually, this represents an invocation of the actual type initializer that is found in the class
                 * file which is relocated into a static method.
                 */
                private Implementation.Context.ExtractableView.InjectedCode injectedCode;

                /**
                 * Creates a class visitor which is capable of redefining an existent class on the fly.
                 *
                 * @param classVisitor          The underlying class visitor to which writes are delegated.
                 * @param implementationContext The implementation context to use for implementing the class file.
                 */
                protected RedefinitionClassVisitor(ClassVisitor classVisitor, Implementation.Context.ExtractableView implementationContext) {
                    super(ASM_API_VERSION, classVisitor);
                    this.implementationContext = implementationContext;
                    List<? extends FieldDescription> fieldDescriptions = instrumentedType.getDeclaredFields();
                    declaredFields = new HashMap<String, FieldDescription>(fieldDescriptions.size());
                    for (FieldDescription fieldDescription : fieldDescriptions) {
                        declaredFields.put(fieldDescription.getName(), fieldDescription);
                    }
                    declarableMethods = new HashMap<String, MethodDescription>(instrumentedMethods.size());
                    for (MethodDescription methodDescription : instrumentedMethods) {
                        declarableMethods.put(methodDescription.getInternalName() + methodDescription.getDescriptor(), methodDescription);
                    }
                    injectedCode = Implementation.Context.ExtractableView.InjectedCode.None.INSTANCE;
                }

                @Override
                public void visit(int classFileVersionNumber,
                                  int modifiers,
                                  String internalName,
                                  String genericSignature,
                                  String superTypeInternalName,
                                  String[] interfaceTypeInternalName) {
                    super.visit(classFileVersionNumber,
                            instrumentedType.getActualModifiers((modifiers & Opcodes.ACC_SUPER) != 0),
                            instrumentedType.getInternalName(),
                            instrumentedType.getGenericSignature(),
                            (instrumentedType.getSuperType() == NO_SUPER_CLASS ?
                                    TypeDescription.OBJECT :
                                    instrumentedType.getSuperType().asRawType()).getInternalName(),
                            instrumentedType.getInterfaces().asRawTypes().toInternalNames());
                    attributeAppender.apply(this, instrumentedType);
                }

                @Override
                public FieldVisitor visitField(int modifiers,
                                               String internalName,
                                               String descriptor,
                                               String genericSignature,
                                               Object defaultValue) {
                    declaredFields.remove(internalName); // Ignore in favor of the class file definition.
                    return super.visitField(modifiers, internalName, descriptor, genericSignature, defaultValue);
                }

                @Override
                public MethodVisitor visitMethod(int modifiers,
                                                 String internalName,
                                                 String descriptor,
                                                 String genericSignature,
                                                 String[] exceptionTypeInternalName) {
                    if (internalName.equals(MethodDescription.TYPE_INITIALIZER_INTERNAL_NAME)) {
                        TypeInitializerInjection injectedCode = new TypeInitializerInjection(instrumentedType);
                        this.injectedCode = injectedCode;
                        return super.visitMethod(injectedCode.getInjectorProxyMethod().getModifiers(),
                                injectedCode.getInjectorProxyMethod().getInternalName(),
                                injectedCode.getInjectorProxyMethod().getDescriptor(),
                                injectedCode.getInjectorProxyMethod().getGenericSignature(),
                                injectedCode.getInjectorProxyMethod().getExceptionTypes().asRawTypes().toInternalNames());
                    }
                    MethodDescription methodDescription = declarableMethods.remove(internalName + descriptor);
                    return methodDescription == RETAIN_METHOD
                            ? super.visitMethod(modifiers, internalName, descriptor, genericSignature, exceptionTypeInternalName)
                            : redefine(methodDescription, (modifiers & Opcodes.ACC_ABSTRACT) != 0);
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
                        return super.visitMethod(methodDescription.getModifiers(),
                                methodDescription.getInternalName(),
                                methodDescription.getDescriptor(),
                                methodDescription.getGenericSignature(),
                                methodDescription.getExceptionTypes().asRawTypes().toInternalNames());
                    }
                    MethodDescription implementedMethod = record.getImplementedMethod();
                    MethodVisitor methodVisitor = super.visitMethod(implementedMethod.getAdjustedModifiers(record.getSort().isImplemented()),
                            implementedMethod.getInternalName(),
                            implementedMethod.getDescriptor(),
                            implementedMethod.getGenericSignature(),
                            implementedMethod.getExceptionTypes().asRawTypes().toInternalNames());
                    return abstractOrigin
                            ? new AttributeObtainingMethodVisitor(methodVisitor, record)
                            : new CodePreservingMethodVisitor(methodVisitor, record, methodRebaseResolver.resolve(implementedMethod.asDefined()));
                }

                @Override
                public void visitEnd() {
                    for (FieldDescription fieldDescription : declaredFields.values()) {
                        fieldPool.target(fieldDescription).apply(cv);
                    }
                    for (MethodDescription methodDescription : declarableMethods.values()) {
                        methodPool.target(methodDescription).apply(cv, implementationContext);
                    }
                    implementationContext.drain(cv, methodPool, injectedCode);
                    super.visitEnd();
                }

                @Override
                public String toString() {
                    return "TypeWriter.Default.ForInlining.RedefinitionClassVisitor{" +
                            "typeWriter=" + TypeWriter.Default.ForInlining.this +
                            ", implementationContext=" + implementationContext +
                            ", declaredFields=" + declaredFields +
                            ", declarableMethods=" + declarableMethods +
                            ", injectedCode=" + injectedCode +
                            '}';
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
                        super(ASM_API_VERSION, actualMethodVisitor);
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
                    public void visitCode() {
                        record.applyBody(actualMethodVisitor, implementationContext);
                        actualMethodVisitor.visitEnd();
                        mv = resolution.isRebased()
                                ? cv.visitMethod(resolution.getResolvedMethod().getModifiers(),
                                resolution.getResolvedMethod().getInternalName(),
                                resolution.getResolvedMethod().getDescriptor(),
                                resolution.getResolvedMethod().getGenericSignature(),
                                resolution.getResolvedMethod().getExceptionTypes().asRawTypes().toInternalNames())
                                : IGNORE_METHOD;
                        super.visitCode();
                    }

                    @Override
                    public void visitMaxs(int maxStack, int maxLocals) {
                        super.visitMaxs(maxStack, Math.max(maxLocals, resolution.getResolvedMethod().getStackSize()));
                    }

                    @Override
                    public String toString() {
                        return "TypeWriter.Default.ForInlining.RedefinitionClassVisitor.CodePreservingMethodVisitor{" +
                                "classVisitor=" + TypeWriter.Default.ForInlining.RedefinitionClassVisitor.this +
                                ", actualMethodVisitor=" + actualMethodVisitor +
                                ", entry=" + record +
                                ", resolution=" + resolution +
                                '}';
                    }
                }

                /**
                 * A method visitor that obtains all attributes and annotations of a method that is found in the
                 * class file but discards all code.
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
                        super(ASM_API_VERSION, actualMethodVisitor);
                        this.actualMethodVisitor = actualMethodVisitor;
                        this.record = record;
                        record.applyHead(actualMethodVisitor);
                    }

                    @Override
                    public AnnotationVisitor visitAnnotationDefault() {
                        return IGNORE_ANNOTATION;
                    }

                    @Override
                    public void visitCode() {
                        mv = IGNORE_METHOD;
                    }

                    @Override
                    public void visitEnd() {
                        record.applyBody(actualMethodVisitor, implementationContext);
                        actualMethodVisitor.visitEnd();
                    }

                    @Override
                    public String toString() {
                        return "TypeWriter.Default.ForInlining.RedefinitionClassVisitor.AttributeObtainingMethodVisitor{" +
                                "classVisitor=" + TypeWriter.Default.ForInlining.RedefinitionClassVisitor.this +
                                ", actualMethodVisitor=" + actualMethodVisitor +
                                ", entry=" + record +
                                '}';
                    }
                }

                /**
                 * A code injection for the type initializer that invokes a method representing the original type initializer
                 * which is copied to a static method.
                 */
                protected class TypeInitializerInjection implements Implementation.Context.ExtractableView.InjectedCode {

                    /**
                     * The method to which the original type initializer code is to be written to.
                     */
                    private final MethodDescription injectorProxyMethod;

                    /**
                     * Creates a new type initializer injection.
                     *
                     * @param instrumentedType The instrumented type.
                     */
                    protected TypeInitializerInjection(TypeDescription instrumentedType) {
                        injectorProxyMethod = new TypeInitializerDelegate(instrumentedType, RandomString.make());
                    }

                    @Override
                    public ByteCodeAppender getByteCodeAppender() {
                        return new ByteCodeAppender.Simple(MethodInvocation.invoke(injectorProxyMethod));
                    }

                    @Override
                    public boolean isDefined() {
                        return true;
                    }

                    /**
                     * Returns the proxy method to which the original type initializer code is written to.
                     *
                     * @return A method description of this proxy method.
                     */
                    public MethodDescription getInjectorProxyMethod() {
                        return injectorProxyMethod;
                    }

                    @Override
                    public String toString() {
                        return "TypeWriter.Default.ForInlining.RedefinitionClassVisitor.TypeInitializerInjection{" +
                                "classVisitor=" + TypeWriter.Default.ForInlining.RedefinitionClassVisitor.this +
                                ", injectorProxyMethod=" + injectorProxyMethod +
                                '}';
                    }
                }
            }

            /**
             * A method containing the original type initializer of a redefined class.
             */
            protected static class TypeInitializerDelegate extends MethodDescription.InDefinedShape.AbstractBase {

                /**
                 * A prefix for the name of the method that represents the original type initializer.
                 */
                private static final String TYPE_INITIALIZER_PROXY_PREFIX = "classInitializer";

                /**
                 * The instrumented type that defines this delegate method.
                 */
                private final TypeDescription instrumentedType;

                /**
                 * The suffix to append to the default prefix in order to avoid naming conflicts.
                 */
                private final String suffix;

                /**
                 * Creates a new type initializer delegate.
                 *
                 * @param instrumentedType The instrumented type that defines this delegate method.
                 * @param suffix           The suffix to append to the default prefix in order to avoid naming conflicts.
                 */
                protected TypeInitializerDelegate(TypeDescription instrumentedType, String suffix) {
                    this.instrumentedType = instrumentedType;
                    this.suffix = suffix;
                }

                @Override
                public TypeDescription getDeclaringType() {
                    return instrumentedType;
                }

                @Override
                public ParameterList<ParameterDescription.InDefinedShape> getParameters() {
                    return new ParameterList.Empty();
                }

                @Override
                public GenericTypeDescription getReturnType() {
                    return TypeDescription.VOID;
                }

                @Override
                public GenericTypeList getExceptionTypes() {
                    return new GenericTypeList.Empty();
                }

                @Override
                public Object getDefaultValue() {
                    return MethodDescription.NO_DEFAULT_VALUE;
                }

                @Override
                public GenericTypeList getTypeVariables() {
                    return new GenericTypeList.Empty();
                }

                @Override
                public AnnotationList getDeclaredAnnotations() {
                    return new AnnotationList.Empty();
                }

                @Override
                public int getModifiers() {
                    return Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC;
                }

                @Override
                public String getInternalName() {
                    return String.format("%s$%s", TYPE_INITIALIZER_PROXY_PREFIX, suffix);
                }
            }

            /**
             * A remapper adapter that does not attempt to reorder method frames which is never the case for a renaming. This
             * adaption might not longer be necessary in the future:
             * <a href="http://forge.ow2.org/tracker/index.php?func=detail&aid=317576&group_id=23&atid=100023">ASM #317576</a>.
             */
            protected static class FramePreservingRemapper extends RemappingClassAdapter {

                /**
                 * Creates a class visitor that renames the instrumented type from the original name to the target name if those
                 * names are not equal.
                 *
                 * @param originalName The instrumented type's original name.
                 * @param targetName   The instrumented type's actual name.
                 * @param classVisitor The class visitor that is responsible for creating the type.
                 * @return An appropriate class visitor.
                 */
                public static ClassVisitor of(String originalName, String targetName, ClassVisitor classVisitor) {
                    return originalName.equals(targetName)
                            ? classVisitor
                            : new FramePreservingRemapper(classVisitor, new SimpleRemapper(originalName, targetName));
                }

                /**
                 * Creates a new frame preserving class remapper.
                 *
                 * @param classVisitor The class visitor that is responsible for writing the class.
                 * @param remapper     The remapper to use for renaming the instrumented type.
                 */
                protected FramePreservingRemapper(ClassVisitor classVisitor, Remapper remapper) {
                    super(ASM_API_VERSION, classVisitor, remapper);
                }

                @Override
                protected MethodVisitor createRemappingMethodAdapter(int modifiers, String adaptedDescriptor, MethodVisitor methodVisitor) {
                    return new FramePreservingMethodRemapper(modifiers, adaptedDescriptor, methodVisitor, remapper);
                }

                @Override
                public String toString() {
                    return "TypeWriter.Default.ForInlining.FramePreservingRemapper{" +
                            "}";
                }

                /**
                 * A method remapper that does not delegate to its underlying variable sorting mechanism as this is never required for
                 * renaming a type. This way, it is not required to hand expanded method frames to this visitor what is otherwise
                 * required for more general remappings that sort local variables.
                 */
                protected static class FramePreservingMethodRemapper extends RemappingMethodAdapter {

                    /**
                     * Creates a new frame preserving method remapper.
                     *
                     * @param modifiers     The method's modifiers.
                     * @param descriptor    The descriptor of the method.
                     * @param methodVisitor The method visitor that is responsible for writing the method.
                     * @param remapper      The remapper to use for renaming the instrumented type.
                     */
                    public FramePreservingMethodRemapper(int modifiers, String descriptor, MethodVisitor methodVisitor, Remapper remapper) {
                        super(ASM_API_VERSION, modifiers, descriptor, methodVisitor, remapper);
                    }

                    @Override
                    public void visitFrame(int type, int sizeLocal, Object[] local, int sizeStack, Object[] stack) {
                        mv.visitFrame(type, sizeLocal, remapEntries(sizeLocal, local), sizeStack, remapEntries(sizeStack, stack));
                    }

                    /**
                     * Remaps a stack map.
                     *
                     * @param size  The size of the stack map.
                     * @param entry The stack map's entries.
                     * @return The remapped entries.
                     */
                    private Object[] remapEntries(int size, Object[] entry) {
                        for (int index = 0; index < size; index++) {
                            if (entry[index] instanceof String) {
                                Object[] newEntry = new Object[size];
                                if (index > 0) {
                                    System.arraycopy(entry, 0, newEntry, 0, index);
                                }
                                do {
                                    Object frame = entry[index];
                                    newEntry[index++] = frame instanceof String
                                            ? remapper.mapType((String) frame)
                                            : frame;
                                } while (index < size);
                                return newEntry;
                            }
                        }
                        return entry;
                    }

                    @Override
                    public String toString() {
                        return "TypeWriter.Default.ForInlining.FramePreservingRemapper.FramePreservingMethodRemapper{" +
                                "remapper=" + remapper +
                                "}";
                    }
                }
            }
        }

        /**
         * A type writer that creates a class file that is not based upon another, existing class.
         *
         * @param <U> The best known loaded type for the dynamically created type.
         */
        public static class ForCreation<U> extends Default<U> {

            /**
             * Creates a new type writer for creating a new type.
             *
             * @param instrumentedType            The instrumented type that is to be written.
             * @param loadedTypeInitializer       The loaded type initializer of the instrumented type.
             * @param typeInitializer             The type initializer of the instrumented type.
             * @param explicitAuxiliaryTypes      A list of explicit auxiliary types that are to be added to the created dynamic type.
             * @param classFileVersion            The class file version of the written type.
             * @param auxiliaryTypeNamingStrategy A naming strategy that is used for naming auxiliary types.
             * @param classVisitorWrapper         A class visitor wrapper to apply during instrumentation.
             * @param attributeAppender           The type attribute appender to apply.
             * @param fieldPool                   The field pool to be used for instrumenting fields.
             * @param methodPool                  The method pool to be used for instrumenting methods.
             * @param instrumentedMethods         A list of all instrumented methods.
             */
            protected ForCreation(TypeDescription instrumentedType,
                                  LoadedTypeInitializer loadedTypeInitializer,
                                  InstrumentedType.TypeInitializer typeInitializer,
                                  List<DynamicType> explicitAuxiliaryTypes,
                                  ClassFileVersion classFileVersion,
                                  AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                                  ClassVisitorWrapper classVisitorWrapper,
                                  TypeAttributeAppender attributeAppender,
                                  FieldPool fieldPool,
                                  MethodPool methodPool,
                                  MethodList instrumentedMethods) {
                super(instrumentedType,
                        loadedTypeInitializer,
                        typeInitializer,
                        explicitAuxiliaryTypes,
                        classFileVersion,
                        auxiliaryTypeNamingStrategy,
                        classVisitorWrapper,
                        attributeAppender,
                        fieldPool,
                        methodPool,
                        instrumentedMethods);
            }

            @Override
            public byte[] create(Implementation.Context.ExtractableView implementationContext) {
                ClassWriter classWriter = new ClassWriter(ASM_MANUAL_FLAG);
                ClassVisitor classVisitor = classVisitorWrapper.wrap(new ValidatingClassVisitor(classWriter));
                classVisitor.visit(classFileVersion.getVersion(),
                        instrumentedType.getActualModifiers(!instrumentedType.isInterface()),
                        instrumentedType.getInternalName(),
                        instrumentedType.getGenericSignature(),
                        (instrumentedType.getSuperType() == null
                                ? TypeDescription.OBJECT
                                : instrumentedType.getSuperType().asRawType()).getInternalName(),
                        instrumentedType.getInterfaces().asRawTypes().toInternalNames());
                attributeAppender.apply(classVisitor, instrumentedType);
                for (FieldDescription fieldDescription : instrumentedType.getDeclaredFields()) {
                    fieldPool.target(fieldDescription).apply(classVisitor);
                }
                for (MethodDescription methodDescription : instrumentedMethods) {
                    methodPool.target(methodDescription).apply(classVisitor, implementationContext);
                }
                implementationContext.drain(classVisitor, methodPool, Implementation.Context.ExtractableView.InjectedCode.None.INSTANCE);
                classVisitor.visitEnd();
                return classWriter.toByteArray();
            }

            @Override
            public String toString() {
                return "TypeWriter.Default.ForCreation{" +
                        "instrumentedType=" + instrumentedType +
                        ", loadedTypeInitializer=" + loadedTypeInitializer +
                        ", typeInitializer=" + typeInitializer +
                        ", explicitAuxiliaryTypes=" + explicitAuxiliaryTypes +
                        ", classFileVersion=" + classFileVersion +
                        ", auxiliaryTypeNamingStrategy=" + auxiliaryTypeNamingStrategy +
                        ", classVisitorWrapper=" + classVisitorWrapper +
                        ", attributeAppender=" + attributeAppender +
                        ", fieldPool=" + fieldPool +
                        ", methodPool=" + methodPool +
                        ", instrumentedMethods=" + instrumentedMethods +
                        "}";
            }
        }
    }
}
