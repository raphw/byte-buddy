package net.bytebuddy.build.gradle.android.connector.adapter.current.asm.translator;

import net.bytebuddy.utility.OpenedClassReader;
import net.bytebuddy.utility.nullability.MaybeNull;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.ModuleVisitor;
import org.objectweb.asm.RecordComponentVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypePath;

import java.util.HashMap;
import java.util.Map;

public class UnwrappingClassVisitor extends ClassVisitor {

    private final net.bytebuddy.jar.asm.ClassVisitor classVisitor;

    public UnwrappingClassVisitor(net.bytebuddy.jar.asm.ClassVisitor classVisitor) {
        super(OpenedClassReader.ASM_API);
        this.classVisitor = classVisitor;
    }

    @MaybeNull
    public static ClassVisitor of(@MaybeNull net.bytebuddy.jar.asm.ClassVisitor classVisitor) {
        return classVisitor == null ? null : new UnwrappingClassVisitor(classVisitor);
    }

    @Override
    public void visit(int version, int access, String name, @MaybeNull String signature, @MaybeNull String superName, @MaybeNull String[] interfaces) {
        classVisitor.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public void visitSource(@MaybeNull String source, @MaybeNull String debug) {
        classVisitor.visitSource(source, debug);
    }

    @Override
    public ModuleVisitor visitModule(String name, int access, @MaybeNull String version) {
        return WrappingModuleVisitor.of(classVisitor.visitModule(name, access, version));
    }

    @Override
    public void visitNestHost(String nestHost) {
        classVisitor.visitNestHost(nestHost);
    }

    @Override
    public void visitOuterClass(String owner, @MaybeNull String name, @MaybeNull String descriptor) {
        classVisitor.visitOuterClass(owner, name, descriptor);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        return WrappingAnnotationVisitor.of(classVisitor.visitAnnotation(descriptor, visible));
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(int typeRef, @MaybeNull TypePath typePath, String descriptor, boolean visible) {
        return WrappingAnnotationVisitor.of(classVisitor.visitTypeAnnotation(typeRef, typePath == null ? null : net.bytebuddy.jar.asm.TypePath.fromString(typePath.toString()), descriptor, visible));
    }

    @Override
    public void visitAttribute(Attribute attribute) {
        /* do nothing */
    }

    @Override
    public void visitNestMember(String nestMember) {
        classVisitor.visitNestMember(nestMember);
    }

    @Override
    public void visitPermittedSubclass(String permittedSubclass) {
        classVisitor.visitPermittedSubclass(permittedSubclass);
    }

    @Override
    public void visitInnerClass(String name, @MaybeNull String outerName, @MaybeNull String innerName, int access) {
        classVisitor.visitInnerClass(name, outerName, innerName, access);
    }

    @Override
    public RecordComponentVisitor visitRecordComponent(String name, String descriptor, @MaybeNull String signature) {
        return WrappingRecordComponentVisitor.of(classVisitor.visitRecordComponent(name, descriptor, signature));
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, @MaybeNull String signature, Object value) {
        return WrappingFieldVisitor.of(classVisitor.visitField(access, name, descriptor, signature, value));
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, @MaybeNull String signature, @MaybeNull String[] exceptions) {
        return WrappingMethodVisitor.of(classVisitor.visitMethod(access, name, descriptor, signature, exceptions));
    }

    @Override
    public void visitEnd() {
        classVisitor.visitEnd();
    }

    public static class WrappingModuleVisitor extends ModuleVisitor {

        private final net.bytebuddy.jar.asm.ModuleVisitor moduleVisitor;

        protected WrappingModuleVisitor(net.bytebuddy.jar.asm.ModuleVisitor moduleVisitor) {
            super(OpenedClassReader.ASM_API);
            this.moduleVisitor = moduleVisitor;
        }

        @MaybeNull
        public static ModuleVisitor of(@MaybeNull net.bytebuddy.jar.asm.ModuleVisitor moduleVisitor) {
            return moduleVisitor == null ? null : new WrappingModuleVisitor(moduleVisitor);
        }

        @Override
        public void visitMainClass(String mainClass) {
            moduleVisitor.visitMainClass(mainClass);
        }

        @Override
        public void visitPackage(String packaze) {
            super.visitPackage(packaze);
        }

        @Override
        public void visitRequire(String module, int access, @MaybeNull String version) {
            moduleVisitor.visitRequire(module, access, version);
        }

        @Override
        public void visitExport(String packaze, int access, @MaybeNull String... modules) {
            moduleVisitor.visitExport(packaze, access, modules);
        }

        @Override
        public void visitOpen(String packaze, int access, @MaybeNull String... modules) {
            moduleVisitor.visitOpen(packaze, access, modules);
        }

        @Override
        public void visitUse(String service) {
            moduleVisitor.visitUse(service);
        }

        @Override
        public void visitProvide(String service, String... providers) {
            moduleVisitor.visitProvide(service, providers);
        }

        @Override
        public void visitEnd() {
            moduleVisitor.visitEnd();
        }
    }

    public static class WrappingAnnotationVisitor extends AnnotationVisitor {

        private final net.bytebuddy.jar.asm.AnnotationVisitor annotationVisitor;

        protected WrappingAnnotationVisitor(net.bytebuddy.jar.asm.AnnotationVisitor annotationVisitor) {
            super(OpenedClassReader.ASM_API);
            this.annotationVisitor = annotationVisitor;
        }

        @MaybeNull
        public static AnnotationVisitor of(@MaybeNull net.bytebuddy.jar.asm.AnnotationVisitor annotationVisitor) {
            return annotationVisitor == null ? null : new WrappingAnnotationVisitor(annotationVisitor);
        }

        @Override
        public void visit(@MaybeNull String name, Object value) {
            annotationVisitor.visit(name, value);
        }

        @Override
        public void visitEnum(@MaybeNull String name, String descriptor, String value) {
            super.visitEnum(name, descriptor, value);
        }

        @Override
        public AnnotationVisitor visitAnnotation(@MaybeNull String name, String descriptor) {
            return WrappingAnnotationVisitor.of(annotationVisitor.visitAnnotation(name, descriptor));
        }

        @Override
        public AnnotationVisitor visitArray(@MaybeNull String name) {
            return WrappingAnnotationVisitor.of(annotationVisitor.visitArray(name));
        }

        @Override
        public void visitEnd() {
            annotationVisitor.visitEnd();
        }
    }

    public static class WrappingRecordComponentVisitor extends RecordComponentVisitor {

        private final net.bytebuddy.jar.asm.RecordComponentVisitor recordComponentVisitor;

        protected WrappingRecordComponentVisitor(net.bytebuddy.jar.asm.RecordComponentVisitor recordComponentVisitor) {
            super(OpenedClassReader.ASM_API);
            this.recordComponentVisitor = recordComponentVisitor;
        }

        @MaybeNull
        public static RecordComponentVisitor of(@MaybeNull net.bytebuddy.jar.asm.RecordComponentVisitor recordComponentVisitor) {
            return recordComponentVisitor == null ? null : new WrappingRecordComponentVisitor(recordComponentVisitor);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            return WrappingAnnotationVisitor.of(recordComponentVisitor.visitAnnotation(descriptor, visible));
        }

        @Override
        public AnnotationVisitor visitTypeAnnotation(int typeRef, @MaybeNull TypePath typePath, String descriptor, boolean visible) {
            return WrappingAnnotationVisitor.of(recordComponentVisitor.visitTypeAnnotation(typeRef, typePath == null ? null : net.bytebuddy.jar.asm.TypePath.fromString(typePath.toString()), descriptor, visible));
        }

        @Override
        public void visitAttribute(Attribute attribute) {
            /* do nothing */
        }

        @Override
        public void visitEnd() {
            recordComponentVisitor.visitEnd();
        }
    }

    public static class WrappingFieldVisitor extends FieldVisitor {

        private final net.bytebuddy.jar.asm.FieldVisitor fieldVisitor;

        protected WrappingFieldVisitor(net.bytebuddy.jar.asm.FieldVisitor fieldVisitor) {
            super(OpenedClassReader.ASM_API);
            this.fieldVisitor = fieldVisitor;
        }

        @MaybeNull
        public static FieldVisitor of(@MaybeNull net.bytebuddy.jar.asm.FieldVisitor fieldVisitor) {
            return fieldVisitor == null ? null : new WrappingFieldVisitor(fieldVisitor);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            return WrappingAnnotationVisitor.of(fieldVisitor.visitAnnotation(descriptor, visible));
        }

        @Override
        public AnnotationVisitor visitTypeAnnotation(int typeRef, @MaybeNull TypePath typePath, String descriptor, boolean visible) {
            return WrappingAnnotationVisitor.of(fieldVisitor.visitTypeAnnotation(typeRef, typePath == null ? null : net.bytebuddy.jar.asm.TypePath.fromString(typePath.toString()), descriptor, visible));
        }

        @Override
        public void visitAttribute(Attribute attribute) {
            /* do nothing */
        }

        @Override
        public void visitEnd() {
            fieldVisitor.visitEnd();
        }
    }

    public static class WrappingMethodVisitor extends MethodVisitor {

        private final net.bytebuddy.jar.asm.MethodVisitor methodVisitor;

        private final Map<Label, net.bytebuddy.jar.asm.Label> labels = new HashMap<>();

        protected WrappingMethodVisitor(net.bytebuddy.jar.asm.MethodVisitor methodVisitor) {
            super(OpenedClassReader.ASM_API);
            this.methodVisitor = methodVisitor;
        }

        @MaybeNull
        public static MethodVisitor of(@MaybeNull net.bytebuddy.jar.asm.MethodVisitor methodVisitor) {
            return methodVisitor == null ? null : new WrappingMethodVisitor(methodVisitor);
        }

        @Override
        public void visitParameter(@MaybeNull String name, int access) {
            methodVisitor.visitParameter(name, access);
        }

        @Override
        public AnnotationVisitor visitAnnotationDefault() {
            return WrappingAnnotationVisitor.of(methodVisitor.visitAnnotationDefault());
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            return WrappingAnnotationVisitor.of(methodVisitor.visitAnnotation(descriptor, visible));
        }

        @Override
        public AnnotationVisitor visitTypeAnnotation(int typeRef, @MaybeNull TypePath typePath, String descriptor, boolean visible) {
            return WrappingAnnotationVisitor.of(methodVisitor.visitTypeAnnotation(typeRef, typePath == null ? null : net.bytebuddy.jar.asm.TypePath.fromString(typePath.toString()), descriptor, visible));
        }

        @Override
        public void visitAnnotableParameterCount(int parameterCount, boolean visible) {
            methodVisitor.visitAnnotableParameterCount(parameterCount, visible);
        }

        @Override
        public AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor, boolean visible) {
            return WrappingAnnotationVisitor.of(methodVisitor.visitParameterAnnotation(parameter, descriptor, visible));
        }

        @Override
        public void visitAttribute(Attribute attribute) {
            /* do nothing */
        }

        @Override
        public void visitCode() {
            methodVisitor.visitCode();
        }

        @Override
        public void visitFrame(int type, int numLocal, @MaybeNull Object[] local, int numStack, @MaybeNull Object[] stack) {
            methodVisitor.visitFrame(type, numLocal, frames(local), numStack, frames(stack));
        }

        @Override
        public void visitInsn(int opcode) {
            methodVisitor.visitInsn(opcode);
        }

        @Override
        public void visitIntInsn(int opcode, int operand) {
            methodVisitor.visitIntInsn(opcode, operand);
        }

        @Override
        public void visitVarInsn(int opcode, int varIndex) {
            methodVisitor.visitVarInsn(opcode, varIndex);
        }

        @Override
        public void visitTypeInsn(int opcode, String type) {
            methodVisitor.visitTypeInsn(opcode, type);
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, @MaybeNull String descriptor) {
            methodVisitor.visitFieldInsn(opcode, owner, name, descriptor);
        }

        @Override
        @SuppressWarnings("deprecation")
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor) {
            methodVisitor.visitMethodInsn(opcode, owner, name, descriptor);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            methodVisitor.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        }

        @Override
        public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
            methodVisitor.visitInvokeDynamicInsn(name, descriptor, handle(bootstrapMethodHandle), ldc(bootstrapMethodArguments));
        }

        @Override
        public void visitJumpInsn(int opcode, Label label) {
            methodVisitor.visitJumpInsn(opcode, label(label));
        }

        @Override
        public void visitLabel(Label label) {
            methodVisitor.visitLabel(label(label));
        }

        @Override
        public void visitLdcInsn(Object value) {
            methodVisitor.visitLdcInsn(ldc(value));
        }

        @Override
        public void visitIincInsn(int varIndex, int increment) {
            methodVisitor.visitIincInsn(varIndex, increment);
        }

        @Override
        public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
            methodVisitor.visitTableSwitchInsn(min, max, label(dflt), label(labels));
        }

        @Override
        public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
            methodVisitor.visitLookupSwitchInsn(label(dflt), keys, label(labels));
        }

        @Override
        public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
            methodVisitor.visitMultiANewArrayInsn(descriptor, numDimensions);
        }

        @Override
        public AnnotationVisitor visitInsnAnnotation(int typeRef, @MaybeNull TypePath typePath, String descriptor, boolean visible) {
            return WrappingAnnotationVisitor.of(methodVisitor.visitInsnAnnotation(typeRef, typePath == null ? null : net.bytebuddy.jar.asm.TypePath.fromString(typePath.toString()), descriptor, visible));
        }

        @Override
        public void visitTryCatchBlock(Label start, Label end, Label handler, @MaybeNull String type) {
            methodVisitor.visitTryCatchBlock(label(start), label(end), label(handler), type);
        }

        @Override
        public AnnotationVisitor visitTryCatchAnnotation(int typeRef, @MaybeNull TypePath typePath, String descriptor, boolean visible) {
            return WrappingAnnotationVisitor.of(methodVisitor.visitTryCatchAnnotation(typeRef, typePath == null ? null : net.bytebuddy.jar.asm.TypePath.fromString(typePath.toString()), descriptor, visible));
        }

        @Override
        public void visitLocalVariable(@MaybeNull String name, @MaybeNull String descriptor, @MaybeNull String signature, Label start, Label end, int index) {
            methodVisitor.visitLocalVariable(name, descriptor, signature, label(start), label(end), index);
        }

        @Override
        public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, @MaybeNull TypePath typePath, Label[] start, Label[] end, int[] index, String descriptor, boolean visible) {
            return WrappingAnnotationVisitor.of(methodVisitor.visitLocalVariableAnnotation(typeRef, typePath == null ? null : net.bytebuddy.jar.asm.TypePath.fromString(typePath.toString()), label(start), label(end), index, descriptor, visible));
        }

        @Override
        public void visitLineNumber(int line, Label start) {
            methodVisitor.visitLineNumber(line, label(start));
        }

        @Override
        public void visitMaxs(int maxStack, int maxLocals) {
            methodVisitor.visitMaxs(maxStack, maxLocals);
        }

        @Override
        public void visitEnd() {
            methodVisitor.visitEnd();
        }

        private net.bytebuddy.jar.asm.Label[] label(Label[] source) {
            net.bytebuddy.jar.asm.Label[] target = new net.bytebuddy.jar.asm.Label[source.length];
            for (int index = 0; index < source.length; index++) {
                target[index] = label(source[index]);
            }
            return target;
        }

        private net.bytebuddy.jar.asm.Label label(Label source) {
            net.bytebuddy.jar.asm.Label target = labels.get(source);
            if (target == null) {
                target = new net.bytebuddy.jar.asm.Label();
                labels.put(source, target);
            }
            return target;
        }

        @MaybeNull
        private Object[] frames(@MaybeNull Object[] source) {
            if (source == null) {
                return null;
            }
            Object[] target = new Object[source.length];
            for (int index = 0; index < source.length; index++) {
                target[index] = source[index] instanceof Label
                        ? label((Label) source[index])
                        : source[index];
            }
            return target;
        }

        private static net.bytebuddy.jar.asm.Handle handle(Handle handle) {
            return new net.bytebuddy.jar.asm.Handle(handle.getTag(),
                    handle.getOwner(),
                    handle.getName(),
                    handle.getDesc(),
                    handle.isInterface());
        }

        private static net.bytebuddy.jar.asm.ConstantDynamic constantDynamic(ConstantDynamic constantDynamic) {
            Object[] argument = new Object[constantDynamic.getBootstrapMethodArgumentCount()];
            for (int index = 0; index < argument.length; index++) {
                argument[index] = ldc(constantDynamic.getBootstrapMethodArgument(index));
            }
            return new net.bytebuddy.jar.asm.ConstantDynamic(constantDynamic.getName(),
                    constantDynamic.getDescriptor(),
                    handle(constantDynamic.getBootstrapMethod()),
                    argument);
        }

        private static Object ldc(Object source) {
            if (source instanceof Handle) {
                return handle((Handle) source);
            } else if (source instanceof Type) {
                return net.bytebuddy.jar.asm.Type.getType(((Type) source).getDescriptor());
            } else if (source instanceof ConstantDynamic) {
                return constantDynamic((ConstantDynamic) source);
            } else {
                return source;
            }
        }

        private static Object[] ldc(Object[] source) {
            Object[] target = new Object[source.length];
            for (int index = 0; index < target.length; index++) {
                target[index] = ldc(source[index]);
            }
            return target;
        }
    }
}