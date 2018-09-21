package net.bytebuddy.asm;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.modifier.TypeManifestation;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.utility.OpenedClassReader;
import org.junit.Test;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Type;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static net.bytebuddy.matcher.ElementMatchers.is;

public class TypeReferenceAdjustmentTest {

    private static final String FOO = "foo";

    @Test
    public void testSuperClass() {
        new ByteBuddy()
                .subclass(Foo.Bar.class)
                .visit(new AssertionVisitorWrapper(false, Foo.class))
                .visit(TypeReferenceAdjustment.strict())
                .make();
    }

    @Test
    public void testInterface() {
        new ByteBuddy()
                .subclass(Qux.Baz.class)
                .visit(new AssertionVisitorWrapper(false, Qux.class))
                .visit(TypeReferenceAdjustment.strict())
                .make();
    }

    @Test
    public void testAnnotation() {
        new ByteBuddy()
                .subclass(Object.class)
                .annotateType(AnnotationDescription.Builder.ofType(Qux.class).build())
                .visit(new AssertionVisitorWrapper(false, Qux.class))
                .visit(TypeReferenceAdjustment.strict())
                .make();
    }

    @Test
    public void testFieldType() {
        new ByteBuddy()
                .subclass(Object.class)
                .defineField(FOO, Foo.class)
                .visit(new AssertionVisitorWrapper(false, Foo.class))
                .visit(TypeReferenceAdjustment.strict())
                .make();
    }

    @Test
    public void testFieldAnnotationType() {
        new ByteBuddy()
                .subclass(Object.class)
                .modifiers(TypeManifestation.ABSTRACT)
                .defineField(FOO, Foo.class)
                .annotateField(AnnotationDescription.Builder.ofType(Qux.class).build())
                .visit(new AssertionVisitorWrapper(false, Qux.class))
                .visit(TypeReferenceAdjustment.strict())
                .make();
    }

    @Test
    public void testMethodReturnType() {
        new ByteBuddy()
                .subclass(Object.class)
                .modifiers(TypeManifestation.ABSTRACT)
                .defineMethod(FOO, Foo.class)
                .withoutCode()
                .visit(new AssertionVisitorWrapper(false, Foo.class))
                .visit(TypeReferenceAdjustment.strict())
                .make();
    }

    @Test
    public void testMethodParameterType() {
        new ByteBuddy()
                .subclass(Object.class)
                .modifiers(TypeManifestation.ABSTRACT)
                .defineMethod(FOO, void.class)
                .withParameters(Foo.class)
                .withoutCode()
                .visit(new AssertionVisitorWrapper(false, Foo.class))
                .visit(TypeReferenceAdjustment.strict())
                .make();
    }

    @Test
    public void testMethodAnnotationType() {
        new ByteBuddy()
                .subclass(Object.class)
                .modifiers(TypeManifestation.ABSTRACT)
                .defineMethod(FOO, void.class)
                .withoutCode()
                .annotateMethod(AnnotationDescription.Builder.ofType(Qux.class).build())
                .visit(new AssertionVisitorWrapper(false, Qux.class))
                .visit(TypeReferenceAdjustment.strict())
                .make();
    }

    @Test
    public void testMethodParameterAnnotationType() {
        new ByteBuddy()
                .subclass(Object.class)
                .modifiers(TypeManifestation.ABSTRACT)
                .defineMethod(FOO, void.class)
                .withParameter(Object.class)
                .annotateParameter(AnnotationDescription.Builder.ofType(Qux.class).build())
                .withoutCode()
                .visit(new AssertionVisitorWrapper(false, Qux.class))
                .visit(TypeReferenceAdjustment.strict())
                .make();
    }

    @Test
    public void testConstantType() {
        new ByteBuddy()
                .subclass(Object.class)
                .modifiers(TypeManifestation.ABSTRACT)
                .defineMethod(FOO, void.class)
                .intercept(FixedValue.value(Foo.class))
                .visit(new AssertionVisitorWrapper(false, Foo.class))
                .visit(TypeReferenceAdjustment.strict())
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testStrictCannotFindType() {
        new ByteBuddy()
                .subclass(Foo.Bar.class)
                .visit(TypeReferenceAdjustment.strict())
                .make(TypePool.Empty.INSTANCE);
    }

    @Test
    public void testRelaxedCannotFindType() {
        new ByteBuddy()
                .subclass(Foo.Bar.class)
                .visit(TypeReferenceAdjustment.relaxed())
                .make(TypePool.Empty.INSTANCE);
    }

    @Test
    public void testFilter() {
        new ByteBuddy()
                .subclass(Foo.Bar.class)
                .visit(new AssertionVisitorWrapper(true, Foo.class))
                .visit(TypeReferenceAdjustment.strict().filter(is(Foo.class)))
                .make();
    }

    public static class Foo {

        public static class Bar {
            /* empty */
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Qux {

        interface Baz {
            /* empty */
        }
    }

    private static class AssertionVisitorWrapper extends AsmVisitorWrapper.AbstractBase {

        private final boolean inverted;

        private final Set<String> internalNames;

        private AssertionVisitorWrapper(boolean inverted, Class<?>... types) {
            this.inverted = inverted;
            internalNames = new HashSet<String>();
            for (Class<?> type : types) {
                internalNames.add(Type.getInternalName(type));
            }
        }

        public ClassVisitor wrap(TypeDescription instrumentedType,
                                 ClassVisitor classVisitor,
                                 Implementation.Context implementationContext,
                                 TypePool typePool,
                                 FieldList<FieldDescription.InDefinedShape> fields,
                                 MethodList<?> methods,
                                 int writerFlags,
                                 int readerFlags) {
            return new AssertionClassVisitor(classVisitor);
        }

        private class AssertionClassVisitor extends ClassVisitor {

            private final Set<String> visited = new HashSet<String>();

            private AssertionClassVisitor(ClassVisitor classVisitor) {
                super(OpenedClassReader.ASM_API, classVisitor);
            }

            @Override
            public void visitInnerClass(String internalName, String outerName, String innerName, int modifiers) {
                visited.add(internalName);
            }

            @Override
            public void visitEnd() {
                if (inverted
                        ? Collections.disjoint(internalNames, visited)
                        : !visited.containsAll(internalNames)) {
                    Set<String> missing = new HashSet<String>(internalNames);
                    missing.removeAll(visited);
                    throw new AssertionError("Missing internal type references: " + missing);
                }
            }
        }
    }
}
