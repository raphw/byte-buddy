package net.bytebuddy.test.utility;

import net.bytebuddy.asm.ClassVisitorWrapper;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;

@SuppressWarnings("unused")
public class DebuggingWrapper implements ClassVisitorWrapper {

    private final PrintWriter printWriter;

    private final Printer printer;

    public DebuggingWrapper(Writer writer, Printer printer) {
        printWriter = new PrintWriter(writer);
        this.printer = printer;
    }

    public DebuggingWrapper(OutputStream outputStream, Printer printer) {
        printWriter = new PrintWriter(outputStream);
        this.printer = printer;
    }

    public static ClassVisitorWrapper makeDefault() {
        return new DebuggingWrapper(System.out, new Textifier());
    }

    @Override
    public ClassVisitor wrap(ClassVisitor classVisitor) {
        return new TraceClassVisitor(classVisitor, printer, printWriter);
    }
}
