package net.bytebuddy;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.PrintWriter;

public class Foo {

    public static void main(String[] args) throws Exception {
        ClassReader r = new ClassReader(Bar.class.getName());
        r.accept(new TraceClassVisitor(new PrintWriter(System.out)), 0);
    }

    class Bar {

    }
}
