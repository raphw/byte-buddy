package net.bytebuddy.instrumentation.attribute.annotation;

import net.bytebuddy.ClassFormatVersion;
import net.bytebuddy.dynamic.loading.ByteArrayClassLoader;
import net.bytebuddy.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.*;

public class AnnotationAppenderDefaultTest {

    private static final int ASM_MANUAL = 0;
    private static final String BAR = "net.bytebuddy.test.Bar";
    private static final String FOOBAR = "foobar";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private AnnotationAppender.Target target;

    private AnnotationAppender annotationAppender;

    @Before
    public void setUp() throws Exception {
        annotationAppender = new AnnotationAppender.Default(target);
    }

    @Retention(RetentionPolicy.RUNTIME)
    public static @interface Foo {

        static class Instance implements Foo {

            @Override
            public Class<? extends Annotation> annotationType() {
                return Foo.class;
            }
        }
    }

    @Test
    public void testNoArgumentAnnotation() throws Exception {
        Class<?> bar = makeTypeWithAnnotation(new Foo.Instance());
        assertThat(bar.getAnnotations().length, is(1));
        assertThat(bar.isAnnotationPresent(Foo.class), is(true));
    }

    @Retention(RetentionPolicy.SOURCE)
    public static @interface FooSourceCodeRetention {

        static class Instance implements FooSourceCodeRetention {

            @Override
            public Class<? extends Annotation> annotationType() {
                return FooSourceCodeRetention.class;
            }
        }
    }

    @Test
    public void testNoArgumentAnnotationSourceCodeRetention() throws Exception {
        Class<?> bar = makeTypeWithAnnotation(new FooSourceCodeRetention.Instance());
        assertThat(bar.getAnnotations().length, is(0));
    }

    @Retention(RetentionPolicy.CLASS)
    public static @interface FooByteCodeRetention {

        static class Instance implements FooByteCodeRetention {

            @Override
            public Class<? extends Annotation> annotationType() {
                return FooByteCodeRetention.class;
            }
        }
    }

    @Test
    public void testNoArgumentAnnotationByteCodeRetention() throws Exception {
        Class<?> bar = makeTypeWithAnnotation(new FooByteCodeRetention.Instance());
        assertThat(bar.getAnnotations().length, is(0));
    }

    public static @interface FooNoRetention {

        static class Instance implements FooNoRetention {

            @Override
            public Class<? extends Annotation> annotationType() {
                return FooNoRetention.class;
            }
        }
    }

    @Test
    public void testNoArgumentAnnotationNoRetention() throws Exception {
        Class<?> bar = makeTypeWithAnnotation(new FooNoRetention.Instance());
        assertThat(bar.getAnnotations().length, is(0));
    }

    @Retention(RetentionPolicy.RUNTIME)
    public static @interface Qux {

        static class Instance implements Qux {

            private final String value;

            public Instance(String value) {
                this.value = value;
            }

            @Override
            public String value() {
                return value;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return Qux.class;
            }
        }

        String value();
    }

    @Test
    public void testSingleArgumentAnnotation() throws Exception {
        Class<?> bar = makeTypeWithAnnotation(new Qux.Instance(FOOBAR));
        assertThat(bar.getAnnotations().length, is(1));
        assertThat(bar.isAnnotationPresent(Qux.class), is(true));
        assertThat(bar.getAnnotation(Qux.class).value(), is(FOOBAR));
    }

    @Retention(RetentionPolicy.RUNTIME)
    public static @interface Baz {

        static class Instance implements Baz {

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

            @Override
            public String value() {
                return value;
            }

            @Override
            public int[] array() {
                return array;
            }

            @Override
            public Foo annotation() {
                return annotation;
            }

            @Override
            public Enum enumeration() {
                return enumeration;
            }

            @Override
            public Class<?> type() {
                return type;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return Baz.class;
            }
        }

        static enum Enum {
            VALUE
        }

        String value();

        int[] array();

        Foo annotation();

        Enum enumeration();

        Class<?> type();
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
        assertEquals(Void.class, bar.getAnnotation(Baz.class).type());
    }

    private Class<?> makeTypeWithAnnotation(Annotation annotation) throws Exception {
        ClassWriter classWriter = new ClassWriter(ASM_MANUAL);
        classWriter.visit(ClassFormatVersion.forCurrentJavaVersion().getVersionNumber(),
                Opcodes.ACC_PUBLIC,
                BAR.replace('.', '/'),
                null,
                Type.getInternalName(Object.class),
                null);
        AnnotationVisitor annotationVisitor = classWriter.visitAnnotation(Type.getDescriptor(annotation.annotationType()), true);
        when(target.visit(any(String.class), anyBoolean())).thenReturn(annotationVisitor);
        AnnotationAppender.AnnotationVisibility annotationVisibility = AnnotationAppender.AnnotationVisibility.of(annotation);
        annotationAppender.append(annotation, annotationVisibility);
        switch (annotationVisibility) {
            case RUNTIME:
                verify(target).visit(Type.getDescriptor(annotation.annotationType()), true);
                verifyNoMoreInteractions(target);
                break;
            case CLASS_FILE:
                verify(target).visit(Type.getDescriptor(annotation.annotationType()), false);
                verifyNoMoreInteractions(target);
                break;
            case INVISIBLE:
                verifyZeroInteractions(target);
                break;
            default:
                fail("Unknown annotation visibility");
        }
        classWriter.visitEnd();
        Class<?> bar = new ByteArrayClassLoader(getClass().getClassLoader(),
                Collections.singletonMap(BAR, classWriter.toByteArray())).loadClass(BAR);
        assertThat(bar.getName(), is(BAR));
        assertEquals(Object.class, bar.getSuperclass());
        return bar;
    }
}
