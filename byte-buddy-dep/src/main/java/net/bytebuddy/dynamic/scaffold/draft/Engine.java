package net.bytebuddy.dynamic.scaffold.draft;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.asm.ClassVisitorWrapper;
import net.bytebuddy.dynamic.scaffold.TypeWriter;
import net.bytebuddy.dynamic.scaffold.inline.ClassFileLocator;
import net.bytebuddy.dynamic.scaffold.inline.MethodFlatteningResolver;
import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.attribute.TypeAttributeAppender;
import net.bytebuddy.instrumentation.field.FieldDescription;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender;
import net.bytebuddy.instrumentation.type.TypeDescription;
import org.objectweb.asm.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface Engine {

    static final int ASM_MANUAL_FLAG = 0;

    static final int ASM_API_VERSION = Opcodes.ASM5;

    byte[] create(Instrumentation.Context instrumentationContext);

    static class ForRedefinition implements Engine {

        // TODO: Class initializer

        public static interface InputStreamProvider {

            static class ForClassFileLocator implements InputStreamProvider {

                private final TypeDescription originalType;

                private final ClassFileLocator classFileLocator;

                public ForClassFileLocator(TypeDescription originalType, ClassFileLocator classFileLocator) {
                    this.originalType = originalType;
                    this.classFileLocator = classFileLocator;
                }

                @Override
                public InputStream create() {
                    return classFileLocator.classFileFor(originalType);
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && classFileLocator.equals(((ForClassFileLocator) other).classFileLocator)
                            && originalType.equals(((ForClassFileLocator) other).originalType);
                }

                @Override
                public int hashCode() {
                    int result = originalType.hashCode();
                    result = 31 * result + classFileLocator.hashCode();
                    return result;
                }

                @Override
                public String toString() {
                    return "Engine.ForRedefinition.InputStreamProvider.ForClassFileLocator{" +
                            "originalType=" + originalType +
                            ", classFileLocator=" + classFileLocator +
                            '}';
                }
            }

            InputStream create();
        }

        private final TypeDescription instrumentedType;
        private final ClassFileVersion classFileVersion;
        private final List<? extends MethodDescription> invokableMethods;
        private final ClassVisitorWrapper classVisitorWrapper;
        private final TypeAttributeAppender attributeAppender;
        private final TypeWriter.FieldPool fieldPool;
        private final TypeWriter.MethodPool methodPool;
        private final InputStreamProvider inputStreamProvider;
        private final MethodFlatteningResolver methodFlatteningResolver;

        public ForRedefinition(TypeDescription instrumentedType,
                               ClassFileVersion classFileVersion,
                               List<? extends MethodDescription> invokableMethods,
                               ClassVisitorWrapper classVisitorWrapper,
                               TypeAttributeAppender attributeAppender,
                               TypeWriter.FieldPool fieldPool,
                               TypeWriter.MethodPool methodPool,
                               InputStreamProvider inputStreamProvider,
                               MethodFlatteningResolver methodFlatteningResolver) {
            this.instrumentedType = instrumentedType;
            this.classFileVersion = classFileVersion;
            this.invokableMethods = invokableMethods;
            this.classVisitorWrapper = classVisitorWrapper;
            this.attributeAppender = attributeAppender;
            this.fieldPool = fieldPool;
            this.methodPool = methodPool;
            this.inputStreamProvider = inputStreamProvider;
            this.methodFlatteningResolver = methodFlatteningResolver;
        }

        @Override
        public byte[] create(Instrumentation.Context instrumentationContext) {
            InputStream classFile = inputStreamProvider.create();
            try {
                try {
                    return doCreate(instrumentationContext, classFile);
                } finally {
                    classFile.close();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private byte[] doCreate(Instrumentation.Context instrumentationContext,
                                InputStream classFile) throws IOException {
            ClassReader classReader = new ClassReader(classFile);
            ClassWriter classWriter = new ClassWriter(classReader, ASM_MANUAL_FLAG);
            classReader.accept(new RedefinitionClassVisitor(classVisitorWrapper.wrap(classWriter),
                    instrumentationContext), ASM_MANUAL_FLAG);
            return classWriter.toByteArray();
        }

        protected class RedefinitionClassVisitor extends ClassVisitor {

            private final Instrumentation.Context instrumentationContext;

            private final Map<String, FieldDescription> declaredFields;
            private final Map<String, MethodDescription> declarableMethods;

            public RedefinitionClassVisitor(ClassVisitor classVisitor,
                                            Instrumentation.Context instrumentationContext) {
                super(ASM_API_VERSION, classVisitor);
                this.instrumentationContext = instrumentationContext;
                List<? extends FieldDescription> fieldDescriptions = instrumentedType.getDeclaredFields();
                declaredFields = new HashMap<String, FieldDescription>(fieldDescriptions.size());
                for (FieldDescription fieldDescription : fieldDescriptions) {
                    declaredFields.put(fieldDescription.getInternalName(), fieldDescription);
                }
                declarableMethods = new HashMap<String, MethodDescription>(invokableMethods.size());
                for (MethodDescription methodDescription : invokableMethods) {
                    declarableMethods.put(methodDescription.getUniqueSignature(), methodDescription);
                }
            }

            @Override
            public void visit(int classFileVersionNumber,
                              int modifiers,
                              String internalName,
                              String genericSignature,
                              String superTypeInternalName,
                              String[] interfaceTypeInternalName) {
                super.visit(classFileVersion.getVersionNumber(), // TODO: Make optional override
                        instrumentedType.getActualModifiers(),
                        instrumentedType.getInternalName(),
                        instrumentedType.getGenericSignature(),
                        instrumentedType.getSupertype() == null ? null : instrumentedType.getSupertype().getInternalName(),
                        instrumentedType.getInterfaces().toInternalNames());
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
                MethodVisitor actualMethodVisitor = super.visitMethod(modifiers,
                        internalName,
                        descriptor,
                        genericSignature,
                        exceptionTypeInternalName);
                MethodDescription methodDescription = declarableMethods.remove(internalName + descriptor);
                return methodDescription == null
                        ? actualMethodVisitor
                        : redefine(methodDescription, actualMethodVisitor, (modifiers & Opcodes.ACC_ABSTRACT) != 0);
            }

            private MethodVisitor redefine(MethodDescription methodDescription,
                                           MethodVisitor actualMethodVisitor,
                                           boolean nonAbstractOrigin) {
                TypeWriter.MethodPool.Entry entry = methodPool.target(methodDescription);
                entry.getAttributeAppender().apply(actualMethodVisitor, methodDescription);
                MethodFlatteningResolver.Resolution resolution = methodFlatteningResolver.resolve(methodDescription);
                return nonAbstractOrigin && resolution.isRedefined()
                        ? new CodePreservingMethodVisitor(actualMethodVisitor, entry.getByteCodeAppender(), methodDescription, this, resolution.getResolvedMethod())
                        : new AttributeObtainingMethodVisitor(actualMethodVisitor, entry.getByteCodeAppender(), methodDescription);
            }

            @Override
            public void visitEnd() {
                for (FieldDescription fieldDescription : declaredFields.values()) {
                    fieldPool.target(fieldDescription).apply(cv, fieldDescription);
                }
                for (MethodDescription methodDescription : declarableMethods.values()) {
                    methodPool.target(methodDescription).apply(cv, instrumentationContext, methodDescription);
                }
                // TODO: Apply instrumentation context methods and fields.
                super.visitEnd();
            }

            private class CodePreservingMethodVisitor extends MethodVisitor {

                private final MethodVisitor actualMethodVisitor;

                private final ByteCodeAppender byteCodeAppender;

                private final MethodDescription methodDescription;

                private final ClassVisitor classVisitor;

                private final MethodDescription redirectionMethod;

                private CodePreservingMethodVisitor(MethodVisitor actualMethodVisitor,
                                                    ByteCodeAppender byteCodeAppender,
                                                    MethodDescription methodDescription,
                                                    ClassVisitor classVisitor,
                                                    MethodDescription redirectionMethod) {
                    super(ASM_API_VERSION, actualMethodVisitor);
                    this.actualMethodVisitor = actualMethodVisitor;
                    this.byteCodeAppender = byteCodeAppender;
                    this.methodDescription = methodDescription;
                    this.classVisitor = classVisitor;
                    this.redirectionMethod = redirectionMethod;
                }

                @Override
                public void visitCode() {
                    if (byteCodeAppender.appendsCode()) {
                        actualMethodVisitor.visitCode();
                        ByteCodeAppender.Size size = byteCodeAppender.apply(actualMethodVisitor,
                                instrumentationContext,
                                methodDescription);
                        actualMethodVisitor.visitMaxs(size.getOperandStackSize(), size.getLocalVariableSize());
                    }
                    actualMethodVisitor.visitEnd();
                    mv = classVisitor.visitMethod(redirectionMethod.getModifiers(),
                            redirectionMethod.getInternalName(),
                            redirectionMethod.getDescriptor(),
                            redirectionMethod.getGenericSignature(),
                            redirectionMethod.getExceptionTypes().toInternalNames());
                    super.visitCode();
                }
            }

            private class AttributeObtainingMethodVisitor extends MethodVisitor {

                private final MethodVisitor actualMethodVisitor;

                private final ByteCodeAppender byteCodeAppender;

                private final MethodDescription methodDescription;

                public AttributeObtainingMethodVisitor(MethodVisitor actualMethodVisitor,
                                                       ByteCodeAppender byteCodeAppender,
                                                       MethodDescription methodDescription) {
                    super(ASM_API_VERSION, actualMethodVisitor);
                    this.actualMethodVisitor = actualMethodVisitor;
                    this.byteCodeAppender = byteCodeAppender;
                    this.methodDescription = methodDescription;
                }

                @Override
                public void visitCode() {
                    cv = null; // Ignore byte code instructions, if existent.
                }

                @Override
                public void visitEnd() {
                    if (byteCodeAppender.appendsCode()) {
                        actualMethodVisitor.visitCode();
                        ByteCodeAppender.Size size = byteCodeAppender.apply(actualMethodVisitor,
                                instrumentationContext,
                                methodDescription);
                        actualMethodVisitor.visitMaxs(size.getOperandStackSize(), size.getLocalVariableSize());
                    }
                    actualMethodVisitor.visitEnd();
                }
            }
        }
    }

    static class ForCreation implements Engine {

        // TODO: Class initializer

        private final TypeDescription instrumentedType;
        private final ClassFileVersion classFileVersion;
        private final List<? extends MethodDescription> invokableMethods;
        private final ClassVisitorWrapper classVisitorWrapper;
        private final TypeAttributeAppender attributeAppender;
        private final TypeWriter.FieldPool fieldPool;
        private final TypeWriter.MethodPool methodPool;

        public ForCreation(TypeDescription instrumentedType,
                           ClassFileVersion classFileVersion,
                           List<? extends MethodDescription> invokableMethods,
                           ClassVisitorWrapper classVisitorWrapper,
                           TypeAttributeAppender attributeAppender,
                           TypeWriter.FieldPool fieldPool,
                           TypeWriter.MethodPool methodPool) {
            this.instrumentedType = instrumentedType;
            this.classFileVersion = classFileVersion;
            this.invokableMethods = invokableMethods;
            this.classVisitorWrapper = classVisitorWrapper;
            this.attributeAppender = attributeAppender;
            this.fieldPool = fieldPool;
            this.methodPool = methodPool;
        }

        @Override
        public byte[] create(Instrumentation.Context instrumentationContext) {
            ClassWriter classWriter = new ClassWriter(ASM_MANUAL_FLAG);
            classWriter.visit(classFileVersion.getVersionNumber(),
                    instrumentedType.getActualModifiers(),
                    instrumentedType.getInternalName(),
                    instrumentedType.getGenericSignature(),
                    instrumentedType.getSupertype().getInternalName(),
                    instrumentedType.getInterfaces().toInternalNames());
            ClassVisitor classVisitor = classVisitorWrapper.wrap(classWriter);
            attributeAppender.apply(classVisitor, instrumentedType);
            for (FieldDescription fieldDescription : instrumentedType.getDeclaredFields()) {
                fieldPool.target(fieldDescription).apply(classVisitor, fieldDescription);
            }
            for (MethodDescription methodDescription : invokableMethods) {
                methodPool.target(methodDescription).apply(classVisitor, instrumentationContext, methodDescription);
            }
            // TODO: Appy instrumentation context methods and fields.
            classVisitor.visitEnd();
            return classWriter.toByteArray();
        }
    }
}
