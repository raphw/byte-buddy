package net.bytebuddy.test.utility;

import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.pool.TypePool;
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

    private final boolean check;

    public DebuggingWrapper(Writer writer, Printer printer, boolean check) {
        this.check = check;
        printWriter = new PrintWriter(writer);
        this.printer = printer;
    }

    public DebuggingWrapper(OutputStream outputStream, Printer printer, boolean check) {
        this.check = check;
        printWriter = new PrintWriter(outputStream);
        this.printer = printer;
    }

    public static AsmVisitorWrapper makeDefault() {
        return makeDefault(true);
    }

    public static AsmVisitorWrapper makeDefault(boolean check) {
        return new DebuggingWrapper(System.out, new Textifier(), check);
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
    public ClassVisitor wrap(TypeDescription instrumentedType,
                             ClassVisitor classVisitor,
                             Implementation.Context implementationContext,
                             TypePool typePool,
                             int writerFlags,
                             int readerFlags) {
        return check
                ? new CheckClassAdapter(new TraceClassVisitor(classVisitor, printer, printWriter))
                : new TraceClassVisitor(classVisitor, printer, printWriter);
    }
}
