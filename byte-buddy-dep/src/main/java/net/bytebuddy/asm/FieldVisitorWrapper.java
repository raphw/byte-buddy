package net.bytebuddy.asm;

import org.objectweb.asm.FieldVisitor;

public interface FieldVisitorWrapper {

    FieldVisitor wrap(FieldVisitor fieldVisitor);
}
