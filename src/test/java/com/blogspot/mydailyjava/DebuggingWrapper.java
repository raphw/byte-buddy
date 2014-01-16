package com.blogspot.mydailyjava;

import com.blogspot.mydailyjava.bytebuddy.asm.ClassVisitorWrapper;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;

@SuppressWarnings("unused")
public class DebuggingWrapper implements ClassVisitorWrapper {

    private final PrintWriter printWriter;

    public DebuggingWrapper(Writer writer) {
        this.printWriter = new PrintWriter(writer);
    }

    public DebuggingWrapper(OutputStream outputStream) {
        this.printWriter = new PrintWriter(outputStream);
    }

    @Override
    public ClassVisitor wrap(ClassVisitor classVisitor) {
        return new TraceClassVisitor(classVisitor, printWriter);
    }
}
