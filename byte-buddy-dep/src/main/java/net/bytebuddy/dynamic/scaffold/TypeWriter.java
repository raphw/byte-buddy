package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.asm.ClassVisitorWrapper;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.LoadedTypeInitializer;
import net.bytebuddy.instrumentation.attribute.FieldAttributeAppender;
import net.bytebuddy.instrumentation.attribute.MethodAttributeAppender;
import net.bytebuddy.instrumentation.attribute.TypeAttributeAppender;
import net.bytebuddy.instrumentation.field.FieldDescription;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.modifier.MethodManifestation;
import org.objectweb.asm.*;

import java.util.Arrays;

import static net.bytebuddy.utility.ByteBuddyCommons.join;

/**
 * A type writer allows an easier creation of a dynamic type by enforcing the writing order
 * (type, annotations, fields, methods) that is required by ASM in order to successfully creating a Java type.
 * <p>&nbsp;</p>
 * Note: This type represents a mutable data structure since it is a wrapper around an ASM
 * {@link org.objectweb.asm.ClassWriter}. Once a phase of this type writer is left the old instances must not longer
 * be used.
 *
 * @param <T> The best known loaded type for the dynamically created type.
 */
public interface TypeWriter<T> {

    /**
     * The ASM flag for deactivating the manual computation of stack map frames or operand stack
     * and local variable array sizes.
     */
    static final int ASM_MANUAL_FLAG = 0;

    /**
     * Creates the {@link net.bytebuddy.dynamic.DynamicType} which is written by this type writer.
     *
     * @param auxiliaryType Any additionally registered auxiliary types to register for the created dynamic type.
     * @return An unloaded dynamic type that is the outcome of the type writing
     */
    DynamicType.Unloaded<T> make(DynamicType... auxiliaryType);

    /**
     * An field pool that allows a lookup for how to implement a field.
     */
    static interface FieldPool {

        /**
         * Returns the field attribute appender that matches a given field description or a default field
         * attribute appender if no appender was registered for the given field.
         *
         * @param fieldDescription The field description of interest.
         * @return The registered field attribute appender for the given field or the default appender if no such
         * appender was found.
         */
        Entry target(FieldDescription fieldDescription);

        /**
         * An entry of a field pool that describes how a field is implemented.
         *
         * @see net.bytebuddy.dynamic.scaffold.TypeWriter.FieldPool
         */
        static interface Entry {

            /**
             * Returns the field attribute appender factory for a given field.
             *
             * @return The attribute appender factory to be applied on the given field.
             */
            FieldAttributeAppender.Factory getFieldAppenderFactory();

            /**
             * Returns the default value for the field that is represented by this entry. This value might be
             * {@code null} if no such value is set.
             *
             * @return The default value for the field that is represented by this entry.
             */
            Object getDefaultValue();

            /**
             * A default implementation of a compiled field registry that simply returns a no-op
             * {@link net.bytebuddy.instrumentation.attribute.FieldAttributeAppender.Factory}
             * for any field.
             */
            static enum NoOp implements Entry {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                @Override
                public FieldAttributeAppender.Factory getFieldAppenderFactory() {
                    return FieldAttributeAppender.NoOp.INSTANCE;
                }

                @Override
                public Object getDefaultValue() {
                    return null;
                }
            }

            /**
             * A simple entry that creates a specific
             * {@link net.bytebuddy.instrumentation.attribute.FieldAttributeAppender.Factory}
             * for any field.
             */
            static class Simple implements Entry {

                /**
                 * The field attribute appender factory that is represented by this entry.
                 */
                private final FieldAttributeAppender.Factory attributeAppenderFactory;

                /**
                 * The field's default value or {@code null} if no default value is set.
                 */
                private final Object defaultValue;

                /**
                 * Creates a new simple entry for a given attribute appender factory.
                 *
                 * @param attributeAppenderFactory The attribute appender factory to be returned.
                 * @param defaultValue             The field's default value or {@code null} if no default value is
                 *                                 set.
                 */
                public Simple(FieldAttributeAppender.Factory attributeAppenderFactory, Object defaultValue) {
                    this.attributeAppenderFactory = attributeAppenderFactory;
                    this.defaultValue = defaultValue;
                }

                @Override
                public FieldAttributeAppender.Factory getFieldAppenderFactory() {
                    return attributeAppenderFactory;
                }

                @Override
                public Object getDefaultValue() {
                    return defaultValue;
                }

                @Override
                public boolean equals(Object other) {
                    if (this == other) return true;
                    if (other == null || getClass() != other.getClass()) return false;
                    Simple simple = (Simple) other;
                    return attributeAppenderFactory.equals(simple.attributeAppenderFactory)
                            && !(defaultValue != null ? !defaultValue.equals(simple.defaultValue) : simple.defaultValue != null);
                }

                @Override
                public int hashCode() {
                    return 31 * attributeAppenderFactory.hashCode() + (defaultValue != null ? defaultValue.hashCode() : 0);
                }

                @Override
                public String toString() {
                    return "TypeWriter.FieldPool.Entry.Simple{" +
                            "attributeAppenderFactory=" + attributeAppenderFactory +
                            ", defaultValue=" + defaultValue +
                            '}';
                }
            }
        }
    }

    /**
     * An method pool that allows a lookup for how to implement a method.
     */
    static interface MethodPool {

        /**
         * Looks up a handler entry for a given method.
         *
         * @param methodDescription The method being processed.
         * @return A handler entry for the given method.
         */
        Entry target(MethodDescription methodDescription);

        /**
         * An entry of a method pool that describes how a method is implemented.
         *
         * @see net.bytebuddy.dynamic.scaffold.TypeWriter.MethodPool
         */
        static interface Entry {

            static interface Factory {

                Entry compile(Instrumentation.Target instrumentationTarget);
            }

            /**
             * Determines if this entry requires a method to be defined for a given instrumentation.
             *
             * @return {@code true} if a method should be defined for a given instrumentation.
             */
            boolean isDefineMethod();

            /**
             * The byte code appender to be used for the instrumentation by this entry. Must not
             * be called if {@link net.bytebuddy.dynamic.scaffold.TypeWriter.MethodPool.Entry#isDefineMethod()}
             * returns {@code false}.
             *
             * @return The byte code appender that is responsible for the instrumentation of a method matched for
             * this entry.
             */
            ByteCodeAppender getByteCodeAppender();

            /**
             * The method attribute appender that is to be used for the instrumentation by this entry.  Must not
             * be called if {@link net.bytebuddy.dynamic.scaffold.TypeWriter.MethodPool.Entry#isDefineMethod()}
             * returns {@code false}.
             *
             * @return The method attribute appender that is responsible for the instrumentation of a method matched for
             * this entry.
             */
            MethodAttributeAppender getAttributeAppender();

            /**
             * A skip entry that instructs to ignore a method.
             */
            static enum Skip implements Entry, Factory {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                @Override
                public boolean isDefineMethod() {
                    return false;
                }

                @Override
                public ByteCodeAppender getByteCodeAppender() {
                    throw new IllegalStateException();
                }

                @Override
                public MethodAttributeAppender getAttributeAppender() {
                    throw new IllegalStateException();
                }

                @Override
                public Entry compile(Instrumentation.Target instrumentationTarget) {
                    return this;
                }
            }

            /**
             * A default implementation of {@link net.bytebuddy.dynamic.scaffold.TypeWriter.MethodPool.Entry}
             * that is not to be ignored but is represented by a tuple of a byte code appender and a method attribute appender.
             */
            static class Simple implements Entry {

                /**
                 * The byte code appender that is represented by this entry.
                 */
                private final ByteCodeAppender byteCodeAppender;

                /**
                 * The method attribute appender that is represented by this entry.
                 */
                private final MethodAttributeAppender methodAttributeAppender;

                /**
                 * Creates a new simple entry of a method pool.
                 *
                 * @param byteCodeAppender        The byte code appender that is represented by this entry.
                 * @param methodAttributeAppender The method attribute appender that is represented by this entry.
                 */
                public Simple(ByteCodeAppender byteCodeAppender, MethodAttributeAppender methodAttributeAppender) {
                    this.byteCodeAppender = byteCodeAppender;
                    this.methodAttributeAppender = methodAttributeAppender;
                }

                @Override
                public boolean isDefineMethod() {
                    return true;
                }

                @Override
                public ByteCodeAppender getByteCodeAppender() {
                    return byteCodeAppender;
                }

                @Override
                public MethodAttributeAppender getAttributeAppender() {
                    return methodAttributeAppender;
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && byteCodeAppender.equals(((Simple) other).byteCodeAppender)
                            && methodAttributeAppender.equals(((Simple) other).methodAttributeAppender);
                }

                @Override
                public int hashCode() {
                    return 31 * byteCodeAppender.hashCode() + methodAttributeAppender.hashCode();
                }

                @Override
                public String toString() {
                    return "TypeWriter.MethodPool.Entry.Simple{" +
                            "byteCodeAppender=" + byteCodeAppender +
                            ", methodAttributeAppender=" + methodAttributeAppender +
                            '}';
                }
            }
        }
    }

    /**
     * Describes a phase that can transition into a phase for writing members of a type.
     *
     * @param <T> The best known loaded type for the dynamically created type.
     */
    static interface MemberPhaseTransitional<T> {

        /**
         * Moves to the member phase.
         *
         * @return This type writer in its member phase.
         */
        InMemberPhase<T> members();
    }

    /**
     * Describes a type writer currently in the general phase, i.e. in the phase before fields or methods
     * are written to the type.
     *
     * @param <T> The best known loaded type for the dynamically created type.
     */
    static interface InGeneralPhase<T> extends TypeWriter<T>, MemberPhaseTransitional<T> {

        /**
         * Writes an attribute to the type that is created by this type writer.
         *
         * @param typeAttributeAppender The type attribute appender to be applied to the type that is represented by
         *                              this type writer.
         * @return This type writer.
         */
        InGeneralPhase<T> attributeType(TypeAttributeAppender typeAttributeAppender);
    }

    /**
     * Describes a type writer currently in the member phase, i.e. in the phase after all type attributes are applied.
     *
     * @param <T> The best known loaded type for the dynamically created type.
     */
    static interface InMemberPhase<T> extends TypeWriter<T> {

        /**
         * Adds a number of fields as described by the argument to the type that is created by this type
         * writer where the annotations are received from the given compiled field registry.
         *
         * @param fieldDescriptions The fields to be added to the type that is created by this type writer.
         * @param fieldPool         The field pool that is queried for finding annotations for written fields.
         * @return This type writer.
         */
        InMemberPhase<T> writeFields(Iterable<? extends FieldDescription> fieldDescriptions,
                                     FieldPool fieldPool);

        /**
         * Adds a number of methods as described by the argument to the type that is created by this type
         * writer where the implementations and annotations are received from the given compiled method registry.
         *
         * @param methodDescriptions The methods to be added to the type that is created by this type writer.
         * @param methodPool         The method pool that is queried for creating implementations for these methods.
         * @return This type writer.
         */
        InMemberPhase<T> writeMethods(Iterable<? extends MethodDescription> methodDescriptions,
                                      MethodPool methodPool);

        InMemberPhase<T> writeRaw(RawInput rawInput);

        static interface RawInput {

            void apply(ClassVisitor classVisitor);
        }
    }

    /**
     * A builder that creates a new type writer for given arguments.
     *
     * @param <T> The best known loaded type for the dynamically created type.
     */
    static class Builder<T> {

        /**
         * The type description of the instrumented type that is represented by this builder.
         */
        private final TypeDescription instrumentedType;

        /**
         * The loaded type initializer of the instrumented type that is represented by this builder.
         */
        private final LoadedTypeInitializer loadedTypeInitializer;

        /**
         * An extractable view of the instrumentation context that is represented by this builder.
         */
        private final Instrumentation.Context.ExtractableView instrumentationContext;

        /**
         * The class file version this instrumented type is to be written in by this builder.
         */
        private final ClassFileVersion classFileVersion;

        /**
         * A provider for a {@link org.objectweb.asm.ClassWriter}.
         */
        private final ClassWriterProvider classWriterProvider;

        /**
         * Creates a new builder.
         *
         * @param instrumentedType       The type description of the instrumented type that is to be created.
         * @param loadedTypeInitializer  The type initializer of the instrumented type that is to be created.
         * @param instrumentationContext An extractable view of the instrumentation context.
         * @param classFileVersion       The class file version this instrumented type is to be written in.
         * @param classWriterProvider    A provider for a {@link org.objectweb.asm.ClassWriter}.
         */
        public Builder(TypeDescription instrumentedType,
                       LoadedTypeInitializer loadedTypeInitializer,
                       Instrumentation.Context.ExtractableView instrumentationContext,
                       ClassFileVersion classFileVersion,
                       ClassWriterProvider classWriterProvider) {
            this.instrumentedType = instrumentedType;
            this.loadedTypeInitializer = loadedTypeInitializer;
            this.instrumentationContext = instrumentationContext;
            this.classFileVersion = classFileVersion;
            this.classWriterProvider = classWriterProvider;
        }

        /**
         * Alters the modifiers of a method to reflect the actual implementation of a method to be {@code abstract}
         * if a method is not implemented or to be non-abstract if it is implemented.
         *
         * @param methodDescription The method being processed.
         * @param appendsCode       {@code true} if the method does append code, i.e. is non-abstract.
         * @return The filter modifiers of this method depending on whether it is {@code abstract} or not.
         */
        private static int overrideModifiers(MethodDescription methodDescription, boolean appendsCode) {
            if (appendsCode && (methodDescription.isAbstract() || methodDescription.isNative())) {
                return methodDescription.getModifiers() & ~MethodManifestation.ABSTRACTION_MASK;
            } else if (!appendsCode && !methodDescription.isAbstract() && !methodDescription.isNative()) {
                return methodDescription.getModifiers() | Opcodes.ACC_ABSTRACT;
            } else {
                return methodDescription.getModifiers();
            }
        }

        /**
         * Creates a new type writer and moves it to its general phase.
         *
         * @param classVisitorWrapper A class visitor wrapper to be applied to the ASM class writing process.
         * @return A new type writer for the given type.
         */
        public InGeneralPhase<T> build(ClassVisitorWrapper classVisitorWrapper) {
            ClassWriter classWriter = classWriterProvider.make();
            ClassVisitor classVisitor = classVisitorWrapper.wrap(classWriter);
            classVisitor.visit(classFileVersion.getVersionNumber(),
                    instrumentedType.getModifiers(),
                    instrumentedType.getInternalName(),
                    instrumentedType.getGenericSignature(),
                    instrumentedType.getSupertype() == null ? null : instrumentedType.getSupertype().getInternalName(),
                    instrumentedType.getInterfaces().toInternalNames());
            return new Handler<T>(classWriter, classVisitor);
        }

        @Override
        public String toString() {
            return "TypeWriter.Builder{" +
                    "instrumentedType=" + instrumentedType +
                    ", typeInitializer=" + loadedTypeInitializer +
                    ", instrumentationContext=" + instrumentationContext +
                    ", classFileVersion=" + classFileVersion +
                    '}';
        }

        /**
         * An implementation of a handler that is capable of writing a type while enforcing the order that is required
         * by the ASM library that is used for carrying out this task.
         *
         * @param <S> The most specific type of the class that is being created by this type writer.
         */
        private class Handler<S> implements TypeWriter<S>, InGeneralPhase<S>, InMemberPhase<S> {

            /**
             * The class writer that is writing the class.
             */
            protected final ClassWriter classWriter;

            /**
             * The top most class visitor that is presented to any entity that is writing the instrumented type.
             */
            protected final ClassVisitor classVisitor;

            /**
             * Creates a new handler.
             *
             * @param classWriter  the class writer that is writing the currently created type.
             * @param classVisitor The top-most class visitor that all instructions are delegated to.
             */
            protected Handler(ClassWriter classWriter, ClassVisitor classVisitor) {
                this.classWriter = classWriter;
                this.classVisitor = classVisitor;
            }

            @Override
            public InGeneralPhase<S> attributeType(TypeAttributeAppender typeAttributeAppender) {
                typeAttributeAppender.apply(classVisitor, instrumentedType);
                return this;
            }

            @Override
            public InMemberPhase<S> members() {
                return this;
            }

            @Override
            public InMemberPhase<S> writeFields(Iterable<? extends FieldDescription> fieldDescriptions,
                                                FieldPool fieldPool) {
                for (FieldDescription fieldDescription : fieldDescriptions) {
                    FieldPool.Entry entry = fieldPool.target(fieldDescription);
                    FieldVisitor fieldVisitor = classVisitor.visitField(fieldDescription.getModifiers(),
                            fieldDescription.getInternalName(),
                            fieldDescription.getDescriptor(),
                            fieldDescription.getGenericSignature(),
                            entry.getDefaultValue());
                    entry.getFieldAppenderFactory()
                            .make(instrumentedType)
                            .apply(fieldVisitor, fieldDescription);
                    fieldVisitor.visitEnd();
                }
                return this;
            }

            @Override
            public InMemberPhase<S> writeMethods(Iterable<? extends MethodDescription> methodDescriptions,
                                                 MethodPool methodPool) {
                for (MethodDescription methodDescription : methodDescriptions) {
                    MethodPool.Entry entry = methodPool.target(methodDescription);
                    if (entry.isDefineMethod()) {
                        boolean appendsCode = entry.getByteCodeAppender().appendsCode();
                        MethodVisitor methodVisitor = classVisitor.visitMethod(
                                overrideModifiers(methodDescription, appendsCode),
                                methodDescription.getInternalName(),
                                methodDescription.getDescriptor(),
                                methodDescription.getGenericSignature(),
                                methodDescription.getExceptionTypes().toInternalNames());
                        entry.getAttributeAppender().apply(methodVisitor, methodDescription);
                        if (appendsCode) {
                            methodVisitor.visitCode();
                            ByteCodeAppender.Size size = entry.getByteCodeAppender().apply(methodVisitor,
                                    instrumentationContext,
                                    methodDescription);
                            methodVisitor.visitMaxs(size.getOperandStackSize(), size.getLocalVariableSize());
                        }
                        methodVisitor.visitEnd();
                    }
                }
                return this;
            }

            @Override
            public InMemberPhase<S> writeRaw(RawInput rawInput) {
                rawInput.apply(classVisitor);
                return this;
            }

            @Override
            public DynamicType.Unloaded<S> make(DynamicType... dynamicType) {
                classVisitor.visitEnd();
                return new DynamicType.Default.Unloaded<S>(instrumentedType,
                        classWriter.toByteArray(),
                        loadedTypeInitializer,
                        join(instrumentationContext.getRegisteredAuxiliaryTypes(), Arrays.asList(dynamicType)));
            }

            @Override
            public String toString() {
                return "TypeWriter.Builder.Handler{" +
                        "builder=" + Builder.this +
                        ", classWriter=" + classWriter +
                        ", classVisitor=" + classVisitor +
                        '}';
            }
        }

        /**
         * A provider for creating a {@link org.objectweb.asm.ClassWriter}.
         */
        public static interface ClassWriterProvider {

            /**
             * Creates a new {@link org.objectweb.asm.ClassWriter} without a predefined constant pool.
             */
            static enum CleanCopy implements ClassWriterProvider {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                @Override
                public ClassWriter make() {
                    return new ClassWriter(ASM_MANUAL_FLAG);
                }
            }

            /**
             * Creates a new {@link org.objectweb.asm.ClassWriter} with a constant pool that is predefined by
             * a given {@link org.objectweb.asm.ClassReader}'s constant pool.
             */
            static class ForClassReader implements ClassWriterProvider {

                /**
                 * The class reader for copying the constant pool from.
                 */
                private final ClassReader classReader;

                /**
                 * Creates a new class writer provider that copies a given class reader's constant pool.
                 *
                 * @param classReader The class reader to copy the constant pool from.
                 */
                public ForClassReader(ClassReader classReader) {
                    this.classReader = classReader;
                }

                @Override
                public ClassWriter make() {
                    return new ClassWriter(classReader, ASM_MANUAL_FLAG);
                }

                @Override
                public String toString() {
                    return "TypeWriter.ClassWriterProvider.ForClassReader{classReader=" + classReader + '}';
                }
            }

            /**
             * Creates a new {@link org.objectweb.asm.ClassWriter}.
             *
             * @return A new class writer.
             */
            ClassWriter make();
        }
    }
}
