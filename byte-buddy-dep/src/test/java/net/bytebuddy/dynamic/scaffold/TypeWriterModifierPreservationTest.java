package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.test.scope.EnclosingType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import net.bytebuddy.utility.OpenedClassReader;

import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class TypeWriterModifierPreservationTest {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {Object.class},
                {String.class},
                {EnclosingType.class},
                {new EnclosingType().localMethod},
                {new EnclosingType().anonymousMethod},
                {new EnclosingType().localConstructor},
                {new EnclosingType().anonymousConstructor},
                {EnclosingType.LOCAL_INITIALIZER},
                {EnclosingType.ANONYMOUS_INITIALIZER},
                {EnclosingType.LOCAL_METHOD},
                {EnclosingType.ANONYMOUS_METHOD},
                {EnclosingType.INNER},
                {EnclosingType.NESTED},
                {EnclosingType.PRIVATE_INNER},
                {EnclosingType.PRIVATE_NESTED},
                {EnclosingType.PROTECTED_INNER},
                {EnclosingType.PROTECTED_NESTED},
                {EnclosingType.PACKAGE_INNER},
                {EnclosingType.PACKAGE_NESTED},
                {EnclosingType.FINAL_NESTED},
                {EnclosingType.FINAL_INNER},
                {EnclosingType.DEPRECATED}
        });
    }

    private final Class<?> type;

    public TypeWriterModifierPreservationTest(Class<?> type) {
        this.type = type;
    }

    @Test
    public void testModifiers() throws Exception {
        TypeModifierExtractor typeModifierExtractor = new TypeModifierExtractor();
        OpenedClassReader.of(ClassFileLocator.ForClassLoader.read(type).resolve()).accept(typeModifierExtractor, 0);
        new ByteBuddy()
                .redefine(type)
                .visit(new TypeValidator.Wrapper(typeModifierExtractor))
                .make();
    }

    private static class TypeModifierExtractor extends ClassVisitor {

        private String name;

        public int modifiers, inner;

        public TypeModifierExtractor() {
            super(Opcodes.ASM6);
        }

        @Override
        public void visit(int version, int modifiers, String name, String signature, String superName, String[] interfaceName) {
            this.modifiers = modifiers;
            this.name = name;
        }

        @Override
        public void visitInnerClass(String name, String outerName, String innerName, int modifiers) {
            if (name.equals(this.name)) {
                inner = modifiers;
            }
        }
    }

    private static class TypeValidator extends ClassVisitor {

        private String name;

        public final int modifiers, inner;

        public TypeValidator(ClassVisitor classVisitor, int modifiers, int inner) {
            super(Opcodes.ASM6, classVisitor);
            this.modifiers = modifiers;
            this.inner = inner;
        }

        @Override
        public void visit(int version, int modifiers, String name, String signature, String superName, String[] interfaceName) {
            this.name = name;
            if (modifiers != this.modifiers) {
                throw new AssertionError("Unexpected modifiers: Observed " + modifiers + " instead of " + this.modifiers);
            }
            super.visit(version, modifiers, name, signature, superName, interfaceName);
        }

        @Override
        public void visitInnerClass(String name, String outerName, String innerName, int modifiers) {
            if (name.equals(this.name) && modifiers != inner) {
                throw new AssertionError("Unexpected inner modifiers: Observed " + modifiers + " instead of " + inner);
            }
            super.visitInnerClass(name, outerName, innerName, modifiers);
        }

        private static class Wrapper extends AsmVisitorWrapper.AbstractBase {

            public final int modifiers, inner;

            public Wrapper(TypeModifierExtractor typeModifierExtractor) {
                modifiers = typeModifierExtractor.modifiers;
                inner = typeModifierExtractor.inner;
            }

            @Override
            public ClassVisitor wrap(TypeDescription instrumentedType,
                                     ClassVisitor classVisitor,
                                     Implementation.Context implementationContext,
                                     TypePool typePool,
                                     FieldList<FieldDescription.InDefinedShape> fields,
                                     MethodList<?> methods,
                                     int writerFlags,
                                     int readerFlags) {
                return new TypeValidator(classVisitor, modifiers, inner);
            }
        }
    }
}
