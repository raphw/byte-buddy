package net.bytebuddy.test.utility;

import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.description.type.TypeDescription;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;

@SuppressWarnings("unused")
public class DebuggingWrapper implements AsmVisitorWrapper {

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

    public static AsmVisitorWrapper makeDefault() {
        return new DebuggingWrapper(System.out, new Textifier());
    }

    @Override
    public int mergeWriter(int flags) {
        return flags;
    }

    @Override
    public int mergeReader(int flags) {
        return flags;
    }

    @Override
    public ClassVisitor wrap(TypeDescription instrumentedType, ClassVisitor classVisitor, int writerFlags, int readerFlags) {
        return new CheckClassAdapter(new TraceClassVisitor(classVisitor, printer, printWriter));
//        return new TraceClassVisitor(classVisitor, printer, printWriter);
    }
}
