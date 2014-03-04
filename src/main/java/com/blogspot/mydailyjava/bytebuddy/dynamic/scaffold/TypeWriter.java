package com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold;

import com.blogspot.mydailyjava.bytebuddy.ClassFormatVersion;
import com.blogspot.mydailyjava.bytebuddy.asm.ClassVisitorWrapper;
import com.blogspot.mydailyjava.bytebuddy.dynamic.DynamicType;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.Instrumentation;
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
 * <p/>
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
         * @param fieldDescriptions     The fields to be added to the type that is created by this type writer.
         * @param compiledFieldRegistry The field registry that handles the annotations for these fields.
         * @return This type writer.
         */
        InFieldPhase<T> write(Iterable<? extends FieldDescription> fieldDescriptions,
                              FieldRegistry.Compiled compiledFieldRegistry);

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
         * @param methodDescriptions     The methods to be added to the type that is created by this type writer.
         * @param compiledMethodRegistry The method registry that handles the annotations for these methods
         * @return This type writer.
         */
        InMethodPhase<T> write(Iterable<? extends MethodDescription> methodDescriptions,
                               MethodRegistry.Compiled compiledMethodRegistry);
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
                return new DynamicType.Default.Unloaded<T>(instrumentedType.getName(),
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
            public InFieldPhase<T> write(Iterable<? extends FieldDescription> fieldDescriptions,
                                         FieldRegistry.Compiled compiledFieldRegistry) {
                for (FieldDescription fieldDescription : fieldDescriptions) {
                    FieldVisitor fieldVisitor = classVisitor.visitField(fieldDescription.getModifiers(),
                            fieldDescription.getInternalName(),
                            fieldDescription.getDescriptor(),
                            null,
                            null);
                    compiledFieldRegistry.target(fieldDescription)
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
            public InMethodPhase<T> write(Iterable<? extends MethodDescription> methodDescriptions,
                                          MethodRegistry.Compiled compiledMethodRegistry) {
                for (MethodDescription methodDescription : methodDescriptions) {
                    MethodRegistry.Compiled.Entry entry = compiledMethodRegistry.target(methodDescription);
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
