package com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold;

import com.blogspot.mydailyjava.bytebuddy.ClassFormatVersion;
import com.blogspot.mydailyjava.bytebuddy.asm.ClassVisitorWrapper;
import com.blogspot.mydailyjava.bytebuddy.dynamic.DynamicType;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.Instrumentation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.attribute.FieldAttributeAppender;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.attribute.TypeAttributeAppender;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.field.FieldDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.InstrumentedType;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

import java.util.Iterator;
import java.util.List;

public interface TypeWriter<T> {

    static final int ASM_MANUAL_FLAG = 0;

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
                throw new IllegalStateException();
            }
        }

        private final List<? extends S> elements;

        public SameThreadCoModifiableIterable(List<? extends S> elements) {
            this.elements = elements;
        }

        @Override
        public Iterator<S> iterator() {
            return new SameThreadCoModifiableIterator();
        }
    }

    static interface InGeneralPhase<T> extends TypeWriter<T> {

        InGeneralPhase<T> attributeType(TypeAttributeAppender typeAttributeAppender);

        InFieldPhase<T> fields();

        InMethodPhase<T> methods();
    }

    static interface InFieldPhase<T> extends TypeWriter<T> {

        InFieldPhase<T> write(Iterable<? extends FieldDescription> fieldDescriptions,
                              FieldRegistry.Compiled compiledFieldRegistry);

        InMethodPhase<T> methods();
    }

    static interface InMethodPhase<T> extends TypeWriter<T> {

        InMethodPhase<T> write(Iterable<? extends MethodDescription> methodDescriptions,
                               MethodRegistry.Compiled compiledMethodRegistry);
    }

    static class Builder<T> {

        protected abstract class AbstractTypeWriter<T> implements TypeWriter<T> {

            protected final ClassWriter classWriter;
            protected final ClassVisitor classVisitor;

            public AbstractTypeWriter(ClassWriter classWriter, ClassVisitor classVisitor) {
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

        protected class GeneralPhaseTypeWriter<T> extends AbstractTypeWriter<T> implements InGeneralPhase<T> {

            public GeneralPhaseTypeWriter(ClassWriter classWriter, ClassVisitor classVisitor) {
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

        protected class FieldPhaseTypeWriter<T> extends AbstractTypeWriter<T> implements InFieldPhase<T> {

            public FieldPhaseTypeWriter(ClassWriter classWriter, ClassVisitor classVisitor) {
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
                    compiledFieldRegistry.target(fieldDescription, FieldAttributeAppender.NoOp.INSTANCE)
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

        protected class MethodPhaseTypeWriter<T> extends AbstractTypeWriter<T> implements InMethodPhase<T> {

            public MethodPhaseTypeWriter(ClassWriter classWriter, ClassVisitor classVisitor) {
                super(classWriter, classVisitor);
            }

            @Override
            public InMethodPhase<T> write(Iterable<? extends MethodDescription> methodDescriptions,
                                          MethodRegistry.Compiled compiledMethodRegistry) {
                for (MethodDescription methodDescription : methodDescriptions) {
                    MethodVisitor methodVisitor = classVisitor.visitMethod(methodDescription.getModifiers(),
                            methodDescription.getInternalName(),
                            methodDescription.getDescriptor(),
                            null,
                            methodDescription.getExceptionTypes().toInternalNames());
                    MethodRegistry.Compiled.Entry entry = compiledMethodRegistry.target(methodDescription,
                            MethodRegistry.Compiled.Entry.ForAbstractMethod.INSTANCE);
                    entry.getAttributeAppender().apply(methodVisitor, methodDescription);
                    if (entry.getByteCodeAppender().appendsCode()) {
                        methodVisitor.visitCode();
                        entry.getByteCodeAppender().apply(methodVisitor, instrumentationContext, methodDescription);
                    }
                    methodVisitor.visitEnd();
                }
                return this;
            }
        }

        private final InstrumentedType instrumentedType;
        private final Instrumentation.Context instrumentationContext;
        private final ClassFormatVersion classFormatVersion;

        public Builder(InstrumentedType instrumentedType,
                       Instrumentation.Context instrumentationContext,
                       ClassFormatVersion classFormatVersion) {
            this.instrumentedType = instrumentedType;
            this.instrumentationContext = instrumentationContext;
            this.classFormatVersion = classFormatVersion;
        }

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

    DynamicType.Unloaded<T> make();
}
