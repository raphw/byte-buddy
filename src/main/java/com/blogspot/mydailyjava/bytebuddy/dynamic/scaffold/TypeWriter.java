package com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold;

import com.blogspot.mydailyjava.bytebuddy.ClassFormatVersion;
import com.blogspot.mydailyjava.bytebuddy.asm.ClassVisitorWrapper;
import com.blogspot.mydailyjava.bytebuddy.dynamic.DynamicType;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.Instrumentation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.attribute.FieldAttributeAppender;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.attribute.MethodAttributeAppender;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.attribute.TypeAttributeAppender;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.field.FieldDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.InstrumentedType;
import com.blogspot.mydailyjava.bytebuddy.modifier.MethodManifestation;
import org.objectweb.asm.*;

import java.util.Iterator;
import java.util.List;

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
     * An iterable view of a list that can be modified within the same thread without breaking
     * the iterator. Instead, the iterator will continue its iteration over the additional entries
     * that were prepended to the list.
     *
     * @param <S> The type of the list elements.
     */
    static class SameThreadCoModifiableIterable<S> implements Iterable<S> {

        private class SameThreadCoModifiableIterator implements Iterator<S> {

            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < elements.size();
            }

            @Override
            public S next() {
                return elements.get(index++);
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        }

        private final List<? extends S> elements;

        /**
         * Creates a new iterable view.
         *
         * @param elements The elements to be represented by this view.
         */
        public SameThreadCoModifiableIterable(List<? extends S> elements) {
            this.elements = elements;
        }

        @Override
        public Iterator<S> iterator() {
            return new SameThreadCoModifiableIterator();
        }
    }

    /**
     * An field pool that allows a lookup for how to implement a field.
     */
    static interface FieldPool {

        /**
         * An entry of a field pool that describes how a field is implemented.
         *
         * @see com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold.TypeWriter.FieldPool
         */
        static interface Entry {

            /**
             * A default implementation of a compiled field registry that simply returns a no-op
             * {@link com.blogspot.mydailyjava.bytebuddy.instrumentation.attribute.FieldAttributeAppender.Factory}
             * for any field.
             */
            static enum NoOp implements Entry {
                INSTANCE;

                @Override
                public FieldAttributeAppender.Factory getFieldAppenderFactory() {
                    return FieldAttributeAppender.NoOp.INSTANCE;
                }
            }

            /**
             * A simple entry that creates a specific
             * {@link com.blogspot.mydailyjava.bytebuddy.instrumentation.attribute.FieldAttributeAppender.Factory}
             * for any field.
             */
            static class Simple implements Entry {

                private final FieldAttributeAppender.Factory attributeAppenderFactory;

                /**
                 * Creates a new simple entry for a given attribute appender factory.
                 *
                 * @param attributeAppenderFactory The attribute appender factory to be returned.
                 */
                public Simple(FieldAttributeAppender.Factory attributeAppenderFactory) {
                    this.attributeAppenderFactory = attributeAppenderFactory;
                }

                @Override
                public FieldAttributeAppender.Factory getFieldAppenderFactory() {
                    return attributeAppenderFactory;
                }
            }

            /**
             * Returns the field attribute appender factory for a given field.
             *
             * @return The attribute appender factory to be applied on the given field.
             */
            FieldAttributeAppender.Factory getFieldAppenderFactory();
        }

        /**
         * Returns the field attribute appender that matches a given field description or a default field
         * attribute appender if no appender was registered for the given field.
         *
         * @param fieldDescription The field description of interest.
         * @return The registered field attribute appender for the given field or the default appender if no such
         * appender was found.
         */
        Entry target(FieldDescription fieldDescription);
    }

    /**
     * An method pool that allows a lookup for how to implement a method.
     */
    static interface MethodPool {

        /**
         * An entry of a method pool that describes how a method is implemented.
         *
         * @see com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold.TypeWriter.MethodPool
         */
        static interface Entry {

            /**
             * A skip entry that instructs to ignore a method.
             */
            static enum Skip implements Entry {
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
            }

            /**
             * A default implementation of {@link com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold.TypeWriter.MethodPool.Entry}
             * that is not to be ignored but is represented by a tuple of a byte code appender and a method attribute appender.
             */
            static class Simple implements Entry {

                private final ByteCodeAppender byteCodeAppender;
                private final MethodAttributeAppender methodAttributeAppender;

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
                public boolean equals(Object o) {
                    return this == o || !(o == null || getClass() != o.getClass())
                            && byteCodeAppender.equals(((Simple) o).byteCodeAppender)
                            && methodAttributeAppender.equals(((Simple) o).methodAttributeAppender);
                }

                @Override
                public int hashCode() {
                    return 31 * byteCodeAppender.hashCode() + methodAttributeAppender.hashCode();
                }

                @Override
                public String toString() {
                    return "Default{" +
                            "byteCodeAppender=" + byteCodeAppender +
                            ", methodAttributeAppender=" + methodAttributeAppender +
                            '}';
                }
            }

            /**
             * Determines if this entry requires a method to be defined for a given instrumentation.
             *
             * @return {@code true} if a method should be defined for a given instrumentation.
             */
            boolean isDefineMethod();

            /**
             * The byte code appender to be used for the instrumentation by this entry. Must not
             * be called if {@link com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold.TypeWriter.MethodPool.Entry#isDefineMethod()}
             * returns {@code false}.
             *
             * @return The byte code appender that is responsible for the instrumentation of a method matched for
             * this entry.
             */
            ByteCodeAppender getByteCodeAppender();

            /**
             * The method attribute appender that is to be used for the instrumentation by this entry.  Must not
             * be called if {@link com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold.TypeWriter.MethodPool.Entry#isDefineMethod()}
             * returns {@code false}.
             *
             * @return The method attribute appender that is responsible for the instrumentation of a method matched for
             * this entry.
             */
            MethodAttributeAppender getAttributeAppender();
        }

        /**
         * Looks up a handler entry for a given method.
         *
         * @param methodDescription The method being processed.
         * @return A handler entry for the given method.
         */
        Entry target(MethodDescription methodDescription);
    }

    /**
     * Describes a type writer currently in the general phase, i.e. in the phase before fields or methods
     * are written to the type.
     *
     * @param <T> The best known loaded type for the dynamically created type.
     */
    static interface InGeneralPhase<T> extends TypeWriter<T> {

        /**
         * Writes an attribute to the type that is created by this type writer.
         *
         * @param typeAttributeAppender The type attribute appender to be applied to the type that is represented by
         *                              this type writer.
         * @return This type writer.
         */
        InGeneralPhase<T> attributeType(TypeAttributeAppender typeAttributeAppender);

        /**
         * Moves to the field phase.
         *
         * @return This type writer in its field phase.
         */
        InFieldPhase<T> fields();

        /**
         * Moves to the method phase.
         *
         * @return This type writer in its method phase.
         */
        InMethodPhase<T> methods();
    }

    /**
     * Describes a type writer currently in the field phase, i.e. in the phase before methods are applied but
     * after any type meta information are written to the type.
     *
     * @param <T> The best known loaded type for the dynamically created type.
     */
    static interface InFieldPhase<T> extends TypeWriter<T> {

        /**
         * Adds a number of fields as described by the argument to the type that is created by this type
         * writer where the annotations are received from the given compiled field registry.
         *
         * @param fieldDescriptions The fields to be added to the type that is created by this type writer.
         * @param fieldPool         The field pool that is queried for finding annotations for written fields.
         * @return This type writer.
         */
        InFieldPhase<T> write(Iterable<? extends FieldDescription> fieldDescriptions, FieldPool fieldPool);

        /**
         * Moves to the method phase.
         *
         * @return This type writer in its method phase.
         */
        InMethodPhase<T> methods();
    }

    /**
     * Describes a type writer currently in the method phase, i.e. in the phase after any type meta information
     * is added to a type and after its fields are written.
     *
     * @param <T> The best known loaded type for the dynamically created type.
     */
    static interface InMethodPhase<T> extends TypeWriter<T> {

        /**
         * Adds a number of methods as described by the argument to the type that is created by this type
         * writer where the implementations and annotations are received from the given compiled method registry.
         *
         * @param methodDescriptions The methods to be added to the type that is created by this type writer.
         * @param methodPool         The method pool that is queried for creating implementations for these methods.
         * @return This type writer.
         */
        InMethodPhase<T> write(Iterable<? extends MethodDescription> methodDescriptions, MethodPool methodPool);
    }

    /**
     * A builder that creates a new type writer for given arguments.
     *
     * @param <T> The best known loaded type for the dynamically created type.
     */
    static class Builder<T> {

        private abstract class AbstractTypeWriter<T> implements TypeWriter<T> {

            protected final ClassWriter classWriter;
            protected final ClassVisitor classVisitor;

            protected AbstractTypeWriter(ClassWriter classWriter, ClassVisitor classVisitor) {
                this.classWriter = classWriter;
                this.classVisitor = classVisitor;
            }

            @Override
            public DynamicType.Unloaded<T> make() {
                classVisitor.visitEnd();
                return new DynamicType.Default.Unloaded<T>(instrumentedType.detach(),
                        classWriter.toByteArray(),
                        instrumentedType.getTypeInitializer(),
                        instrumentationContext.getRegisteredAuxiliaryTypes());
            }
        }

        private class GeneralPhaseTypeWriter<T> extends AbstractTypeWriter<T> implements InGeneralPhase<T> {

            private GeneralPhaseTypeWriter(ClassWriter classWriter, ClassVisitor classVisitor) {
                super(classWriter, classVisitor);
            }

            @Override
            public InGeneralPhase<T> attributeType(TypeAttributeAppender typeAttributeAppender) {
                typeAttributeAppender.apply(classVisitor, instrumentedType);
                return this;
            }

            @Override
            public InFieldPhase<T> fields() {
                return new FieldPhaseTypeWriter<T>(classWriter, classVisitor);
            }

            @Override
            public InMethodPhase<T> methods() {
                return new MethodPhaseTypeWriter<T>(classWriter, classVisitor);
            }
        }

        private class FieldPhaseTypeWriter<T> extends AbstractTypeWriter<T> implements InFieldPhase<T> {

            private FieldPhaseTypeWriter(ClassWriter classWriter, ClassVisitor classVisitor) {
                super(classWriter, classVisitor);
            }

            @Override
            public InFieldPhase<T> write(Iterable<? extends FieldDescription> fieldDescriptions, FieldPool fieldPool) {
                for (FieldDescription fieldDescription : fieldDescriptions) {
                    FieldVisitor fieldVisitor = classVisitor.visitField(fieldDescription.getModifiers(),
                            fieldDescription.getInternalName(),
                            fieldDescription.getDescriptor(),
                            null,
                            null);
                    fieldPool.target(fieldDescription)
                            .getFieldAppenderFactory()
                            .make(instrumentedType)
                            .apply(fieldVisitor, fieldDescription);
                    fieldVisitor.visitEnd();
                }
                return this;
            }

            @Override
            public InMethodPhase<T> methods() {
                return new MethodPhaseTypeWriter<T>(classWriter, classVisitor);
            }
        }

        private class MethodPhaseTypeWriter<T> extends AbstractTypeWriter<T> implements InMethodPhase<T> {

            private MethodPhaseTypeWriter(ClassWriter classWriter, ClassVisitor classVisitor) {
                super(classWriter, classVisitor);
            }

            @Override
            public InMethodPhase<T> write(Iterable<? extends MethodDescription> methodDescriptions, MethodPool methodPool) {
                for (MethodDescription methodDescription : methodDescriptions) {
                    MethodPool.Entry entry = methodPool.target(methodDescription);
                    if (entry.isDefineMethod()) {
                        boolean appendsCode = entry.getByteCodeAppender().appendsCode();
                        MethodVisitor methodVisitor = classVisitor.visitMethod(
                                overrideModifiers(methodDescription, appendsCode),
                                methodDescription.getInternalName(),
                                methodDescription.getDescriptor(),
                                null,
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
        }

        private static int overrideModifiers(MethodDescription methodDescription, boolean appendsCode) {
            if (appendsCode && (methodDescription.isAbstract() || methodDescription.isNative())) {
                return methodDescription.getModifiers() & ~MethodManifestation.ABSTRACTION_MASK;
            } else if (!appendsCode && !methodDescription.isAbstract() && !methodDescription.isNative()) {
                return methodDescription.getModifiers() | Opcodes.ACC_ABSTRACT;
            } else {
                return methodDescription.getModifiers();
            }
        }

        private final InstrumentedType instrumentedType;
        private final Instrumentation.Context instrumentationContext;
        private final ClassFormatVersion classFormatVersion;

        /**
         * Creates a new builder for a given instrumented type.
         *
         * @param instrumentedType       The instrumented type to be written.
         * @param instrumentationContext The instrumentation context for this instrumentation.
         * @param classFormatVersion     The class format version for the type that is written.
         */
        public Builder(InstrumentedType instrumentedType,
                       Instrumentation.Context instrumentationContext,
                       ClassFormatVersion classFormatVersion) {
            this.instrumentedType = instrumentedType;
            this.instrumentationContext = instrumentationContext;
            this.classFormatVersion = classFormatVersion;
        }

        /**
         * Creates a new type writer and moves it to its general phase.
         *
         * @param classVisitorWrapper A class visitor wrapper to be applied to the ASM class writing process.
         * @return A new type writer for the given type.
         */
        public InGeneralPhase<T> build(ClassVisitorWrapper classVisitorWrapper) {
            ClassWriter classWriter = new ClassWriter(ASM_MANUAL_FLAG);
            ClassVisitor classVisitor = classVisitorWrapper.wrap(classWriter);
            classVisitor.visit(classFormatVersion.getVersionNumber(),
                    instrumentedType.getModifiers(),
                    instrumentedType.getInternalName(),
                    null,
                    instrumentedType.getSupertype() == null ? null : instrumentedType.getSupertype().getInternalName(),
                    instrumentedType.getInterfaces().toInternalNames());
            return new GeneralPhaseTypeWriter<T>(classWriter, classVisitor);
        }
    }

    /**
     * Creates the dynamic type.
     *
     * @return An unloaded dynamic type that is the outcome of the type writing
     */
    DynamicType.Unloaded<T> make();
}
