/*
 * Copyright 2014 - 2020 Rafael Winterhalter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
import net.bytebuddy.description.type.*;
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
         * Looks up a handler entry for a given field.
         *
         * @param fieldDescription The field being processed.
         * @return A handler entry for the given field.
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
            @HashCodeAndEqualsPlugin.Enhance
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

                /**
                 * {@inheritDoc}
                 */
                public boolean isImplicit() {
                    return true;
                }

                /**
                 * {@inheritDoc}
                 */
                public FieldDescription getField() {
                    return fieldDescription;
                }

                /**
                 * {@inheritDoc}
                 */
                public FieldAttributeAppender getFieldAppender() {
                    throw new IllegalStateException("An implicit field record does not expose a field appender: " + this);
                }

                /**
                 * {@inheritDoc}
                 */
                public Object resolveDefault(Object defaultValue) {
                    throw new IllegalStateException("An implicit field record does not expose a default value: " + this);
                }

                /**
                 * {@inheritDoc}
                 */
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

                /**
                 * {@inheritDoc}
                 */
                public void apply(FieldVisitor fieldVisitor, AnnotationValueFilter.Factory annotationValueFilterFactory) {
                    throw new IllegalStateException("An implicit field record is not intended for partial application: " + this);
                }
            }

            /**
             * A record for a rich field with attributes and a potential default value.
             */
            @HashCodeAndEqualsPlugin.Enhance
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

                /**
                 * {@inheritDoc}
                 */
                public boolean isImplicit() {
                    return false;
                }

                /**
                 * {@inheritDoc}
                 */
                public FieldDescription getField() {
                    return fieldDescription;
                }

                /**
                 * {@inheritDoc}
                 */
                public FieldAttributeAppender getFieldAppender() {
                    return attributeAppender;
                }

                /**
                 * {@inheritDoc}
                 */
                public Object resolveDefault(Object defaultValue) {
                    return this.defaultValue == null
                            ? defaultValue
                            : this.defaultValue;
                }

                /**
                 * {@inheritDoc}
                 */
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

                /**
                 * {@inheritDoc}
                 */
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

            /**
             * {@inheritDoc}
             */
            public Record target(FieldDescription fieldDescription) {
                throw new IllegalStateException("Cannot look up field from disabled pool");
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

                /**
                 * {@inheritDoc}
                 */
                public void apply(ClassVisitor classVisitor, Implementation.Context implementationContext, AnnotationValueFilter.Factory annotationValueFilterFactory) {
                    /* do nothing */
                }

                /**
                 * {@inheritDoc}
                 */
                public void applyBody(MethodVisitor methodVisitor, Implementation.Context implementationContext, AnnotationValueFilter.Factory annotationValueFilterFactory) {
                    throw new IllegalStateException("Cannot apply body for non-implemented method on " + methodDescription);
                }

                /**
                 * {@inheritDoc}
                 */
                public void applyAttributes(MethodVisitor methodVisitor, AnnotationValueFilter.Factory annotationValueFilterFactory) {
                    /* do nothing */
                }

                /**
                 * {@inheritDoc}
                 */
                public ByteCodeAppender.Size applyCode(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
                    throw new IllegalStateException("Cannot apply code for non-implemented method on " + methodDescription);
                }

                /**
                 * {@inheritDoc}
                 */
                public void applyHead(MethodVisitor methodVisitor) {
                    throw new IllegalStateException("Cannot apply head for non-implemented method on " + methodDescription);
                }

                /**
                 * {@inheritDoc}
                 */
                public MethodDescription getMethod() {
                    return methodDescription;
                }

                /**
                 * {@inheritDoc}
                 */
                public Visibility getVisibility() {
                    return methodDescription.getVisibility();
                }

                /**
                 * {@inheritDoc}
                 */
                public Sort getSort() {
                    return Sort.SKIPPED;
                }

                /**
                 * {@inheritDoc}
                 */
                public Record prepend(ByteCodeAppender byteCodeAppender) {
                    return new ForDefinedMethod.WithBody(methodDescription, new ByteCodeAppender.Compound(byteCodeAppender,
                            new ByteCodeAppender.Simple(DefaultValue.of(methodDescription.getReturnType()), MethodReturn.of(methodDescription.getReturnType()))));
                }
            }

            /**
             * A base implementation of an abstract entry that defines a method.
             */
            abstract class ForDefinedMethod implements Record {

                /**
                 * {@inheritDoc}
                 */
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
                 * Describes an entry that defines a method as byte code.
                 */
                @HashCodeAndEqualsPlugin.Enhance
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

                    /**
                     * {@inheritDoc}
                     */
                    public MethodDescription getMethod() {
                        return methodDescription;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Sort getSort() {
                        return Sort.IMPLEMENTED;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Visibility getVisibility() {
                        return visibility;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void applyHead(MethodVisitor methodVisitor) {
                        /* do nothing */
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void applyBody(MethodVisitor methodVisitor, Implementation.Context implementationContext, AnnotationValueFilter.Factory annotationValueFilterFactory) {
                        applyAttributes(methodVisitor, annotationValueFilterFactory);
                        methodVisitor.visitCode();
                        ByteCodeAppender.Size size = applyCode(methodVisitor, implementationContext);
                        methodVisitor.visitMaxs(size.getOperandStackSize(), size.getLocalVariableSize());
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void applyAttributes(MethodVisitor methodVisitor, AnnotationValueFilter.Factory annotationValueFilterFactory) {
                        methodAttributeAppender.apply(methodVisitor, methodDescription, annotationValueFilterFactory.on(methodDescription));
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public ByteCodeAppender.Size applyCode(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
                        return byteCodeAppender.apply(methodVisitor, implementationContext, methodDescription);
                    }

                    /**
                     * {@inheritDoc}
                     */
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

                    /**
                     * {@inheritDoc}
                     */
                    public MethodDescription getMethod() {
                        return methodDescription;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Sort getSort() {
                        return Sort.DEFINED;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Visibility getVisibility() {
                        return visibility;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void applyHead(MethodVisitor methodVisitor) {
                        /* do nothing */
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void applyBody(MethodVisitor methodVisitor, Implementation.Context implementationContext, AnnotationValueFilter.Factory annotationValueFilterFactory) {
                        applyAttributes(methodVisitor, annotationValueFilterFactory);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void applyAttributes(MethodVisitor methodVisitor, AnnotationValueFilter.Factory annotationValueFilterFactory) {
                        methodAttributeAppender.apply(methodVisitor, methodDescription, annotationValueFilterFactory.on(methodDescription));
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public ByteCodeAppender.Size applyCode(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
                        throw new IllegalStateException("Cannot apply code for abstract method on " + methodDescription);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Record prepend(ByteCodeAppender byteCodeAppender) {
                        throw new IllegalStateException("Cannot prepend code for abstract method on " + methodDescription);
                    }
                }

                /**
                 * Describes an entry that defines a method with a default annotation value.
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

                    /**
                     * {@inheritDoc}
                     */
                    public MethodDescription getMethod() {
                        return methodDescription;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Sort getSort() {
                        return Sort.DEFINED;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Visibility getVisibility() {
                        return methodDescription.getVisibility();
                    }

                    /**
                     * {@inheritDoc}
                     */
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

                    /**
                     * {@inheritDoc}
                     */
                    public void applyBody(MethodVisitor methodVisitor, Implementation.Context implementationContext, AnnotationValueFilter.Factory annotationValueFilterFactory) {
                        methodAttributeAppender.apply(methodVisitor, methodDescription, annotationValueFilterFactory.on(methodDescription));
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void applyAttributes(MethodVisitor methodVisitor, AnnotationValueFilter.Factory annotationValueFilterFactory) {
                        throw new IllegalStateException("Cannot apply attributes for default value on " + methodDescription);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public ByteCodeAppender.Size applyCode(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
                        throw new IllegalStateException("Cannot apply code for default value on " + methodDescription);
                    }

                    /**
                     * {@inheritDoc}
                     */
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

                    /**
                     * {@inheritDoc}
                     */
                    public MethodDescription getMethod() {
                        return visibilityBridge;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Sort getSort() {
                        return Sort.IMPLEMENTED;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Visibility getVisibility() {
                        return bridgeTarget.getVisibility();
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Record prepend(ByteCodeAppender byteCodeAppender) {
                        return new ForDefinedMethod.WithBody(visibilityBridge,
                                new ByteCodeAppender.Compound(this, byteCodeAppender),
                                attributeAppender,
                                bridgeTarget.getVisibility());
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void applyHead(MethodVisitor methodVisitor) {
                        /* do nothing */
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void applyBody(MethodVisitor methodVisitor, Implementation.Context implementationContext, AnnotationValueFilter.Factory annotationValueFilterFactory) {
                        applyAttributes(methodVisitor, annotationValueFilterFactory);
                        methodVisitor.visitCode();
                        ByteCodeAppender.Size size = applyCode(methodVisitor, implementationContext);
                        methodVisitor.visitMaxs(size.getOperandStackSize(), size.getLocalVariableSize());
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void applyAttributes(MethodVisitor methodVisitor, AnnotationValueFilter.Factory annotationValueFilterFactory) {
                        attributeAppender.apply(methodVisitor, visibilityBridge, annotationValueFilterFactory.on(visibilityBridge));
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Size applyCode(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
                        return apply(methodVisitor, implementationContext, visibilityBridge);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext, MethodDescription instrumentedMethod) {
                        return new ByteCodeAppender.Simple(
                                MethodVariableAccess.allArgumentsOf(instrumentedMethod).prependThisReference(),
                                MethodInvocation.invoke(bridgeTarget).special(bridgeType),
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

                        /**
                         * {@inheritDoc}
                         */
                        public TypeDescription getDeclaringType() {
                            return instrumentedType;
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public ParameterList<ParameterDescription.InDefinedShape> getParameters() {
                            return new ParameterList.Explicit.ForTypes(this, bridgeTarget.getParameters().asTypeList().asRawTypes());
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public TypeDescription.Generic getReturnType() {
                            return bridgeTarget.getReturnType().asRawType();
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public TypeList.Generic getExceptionTypes() {
                            return bridgeTarget.getExceptionTypes().asRawTypes();
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public AnnotationValue<?, ?> getDefaultValue() {
                            return AnnotationValue.UNDEFINED;
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public TypeList.Generic getTypeVariables() {
                            return new TypeList.Generic.Empty();
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public AnnotationList getDeclaredAnnotations() {
                            return bridgeTarget.getDeclaredAnnotations();
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public int getModifiers() {
                            return (bridgeTarget.getModifiers() | Opcodes.ACC_SYNTHETIC | Opcodes.ACC_BRIDGE) & ~Opcodes.ACC_NATIVE;
                        }

                        /**
                         * {@inheritDoc}
                         */
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
            @HashCodeAndEqualsPlugin.Enhance
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

                /**
                 * {@inheritDoc}
                 */
                public Sort getSort() {
                    return delegate.getSort();
                }

                /**
                 * {@inheritDoc}
                 */
                public MethodDescription getMethod() {
                    return bridgeTarget;
                }

                /**
                 * {@inheritDoc}
                 */
                public Visibility getVisibility() {
                    return delegate.getVisibility();
                }

                /**
                 * {@inheritDoc}
                 */
                public Record prepend(ByteCodeAppender byteCodeAppender) {
                    return new AccessBridgeWrapper(delegate.prepend(byteCodeAppender), instrumentedType, bridgeTarget, bridgeTypes, attributeAppender);
                }

                /**
                 * {@inheritDoc}
                 */
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

                /**
                 * {@inheritDoc}
                 */
                public void applyHead(MethodVisitor methodVisitor) {
                    delegate.applyHead(methodVisitor);
                }

                /**
                 * {@inheritDoc}
                 */
                public void applyBody(MethodVisitor methodVisitor,
                                      Implementation.Context implementationContext,
                                      AnnotationValueFilter.Factory annotationValueFilterFactory) {
                    delegate.applyBody(methodVisitor, implementationContext, annotationValueFilterFactory);
                }

                /**
                 * {@inheritDoc}
                 */
                public void applyAttributes(MethodVisitor methodVisitor, AnnotationValueFilter.Factory annotationValueFilterFactory) {
                    delegate.applyAttributes(methodVisitor, annotationValueFilterFactory);
                }

                /**
                 * {@inheritDoc}
                 */
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

                    /**
                     * {@inheritDoc}
                     */
                    public TypeDescription getDeclaringType() {
                        return instrumentedType;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public ParameterList<ParameterDescription.InDefinedShape> getParameters() {
                        return new ParameterList.Explicit.ForTypes(this, bridgeType.getParameterTypes());
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public TypeDescription.Generic getReturnType() {
                        return bridgeType.getReturnType().asGenericType();
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public TypeList.Generic getExceptionTypes() {
                        return bridgeTarget.getExceptionTypes().accept(TypeDescription.Generic.Visitor.TypeErasing.INSTANCE);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public AnnotationValue<?, ?> getDefaultValue() {
                        return AnnotationValue.UNDEFINED;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public TypeList.Generic getTypeVariables() {
                        return new TypeList.Generic.Empty();
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public AnnotationList getDeclaredAnnotations() {
                        return new AnnotationList.Empty();
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public int getModifiers() {
                        return (bridgeTarget.getModifiers() | Opcodes.ACC_BRIDGE | Opcodes.ACC_SYNTHETIC) & ~(Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE);
                    }

                    /**
                     * {@inheritDoc}
                     */
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

                    /**
                     * {@inheritDoc}
                     */
                    public TypeDescription getDeclaringType() {
                        return instrumentedType;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public ParameterList<ParameterDescription.InDefinedShape> getParameters() {
                        return new ParameterList.ForTokens(this, bridgeTarget.getParameters().asTokenList(is(instrumentedType)));
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public TypeDescription.Generic getReturnType() {
                        return bridgeTarget.getReturnType();
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public TypeList.Generic getExceptionTypes() {
                        return bridgeTarget.getExceptionTypes();
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public AnnotationValue<?, ?> getDefaultValue() {
                        return bridgeTarget.getDefaultValue();
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public TypeList.Generic getTypeVariables() {
                        return bridgeTarget.getTypeVariables();
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public AnnotationList getDeclaredAnnotations() {
                        return bridgeTarget.getDeclaredAnnotations();
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public int getModifiers() {
                        return bridgeTarget.getModifiers();
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public String getInternalName() {
                        return bridgeTarget.getInternalName();
                    }
                }
            }
        }
    }

    /**
     * An record component pool that allows a lookup for how to implement a record component.
     */
    interface RecordComponentPool {

        /**
         * Looks up a handler entry for a given record component.
         *
         * @param recordComponentDescription The record component being processed.
         * @return A handler entry for the given record component.
         */
        Record target(RecordComponentDescription recordComponentDescription);

        /**
         * An entry of a record component pool that describes how a record component is implemented.
         *
         * @see RecordComponentPool
         */
        interface Record {

            /**
             * Determines if this record is implicit, i.e is not defined by a {@link RecordComponentPool}.
             *
             * @return {@code true} if this record is implicit.
             */
            boolean isImplicit();

            /**
             * Returns the record component that this record represents.
             *
             * @return The record component that this record represents.
             */
            RecordComponentDescription getRecordComponent();

            /**
             * Returns the record component attribute appender for a given record component.
             *
             * @return The record component appender to be applied on the given field.
             */
            RecordComponentAttributeAppender getRecordComponentAppender();

            /**
             * Writes this record to a given class visitor.
             *
             * @param classVisitor                 The class visitor to which this record is to be written to.
             * @param annotationValueFilterFactory The annotation value filter factory to apply when writing annotations.
             */
            void apply(ClassVisitor classVisitor, AnnotationValueFilter.Factory annotationValueFilterFactory);

            /**
             * Applies this record to a record component visitor. This is not possible for implicit records.
             *
             * @param recordComponentVisitor       The record component visitor onto which this record is to be applied.
             * @param annotationValueFilterFactory The annotation value filter factory to use for annotations.
             */
            void apply(RecordComponentVisitor recordComponentVisitor, AnnotationValueFilter.Factory annotationValueFilterFactory);

            /**
             * A record for a simple field without a default value where all of the record component's declared annotations are appended.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class ForImplicitRecordComponent implements Record {

                /**
                 * The implemented record component.
                 */
                private final RecordComponentDescription recordComponentDescription;

                /**
                 * Creates a new record for a simple record component.
                 *
                 * @param recordComponentDescription The described record component.
                 */
                public ForImplicitRecordComponent(RecordComponentDescription recordComponentDescription) {
                    this.recordComponentDescription = recordComponentDescription;
                }

                /**
                 * {@inheritDoc}
                 */
                public boolean isImplicit() {
                    return true;
                }

                /**
                 * {@inheritDoc}
                 */
                public RecordComponentDescription getRecordComponent() {
                    return recordComponentDescription;
                }

                /**
                 * {@inheritDoc}
                 */
                public RecordComponentAttributeAppender getRecordComponentAppender() {
                    throw new IllegalStateException("An implicit field record does not expose a field appender: " + this);
                }

                /**
                 * {@inheritDoc}
                 */
                public void apply(ClassVisitor classVisitor, AnnotationValueFilter.Factory annotationValueFilterFactory) {
                    RecordComponentVisitor recordComponentVisitor = classVisitor.visitRecordComponent(recordComponentDescription.getActualName(),
                            recordComponentDescription.getDescriptor(),
                            recordComponentDescription.getGenericSignature());
                    if (recordComponentVisitor != null) {
                        RecordComponentAttributeAppender.ForInstrumentedRecordComponent.INSTANCE.apply(recordComponentVisitor,
                                recordComponentDescription,
                                annotationValueFilterFactory.on(recordComponentDescription));
                        recordComponentVisitor.visitEnd();
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public void apply(RecordComponentVisitor recordComponentVisitor, AnnotationValueFilter.Factory annotationValueFilterFactory) {
                    throw new IllegalStateException("An implicit field record is not intended for partial application: " + this);
                }
            }

            /**
             * A record for a rich record component with attributes.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class ForExplicitRecordComponent implements Record {

                /**
                 * The attribute appender for the record component.
                 */
                private final RecordComponentAttributeAppender attributeAppender;

                /**
                 * The implemented record component.
                 */
                private final RecordComponentDescription recordComponentDescription;

                /**
                 * Creates a record for a rich record component.
                 *
                 * @param attributeAppender           The attribute appender for the record component.
                 * @param recordComponentDescription  The implemented record component.
                 */
                public ForExplicitRecordComponent(RecordComponentAttributeAppender attributeAppender, RecordComponentDescription recordComponentDescription) {
                    this.attributeAppender = attributeAppender;
                    this.recordComponentDescription = recordComponentDescription;
                }

                /**
                 * {@inheritDoc}
                 */
                public boolean isImplicit() {
                    return false;
                }

                /**
                 * {@inheritDoc}
                 */
                public RecordComponentDescription getRecordComponent() {
                    return recordComponentDescription;
                }

                /**
                 * {@inheritDoc}
                 */
                public RecordComponentAttributeAppender getRecordComponentAppender() {
                    return attributeAppender;
                }

                /**
                 * {@inheritDoc}
                 */
                public void apply(ClassVisitor classVisitor, AnnotationValueFilter.Factory annotationValueFilterFactory) {
                    RecordComponentVisitor recordComponentVisitor = classVisitor.visitRecordComponent(recordComponentDescription.getActualName(),
                            recordComponentDescription.getDescriptor(),
                            recordComponentDescription.getGenericSignature());
                    if (recordComponentVisitor != null) {
                        attributeAppender.apply(recordComponentVisitor, recordComponentDescription, annotationValueFilterFactory.on(recordComponentDescription));
                        recordComponentVisitor.visitEnd();
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public void apply(RecordComponentVisitor recordComponentVisitor, AnnotationValueFilter.Factory annotationValueFilterFactory) {
                    attributeAppender.apply(recordComponentVisitor, recordComponentDescription, annotationValueFilterFactory.on(recordComponentDescription));
                }
            }
        }

        /**
         * A record component pool that does not allow any look ups.
         */
        enum Disabled implements RecordComponentPool {

            /**
             * The singleton instance.
             */
            INSTANCE;

            /**
             * {@inheritDoc}
             */
            public Record target(RecordComponentDescription recordComponentDescription) {
                throw new IllegalStateException("Cannot look up record component from disabled pool");
            }
        }
    }

    /**
     * A default implementation of a {@link net.bytebuddy.dynamic.scaffold.TypeWriter}.
     *
     * @param <S> The best known loaded type for the dynamically created type.
     */
    @HashCodeAndEqualsPlugin.Enhance
    abstract class Default<S> implements TypeWriter<S> {

        /**
         * Indicates an empty reference in a class file which is expressed by {@code null}.
         */
        private static final String NO_REFERENCE = null;

        /**
         * A folder for dumping class files or {@code null} if no dump should be generated.
         */
        protected static final String DUMP_FOLDER;

        /*
         * Reads the dumping property that is set at program start up. This might cause an error because of security constraints.
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
         * The record component pool to use.
         */
        protected final RecordComponentPool recordComponentPool;

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
         * The instrumented type's record components.
         */
        protected final RecordComponentList<RecordComponentDescription.InDefinedShape> recordComponents;

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
         * The class writer strategy to use.
         */
        protected final ClassWriterStrategy classWriterStrategy;

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
         * @param recordComponentPool          The record component pool to use.
         * @param auxiliaryTypes               The explicit auxiliary types to add to the created type.
         * @param fields                       The instrumented type's declared fields.
         * @param methods                      The instrumented type's declared and virtually inherited methods.
         * @param instrumentedMethods          The instrumented methods relevant to this type creation.
         * @param recordComponents             The instrumented type's record components.
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
                          RecordComponentPool recordComponentPool,
                          List<? extends DynamicType> auxiliaryTypes,
                          FieldList<FieldDescription.InDefinedShape> fields,
                          MethodList<?> methods,
                          MethodList<?> instrumentedMethods,
                          RecordComponentList<RecordComponentDescription.InDefinedShape> recordComponents,
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
            this.recordComponentPool = recordComponentPool;
            this.auxiliaryTypes = auxiliaryTypes;
            this.fields = fields;
            this.methods = methods;
            this.instrumentedMethods = instrumentedMethods;
            this.recordComponents = recordComponents;
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
         * @param recordComponentPool          The record component pool to use.
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
                                                    RecordComponentPool recordComponentPool,
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
                    recordComponentPool,
                    auxiliaryTypes,
                    methodRegistry.getInstrumentedType().getDeclaredFields(),
                    methodRegistry.getMethods(),
                    methodRegistry.getInstrumentedMethods(),
                    methodRegistry.getInstrumentedType().getRecordComponents(),
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
         * @param recordComponentPool          The record component pool to use.
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
                                                        RecordComponentPool recordComponentPool,
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
                    recordComponentPool,
                    auxiliaryTypes,
                    methodRegistry.getInstrumentedType().getDeclaredFields(),
                    methodRegistry.getMethods(),
                    methodRegistry.getInstrumentedMethods(),
                    methodRegistry.getInstrumentedType().getRecordComponents(),
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
         * @param recordComponentPool          The record component pool to use.
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
         * @param methodRebaseResolver         The method rebase resolver to use for rebasing names.
         * @param <U>                          A loaded type that the instrumented type guarantees to subclass.
         * @return A suitable type writer.
         */
        public static <U> TypeWriter<U> forRebasing(MethodRegistry.Prepared methodRegistry,
                                                    List<? extends DynamicType> auxiliaryTypes,
                                                    FieldPool fieldPool,
                                                    RecordComponentPool recordComponentPool,
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
                    recordComponentPool,
                    CompoundList.of(auxiliaryTypes, methodRebaseResolver.getAuxiliaryTypes()),
                    methodRegistry.getInstrumentedType().getDeclaredFields(),
                    methodRegistry.getMethods(),
                    methodRegistry.getInstrumentedMethods(),
                    methodRegistry.getInstrumentedType().getRecordComponents(),
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
         * Creates a type writer for decorating a type.
         *
         * @param instrumentedType             The instrumented type.
         * @param classFileVersion             The class file version to use when no explicit class file version is applied.
         * @param auxiliaryTypes               A list of explicitly required auxiliary types.
         * @param methods                      The methods to instrument.
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

        /**
         * {@inheritDoc}
         */
        @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION", justification = "Setting a debugging property should never change the program outcome")
        public DynamicType.Unloaded<S> make(TypeResolutionStrategy.Resolved typeResolutionStrategy) {
            ClassDumpAction.Dispatcher dispatcher = DUMP_FOLDER == null
                    ? ClassDumpAction.Dispatcher.Disabled.INSTANCE
                    : new ClassDumpAction.Dispatcher.Enabled(DUMP_FOLDER, System.currentTimeMillis());
            UnresolvedType unresolvedType = create(typeResolutionStrategy.injectedInto(typeInitializer), dispatcher);
            dispatcher.dump(instrumentedType, false, unresolvedType.getBinaryRepresentation());
            return unresolvedType.toDynamicType(typeResolutionStrategy);
        }

        /**
         * Creates an unresolved version of the dynamic type.
         *
         * @param typeInitializer The type initializer to use.
         * @param dispatcher      A dispatcher for dumping class files.
         * @return An unresolved type.
         */
        protected abstract UnresolvedType create(TypeInitializer typeInitializer, ClassDumpAction.Dispatcher dispatcher);

        /**
         * An unresolved type.
         */
        @HashCodeAndEqualsPlugin.Enhance(includeSyntheticFields = true)
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
                boolean record;
                if ((modifiers & Opcodes.ACC_RECORD) != 0) {
                    constraints.add(Constraint.ForRecord.INSTANCE);
                    record = true;
                } else {
                    record = false;
                }
                constraint = new Constraint.Compound(constraints);
                constraint.assertType(modifiers, interfaces != null, signature != null);
                if (record) {
                    constraint.assertRecord();
                }
                super.visit(version, modifiers, name, signature, superName, interfaces);
            }

            @Override
            public void visitPermittedSubclass(String permittedSubclass) {
                constraint.assertPermittedSubclass();
                super.visitPermittedSubclass(permittedSubclass);
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
            public void visitNestHost(String nestHost) {
                constraint.assertNestMate();
                super.visitNestHost(nestHost);
            }

            @Override
            public void visitNestMember(String nestMember) {
                constraint.assertNestMate();
                super.visitNestMember(nestMember);
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
                 * Asserts the presence of a record component.
                 */
                void assertRecord();

                /**
                 * Asserts the presence of a permitted subclass.
                 */
                void assertPermittedSubclass();

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

                    /**
                     * {@inheritDoc}
                     */
                    public void assertType(int modifier, boolean definesInterfaces, boolean isGeneric) {
                        /* do nothing */
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertField(String name, boolean isPublic, boolean isStatic, boolean isFinal, boolean isGeneric) {
                        /* do nothing */
                    }

                    /**
                     * {@inheritDoc}
                     */
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

                    /**
                     * {@inheritDoc}
                     */
                    public void assertAnnotation() {
                        /* do nothing */
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertTypeAnnotation() {
                        /* do nothing */
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertDefaultValue(String name) {
                        throw new IllegalStateException("Cannot define default value for '" + name + "' for non-annotation type");
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertDefaultMethodCall() {
                        /* do nothing */
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertTypeInConstantPool() {
                        /* do nothing */
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertMethodTypeInConstantPool() {
                        /* do nothing */
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertHandleInConstantPool() {
                        /* do nothing */
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertInvokeDynamic() {
                        /* do nothing */
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertSubRoutine() {
                        /* do nothing */
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertDynamicValueInConstantPool() {
                        /* do nothing */
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertNestMate() {
                        /* do nothing */
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertRecord() {
                        /* do nothing */
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertPermittedSubclass() {
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

                    /**
                     * {@inheritDoc}
                     */
                    public void assertField(String name, boolean isPublic, boolean isStatic, boolean isFinal, boolean isGeneric) {
                        throw new IllegalStateException("Cannot define a field for a package description type");
                    }

                    /**
                     * {@inheritDoc}
                     */
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

                    /**
                     * {@inheritDoc}
                     */
                    public void assertAnnotation() {
                        /* do nothing */
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertTypeAnnotation() {
                        /* do nothing */
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertDefaultValue(String name) {
                        /* do nothing, implicit by forbidding methods */
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertDefaultMethodCall() {
                        /* do nothing */
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertTypeInConstantPool() {
                        /* do nothing */
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertMethodTypeInConstantPool() {
                        /* do nothing */
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertHandleInConstantPool() {
                        /* do nothing */
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertInvokeDynamic() {
                        /* do nothing */
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertSubRoutine() {
                        /* do nothing */
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertType(int modifier, boolean definesInterfaces, boolean isGeneric) {
                        if (modifier != PackageDescription.PACKAGE_MODIFIERS) {
                            throw new IllegalStateException("A package description type must define " + PackageDescription.PACKAGE_MODIFIERS + " as modifier");
                        } else if (definesInterfaces) {
                            throw new IllegalStateException("Cannot implement interface for package type");
                        }
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertDynamicValueInConstantPool() {
                        /* do nothing */
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertNestMate() {
                        /* do nothing */
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertRecord() {
                        /* do nothing */
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertPermittedSubclass() {
                        /* do nothing */
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

                    /**
                     * {@inheritDoc}
                     */
                    public void assertField(String name, boolean isPublic, boolean isStatic, boolean isFinal, boolean isGeneric) {
                        if (!isStatic || !isPublic || !isFinal) {
                            throw new IllegalStateException("Cannot only define public, static, final field '" + name + "' for interface type");
                        }
                    }

                    /**
                     * {@inheritDoc}
                     */
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

                    /**
                     * {@inheritDoc}
                     */
                    public void assertAnnotation() {
                        /* do nothing */
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertTypeAnnotation() {
                        /* do nothing */
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertDefaultValue(String name) {
                        throw new IllegalStateException("Cannot define default value for '" + name + "' for non-annotation type");
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertDefaultMethodCall() {
                        /* do nothing */
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertType(int modifier, boolean definesInterfaces, boolean isGeneric) {
                        /* do nothing */
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertTypeInConstantPool() {
                        /* do nothing */
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertMethodTypeInConstantPool() {
                        /* do nothing */
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertHandleInConstantPool() {
                        /* do nothing */
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertInvokeDynamic() {
                        /* do nothing */
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertSubRoutine() {
                        /* do nothing */
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertDynamicValueInConstantPool() {
                        /* do nothing */
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertNestMate() {
                        /* do nothing */
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertRecord() {
                        /* do nothing */
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertPermittedSubclass() {
                        /* do nothing */
                    }
                }

                /**
                 * Represents the constraint of a record type.
                 */
                enum ForRecord implements Constraint {

                    /**
                     * The singleton instance.
                     */
                    INSTANCE;

                    /**
                     * {@inheritDoc}
                     */
                    public void assertField(String name, boolean isPublic, boolean isStatic, boolean isFinal, boolean isGeneric) {
                        /* do nothing */
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertMethod(String name,
                                             boolean isAbstract,
                                             boolean isPublic,
                                             boolean isPrivate,
                                             boolean isStatic,
                                             boolean isVirtual,
                                             boolean isConstructor,
                                             boolean isDefaultValueIncompatible,
                                             boolean isGeneric) {
                        /* do nothing */
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertAnnotation() {
                        /* do nothing */
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertTypeAnnotation() {
                        /* do nothing */
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertDefaultValue(String name) {
                        /* do nothing */
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertDefaultMethodCall() {
                        /* do nothing */
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertType(int modifier, boolean definesInterfaces, boolean isGeneric) {
                        if ((modifier & Opcodes.ACC_ABSTRACT) != 0) {
                            throw new IllegalStateException("Cannot define a record class as abstract");
                        }
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertTypeInConstantPool() {
                        /* do nothing */
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertMethodTypeInConstantPool() {
                        /* do nothing */
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertHandleInConstantPool() {
                        /* do nothing */
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertInvokeDynamic() {
                        /* do nothing */
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertSubRoutine() {
                        /* do nothing */
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertDynamicValueInConstantPool() {
                        /* do nothing */
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertNestMate() {
                        /* do nothing */
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertRecord() {
                        /* do nothing */
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertPermittedSubclass() {
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

                    /**
                     * {@inheritDoc}
                     */
                    public void assertField(String name, boolean isPublic, boolean isStatic, boolean isFinal, boolean isGeneric) {
                        if (!isStatic || !isPublic || !isFinal) {
                            throw new IllegalStateException("Cannot only define public, static, final field '" + name + "' for interface type");
                        }
                    }

                    /**
                     * {@inheritDoc}
                     */
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

                    /**
                     * {@inheritDoc}
                     */
                    public void assertAnnotation() {
                        /* do nothing */
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertTypeAnnotation() {
                        /* do nothing */
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertDefaultValue(String name) {
                        /* do nothing */
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertDefaultMethodCall() {
                        /* do nothing */
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertType(int modifier, boolean definesInterfaces, boolean isGeneric) {
                        if ((modifier & Opcodes.ACC_INTERFACE) == 0) {
                            throw new IllegalStateException("Cannot define annotation type without interface modifier");
                        }
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertTypeInConstantPool() {
                        /* do nothing */
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertMethodTypeInConstantPool() {
                        /* do nothing */
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertHandleInConstantPool() {
                        /* do nothing */
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertInvokeDynamic() {
                        /* do nothing */
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertSubRoutine() {
                        /* do nothing */
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertDynamicValueInConstantPool() {
                        /* do nothing */
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertNestMate() {
                        /* do nothing */
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertRecord() {
                        /* do nothing */
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertPermittedSubclass() {
                        /* do nothing */
                    }
                }

                /**
                 * Represents the constraint implied by a class file version.
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

                    /**
                     * {@inheritDoc}
                     */
                    public void assertType(int modifiers, boolean definesInterfaces, boolean isGeneric) {
                        if ((modifiers & Opcodes.ACC_ANNOTATION) != 0 && !classFileVersion.isAtLeast(ClassFileVersion.JAVA_V5)) {
                            throw new IllegalStateException("Cannot define annotation type for class file version " + classFileVersion);
                        } else if (isGeneric && !classFileVersion.isAtLeast(ClassFileVersion.JAVA_V4)) { // JSR14 allows for generic 1.4 classes.
                            throw new IllegalStateException("Cannot define a generic type for class file version " + classFileVersion);
                        }
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertField(String name, boolean isPublic, boolean isStatic, boolean isFinal, boolean isGeneric) {
                        if (isGeneric && !classFileVersion.isAtLeast(ClassFileVersion.JAVA_V4)) { // JSR14 allows for generic 1.4 classes.
                            throw new IllegalStateException("Cannot define generic field '" + name + "' for class file version " + classFileVersion);
                        }
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertMethod(String name,
                                             boolean isAbstract,
                                             boolean isPublic,
                                             boolean isPrivate,
                                             boolean isStatic,
                                             boolean isVirtual,
                                             boolean isConstructor,
                                             boolean isDefaultValueIncompatible,
                                             boolean isGeneric) {
                        if (isGeneric && !classFileVersion.isAtLeast(ClassFileVersion.JAVA_V4)) { // JSR14 allows for generic 1.4 classes.
                            throw new IllegalStateException("Cannot define generic method '" + name + "' for class file version " + classFileVersion);
                        } else if (!isVirtual && isAbstract) {
                            throw new IllegalStateException("Cannot define static or non-virtual method '" + name + "' to be abstract");
                        }
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertAnnotation() {
                        if (classFileVersion.isLessThan(ClassFileVersion.JAVA_V5)) {
                            throw new IllegalStateException("Cannot write annotations for class file version " + classFileVersion);
                        }
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertTypeAnnotation() {
                        if (classFileVersion.isLessThan(ClassFileVersion.JAVA_V5)) {
                            throw new IllegalStateException("Cannot write type annotations for class file version " + classFileVersion);
                        }
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertDefaultValue(String name) {
                        /* do nothing, implicitly checked by type assertion */
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertDefaultMethodCall() {
                        if (classFileVersion.isLessThan(ClassFileVersion.JAVA_V8)) {
                            throw new IllegalStateException("Cannot invoke default method for class file version " + classFileVersion);
                        }
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertTypeInConstantPool() {
                        if (classFileVersion.isLessThan(ClassFileVersion.JAVA_V5)) {
                            throw new IllegalStateException("Cannot write type to constant pool for class file version " + classFileVersion);
                        }
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertMethodTypeInConstantPool() {
                        if (classFileVersion.isLessThan(ClassFileVersion.JAVA_V7)) {
                            throw new IllegalStateException("Cannot write method type to constant pool for class file version " + classFileVersion);
                        }
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertHandleInConstantPool() {
                        if (classFileVersion.isLessThan(ClassFileVersion.JAVA_V7)) {
                            throw new IllegalStateException("Cannot write method handle to constant pool for class file version " + classFileVersion);
                        }
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertInvokeDynamic() {
                        if (classFileVersion.isLessThan(ClassFileVersion.JAVA_V7)) {
                            throw new IllegalStateException("Cannot write invoke dynamic instruction for class file version " + classFileVersion);
                        }
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertSubRoutine() {
                        if (classFileVersion.isGreaterThan(ClassFileVersion.JAVA_V5)) {
                            throw new IllegalStateException("Cannot write subroutine for class file version " + classFileVersion);
                        }
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertDynamicValueInConstantPool() {
                        if (classFileVersion.isLessThan(ClassFileVersion.JAVA_V11)) {
                            throw new IllegalStateException("Cannot write dynamic constant for class file version " + classFileVersion);
                        }
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertNestMate() {
                        if (classFileVersion.isLessThan(ClassFileVersion.JAVA_V11)) {
                            throw new IllegalStateException("Cannot define nest mate for class file version " + classFileVersion);
                        }
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertRecord() {
                        if (classFileVersion.isLessThan(ClassFileVersion.JAVA_V14)) {
                            throw new IllegalStateException("Cannot define record for class file version " + classFileVersion);
                        }
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertPermittedSubclass() {
                        if (classFileVersion.isLessThan(ClassFileVersion.JAVA_V15)) {
                            throw new IllegalStateException("Cannot define permitted subclasses for class file version " + classFileVersion);
                        }
                    }
                }

                /**
                 * A constraint implementation that summarizes several constraints.
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

                    /**
                     * {@inheritDoc}
                     */
                    public void assertType(int modifier, boolean definesInterfaces, boolean isGeneric) {
                        for (Constraint constraint : constraints) {
                            constraint.assertType(modifier, definesInterfaces, isGeneric);
                        }
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertField(String name, boolean isPublic, boolean isStatic, boolean isFinal, boolean isGeneric) {
                        for (Constraint constraint : constraints) {
                            constraint.assertField(name, isPublic, isStatic, isFinal, isGeneric);
                        }
                    }

                    /**
                     * {@inheritDoc}
                     */
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

                    /**
                     * {@inheritDoc}
                     */
                    public void assertDefaultValue(String name) {
                        for (Constraint constraint : constraints) {
                            constraint.assertDefaultValue(name);
                        }
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertDefaultMethodCall() {
                        for (Constraint constraint : constraints) {
                            constraint.assertDefaultMethodCall();
                        }
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertAnnotation() {
                        for (Constraint constraint : constraints) {
                            constraint.assertAnnotation();
                        }
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertTypeAnnotation() {
                        for (Constraint constraint : constraints) {
                            constraint.assertTypeAnnotation();
                        }
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertTypeInConstantPool() {
                        for (Constraint constraint : constraints) {
                            constraint.assertTypeInConstantPool();
                        }
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertMethodTypeInConstantPool() {
                        for (Constraint constraint : constraints) {
                            constraint.assertMethodTypeInConstantPool();
                        }
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertHandleInConstantPool() {
                        for (Constraint constraint : constraints) {
                            constraint.assertHandleInConstantPool();
                        }
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertInvokeDynamic() {
                        for (Constraint constraint : constraints) {
                            constraint.assertInvokeDynamic();
                        }
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertSubRoutine() {
                        for (Constraint constraint : constraints) {
                            constraint.assertSubRoutine();
                        }
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertDynamicValueInConstantPool() {
                        for (Constraint constraint : constraints) {
                            constraint.assertDynamicValueInConstantPool();
                        }
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertNestMate() {
                        for (Constraint constraint : constraints) {
                            constraint.assertNestMate();
                        }
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertRecord() {
                        for (Constraint constraint : constraints) {
                            constraint.assertRecord();
                        }
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void assertPermittedSubclass() {
                        for (Constraint constraint : constraints) {
                            constraint.assertPermittedSubclass();
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
                public void visitLdcInsn(Object value) {
                    if (value instanceof Type) {
                        Type type = (Type) value;
                        switch (type.getSort()) {
                            case Type.OBJECT:
                            case Type.ARRAY:
                                constraint.assertTypeInConstantPool();
                                break;
                            case Type.METHOD:
                                constraint.assertMethodTypeInConstantPool();
                                break;
                        }
                    } else if (value instanceof Handle) {
                        constraint.assertHandleInConstantPool();
                    } else if (value instanceof ConstantDynamic) {
                        constraint.assertDynamicValueInConstantPool();
                    }
                    super.visitLdcInsn(value);
                }

                @Override
                public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                    if (isInterface && opcode == Opcodes.INVOKESPECIAL) {
                        constraint.assertDefaultMethodCall();
                    }
                    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                }

                @Override
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
         * A type writer that inlines the created type into an existing class file.
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
             * Indicates that a record component should be ignored.
             */
            private static final RecordComponentVisitor IGNORE_RECORD_COMPONENT = null;

            /**
             * Indicates that an annotation should be ignored.
             */
            private static final AnnotationVisitor IGNORE_ANNOTATION = null;

            /**
             * The original type's description.
             */
            protected final TypeDescription originalType;

            /**
             * The class file locator for locating the original type's class file.
             */
            protected final ClassFileLocator classFileLocator;

            /**
             * Creates a new inlining type writer.
             *
             * @param instrumentedType             The instrumented type to be created.
             * @param classFileVersion             The class file specified by the user.
             * @param fieldPool                    The field pool to use.
             * @param recordComponentPool          The record component pool to use.
             * @param auxiliaryTypes               The explicit auxiliary types to add to the created type.
             * @param fields                       The instrumented type's declared fields.
             * @param methods                      The instrumented type's declared and virtually inherited methods.
             * @param instrumentedMethods          The instrumented methods relevant to this type creation.
             * @param recordComponents             The instrumented type's record components.
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
                                  RecordComponentPool recordComponentPool,
                                  List<? extends DynamicType> auxiliaryTypes,
                                  FieldList<FieldDescription.InDefinedShape> fields,
                                  MethodList<?> methods,
                                  MethodList<?> instrumentedMethods,
                                  RecordComponentList<RecordComponentDescription.InDefinedShape> recordComponents,
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
                        recordComponentPool,
                        auxiliaryTypes,
                        fields,
                        methods,
                        instrumentedMethods,
                        recordComponents,
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
            protected UnresolvedType create(TypeInitializer typeInitializer, ClassDumpAction.Dispatcher dispatcher) {
                try {
                    int writerFlags = asmVisitorWrapper.mergeWriter(AsmVisitorWrapper.NO_FLAGS);
                    int readerFlags = asmVisitorWrapper.mergeReader(AsmVisitorWrapper.NO_FLAGS);
                    byte[] binaryRepresentation = classFileLocator.locate(originalType.getName()).resolve();
                    dispatcher.dump(instrumentedType, true, binaryRepresentation);
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
             * Creates a class visitor which weaves all changes and additions on the fly.
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
                 * @param recordComponentPool          The record component pool to use.
                 * @param auxiliaryTypes               The explicit auxiliary types to add to the created type.
                 * @param fields                       The instrumented type's declared fields.
                 * @param methods                      The instrumented type's declared and virtually inherited methods.
                 * @param instrumentedMethods          The instrumented methods relevant to this type creation.
                 * @param recordComponents             The instrumented type's record components.
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
                                             RecordComponentPool recordComponentPool,
                                             List<? extends DynamicType> auxiliaryTypes,
                                             FieldList<FieldDescription.InDefinedShape> fields,
                                             MethodList<?> methods, MethodList<?> instrumentedMethods,
                                             RecordComponentList<RecordComponentDescription.InDefinedShape> recordComponents,
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
                            recordComponentPool,
                            auxiliaryTypes,
                            fields,
                            methods,
                            instrumentedMethods,
                            recordComponents,
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

                /**
                 * {@inheritDoc}
                 */
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

                        /**
                         * {@inheritDoc}
                         */
                        public void complete(ClassVisitor classVisitor, Implementation.Context.ExtractableView implementationContext) {
                            implementationContext.drain(this, classVisitor, annotationValueFilterFactory);
                        }
                    }

                    /**
                     * An initialization handler that appends code to a previously visited type initializer.
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
                         * Invoked after the user code was visited.
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
                        public abstract void visitEnd();

                        /**
                         * {@inheritDoc}
                         */
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

                        /**
                         * {@inheritDoc}
                         */
                        public void complete(ClassVisitor classVisitor, Implementation.Context.ExtractableView implementationContext) {
                            implementationContext.drain(this, classVisitor, annotationValueFilterFactory);
                            mv.visitMaxs(stackSize, localVariableLength);
                            mv.visitEnd();
                        }

                        /**
                         * A frame writer is responsible for adding empty frames on jump instructions.
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

                                /**
                                 * {@inheritDoc}
                                 */
                                public void onFrame(int type, int localVariableLength) {
                                    /* do nothing */
                                }

                                /**
                                 * {@inheritDoc}
                                 */
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

                                /**
                                 * {@inheritDoc}
                                 */
                                public void onFrame(int type, int localVariableLength) {
                                    /* do nothing */
                                }

                                /**
                                 * {@inheritDoc}
                                 */
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

                                /**
                                 * {@inheritDoc}
                                 */
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

                                /**
                                 * {@inheritDoc}
                                 */
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
                         * {@link TypeInitializer} registrations.
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
                            public void visitEnd() {
                                /* do nothing */
                            }

                            /**
                             * An initialization handler that appends code to a previously visited type initializer without allowing active
                             * {@link TypeInitializer} registrations and without an active record.
                             */
                            protected static class WithoutActiveRecord extends WithoutDrain {

                                /**
                                 * Creates a new appending initialization handler without a drain and without an active record.
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
                             * {@link TypeInitializer} registrations and with an active record.
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
                         * {@link TypeInitializer} registrations.
                         */
                        protected abstract static class WithDrain extends Appending {

                            /**
                             * A label marking the beginning of the appended code.
                             */
                            protected final Label appended;

                            /**
                             * A label marking the beginning og the original type initializer's code.
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
                            public void visitEnd() {
                                mv.visitLabel(appended);
                                frameWriter.emitFrame(mv);
                            }

                            @Override
                            protected void onComplete(Implementation.Context implementationContext) {
                                mv.visitJumpInsn(Opcodes.GOTO, original);
                                onAfterComplete(implementationContext);
                            }

                            /**
                             * Invoked after completion of writing the type initializer.
                             *
                             * @param implementationContext The implementation context to use.
                             */
                            protected abstract void onAfterComplete(Implementation.Context implementationContext);

                            /**
                             * A code appending initialization handler with a drain that does not apply an explicit record.
                             */
                            protected static class WithoutActiveRecord extends WithDrain {

                                /**
                                 * Creates a new appending initialization handler with a drain and without an active record.
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
                                protected void onAfterComplete(Implementation.Context implementationContext) {
                                    /* do nothing */
                                }
                            }

                            /**
                             * A code appending initialization handler with a drain that applies an explicit record.
                             */
                            protected static class WithActiveRecord extends WithDrain {

                                /**
                                 * A label indicating the beginning of the record's code.
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
                                protected void onAfterComplete(Implementation.Context implementationContext) {
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
                 * A class visitor which is capable of applying a redefinition of an existing class file.
                 */
                @SuppressFBWarnings(value = "UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", justification = "Field access order is implied by ASM")
                protected class RedefinitionClassVisitor extends MetadataAwareClassVisitor {

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
                     * A mapping of record components to write by their names.
                     */
                    private final LinkedHashMap<String, RecordComponentDescription> declarableRecordComponents;

                    /**
                     * A set of internal names of all nest members not yet defined by this type. If this type is not a nest host, this set is empty.
                     */
                    private final Set<String> nestMembers;

                    /**
                     * A mapping of the internal names of all declared types to their description.
                     */
                    private final LinkedHashMap<String, TypeDescription> declaredTypes;

                    /**
                     * A list of internal names of permitted subclasses to include.
                     */
                    private final List<String> permittedSubclasses;

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
                        declarableRecordComponents = new LinkedHashMap<String, RecordComponentDescription>();
                        for (RecordComponentDescription recordComponentDescription : recordComponents) {
                            declarableRecordComponents.put(recordComponentDescription.getActualName(), recordComponentDescription);
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
                        permittedSubclasses = new ArrayList<String>(instrumentedType.getPermittedSubclasses().size());
                        for (TypeDescription typeDescription : instrumentedType.getPermittedSubclasses()) {
                            permittedSubclasses.add(typeDescription.getInternalName());
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
                                        | (((modifiers & Opcodes.ACC_FINAL) != 0 && instrumentedType.isAnonymousType()) ? Opcodes.ACC_FINAL : 0),
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
                    protected void onNestHost() {
                        if (!instrumentedType.isNestHost()) {
                            cv.visitNestHost(instrumentedType.getNestHost().getInternalName());
                        }
                    }

                    @Override
                    protected void onVisitPermittedSubclass(String permittedSubclass) {
                        if (permittedSubclasses.remove(permittedSubclass)) {
                            cv.visitPermittedSubclass(permittedSubclass);
                        }
                    }

                    @Override
                    protected void onAfterPermittedSubclasses() {
                        for (String permittedSubclass : permittedSubclasses) {
                            cv.visitPermittedSubclass(permittedSubclass);
                        }
                    }

                    @Override
                    protected void onVisitOuterClass(String owner, String name, String descriptor) {
                        try { // The Groovy compiler often gets this attribute wrong such that this safety just retains it.
                            onOuterType();
                        } catch (Throwable ignored) {
                            cv.visitOuterClass(owner, name, descriptor);
                        }
                    }

                    @Override
                    protected void onOuterType() {
                        MethodDescription.InDefinedShape enclosingMethod = instrumentedType.getEnclosingMethod();
                        if (enclosingMethod != null) {
                            cv.visitOuterClass(enclosingMethod.getDeclaringType().getInternalName(),
                                    enclosingMethod.getInternalName(),
                                    enclosingMethod.getDescriptor());
                        } else if (instrumentedType.isLocalType() || instrumentedType.isAnonymousType()) {
                            cv.visitOuterClass(instrumentedType.getEnclosingType().getInternalName(), NO_REFERENCE, NO_REFERENCE);
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
                    protected RecordComponentVisitor onVisitRecordComponent(String name, String descriptor, String genericSignature) {
                        RecordComponentDescription recordComponentDescription = declarableRecordComponents.remove(name);
                        if (recordComponentDescription != null) {
                            RecordComponentPool.Record record = recordComponentPool.target(recordComponentDescription);
                            if (!record.isImplicit()) {
                                return redefine(record, genericSignature);
                            }
                        }
                        return cv.visitRecordComponent(name, descriptor, genericSignature);
                    }

                    /**
                     * Redefines a record component using the given explicit record component pool record.
                     *
                     * @param record           The record component pool record to apply during visitation of the existing record.
                     * @param genericSignature The record component's original generic signature which can be {@code null}.
                     * @return A record component visitor for visiting the existing record component definition.
                     */
                    protected RecordComponentVisitor redefine(RecordComponentPool.Record record, String genericSignature) {
                        RecordComponentDescription recordComponentDescription = record.getRecordComponent();
                        RecordComponentVisitor recordComponentVisitor = cv.visitRecordComponent(recordComponentDescription.getActualName(),
                                recordComponentDescription.getDescriptor(),
                                TypeDescription.AbstractBase.RAW_TYPES
                                        ? genericSignature
                                        : recordComponentDescription.getGenericSignature());
                        return recordComponentVisitor == null
                                ? IGNORE_RECORD_COMPONENT
                                : new AttributeObtainingRecordComponentVisitor(recordComponentVisitor, record);
                    }

                    @Override
                    protected void onAfterRecordComponents() {
                        for (RecordComponentDescription recordComponent : declarableRecordComponents.values()) {
                            recordComponentPool.target(recordComponent).apply(cv, annotationValueFilterFactory);
                        }
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
                     * Redefines a field using the given explicit field pool record and default value.
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
                        if (!internalName.equals(instrumentedType.getInternalName())) {
                            TypeDescription declaredType = declaredTypes.remove(internalName);
                            if (declaredType == null) {
                                cv.visitInnerClass(internalName, outerName, innerName, modifiers);
                            } else {
                                cv.visitInnerClass(internalName,
                                        // The second condition is added to retain the structure of some Java 6 compiled classes
                                        declaredType.isMemberType() || outerName != null && innerName == null && declaredType.isAnonymousType()
                                                ? instrumentedType.getInternalName()
                                                : NO_REFERENCE,
                                        declaredType.isAnonymousType()
                                                ? NO_REFERENCE
                                                : declaredType.getSimpleName(),
                                        declaredType.getModifiers());
                            }
                        }
                    }

                    @Override
                    protected void onVisitNestMember(String nestMember) {
                        if (instrumentedType.isNestHost() && nestMembers.remove(nestMember)) {
                            cv.visitNestMember(nestMember);
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
                        TypeDescription declaringType = instrumentedType.getDeclaringType();
                        if (declaringType != null) {
                            cv.visitInnerClass(instrumentedType.getInternalName(),
                                    declaringType.getInternalName(),
                                    instrumentedType.getSimpleName(),
                                    instrumentedType.getModifiers());
                        } else if (instrumentedType.isLocalType()) {
                            cv.visitInnerClass(instrumentedType.getInternalName(),
                                    NO_REFERENCE,
                                    instrumentedType.getSimpleName(),
                                    instrumentedType.getModifiers());
                        } else if (instrumentedType.isAnonymousType()) {
                            cv.visitInnerClass(instrumentedType.getInternalName(),
                                    NO_REFERENCE,
                                    NO_REFERENCE,
                                    instrumentedType.getModifiers());
                        }
                        for (TypeDescription typeDescription : declaredTypes.values()) {
                            cv.visitInnerClass(typeDescription.getInternalName(),
                                    typeDescription.isMemberType()
                                            ? instrumentedType.getInternalName()
                                            : NO_REFERENCE,
                                    typeDescription.isAnonymousType()
                                            ? NO_REFERENCE
                                            : typeDescription.getSimpleName(),
                                    typeDescription.getModifiers());
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
                     * A record component visitor that obtains all attributes and annotations of a record component that is found
                     * in the class file but discards all code.
                     */
                    protected class AttributeObtainingRecordComponentVisitor extends RecordComponentVisitor {

                        /**
                         * The record component pool record to apply onto the record component visitor.
                         */
                        private final RecordComponentPool.Record record;

                        /**
                         * Creates a new attribute obtaining record component visitor.
                         *
                         * @param recordComponentVisitor The record component visitor to delegate to.
                         * @param record                 The record component pool record to apply onto the record component visitor.
                         */
                        protected AttributeObtainingRecordComponentVisitor(RecordComponentVisitor recordComponentVisitor, RecordComponentPool.Record record) {
                            super(OpenedClassReader.ASM_API, recordComponentVisitor);
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
                            record.apply(getDelegate(), annotationValueFilterFactory);
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
             * A default type writer that only applies a type decoration.
             *
             * @param <V> The best known loaded type for the dynamically created type.
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
                            RecordComponentPool.Disabled.INSTANCE,
                            auxiliaryTypes,
                            new LazyFieldList(instrumentedType),
                            methods,
                            new MethodList.Empty<MethodDescription>(),
                            new RecordComponentList.Empty<RecordComponentDescription.InDefinedShape>(), // TODO
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

                /**
                 * {@inheritDoc}
                 */
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
                 * A field list that only reads fields lazy to avoid an eager lookup since fields are often not required.
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

                    /**
                     * {@inheritDoc}
                     */
                    public FieldDescription.InDefinedShape get(int index) {
                        return instrumentedType.getDeclaredFields().get(index);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public int size() {
                        return instrumentedType.getDeclaredFields().size();
                    }
                }

                /**
                 * A class visitor that decorates an existing type.
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

                    /**
                     * {@inheritDoc}
                     */
                    public void apply(ClassVisitor classVisitor, TypeInitializer typeInitializer, Implementation.Context implementationContext) {
                        /* do nothing */
                    }
                }
            }
        }

        /**
         * A type writer that creates a class file that is not based upon another, existing class.
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
             * Creates a new default type writer for creating a new type that is not based on an existing class file.
             *
             * @param instrumentedType             The instrumented type to be created.
             * @param classFileVersion             The class file version to write the instrumented type in and to apply when creating auxiliary types.
             * @param fieldPool                    The field pool to use.
             * @param methodPool                   The method pool to use.
             * @param recordComponentPool          The record component pool to use.
             * @param auxiliaryTypes               A list of auxiliary types to add to the created type.
             * @param fields                       The instrumented type's declared fields.
             * @param methods                      The instrumented type's declared and virtually inherited methods.
             * @param instrumentedMethods          The instrumented methods relevant to this type creation.
             * @param recordComponents             The instrumented type's record components.
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
                                  RecordComponentPool recordComponentPool,
                                  List<? extends DynamicType> auxiliaryTypes,
                                  FieldList<FieldDescription.InDefinedShape> fields,
                                  MethodList<?> methods,
                                  MethodList<?> instrumentedMethods,
                                  RecordComponentList<RecordComponentDescription.InDefinedShape> recordComponents,
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
                        recordComponentPool,
                        auxiliaryTypes,
                        fields,
                        methods,
                        instrumentedMethods,
                        recordComponents,
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
            protected UnresolvedType create(TypeInitializer typeInitializer, ClassDumpAction.Dispatcher dispatcher) {
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
                    classVisitor.visitNestHost(instrumentedType.getNestHost().getInternalName());
                }
                for (TypeDescription typeDescription : instrumentedType.getPermittedSubclasses()) {
                    classVisitor.visitPermittedSubclass(typeDescription.getInternalName());
                }
                MethodDescription.InDefinedShape enclosingMethod = instrumentedType.getEnclosingMethod();
                if (enclosingMethod != null) {
                    classVisitor.visitOuterClass(enclosingMethod.getDeclaringType().getInternalName(),
                            enclosingMethod.getInternalName(),
                            enclosingMethod.getDescriptor());
                } else if (instrumentedType.isLocalType() || instrumentedType.isAnonymousType()) {
                    classVisitor.visitOuterClass(instrumentedType.getEnclosingType().getInternalName(), NO_REFERENCE, NO_REFERENCE);
                }
                typeAttributeAppender.apply(classVisitor, instrumentedType, annotationValueFilterFactory.on(instrumentedType));
                for (RecordComponentDescription recordComponentDescription : recordComponents) {
                    recordComponentPool.target(recordComponentDescription).apply(classVisitor, annotationValueFilterFactory);
                }
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
                        classVisitor.visitNestMember(typeDescription.getInternalName());
                    }
                }
                TypeDescription declaringType = instrumentedType.getDeclaringType();
                if (declaringType != null) {
                    classVisitor.visitInnerClass(instrumentedType.getInternalName(),
                            declaringType.getInternalName(),
                            instrumentedType.getSimpleName(),
                            instrumentedType.getModifiers());
                } else if (instrumentedType.isLocalType()) {
                    classVisitor.visitInnerClass(instrumentedType.getInternalName(),
                            NO_REFERENCE,
                            instrumentedType.getSimpleName(),
                            instrumentedType.getModifiers());
                } else if (instrumentedType.isAnonymousType()) {
                    classVisitor.visitInnerClass(instrumentedType.getInternalName(),
                            NO_REFERENCE,
                            NO_REFERENCE,
                            instrumentedType.getModifiers());
                }
                for (TypeDescription typeDescription : instrumentedType.getDeclaredTypes()) {
                    classVisitor.visitInnerClass(typeDescription.getInternalName(),
                            typeDescription.isMemberType()
                                    ? instrumentedType.getInternalName()
                                    : NO_REFERENCE,
                            typeDescription.isAnonymousType()
                                    ? NO_REFERENCE
                                    : typeDescription.getSimpleName(),
                            typeDescription.getModifiers());
                }
                classVisitor.visitEnd();
                return new UnresolvedType(classWriter.toByteArray(), implementationContext.getAuxiliaryTypes());
            }
        }

        /**
         * An action to write a class file to the dumping location.
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
             * The suffix to append to the dumped class file.
             */
            private final long suffix;

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
             * @param suffix               The suffix to append to the dumped class file.
             * @param binaryRepresentation The type's binary representation.
             */
            protected ClassDumpAction(String target, TypeDescription instrumentedType, boolean original, long suffix, byte[] binaryRepresentation) {
                this.target = target;
                this.instrumentedType = instrumentedType;
                this.original = original;
                this.suffix = suffix;
                this.binaryRepresentation = binaryRepresentation;
            }

            /**
             * {@inheritDoc}
             */
            public Void run() throws Exception {
                OutputStream outputStream = new FileOutputStream(new File(target, instrumentedType.getName()
                        + (original ? "-original." : ".")
                        + suffix
                        + ".class"));
                try {
                    outputStream.write(binaryRepresentation);
                    return NOTHING;
                } finally {
                    outputStream.close();
                }
            }

            /**
             * A dispatcher for dumping class files to the file system.
             */
            protected interface Dispatcher {

                /**
                 * Dumps a class file to the file system.
                 *
                 * @param instrumentedType     The type to dump.
                 * @param original             {@code true} if the class file is in its original state.
                 * @param binaryRepresentation The class file's binary representation.
                 */
                void dump(TypeDescription instrumentedType, boolean original, byte[] binaryRepresentation);

                /**
                 * A disabled dispatcher that does not dump any class files.
                 */
                enum Disabled implements Dispatcher {

                    /**
                     * The singleton instance.
                     */
                    INSTANCE;

                    /**
                     * {@inheritDoc}
                     */
                    public void dump(TypeDescription instrumentedType, boolean original, byte[] binaryRepresentation) {
                        /* do nothing */
                    }
                }

                /**
                 * An enabled dispatcher that dumps class files to a given folder.
                 */
                @HashCodeAndEqualsPlugin.Enhance
                class Enabled implements Dispatcher {

                    /**
                     * The folder to write class files to.
                     */
                    private final String folder;

                    /**
                     * The timestamp to append.
                     */
                    private final long timestamp;

                    /**
                     * Creates a new dispatcher for dumping class files.
                     *
                     * @param folder    The folder to write class files to.
                     * @param timestamp The timestamp to append.
                     */
                    protected Enabled(String folder, long timestamp) {
                        this.folder = folder;
                        this.timestamp = timestamp;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void dump(TypeDescription instrumentedType, boolean original, byte[] binaryRepresentation) {
                        try {
                            AccessController.doPrivileged(new ClassDumpAction(folder, instrumentedType, original, timestamp, binaryRepresentation));
                        } catch (Exception exception) {
                            exception.printStackTrace();
                        }
                    }
                }
            }
        }
    }
}
