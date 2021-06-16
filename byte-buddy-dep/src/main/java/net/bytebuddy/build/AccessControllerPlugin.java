package net.bytebuddy.build;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.modifier.FieldManifestation;
import net.bytebuddy.description.modifier.Ownership;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.utility.OpenedClassReader;
import org.objectweb.asm.*;

import java.lang.annotation.*;
import java.security.PrivilegedAction;
import java.util.HashSet;
import java.util.Set;

import static net.bytebuddy.matcher.ElementMatchers.*;

@HashCodeAndEqualsPlugin.Enhance
public class AccessControllerPlugin extends Plugin.ForElementMatcher implements Plugin.Factory {

    private static final String ACCESS_CONTROLLER = "java.security.AccessController";

    private static final String NAME = "ACCESS_CONTROLLER";

    private static final Object[] EMPTY = new Object[0];

    private static final Set<MethodDescription.SignatureToken> SIGNATURES;

    static {
        SIGNATURES = new HashSet<>();
        SIGNATURES.add(new MethodDescription.SignatureToken("doPrivileged", TypeDescription.OBJECT, TypeDescription.ForLoadedType.of(PrivilegedAction.class)));
    }

    private final String property;

    public AccessControllerPlugin() {
        this(null);
    }

    public AccessControllerPlugin(String property) {
        super(declaresMethod(isAnnotatedWith(Enhance.class)));
        this.property = property;
    }

    /**
     * {@inheritDoc}
     */
    public Plugin make() {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public DynamicType.Builder<?> apply(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassFileLocator classFileLocator) {
        String name = NAME;
        while (!typeDescription.getDeclaredFields().filter(named(name)).isEmpty()) {
            name += "$";
        }
        return builder
                .defineField(name, boolean.class, Visibility.PRIVATE, Ownership.STATIC, FieldManifestation.FINAL)
                .visit(new AsmVisitorWrapper.ForDeclaredMethods().method(isAnnotatedWith(Enhance.class), new AccessControlWrapper(name)))
                .initializer(property == null
                        ? new Initializer.WithoutProperty(typeDescription, name)
                        : new Initializer.WithProperty(typeDescription, name, property));
    }

    /**
     * {@inheritDoc}
     */
    public void close() {
        /* do nothing */
    }

    protected abstract static class Initializer implements ByteCodeAppender {

        private final TypeDescription instrumentedType;

        private final String name;

        protected Initializer(TypeDescription instrumentedType, String name) {
            this.instrumentedType = instrumentedType;
            this.name = name;
        }

        /**
         * {@inheritDoc}
         */
        public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext, MethodDescription instrumentedMethod) {
            Label start = new Label(), end = new Label(), classNotFound = new Label(), securityException = new Label(), complete = new Label();
            methodVisitor.visitTryCatchBlock(start, end, classNotFound, Type.getInternalName(ClassNotFoundException.class));
            methodVisitor.visitTryCatchBlock(start, end, securityException, Type.getInternalName(SecurityException.class));
            methodVisitor.visitLabel(start);
            methodVisitor.visitLdcInsn(ACCESS_CONTROLLER);
            methodVisitor.visitInsn(Opcodes.ICONST_0);
            methodVisitor.visitInsn(Opcodes.ACONST_NULL);
            methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC,
                    Type.getInternalName(Class.class),
                    "forName",
                    Type.getMethodDescriptor(Type.getType(Class.class),
                            Type.getType(String.class),
                            Type.getType(boolean.class),
                            Type.getType(ClassLoader.class)),
                    false);
            methodVisitor.visitInsn(Opcodes.POP);
            int size = onAccessController(methodVisitor);
            methodVisitor.visitFieldInsn(Opcodes.PUTSTATIC, instrumentedType.getInternalName(), name, Type.getDescriptor(boolean.class));
            methodVisitor.visitLabel(end);
            methodVisitor.visitJumpInsn(Opcodes.GOTO, complete);
            methodVisitor.visitLabel(classNotFound);
            if (implementationContext.getClassFileVersion().isAtLeast(ClassFileVersion.JAVA_V6)) {
                methodVisitor.visitFrame(Opcodes.F_SAME1, EMPTY.length, EMPTY, 1, new Object[]{Type.getInternalName(ClassNotFoundException.class)});
            }
            methodVisitor.visitInsn(Opcodes.POP);
            methodVisitor.visitInsn(Opcodes.ICONST_0);
            methodVisitor.visitFieldInsn(Opcodes.PUTSTATIC, instrumentedType.getInternalName(), name, Type.getDescriptor(boolean.class));
            methodVisitor.visitJumpInsn(Opcodes.GOTO, complete);
            methodVisitor.visitLabel(securityException);
            if (implementationContext.getClassFileVersion().isAtLeast(ClassFileVersion.JAVA_V6)) {
                methodVisitor.visitFrame(Opcodes.F_SAME1, EMPTY.length, EMPTY, 1, new Object[]{Type.getInternalName(SecurityException.class)});
            }
            methodVisitor.visitInsn(Opcodes.POP);
            methodVisitor.visitInsn(Opcodes.ICONST_1);
            methodVisitor.visitFieldInsn(Opcodes.PUTSTATIC, instrumentedType.getInternalName(), name, Type.getDescriptor(boolean.class));
            methodVisitor.visitLabel(complete);
            if (implementationContext.getClassFileVersion().isAtLeast(ClassFileVersion.JAVA_V6)) {
                methodVisitor.visitFrame(Opcodes.F_SAME, EMPTY.length, EMPTY, EMPTY.length, EMPTY);
            }
            return new Size(Math.max(3, size), 0);
        }

        protected abstract int onAccessController(MethodVisitor methodVisitor);

        protected static class WithProperty extends Initializer {

            private final String property;

            protected WithProperty(TypeDescription instrumentedType, String field, String property) {
                super(instrumentedType, field);
                this.property = property;
            }

            @Override
            protected int onAccessController(MethodVisitor methodVisitor) {
                methodVisitor.visitLdcInsn(property);
                methodVisitor.visitLdcInsn("true");
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC,
                        Type.getInternalName(System.class),
                        "getProperty",
                        Type.getMethodDescriptor(Type.getType(String.class), Type.getType(String.class), Type.getType(String.class)),
                        false);
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC,
                        Type.getInternalName(Boolean.class),
                        "parseBoolean",
                        Type.getMethodDescriptor(Type.getType(boolean.class), Type.getType(String.class)),
                        false);
                return 2;
            }
        }

        protected static class WithoutProperty extends Initializer {

            protected WithoutProperty(TypeDescription instrumentedType, String name) {
                super(instrumentedType, name);
            }

            @Override
            protected int onAccessController(MethodVisitor methodVisitor) {
                methodVisitor.visitInsn(Opcodes.ICONST_1);
                return 1;
            }
        }
    }

    protected static class AccessControlWrapper implements AsmVisitorWrapper.ForDeclaredMethods.MethodVisitorWrapper {

        private final String name;

        protected AccessControlWrapper(String name) {
            this.name = name;
        }

        /**
         * {@inheritDoc}
         */
        public MethodVisitor wrap(TypeDescription instrumentedType,
                                  MethodDescription instrumentedMethod,
                                  MethodVisitor methodVisitor,
                                  Implementation.Context implementationContext,
                                  TypePool typePool,
                                  int writerFlags,
                                  int readerFlags) {
            if (!SIGNATURES.contains(instrumentedMethod.asDefined().asSignatureToken())) {
                throw new IllegalStateException();
            } else if (instrumentedMethod.isPublic() || instrumentedMethod.isProtected()) {
                throw new IllegalStateException();
            }
            return new PrefixingMethodVisitor(methodVisitor,
                    instrumentedType,
                    instrumentedMethod.asDefined(),
                    name,
                    (writerFlags & ClassWriter.COMPUTE_FRAMES) == 0 && implementationContext.getClassFileVersion().isAtLeast(ClassFileVersion.JAVA_V6));
        }

        protected static class PrefixingMethodVisitor extends MethodVisitor {

            private final TypeDescription instrumentedType;

            private final MethodDescription.InDefinedShape instrumentedMethod;

            private final String name;

            private final boolean frames;

            protected PrefixingMethodVisitor(MethodVisitor methodVisitor,
                                             TypeDescription instrumentedType,
                                             MethodDescription.InDefinedShape instrumentedMethod,
                                             String name,
                                             boolean frames) {
                super(OpenedClassReader.ASM_API, methodVisitor);
                this.instrumentedType = instrumentedType;
                this.instrumentedMethod = instrumentedMethod;
                this.name = name;
                this.frames = frames;
            }

            @Override
            public void visitCode() {
                mv.visitCode();
                mv.visitFieldInsn(Opcodes.GETSTATIC, instrumentedType.getInternalName(), name, Type.getDescriptor(boolean.class));
                Label label = new Label();
                mv.visitJumpInsn(Opcodes.IFEQ, label);
                for (ParameterDescription parameterDescription : instrumentedMethod.getParameters()) {
                    mv.visitVarInsn(Type.getType(parameterDescription.getType().asErasure().getDescriptor()).getOpcode(Opcodes.ILOAD),
                            parameterDescription.getOffset());
                }
                mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                        ACCESS_CONTROLLER.replace('.', '/'),
                        instrumentedMethod.getName(),
                        instrumentedMethod.getDescriptor(),
                        false);
                mv.visitInsn(Type.getType(instrumentedMethod.getReturnType().asErasure().getDescriptor()).getOpcode(Opcodes.IRETURN));
                mv.visitLabel(label);
                if (frames) {
                    mv.visitFrame(Opcodes.F_SAME, EMPTY.length, EMPTY, EMPTY.length, EMPTY);
                }
            }

            @Override
            public void visitMaxs(int size, int length) { // TODO
                mv.visitMaxs(Math.max(Math.max(instrumentedMethod.getStackSize(), instrumentedMethod.getReturnType().getStackSize().getSize()), size), length);
            }
        }
    }

    @Documented
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Enhance {

    }
}
