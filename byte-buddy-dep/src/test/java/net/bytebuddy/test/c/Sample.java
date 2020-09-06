package net.bytebuddy.test.c;

import net.bytebuddy.agent.ByteBuddyAgent;
import org.objectweb.asm.*;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

public class Sample {

    public static void main(String[] args) throws Exception {
        ClassReader reader = new ClassReader("net.bytebuddy.test.c.NativeSample");
        ClassWriter writer = new ClassWriter(0);
        reader.accept(new ClassVisitor(Opcodes.ASM8, writer) {

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                if (name.equals("foo")) {
                    super.visitMethod(Opcodes.ACC_PRIVATE | Opcodes.ACC_NATIVE, "$$renamed$$foo", descriptor, signature, exceptions).visitEnd();
                    MethodVisitor visitor = super.visitMethod(Opcodes.ACC_PUBLIC, name, descriptor, signature, exceptions);
                    visitor.visitCode();
                    visitor.visitVarInsn(Opcodes.ALOAD, 0);
                    visitor.visitVarInsn(Opcodes.ILOAD, 1);
                    visitor.visitInsn(Opcodes.ICONST_2);
                    visitor.visitMethodInsn(Opcodes.INVOKESPECIAL, "net/bytebuddy/test/c/NativeSample", "$$renamed$$foo", descriptor, false);
                    visitor.visitInsn(Opcodes.IRETURN);
                    visitor.visitMaxs(3, 3);
                    visitor.visitEnd();
                    return null;
                } else {
                    return super.visitMethod(access, name, descriptor, signature, exceptions);
                }
            }
        }, 0);


        ClassFileTransformer transformer = new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader,
                                    String className,
                                    Class<?> classBeingRedefined,
                                    ProtectionDomain protectionDomain,
                                    byte[] classfileBuffer) {
                if ("net/bytebuddy/test/c/NativeSample".equals(className)) {
                    return writer.toByteArray();
                } else {
                    return null;
                }
            }
        };

        Class.forName("net.bytebuddy.test.c.NativeSample");

        Instrumentation instrumentation = ByteBuddyAgent.install();
        instrumentation.addTransformer(transformer, true);
        instrumentation.setNativeMethodPrefix(transformer, "$$renamed$$");

        instrumentation.retransformClasses(NativeSample.class);

        int result = new NativeSample().foo(42, 1);
        System.out.println(result);
    }

}
