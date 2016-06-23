package net.bytebuddy.dynamic.scaffold;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.PackageDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.inline.MethodRebaseResolver;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.LoadedTypeInitializer;
import net.bytebuddy.implementation.attribute.*;
import net.bytebuddy.implementation.auxiliary.AuxiliaryType;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.assign.TypeCasting;
import net.bytebuddy.implementation.bytecode.member.MethodInvocation;
import net.bytebuddy.implementation.bytecode.member.MethodReturn;
import net.bytebuddy.implementation.bytecode.member.MethodVariableAccess;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.utility.CompoundList;
import net.bytebuddy.utility.RandomString;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
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

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && fieldDescription.equals(((ForImplicitField) other).fieldDescription);
                }

                @Override
                public int hashCode() {
                    return fieldDescription.hashCode();
                }

                @Override
                public String toString() {
                    return "TypeWriter.FieldPool.Record.ForImplicitField{" +
                            "fieldDescription=" + fieldDescription +
                            '}';
                }
            }

            /**
             * A record for a rich field with attributes and a potential default value.
             */
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

                @Override
                public boolean equals(Object other) {
                    if (this == other) return true;
                    if (other == null || getClass() != other.getClass()) return false;
                    ForExplicitField that = (ForExplicitField) other;
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
                    return "TypeWriter.FieldPool.Record.ForExplicitField{" +
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
             * Returns the method that is implemented where the returned method resembles a potential transformation. An implemented
             * method is only defined if a method is not {@link Record.Sort#SKIPPED}.
             *
             * @return The implemented method.
             */
            MethodDescription getMethod();

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
                public void apply(ClassVisitor classVisitor, Implementation.Context implementationContext, AnnotationValueFilter.Factory annotationValueFilterFactory) {
                    /* do nothing */
                }

                @Override
                public void applyBody(MethodVisitor methodVisitor, Implementation.Context implementationContext, AnnotationValueFilter.Factory annotationValueFilterFactory) {
                    throw new IllegalStateException("Cannot apply headless implementation for method that should be skipped");
                }

                @Override
                public void applyHead(MethodVisitor methodVisitor) {
                    throw new IllegalStateException("Cannot apply headless implementation for method that should be skipped");
                }

                @Override
                public MethodDescription getMethod() {
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
                public void apply(ClassVisitor classVisitor, Implementation.Context implementationContext, AnnotationValueFilter.Factory annotationValueFilterFactory) {
                    MethodVisitor methodVisitor = classVisitor.visitMethod(getMethod().getActualModifiers(getSort().isImplemented()),
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
                    public MethodDescription getMethod() {
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
                    public void applyBody(MethodVisitor methodVisitor, Implementation.Context implementationContext, AnnotationValueFilter.Factory annotationValueFilterFactory) {
                        methodAttributeAppender.apply(methodVisitor, methodDescription, annotationValueFilterFactory.on(methodDescription));
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
                    public MethodDescription getMethod() {
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
                    public void applyBody(MethodVisitor methodVisitor, Implementation.Context implementationContext, AnnotationValueFilter.Factory annotationValueFilterFactory) {
                        methodAttributeAppender.apply(methodVisitor, methodDescription, annotationValueFilterFactory.on(methodDescription));
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
                    public MethodDescription getMethod() {
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
                                methodDescription.getReturnType().asErasure(),
                                AnnotationAppender.NO_NAME,
                                annotationValue);
                        annotationVisitor.visitEnd();
                    }

                    @Override
                    public void applyBody(MethodVisitor methodVisitor, Implementation.Context implementationContext, AnnotationValueFilter.Factory annotationValueFilterFactory) {
                        methodAttributeAppender.apply(methodVisitor, methodDescription, annotationValueFilterFactory.on(methodDescription));
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
                    public Record prepend(ByteCodeAppender byteCodeAppender) {
                        return new ForDefinedMethod.WithBody(visibilityBridge, new ByteCodeAppender.Compound(this, byteCodeAppender), attributeAppender);
                    }

                    @Override
                    public void applyHead(MethodVisitor methodVisitor) {
                        /* do nothing */
                    }

                    @Override
                    public void applyBody(MethodVisitor methodVisitor, Implementation.Context implementationContext, AnnotationValueFilter.Factory annotationValueFilterFactory) {
                        attributeAppender.apply(methodVisitor, visibilityBridge, annotationValueFilterFactory.on(visibilityBridge));
                        methodVisitor.visitCode();
                        ByteCodeAppender.Size size = apply(methodVisitor, implementationContext, visibilityBridge);
                        methodVisitor.visitMaxs(size.getOperandStackSize(), size.getLocalVariableSize());
                    }

                    @Override
                    public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext, MethodDescription instrumentedMethod) {
                        return new ByteCodeAppender.Simple(
                                MethodVariableAccess.allArgumentsOf(instrumentedMethod).prependThisReference(),
                                MethodInvocation.invoke(bridgeTarget).special(superClass),
                                MethodReturn.returning(instrumentedMethod.getReturnType().asErasure())
                        ).apply(methodVisitor, implementationContext, instrumentedMethod);
                    }

                    @Override
                    public boolean equals(Object other) {
                        if (this == other) return true;
                        if (other == null || getClass() != other.getClass()) return false;
                        OfVisibilityBridge that = (OfVisibilityBridge) other;
                        return visibilityBridge.equals(that.visibilityBridge)
                                && bridgeTarget.equals(that.bridgeTarget)
                                && superClass.equals(that.superClass)
                                && attributeAppender.equals(that.attributeAppender);
                    }

                    @Override
                    public int hashCode() {
                        int result = visibilityBridge.hashCode();
                        result = 31 * result + bridgeTarget.hashCode();
                        result = 31 * result + superClass.hashCode();
                        result = 31 * result + attributeAppender.hashCode();
                        return result;
                    }

                    @Override
                    public String toString() {
                        return "TypeWriter.MethodPool.Record.ForDefinedMethod.OfVisibilityBridge{" +
                                "visibilityBridge=" + visibilityBridge +
                                ", bridgeTarget=" + bridgeTarget +
                                ", superClass=" + superClass +
                                ", attributeAppender=" + attributeAppender +
                                '}';
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
                        public Object getDefaultValue() {
                            return MethodDescription.NO_DEFAULT_VALUE;
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
                    return bridgeTypes.isEmpty() || (instrumentedType.isInterface() && !delegate.getSort().isImplemented())
                            ? delegate
                            : new AccessBridgeWrapper(delegate, instrumentedType, bridgeTarget, bridgeTypes, attributeAppender);
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
                                MethodReturn.returning(bridgeMethod.getReturnType().asErasure())
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
                    public TypeDescription.Generic getReturnType() {
                        return bridgeType.getReturnType().asGenericType();
                    }

                    @Override
                    public TypeList.Generic getExceptionTypes() {
                        return bridgeTarget.getExceptionTypes().accept(TypeDescription.Generic.Visitor.TypeErasing.INSTANCE);
                    }

                    @Override
                    public Object getDefaultValue() {
                        return MethodDescription.NO_DEFAULT_VALUE;
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
                    public Object getDefaultValue() {
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
                dumpFolder = System.getProperty(DUMP_PROPERTY);
            } catch (RuntimeException exception) {
                dumpFolder = null;
                Logger.getLogger("net.bytebuddy").warning("Could not enable dumping of class files: " + exception.getMessage());
            }
            DUMP_FOLDER = dumpFolder;
        }

        /**
         * The instrumented type to be created.
         */
        protected final TypeDescription instrumentedType;

        /**
         * The field pool to use.
         */
        protected final FieldPool fieldPool;

        /**
         * The method pool to use.
         */
        protected final MethodPool methodPool;

        /**
         * The explicit auxiliary types to add to the created type.
         */
        protected final List<DynamicType> explicitAuxiliaryTypes;

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
         * The class file version to use when no explicit class file version is applied.
         */
        protected final ClassFileVersion classFileVersion;

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
         * Creates a new default type writer.
         *
         * @param instrumentedType             The instrumented type to be created.
         * @param fieldPool                    The field pool to use.
         * @param methodPool                   The method pool to use.
         * @param explicitAuxiliaryTypes       The explicit auxiliary types to add to the created type.
         * @param instrumentedMethods          The instrumented methods relevant to this type creation.
         * @param loadedTypeInitializer        The loaded type initializer to apply onto the created type after loading.
         * @param typeInitializer              The type initializer to include in the created type's type initializer.
         * @param typeAttributeAppender        The type attribute appender to apply onto the instrumented type.
         * @param asmVisitorWrapper            The ASM visitor wrapper to apply onto the class writer.
         * @param classFileVersion             The class file version to use when no explicit class file version is applied.
         * @param annotationValueFilterFactory The annotation value filter factory to apply.
         * @param annotationRetention          The annotation retention to apply.
         * @param auxiliaryTypeNamingStrategy  The naming strategy for auxiliary types to apply.
         * @param implementationContextFactory The implementation context factory to apply.
         * @param typeValidation               Determines if a type should be explicitly validated.
         */
        protected Default(TypeDescription instrumentedType,
                          FieldPool fieldPool,
                          MethodPool methodPool,
                          List<DynamicType> explicitAuxiliaryTypes,
                          MethodList<?> instrumentedMethods,
                          LoadedTypeInitializer loadedTypeInitializer,
                          TypeInitializer typeInitializer,
                          TypeAttributeAppender typeAttributeAppender,
                          AsmVisitorWrapper asmVisitorWrapper,
                          ClassFileVersion classFileVersion,
                          AnnotationValueFilter.Factory annotationValueFilterFactory,
                          AnnotationRetention annotationRetention,
                          AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                          Implementation.Context.Factory implementationContextFactory,
                          TypeValidation typeValidation) {
            this.instrumentedType = instrumentedType;
            this.fieldPool = fieldPool;
            this.methodPool = methodPool;
            this.explicitAuxiliaryTypes = explicitAuxiliaryTypes;
            this.instrumentedMethods = instrumentedMethods;
            this.loadedTypeInitializer = loadedTypeInitializer;
            this.typeInitializer = typeInitializer;
            this.typeAttributeAppender = typeAttributeAppender;
            this.asmVisitorWrapper = asmVisitorWrapper;
            this.classFileVersion = classFileVersion;
            this.auxiliaryTypeNamingStrategy = auxiliaryTypeNamingStrategy;
            this.annotationValueFilterFactory = annotationValueFilterFactory;
            this.annotationRetention = annotationRetention;
            this.implementationContextFactory = implementationContextFactory;
            this.typeValidation = typeValidation;
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
                                                    TypeValidation typeValidation) {
            return new ForCreation<U>(methodRegistry.getInstrumentedType(),
                    fieldPool,
                    methodRegistry,
                    Collections.<DynamicType>emptyList(),
                    methodRegistry.getInstrumentedMethods(),
                    methodRegistry.getLoadedTypeInitializer(),
                    methodRegistry.getTypeInitializer(),
                    typeAttributeAppender,
                    asmVisitorWrapper,
                    classFileVersion,
                    annotationValueFilterFactory,
                    annotationRetention,
                    auxiliaryTypeNamingStrategy,
                    implementationContextFactory,
                    typeValidation);
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
         * @param originalType                 The original type that is being redefined or rebased.
         * @param classFileLocator             The class file locator for locating the original type's class file.
         * @param <U>                          A loaded type that the instrumented type guarantees to subclass.
         * @return A suitable type writer.
         */
        public static <U> TypeWriter<U> forRedefinition(MethodRegistry.Compiled methodRegistry,
                                                        FieldPool fieldPool,
                                                        TypeAttributeAppender typeAttributeAppender,
                                                        AsmVisitorWrapper asmVisitorWrapper,
                                                        ClassFileVersion classFileVersion,
                                                        AnnotationValueFilter.Factory annotationValueFilterFactory,
                                                        AnnotationRetention annotationRetention,
                                                        AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                                                        Implementation.Context.Factory implementationContextFactory,
                                                        TypeValidation typeValidation,
                                                        TypeDescription originalType,
                                                        ClassFileLocator classFileLocator) {
            return new ForInlining<U>(methodRegistry.getInstrumentedType(),
                    fieldPool,
                    methodRegistry,
                    Collections.<DynamicType>emptyList(),
                    methodRegistry.getInstrumentedMethods(),
                    methodRegistry.getLoadedTypeInitializer(),
                    methodRegistry.getTypeInitializer(),
                    typeAttributeAppender,
                    asmVisitorWrapper,
                    classFileVersion,
                    annotationValueFilterFactory,
                    annotationRetention,
                    auxiliaryTypeNamingStrategy,
                    implementationContextFactory,
                    typeValidation,
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
         * @param originalType                 The original type that is being redefined or rebased.
         * @param classFileLocator             The class file locator for locating the original type's class file.
         * @param methodRebaseResolver         The method rebase resolver to use for rebasing names.
         * @param <U>                          A loaded type that the instrumented type guarantees to subclass.
         * @return A suitable type writer.
         */
        public static <U> TypeWriter<U> forRebasing(MethodRegistry.Compiled methodRegistry,
                                                    FieldPool fieldPool,
                                                    TypeAttributeAppender typeAttributeAppender,
                                                    AsmVisitorWrapper asmVisitorWrapper,
                                                    ClassFileVersion classFileVersion,
                                                    AnnotationValueFilter.Factory annotationValueFilterFactory,
                                                    AnnotationRetention annotationRetention,
                                                    AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                                                    Implementation.Context.Factory implementationContextFactory,
                                                    TypeValidation typeValidation,
                                                    TypeDescription originalType,
                                                    ClassFileLocator classFileLocator,
                                                    MethodRebaseResolver methodRebaseResolver) {
            return new ForInlining<U>(methodRegistry.getInstrumentedType(),
                    fieldPool,
                    methodRegistry,
                    methodRebaseResolver.getAuxiliaryTypes(),
                    methodRegistry.getInstrumentedMethods(),
                    methodRegistry.getLoadedTypeInitializer(),
                    methodRegistry.getTypeInitializer(),
                    typeAttributeAppender,
                    asmVisitorWrapper,
                    classFileVersion,
                    annotationValueFilterFactory,
                    annotationRetention,
                    auxiliaryTypeNamingStrategy,
                    implementationContextFactory,
                    typeValidation,
                    originalType,
                    classFileLocator,
                    methodRebaseResolver);
        }

        @Override
        public DynamicType.Unloaded<S> make() {
            Implementation.Context.ExtractableView implementationContext = implementationContextFactory.make(instrumentedType,
                    auxiliaryTypeNamingStrategy,
                    typeInitializer,
                    classFileVersion);
            byte[] binaryRepresentation = create(implementationContext);
            if (DUMP_FOLDER != null) {
                try {
                    OutputStream outputStream = new FileOutputStream(new File(DUMP_FOLDER, instrumentedType.getName() + "." + System.currentTimeMillis()));
                    try {
                        outputStream.write(binaryRepresentation);
                    } finally {
                        outputStream.close();
                    }
                } catch (Exception exception) {
                    Logger.getLogger("net.bytebuddy").warning("Could not dump class file for " + instrumentedType + ": " + exception.getMessage());
                }
            }
            return new DynamicType.Default.Unloaded<S>(instrumentedType,
                    binaryRepresentation,
                    loadedTypeInitializer,
                    CompoundList.of(explicitAuxiliaryTypes, implementationContext.getRegisteredAuxiliaryTypes()));
        }

        /**
         * Creates the instrumented type.
         *
         * @param implementationContext The implementation context to use.
         * @return A byte array that is represented by the instrumented type.
         */
        protected abstract byte[] create(Implementation.Context.ExtractableView implementationContext);

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            Default<?> aDefault = (Default<?>) other;
            return instrumentedType.equals(aDefault.instrumentedType)
                    && fieldPool.equals(aDefault.fieldPool)
                    && methodPool.equals(aDefault.methodPool)
                    && explicitAuxiliaryTypes.equals(aDefault.explicitAuxiliaryTypes)
                    && instrumentedMethods.equals(aDefault.instrumentedMethods)
                    && loadedTypeInitializer.equals(aDefault.loadedTypeInitializer)
                    && typeInitializer.equals(aDefault.typeInitializer)
                    && typeAttributeAppender.equals(aDefault.typeAttributeAppender)
                    && asmVisitorWrapper.equals(aDefault.asmVisitorWrapper)
                    && classFileVersion.equals(aDefault.classFileVersion)
                    && annotationValueFilterFactory.equals(aDefault.annotationValueFilterFactory)
                    && annotationRetention == aDefault.annotationRetention
                    && auxiliaryTypeNamingStrategy.equals(aDefault.auxiliaryTypeNamingStrategy)
                    && implementationContextFactory.equals(aDefault.implementationContextFactory)
                    && typeValidation.equals(aDefault.typeValidation);
        }

        @Override
        public int hashCode() {
            int result = instrumentedType.hashCode();
            result = 31 * result + fieldPool.hashCode();
            result = 31 * result + methodPool.hashCode();
            result = 31 * result + explicitAuxiliaryTypes.hashCode();
            result = 31 * result + instrumentedMethods.hashCode();
            result = 31 * result + loadedTypeInitializer.hashCode();
            result = 31 * result + typeInitializer.hashCode();
            result = 31 * result + typeAttributeAppender.hashCode();
            result = 31 * result + asmVisitorWrapper.hashCode();
            result = 31 * result + classFileVersion.hashCode();
            result = 31 * result + annotationValueFilterFactory.hashCode();
            result = 31 * result + annotationRetention.hashCode();
            result = 31 * result + auxiliaryTypeNamingStrategy.hashCode();
            result = 31 * result + implementationContextFactory.hashCode();
            result = 31 * result + typeValidation.hashCode();
            return result;
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
                        (modifiers & Opcodes.ACC_STATIC) != 0,
                        !descriptor.startsWith(NO_PARAMETERS) || descriptor.endsWith(RETURNS_VOID),
                        name.equals(MethodDescription.CONSTRUCTOR_INTERNAL_NAME)
                                || name.equals(MethodDescription.TYPE_INITIALIZER_INTERNAL_NAME)
                                || (modifiers & Opcodes.ACC_PRIVATE) != 0,
                        signature != null);
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
            protected interface Constraint {

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
                 * @param isStatic                   {@code true} if this method is static.
                 * @param isDefaultValueIncompatible {@code true} if a method's signature cannot describe an annotation property method.
                 * @param isNonStaticNonVirtual      {@code true} if the method is non-virtual and non-static, i.e. a constructor, type initializer or private.
                 * @param isGeneric                  {@code true} if this method defines a generic signature.
                 */
                void assertMethod(String name,
                                  boolean isAbstract,
                                  boolean isPublic,
                                  boolean isStatic,
                                  boolean isDefaultValueIncompatible,
                                  boolean isNonStaticNonVirtual,
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
                 * Asserts if the type can legally represent a package description.
                 *
                 * @param modifier          The modifier that is to be written to the type.
                 * @param definesInterfaces {@code true} if this type implements at least one interface.
                 * @param isGeneric         {@code true} if this type defines a generic type signature.
                 */
                void assertType(int modifier, boolean definesInterfaces, boolean isGeneric);

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
                    public void assertField(String name, boolean isPublic, boolean isStatic, boolean isGeneric) {
                        /* do nothing */
                    }

                    @Override
                    public void assertMethod(String name,
                                             boolean isAbstract,
                                             boolean isPublic,
                                             boolean isStatic,
                                             boolean isDefaultValueIncompatible,
                                             boolean isNonStaticNonVirtual,
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
                    public String toString() {
                        return "TypeWriter.Default.ValidatingClassVisitor.Constraint.ForClass." + name();
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
                                             boolean isStatic,
                                             boolean isDefaultValueIncompatible,
                                             boolean isNonStaticNonVirtual,
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
                    public String toString() {
                        return "TypeWriter.Default.ValidatingClassVisitor.Constraint.ForPackageType." + name();
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
                                             boolean isStatic,
                                             boolean isDefaultValueIncompatible,
                                             boolean isNonStaticNonVirtual,
                                             boolean isGeneric) {
                        if (!name.equals(MethodDescription.TYPE_INITIALIZER_INTERNAL_NAME)) {
                            if (!isPublic || isNonStaticNonVirtual) {
                                throw new IllegalStateException("Cannot define non-public or non-virtual method '" + name + "' for interface type");
                            } else if (classic && isStatic) {
                                throw new IllegalStateException("Cannot define static method '" + name + "' for a pre-Java 8 interface type");
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
                    public String toString() {
                        return "TypeWriter.Default.ValidatingClassVisitor.Constraint.ForInterface." + name();
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
                                             boolean isStatic,
                                             boolean isDefaultValueIncompatible,
                                             boolean isNonStaticNonVirtual,
                                             boolean isGeneric) {
                        if (!name.equals(MethodDescription.TYPE_INITIALIZER_INTERNAL_NAME)) {
                            if (!isPublic || isNonStaticNonVirtual) {
                                throw new IllegalStateException("Cannot define non-public, non-abstract or non-virtual method '" + name + "' for annotation type");
                            } else if (classic && isStatic) {
                                throw new IllegalStateException("Cannot define static method '" + name + "' for a pre-Java 8 annotation type");
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
                    public String toString() {
                        return "TypeWriter.Default.ValidatingClassVisitor.Constraint.ForAnnotation." + name();
                    }
                }

                /**
                 * Represents the constraint implied by a class file version.
                 */
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
                                             boolean isStatic,
                                             boolean isDefaultValueIncompatible,
                                             boolean isNonStaticNonVirtual,
                                             boolean isGeneric) {
                        if (isGeneric && !classFileVersion.isAtLeast(ClassFileVersion.JAVA_V5)) {
                            throw new IllegalStateException("Cannot define generic method '" + name + "' for class file version " + classFileVersion);
                        } else if ((isStatic || isNonStaticNonVirtual) && isAbstract) {
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

                    @Override
                    public boolean equals(Object other) {
                        return this == other || !(other == null || getClass() != other.getClass())
                                && classFileVersion.equals(((ForClassFileVersion) other).classFileVersion);
                    }

                    @Override
                    public int hashCode() {
                        return classFileVersion.hashCode();
                    }

                    @Override
                    public String toString() {
                        return "TypeWriter.Default.ValidatingClassVisitor.Constraint.ForClassFileVersion{" +
                                "classFileVersion=" + classFileVersion +
                                '}';
                    }
                }

                /**
                 * A constraint implementation that summarizes several constraints.
                 */
                class Compound implements Constraint {

                    /**
                     * A list of constraints that is enforced in the given order.
                     */
                    private final List<? extends Constraint> constraints;

                    /**
                     * Creates a new compound constraint.
                     *
                     * @param constraints A list of constraints that is enforced in the given order.
                     */
                    public Compound(List<? extends Constraint> constraints) {
                        this.constraints = constraints;
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
                                             boolean isStatic,
                                             boolean isDefaultValueIncompatible,
                                             boolean isNonStaticNonVirtual,
                                             boolean isGeneric) {
                        for (Constraint constraint : constraints) {
                            constraint.assertMethod(name,
                                    isAbstract,
                                    isPublic,
                                    isStatic,
                                    isDefaultValueIncompatible,
                                    isNonStaticNonVirtual,
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
                    public void assertType(int modifier, boolean definesInterfaces, boolean isGeneric) {
                        for (Constraint constraint : constraints) {
                            constraint.assertType(modifier, definesInterfaces, isGeneric);
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
                    public boolean equals(Object other) {
                        return this == other || !(other == null || getClass() != other.getClass())
                                && constraints.equals(((Compound) other).constraints);
                    }

                    @Override
                    public int hashCode() {
                        return constraints.hashCode();
                    }

                    @Override
                    public String toString() {
                        return "TypeWriter.Default.ValidatingClassVisitor.Constraint.Compound{" +
                                "constraints=" + constraints +
                                '}';
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

                @Override
                public String toString() {
                    return "TypeWriter.Default.ValidatingClassVisitor.ValidatingFieldVisitor{" +
                            "classVisitor=" + ValidatingClassVisitor.this +
                            '}';
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
         * A class writer that piggy-backs on Byte Buddy's {@link ClassFileLocator} to avoid class loading or look-up errors when redefining a class.
         * This is not available when creating a new class where automatic frame computation is however not normally a requirement.
         */
        protected static class FrameComputingClassWriter extends ClassWriter {

            /**
             * The type pool to query.
             */
            private final TypePool typePool;

            /**
             * Creates a new frame computing class writer.
             *
             * @param classReader The class reader from which the original class is read.
             * @param flags       The flags to be handed to the writer.
             * @param typePool    The type pool to use.
             */
            protected FrameComputingClassWriter(ClassReader classReader, int flags, TypePool typePool) {
                super(classReader, flags);
                this.typePool = typePool;
            }

            /**
             * @param classReader      The class reader from which the original class is read.
             * @param flags            The flags to be handed to the writer.
             * @param classFileLocator The class file locator to use.
             * @return An appropriate class writer.
             */
            protected static ClassWriter of(ClassReader classReader, int flags, ClassFileLocator classFileLocator) {
                return (flags & ClassWriter.COMPUTE_FRAMES) != 0
                        ? new FrameComputingClassWriter(classReader, flags, TypePool.Default.of(classFileLocator))
                        : new ClassWriter(classReader, flags);
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

            @Override
            public String toString() {
                return "TypeWriter.Default.FrameComputingClassWriter{" +
                        "typePool=" + typePool +
                        '}';
            }
        }

        /**
         * A type writer that inlines the created type into an existing class file.
         *
         * @param <U> The best known loaded type for the dynamically created type.
         */
        public static class ForInlining<U> extends Default<U> {

            /**
             * Indicates that a method should be ignored.
             */
            private static final MethodVisitor IGNORE_METHOD = null;

            /**
             * Indicates that an annotation should be ignored.
             */
            private static final AnnotationVisitor IGNORE_ANNOTATION = null;

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
             * @param fieldPool                    The field pool to use.
             * @param methodPool                   The method pool to use.
             * @param explicitAuxiliaryTypes       The explicit auxiliary types to add to the created type.
             * @param instrumentedMethods          The instrumented methods relevant to this type creation.
             * @param loadedTypeInitializer        The loaded type initializer to apply onto the created type after loading.
             * @param typeInitializer              The type initializer to include in the created type's type initializer.
             * @param typeAttributeAppender        The type attribute appender to apply onto the instrumented type.
             * @param asmVisitorWrapper            The ASM visitor wrapper to apply onto the class writer.
             * @param classFileVersion             The class file version to define auxiliary types in.
             * @param annotationValueFilterFactory The annotation value filter factory to apply.
             * @param annotationRetention          The annotation retention to apply.
             * @param auxiliaryTypeNamingStrategy  The naming strategy for auxiliary types to apply.
             * @param implementationContextFactory The implementation context factory to apply.
             * @param typeValidation               Determines if a type should be explicitly validated.
             * @param originalType                 The original type that is being redefined or rebased.
             * @param classFileLocator             The class file locator for locating the original type's class file.
             * @param methodRebaseResolver         The method rebase resolver to use for rebasing methods.
             */
            protected ForInlining(TypeDescription instrumentedType,
                                  FieldPool fieldPool,
                                  MethodPool methodPool,
                                  List<DynamicType> explicitAuxiliaryTypes,
                                  MethodList<?> instrumentedMethods,
                                  LoadedTypeInitializer loadedTypeInitializer,
                                  TypeInitializer typeInitializer,
                                  TypeAttributeAppender typeAttributeAppender,
                                  AsmVisitorWrapper asmVisitorWrapper,
                                  ClassFileVersion classFileVersion,
                                  AnnotationValueFilter.Factory annotationValueFilterFactory,
                                  AnnotationRetention annotationRetention,
                                  AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                                  Implementation.Context.Factory implementationContextFactory,
                                  TypeValidation typeValidation,
                                  TypeDescription originalType,
                                  ClassFileLocator classFileLocator,
                                  MethodRebaseResolver methodRebaseResolver) {
                super(instrumentedType,
                        fieldPool,
                        methodPool,
                        explicitAuxiliaryTypes,
                        instrumentedMethods,
                        loadedTypeInitializer,
                        typeInitializer,
                        typeAttributeAppender,
                        asmVisitorWrapper,
                        classFileVersion,
                        annotationValueFilterFactory,
                        annotationRetention,
                        auxiliaryTypeNamingStrategy,
                        implementationContextFactory,
                        typeValidation);
                this.originalType = originalType;
                this.classFileLocator = classFileLocator;
                this.methodRebaseResolver = methodRebaseResolver;
            }

            @Override
            public byte[] create(Implementation.Context.ExtractableView implementationContext) {
                try {
                    ClassFileLocator.Resolution resolution = classFileLocator.locate(originalType.getName());
                    if (!resolution.isResolved()) {
                        throw new IllegalArgumentException("Cannot locate the class file for " + originalType + " using " + classFileLocator);
                    }
                    return doCreate(implementationContext, resolution.resolve());
                } catch (IOException exception) {
                    throw new RuntimeException("The class file could not be written", exception);
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
                int writerFlags = asmVisitorWrapper.mergeWriter(AsmVisitorWrapper.NO_FLAGS), readerFlags = asmVisitorWrapper.mergeReader(AsmVisitorWrapper.NO_FLAGS);
                ClassReader classReader = new ClassReader(binaryRepresentation);
                ClassWriter classWriter = FrameComputingClassWriter.of(classReader, writerFlags, classFileLocator);
                classReader.accept(writeTo(asmVisitorWrapper.wrap(instrumentedType,
                        ValidatingClassVisitor.of(classWriter, typeValidation),
                        writerFlags,
                        readerFlags), implementationContext), readerFlags);
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
                return originalType.getName().equals(instrumentedType.getName())
                        ? new RedefinitionClassVisitor(classVisitor, implementationContext)
                        : new ClassRemapper(new RedefinitionClassVisitor(classVisitor, implementationContext), new SimpleRemapper(originalType.getInternalName(), instrumentedType.getInternalName()));
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) return true;
                if (other == null || getClass() != other.getClass()) return false;
                if (!super.equals(other)) return false;
                ForInlining<?> that = (ForInlining<?>) other;
                return originalType.equals(that.originalType)
                        && classFileLocator.equals(that.classFileLocator)
                        && methodRebaseResolver.equals(that.methodRebaseResolver);
            }

            @Override
            public int hashCode() {
                int result = super.hashCode();
                result = 31 * result + originalType.hashCode();
                result = 31 * result + classFileLocator.hashCode();
                result = 31 * result + methodRebaseResolver.hashCode();
                return result;
            }

            @Override
            public String toString() {
                return "TypeWriter.Default.ForInlining{" +
                        "instrumentedType=" + instrumentedType +
                        ", fieldPool=" + fieldPool +
                        ", methodPool=" + methodPool +
                        ", explicitAuxiliaryTypes=" + explicitAuxiliaryTypes +
                        ", instrumentedMethods=" + instrumentedMethods +
                        ", loadedTypeInitializer=" + loadedTypeInitializer +
                        ", typeInitializer=" + typeInitializer +
                        ", typeAttributeAppender=" + typeAttributeAppender +
                        ", asmVisitorWrapper=" + asmVisitorWrapper +
                        ", classFileVersion=" + classFileVersion +
                        ", annotationValueFilterFactory=" + annotationValueFilterFactory +
                        ", annotationRetention=" + annotationRetention +
                        ", auxiliaryTypeNamingStrategy=" + auxiliaryTypeNamingStrategy +
                        ", implementationContextFactory=" + implementationContextFactory +
                        ", typeValidation=" + typeValidation +
                        ", originalType=" + originalType +
                        ", classFileLocator=" + classFileLocator +
                        ", methodRebaseResolver=" + methodRebaseResolver +
                        '}';
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
                    return new ParameterList.Empty<ParameterDescription.InDefinedShape>();
                }

                @Override
                public TypeDescription.Generic getReturnType() {
                    return TypeDescription.Generic.VOID;
                }

                @Override
                public TypeList.Generic getExceptionTypes() {
                    return new TypeList.Generic.Empty();
                }

                @Override
                public Object getDefaultValue() {
                    return MethodDescription.NO_DEFAULT_VALUE;
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
                    return Opcodes.ACC_SYNTHETIC | Opcodes.ACC_STATIC | (instrumentedType.isInterface()
                            ? Opcodes.ACC_PUBLIC
                            : Opcodes.ACC_PRIVATE);
                }

                @Override
                public String getInternalName() {
                    return String.format("%s$%s", TYPE_INITIALIZER_PROXY_PREFIX, suffix);
                }
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
                    super(Opcodes.ASM5, classVisitor);
                    this.implementationContext = implementationContext;
                    List<? extends FieldDescription> fieldDescriptions = instrumentedType.getDeclaredFields();
                    declaredFields = new HashMap<String, FieldDescription>();
                    for (FieldDescription fieldDescription : fieldDescriptions) {
                        declaredFields.put(fieldDescription.getName(), fieldDescription);
                    }
                    declarableMethods = new HashMap<String, MethodDescription>();
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
                                  String superClassInternalName,
                                  String[] interfaceTypeInternalName) {
                    super.visit(classFileVersionNumber,
                            instrumentedType.getActualModifiers((modifiers & Opcodes.ACC_SUPER) != 0 && !instrumentedType.isInterface()),
                            instrumentedType.getInternalName(),
                            instrumentedType.getGenericSignature(),
                            (instrumentedType.getSuperClass() == null ?
                                    TypeDescription.OBJECT :
                                    instrumentedType.getSuperClass().asErasure()).getInternalName(),
                            instrumentedType.getInterfaces().asErasures().toInternalNames());
                    implementationContext.setClassFileVersion(ClassFileVersion.ofMinorMajor(classFileVersionNumber));
                    typeAttributeAppender.apply(cv, instrumentedType, annotationValueFilterFactory.on(instrumentedType));
                    if (!ClassFileVersion.ofMinorMajor(classFileVersionNumber).isAtLeast(ClassFileVersion.JAVA_V8) && instrumentedType.isInterface()) {
                        implementationContext.prohibitTypeInitializer();
                    }
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
                    FieldDescription fieldDescription = declaredFields.remove(internalName);
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
                                                 String[] exceptionTypeInternalName) {
                    if (internalName.equals(MethodDescription.TYPE_INITIALIZER_INTERNAL_NAME)) {
                        if (implementationContext.isRetainTypeInitializer()) {
                            return super.visitMethod(modifiers, internalName, descriptor, genericSignature, exceptionTypeInternalName);
                        } else {
                            TypeInitializerInjection injectedCode = new TypeInitializerInjection(new TypeInitializerDelegate(instrumentedType, RandomString.make()));
                            this.injectedCode = injectedCode;
                            return super.visitMethod(injectedCode.getInjectorProxyMethod().getActualModifiers(),
                                    injectedCode.getInjectorProxyMethod().getInternalName(),
                                    injectedCode.getInjectorProxyMethod().getDescriptor(),
                                    injectedCode.getInjectorProxyMethod().getGenericSignature(),
                                    injectedCode.getInjectorProxyMethod().getExceptionTypes().asErasures().toInternalNames());
                        }
                    }
                    MethodDescription methodDescription = declarableMethods.remove(internalName + descriptor);
                    return methodDescription == null
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
                        return super.visitMethod(methodDescription.getActualModifiers(),
                                methodDescription.getInternalName(),
                                methodDescription.getDescriptor(),
                                methodDescription.getGenericSignature(),
                                methodDescription.getExceptionTypes().asErasures().toInternalNames());
                    }
                    MethodDescription implementedMethod = record.getMethod();
                    MethodVisitor methodVisitor = super.visitMethod(implementedMethod.getActualModifiers(record.getSort().isImplemented()),
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
                    for (FieldDescription fieldDescription : declaredFields.values()) {
                        fieldPool.target(fieldDescription).apply(cv, annotationValueFilterFactory);
                    }
                    for (MethodDescription methodDescription : declarableMethods.values()) {
                        methodPool.target(methodDescription).apply(cv, implementationContext, annotationValueFilterFactory);
                    }
                    implementationContext.drain(cv, methodPool, injectedCode, annotationValueFilterFactory);
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

                    @Override
                    public String toString() {
                        return "TypeWriter.Default.ForInlining.RedefinitionClassVisitor.AttributeObtainingFieldVisitor{" +
                                "classVisitor=" + TypeWriter.Default.ForInlining.RedefinitionClassVisitor.this +
                                ", record=" + record +
                                '}';
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

                    @Override
                    public String toString() {
                        return "TypeWriter.Default.ForInlining.RedefinitionClassVisitor.CodePreservingMethodVisitor{" +
                                "classVisitor=" + TypeWriter.Default.ForInlining.RedefinitionClassVisitor.this +
                                ", actualMethodVisitor=" + actualMethodVisitor +
                                ", record=" + record +
                                ", resolution=" + resolution +
                                '}';
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

                    @Override
                    public String toString() {
                        return "TypeWriter.Default.ForInlining.RedefinitionClassVisitor.AttributeObtainingMethodVisitor{" +
                                "classVisitor=" + TypeWriter.Default.ForInlining.RedefinitionClassVisitor.this +
                                ", actualMethodVisitor=" + actualMethodVisitor +
                                ", record=" + record +
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
                     * @param injectorProxyMethod The method to which the original type initializer code is to be written to.
                     */
                    protected TypeInitializerInjection(MethodDescription injectorProxyMethod) {
                        this.injectorProxyMethod = injectorProxyMethod;
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
        }

        /**
         * A type writer that creates a class file that is not based upon another, existing class.
         *
         * @param <U> The best known loaded type for the dynamically created type.
         */
        public static class ForCreation<U> extends Default<U> {

            /**
             * Creates a new default type writer for creating a new type that is not based on an existing class file.
             *
             * @param instrumentedType             The instrumented type to be created.
             * @param fieldPool                    The field pool to use.
             * @param methodPool                   The method pool to use.
             * @param explicitAuxiliaryTypes       The explicit auxiliary types to add to the created type.
             * @param instrumentedMethods          The instrumented methods relevant to this type creation.
             * @param loadedTypeInitializer        The loaded type initializer to apply onto the created type after loading.
             * @param typeInitializer              The type initializer to include in the created type's type initializer.
             * @param typeAttributeAppender        The type attribute appender to apply onto the instrumented type.
             * @param asmVisitorWrapper            The ASM visitor wrapper to apply onto the class writer.
             * @param classFileVersion             The class file version to write the instrumented type in and to apply when creating auxiliary types.
             * @param annotationValueFilterFactory The annotation value filter factory to apply.
             * @param annotationRetention          The annotation retention to apply.
             * @param auxiliaryTypeNamingStrategy  The naming strategy for auxiliary types to apply.
             * @param implementationContextFactory The implementation context factory to apply.
             * @param typeValidation               Determines if a type should be explicitly validated.
             */
            protected ForCreation(TypeDescription instrumentedType,
                                  FieldPool fieldPool,
                                  MethodPool methodPool,
                                  List<DynamicType> explicitAuxiliaryTypes,
                                  MethodList<?> instrumentedMethods,
                                  LoadedTypeInitializer loadedTypeInitializer,
                                  TypeInitializer typeInitializer,
                                  TypeAttributeAppender typeAttributeAppender,
                                  AsmVisitorWrapper asmVisitorWrapper,
                                  ClassFileVersion classFileVersion,
                                  AnnotationValueFilter.Factory annotationValueFilterFactory,
                                  AnnotationRetention annotationRetention,
                                  AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy,
                                  Implementation.Context.Factory implementationContextFactory,
                                  TypeValidation typeValidation) {
                super(instrumentedType,
                        fieldPool,
                        methodPool,
                        explicitAuxiliaryTypes,
                        instrumentedMethods,
                        loadedTypeInitializer,
                        typeInitializer,
                        typeAttributeAppender,
                        asmVisitorWrapper,
                        classFileVersion,
                        annotationValueFilterFactory,
                        annotationRetention,
                        auxiliaryTypeNamingStrategy,
                        implementationContextFactory,
                        typeValidation);
            }

            @Override
            public byte[] create(Implementation.Context.ExtractableView implementationContext) {
                int writerFlags = asmVisitorWrapper.mergeWriter(AsmVisitorWrapper.NO_FLAGS);
                ClassWriter classWriter = new ClassWriter(writerFlags);
                ClassVisitor classVisitor = asmVisitorWrapper.wrap(instrumentedType,
                        ValidatingClassVisitor.of(classWriter, typeValidation),
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
                implementationContext.setClassFileVersion(classFileVersion);
                typeAttributeAppender.apply(classVisitor, instrumentedType, annotationValueFilterFactory.on(instrumentedType));
                for (FieldDescription fieldDescription : instrumentedType.getDeclaredFields()) {
                    fieldPool.target(fieldDescription).apply(classVisitor, annotationValueFilterFactory);
                }
                for (MethodDescription methodDescription : instrumentedMethods) {
                    methodPool.target(methodDescription).apply(classVisitor, implementationContext, annotationValueFilterFactory);
                }
                implementationContext.drain(classVisitor, methodPool, Implementation.Context.ExtractableView.InjectedCode.None.INSTANCE, annotationValueFilterFactory);
                classVisitor.visitEnd();
                return classWriter.toByteArray();
            }

            @Override
            public String toString() {
                return "TypeWriter.Default.ForCreation{" +
                        "instrumentedType=" + instrumentedType +
                        ", fieldPool=" + fieldPool +
                        ", methodPool=" + methodPool +
                        ", explicitAuxiliaryTypes=" + explicitAuxiliaryTypes +
                        ", instrumentedMethods=" + instrumentedMethods +
                        ", loadedTypeInitializer=" + loadedTypeInitializer +
                        ", typeInitializer=" + typeInitializer +
                        ", typeAttributeAppender=" + typeAttributeAppender +
                        ", asmVisitorWrapper=" + asmVisitorWrapper +
                        ", classFileVersion=" + classFileVersion +
                        ", annotationValueFilterFactory=" + annotationValueFilterFactory +
                        ", annotationRetention=" + annotationRetention +
                        ", auxiliaryTypeNamingStrategy=" + auxiliaryTypeNamingStrategy +
                        ", implementationContextFactory=" + implementationContextFactory +
                        ", typeValidation=" + typeValidation +
                        '}';
            }
        }
    }
}
