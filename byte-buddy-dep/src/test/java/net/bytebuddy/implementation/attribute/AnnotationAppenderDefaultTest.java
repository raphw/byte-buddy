package net.bytebuddy.implementation.attribute;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.loading.ByteArrayClassLoader;
import net.bytebuddy.test.utility.JavaVersionRule;
import net.bytebuddy.utility.AsmClassWriter;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypeReference;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class AnnotationAppenderDefaultTest {

    private static final String BAR = "net.bytebuddy.test.Bar";

    private static final String FOOBAR = "foobar";

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    @Mock
    private AnnotationAppender.Target target;

    @Mock
    private AnnotationValueFilter valueFilter;

    private AnnotationAppender annotationAppender;

    @Before
    public void setUp() throws Exception {
        annotationAppender = new AnnotationAppender.Default(target);
    }

    @Test
    public void testNoArgumentAnnotation() throws Exception {
        Class<?> bar = makeTypeWithAnnotation(new Foo.Instance());
        assertThat(bar.getAnnotations().length, is(1));
        assertThat(bar.isAnnotationPresent(Foo.class), is(true));
    }

    @Test
    public void testNoArgumentAnnotationSourceCodeRetention() throws Exception {
        Class<?> bar = makeTypeWithAnnotation(new FooSourceCodeRetention.Instance());
        assertThat(bar.getAnnotations().length, is(0));
    }

    @Test
    public void testNoArgumentAnnotationByteCodeRetention() throws Exception {
        Class<?> bar = makeTypeWithAnnotation(new FooByteCodeRetention.Instance());
        assertThat(bar.getAnnotations().length, is(0));
    }

    @Test
    public void testNoArgumentAnnotationNoRetention() throws Exception {
        Class<?> bar = makeTypeWithAnnotation(new FooNoRetention.Instance());
        assertThat(bar.getAnnotations().length, is(0));
    }

    @Test
    public void testSingleArgumentAnnotation() throws Exception {
        Class<?> bar = makeTypeWithAnnotation(new Qux.Instance(FOOBAR));
        assertThat(bar.getAnnotations().length, is(1));
        assertThat(bar.isAnnotationPresent(Qux.class), is(true));
        assertThat(bar.getAnnotation(Qux.class).value(), is(FOOBAR));
    }

    @Test
    public void testMultipleArgumentAnnotation() throws Exception {
        int[] array = {2, 3, 4};
        Class<?> bar = makeTypeWithAnnotation(new Baz.Instance(FOOBAR, array, new Foo.Instance(), Baz.Enum.VALUE, Void.class));
        assertThat(bar.getAnnotations().length, is(1));
        assertThat(bar.isAnnotationPresent(Baz.class), is(true));
        assertThat(bar.getAnnotation(Baz.class).value(), is(FOOBAR));
        assertThat(bar.getAnnotation(Baz.class).array(), is(array));
        assertThat(bar.getAnnotation(Baz.class).annotation(), is((Foo) new Foo.Instance()));
        assertThat(bar.getAnnotation(Baz.class).enumeration(), is(Baz.Enum.VALUE));
        assertThat(bar.getAnnotation(Baz.class).type(), CoreMatchers.<Class<?>>is(Void.class));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testNoArgumentTypeAnnotation() throws Exception {
        Class<?> bar = makeTypeWithSuperClassAnnotation(new Foo.Instance());
        TypeDescription.Generic.AnnotationReader annotationReader = new TypeDescription.Generic.AnnotationReader.Delegator.ForLoadedSuperClass(bar);
        assertThat(annotationReader.asList().size(), is(1));
        assertThat(annotationReader.asList().isAnnotationPresent(Foo.class), is(true));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testNoArgumentTypeAnnotationSourceCodeRetention() throws Exception {
        Class<?> bar = makeTypeWithSuperClassAnnotation(new FooSourceCodeRetention.Instance());
        TypeDescription.Generic.AnnotationReader annotationReader = new TypeDescription.Generic.AnnotationReader.Delegator.ForLoadedSuperClass(bar);
        assertThat(annotationReader.asList().size(), is(0));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testNoArgumentTypeAnnotationByteCodeRetention() throws Exception {
        Class<?> bar = makeTypeWithSuperClassAnnotation(new FooByteCodeRetention.Instance());
        TypeDescription.Generic.AnnotationReader annotationReader = new TypeDescription.Generic.AnnotationReader.Delegator.ForLoadedSuperClass(bar);
        assertThat(annotationReader.asList().size(), is(0));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testNoArgumentTypeAnnotationNoRetention() throws Exception {
        Class<?> bar = makeTypeWithSuperClassAnnotation(new FooNoRetention.Instance());
        TypeDescription.Generic.AnnotationReader annotationReader = new TypeDescription.Generic.AnnotationReader.Delegator.ForLoadedSuperClass(bar);
        assertThat(annotationReader.asList().size(), is(0));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testSingleTypeArgumentAnnotation() throws Exception {
        Class<?> bar = makeTypeWithSuperClassAnnotation(new Qux.Instance(FOOBAR));
        TypeDescription.Generic.AnnotationReader annotationReader = new TypeDescription.Generic.AnnotationReader.Delegator.ForLoadedSuperClass(bar);
        assertThat(annotationReader.asList().size(), is(1));
        assertThat(annotationReader.asList().isAnnotationPresent(Qux.class), is(true));
        assertThat(annotationReader.asList().ofType(Qux.class).load().value(), is(FOOBAR));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testMultipleTypeArgumentAnnotation() throws Exception {
        int[] array = {2, 3, 4};
        Class<?> bar = makeTypeWithSuperClassAnnotation(new Baz.Instance(FOOBAR, array, new Foo.Instance(), Baz.Enum.VALUE, Void.class));
        TypeDescription.Generic.AnnotationReader annotationReader = new TypeDescription.Generic.AnnotationReader.Delegator.ForLoadedSuperClass(bar);
        assertThat(annotationReader.asList().size(), is(1));
        assertThat(annotationReader.asList().isAnnotationPresent(Baz.class), is(true));
        assertThat(annotationReader.asList().ofType(Baz.class).load().value(), is(FOOBAR));
        assertThat(annotationReader.asList().ofType(Baz.class).load().array(), is(array));
        assertThat(annotationReader.asList().ofType(Baz.class).load().annotation(), is((Foo) new Foo.Instance()));
        assertThat(annotationReader.asList().ofType(Baz.class).load().enumeration(), is(Baz.Enum.VALUE));
        assertThat(annotationReader.asList().ofType(Baz.class).load().type(), CoreMatchers.<Class<?>>is(Void.class));
    }

    private Class<?> makeTypeWithAnnotation(Annotation annotation) throws Exception {
        when(valueFilter.isRelevant(any(AnnotationDescription.class), any(MethodDescription.InDefinedShape.class))).thenReturn(true);
        AsmClassWriter classWriter = AsmClassWriter.Factory.Default.IMPLICIT.make(AsmVisitorWrapper.NO_FLAGS);
        classWriter.getVisitor().visit(ClassFileVersion.ofThisVm().getMinorMajorVersion(),
                Opcodes.ACC_PUBLIC,
                BAR.replace('.', '/'),
                null,
                Type.getInternalName(Object.class),
                null);
        AnnotationVisitor annotationVisitor = classWriter.getVisitor().visitAnnotation(Type.getDescriptor(annotation.annotationType()), true);
        when(target.visit(any(String.class), anyBoolean())).thenReturn(annotationVisitor);
        AnnotationDescription annotationDescription = AnnotationDescription.ForLoadedAnnotation.of(annotation);
        annotationAppender.append(annotationDescription, valueFilter);
        classWriter.getVisitor().visitEnd();
        Class<?> bar = new ByteArrayClassLoader(getClass().getClassLoader(), Collections.singletonMap(BAR, classWriter.getBinaryRepresentation())).loadClass(BAR);
        assertThat(bar.getName(), is(BAR));
        assertThat(bar.getSuperclass(), CoreMatchers.<Class<?>>is(Object.class));
        return bar;
    }

    private Class<?> makeTypeWithSuperClassAnnotation(Annotation annotation) throws Exception {
        when(valueFilter.isRelevant(any(AnnotationDescription.class), any(MethodDescription.InDefinedShape.class))).thenReturn(true);
        AsmClassWriter classWriter = AsmClassWriter.Factory.Default.IMPLICIT.make(AsmVisitorWrapper.NO_FLAGS);
        classWriter.getVisitor().visit(ClassFileVersion.ofThisVm().getMinorMajorVersion(),
                Opcodes.ACC_PUBLIC,
                BAR.replace('.', '/'),
                null,
                Type.getInternalName(Object.class),
                null);
        AnnotationVisitor annotationVisitor = classWriter.getVisitor().visitTypeAnnotation(TypeReference.newSuperTypeReference(-1).getValue(),
                null,
                Type.getDescriptor(annotation.annotationType()),
                true);
        when(target.visit(any(String.class), anyBoolean())).thenReturn(annotationVisitor);
        AnnotationDescription annotationDescription = AnnotationDescription.ForLoadedAnnotation.of(annotation);
        annotationAppender.append(annotationDescription, valueFilter);
        classWriter.getVisitor().visitEnd();
        Class<?> bar = new ByteArrayClassLoader(getClass().getClassLoader(), Collections.singletonMap(BAR, classWriter.getBinaryRepresentation())).loadClass(BAR);
        assertThat(bar.getName(), is(BAR));
        assertThat(bar.getSuperclass(), CoreMatchers.<Class<?>>is(Object.class));
        return bar;
    }

    @Test
    public void testSourceRetentionAnnotation() throws Exception {
        AnnotationVisitor annotationVisitor = mock(AnnotationVisitor.class);
        when(target.visit(anyString(), anyBoolean())).thenReturn(annotationVisitor);
        AnnotationDescription annotationDescription = mock(AnnotationDescription.class);
        when(annotationDescription.getRetention()).thenReturn(RetentionPolicy.SOURCE);
        annotationAppender.append(annotationDescription, valueFilter);
        verifyNoMoreInteractions(valueFilter);
        verifyNoMoreInteractions(annotationVisitor);
    }

    @Test
    public void testSourceRetentionTypeAnnotation() throws Exception {
        AnnotationVisitor annotationVisitor = mock(AnnotationVisitor.class);
        when(target.visit(anyString(), anyBoolean())).thenReturn(annotationVisitor);
        AnnotationDescription annotationDescription = mock(AnnotationDescription.class);
        when(annotationDescription.getRetention()).thenReturn(RetentionPolicy.SOURCE);
        annotationAppender.append(annotationDescription, valueFilter, 0, null);
        verifyNoMoreInteractions(valueFilter);
        verifyNoMoreInteractions(annotationVisitor);
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Foo {

        @SuppressWarnings("all")
        class Instance implements Foo {

            public Class<? extends Annotation> annotationType() {
                return Foo.class;
            }
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface FooSourceCodeRetention {

        @SuppressWarnings("all")
        class Instance implements FooSourceCodeRetention {

            public Class<? extends Annotation> annotationType() {
                return FooSourceCodeRetention.class;
            }
        }
    }

    @Retention(RetentionPolicy.CLASS)
    public @interface FooByteCodeRetention {

        @SuppressWarnings("all")
        class Instance implements FooByteCodeRetention {

            public Class<? extends Annotation> annotationType() {
                return FooByteCodeRetention.class;
            }
        }
    }

    public @interface FooNoRetention {

        @SuppressWarnings("all")
        class Instance implements FooNoRetention {

            public Class<? extends Annotation> annotationType() {
                return FooNoRetention.class;
            }
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Qux {

        String value();

        @SuppressWarnings("all")
        class Instance implements Qux {

            private final String value;

            public Instance(String value) {
                this.value = value;
            }

            public String value() {
                return value;
            }

            public Class<? extends Annotation> annotationType() {
                return Qux.class;
            }
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Baz {

        String value();

        int[] array();

        Foo annotation();

        Enum enumeration();

        Class<?> type();

        enum Enum {
            VALUE
        }

        @SuppressWarnings("all")
        class Instance implements Baz {

            private final String value;

            private final int[] array;

            private final Foo annotation;

            private final Enum enumeration;

            private final Class<?> type;

            public Instance(String value, int[] array, Foo annotation, Enum enumeration, Class<?> type) {
                this.value = value;
                this.array = array;
                this.annotation = annotation;
                this.enumeration = enumeration;
                this.type = type;
            }

            public String value() {
                return value;
            }

            public int[] array() {
                return array;
            }

            public Foo annotation() {
                return annotation;
            }

            public Enum enumeration() {
                return enumeration;
            }

            public Class<?> type() {
                return type;
            }

            public Class<? extends Annotation> annotationType() {
                return Baz.class;
            }
        }
    }
}
