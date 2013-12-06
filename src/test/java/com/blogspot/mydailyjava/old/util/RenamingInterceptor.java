package com.blogspot.mydailyjava.old.util;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Random;

public class RenamingInterceptor extends ClassVisitor {

    private static final String CONSTRUCTOR_METHOD_NAME = "<init>";

    private static final Random RANDOM = new Random();

    public RenamingInterceptor(ClassVisitor cv) {
        super(Opcodes.ASM4, cv);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, makeName(name), signature, name, interfaces);
    }

    private static String makeName(String name) {
        return name.concat(String.valueOf(RANDOM.nextInt()));
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        if (!CONSTRUCTOR_METHOD_NAME.equals(name)) {
            return cv.visitMethod(access, name, desc, signature, exceptions);
        } else {
            return null;
        }
    }
}
