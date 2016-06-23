package net.bytebuddy;

import net.bytebuddy.agent.ByteBuddyAgent;
import org.objectweb.asm.*;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.AccessControlContext;
import java.security.ProtectionDomain;

public class SecurityDemonstration {

    public static void main(String[] args) throws Exception {
        Instrumentation instrumentation = ByteBuddyAgent.install(); // Simulate external attach.

        System.setSecurityManager(new SecurityManager());

        final long id = Thread.currentThread().getId();

        instrumentation.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader,
                                    String className,
                                    final Class<?> classBeingRedefined,
                                    ProtectionDomain protectionDomain,
                                    byte[] classfileBuffer) throws IllegalClassFormatException {
                if (classBeingRedefined != AccessControlContext.class) {
                    return null;
                }
                try {
                    ClassReader classReader = new ClassReader(classfileBuffer);
                    ClassWriter classWriter = new ClassWriter(classReader, 0);

                    classReader.accept(new ClassVisitor(Opcodes.ASM5, classWriter) {
                        @Override
                        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                            MethodVisitor methodVisitor = super.visitMethod(access, name, desc, signature, exceptions);
                            if (name.equals("checkPermission")) {
//                                methodVisitor = new SingleIdVisitor(methodVisitor, id);
                                methodVisitor = new MultipleIdVisitor(methodVisitor, new long[]{id});
                            }
                            return methodVisitor;
                        }
                    }, 0);

                    return classWriter.toByteArray();
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new IllegalClassFormatException();
                }
            }

        }, true);

        instrumentation.retransformClasses(AccessControlContext.class);

        // The following action causes a security check and should raise an error.
        System.getProperty("foo");

        System.out.println("Hurray!");
    }

    private static class SingleIdVisitor extends MethodVisitor {

        private final long id;

        public SingleIdVisitor(MethodVisitor mv, long id) {
            super(Opcodes.ASM5, mv);
            this.id = id;
        }

        @Override
        public void visitCode() {
            super.visitCode();
            super.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;", false);
            super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Thread", "getId", "()J", false);
            super.visitLdcInsn(id);
            super.visitInsn(Opcodes.LCMP);
            Label skipShortCut = new Label();
            super.visitJumpInsn(Opcodes.IFNE, skipShortCut);
            super.visitInsn(Opcodes.RETURN);
            super.visitLabel(skipShortCut);
            super.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        }

        @Override
        public void visitMaxs(int maxStack, int maxLocals) {
            super.visitMaxs(Math.max(4, maxStack), maxLocals);
        }
    }

    private static class MultipleIdVisitor extends MethodVisitor {

        private final long[] id;

        public MultipleIdVisitor(MethodVisitor mv, long[] id) {
            super(Opcodes.ASM5, mv);
            this.id = id;
        }

        @Override
        public void visitCode() {
            super.visitCode();

            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;", false);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Thread", "getId", "()J", false);
            mv.visitVarInsn(Opcodes.LSTORE, 2);
            mv.visitInsn(Opcodes.ICONST_3);
            mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_LONG);
            int index = 0;
            for (long id : this.id) {
                mv.visitInsn(Opcodes.DUP);
                mv.visitLdcInsn(index++);
                mv.visitLdcInsn(id);
                mv.visitInsn(Opcodes.LASTORE);
            }
            mv.visitVarInsn(Opcodes.ASTORE, 4);
            mv.visitVarInsn(Opcodes.ALOAD, 4);
            mv.visitInsn(Opcodes.ARRAYLENGTH);
            mv.visitVarInsn(Opcodes.ISTORE, 5);
            mv.visitInsn(Opcodes.ICONST_0);
            mv.visitVarInsn(Opcodes.ISTORE, 6);
            Label startOfLoop = new Label();
            mv.visitLabel(startOfLoop);
            mv.visitFrame(Opcodes.F_FULL, 6, new Object[]{"java/security/AccessControlContext", "java/security/Permission", Opcodes.LONG, "[J", Opcodes.INTEGER, Opcodes.INTEGER}, 0, new Object[]{});
            mv.visitVarInsn(Opcodes.ILOAD, 6);
            mv.visitVarInsn(Opcodes.ILOAD, 5);
            Label endOfLoop = new Label();
            mv.visitJumpInsn(Opcodes.IF_ICMPGE, endOfLoop);
            mv.visitVarInsn(Opcodes.ALOAD, 4);
            mv.visitVarInsn(Opcodes.ILOAD, 6);
            mv.visitInsn(Opcodes.LALOAD);
            mv.visitVarInsn(Opcodes.LLOAD, 2);
            mv.visitInsn(Opcodes.LCMP);
            Label noWhiteList = new Label();
            mv.visitJumpInsn(Opcodes.IFNE, noWhiteList);
            mv.visitInsn(Opcodes.RETURN);
            mv.visitLabel(noWhiteList);
            mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            mv.visitIincInsn(6, 1);
            mv.visitJumpInsn(Opcodes.GOTO, startOfLoop);
            mv.visitLabel(endOfLoop);
            mv.visitFrame(Opcodes.F_FULL, 2, new Object[]{"java/security/AccessControlContext", "java/security/Permission"}, 0, new Object[]{});
        }

        @Override
        public void visitMaxs(int maxStack, int maxLocals) {
            super.visitMaxs(Math.max(5, maxStack), Math.max(7, maxLocals));
        }
    }
}