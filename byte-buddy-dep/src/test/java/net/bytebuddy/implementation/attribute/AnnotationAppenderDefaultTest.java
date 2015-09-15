package net.bytebuddy.implementation.attribute;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.loading.ByteArrayClassLoader;
import net.bytebuddy.dynamic.loading.PackageDefinitionStrategy;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
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
import java.security.AccessController;
import java.security.ProtectionDomain;
import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class AnnotationAppenderDefaultTest {

    private static final ProtectionDomain DEFAULT_PROTECTION_DOMAIN = null;

    private static final int ASM_MANUAL = 0;

    private static final String BAR = "net.bytebuddy.test.Bar";

    private static final String FOOBAR = "foobar";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private AnnotationAppender.Target target;

    @Mock
    private AnnotationAppender.ValueFilter valueFilter;

    private AnnotationAppender annotationAppender;

    @Before
    public void setUp() throws Exception {
        annotationAppender = new AnnotationAppender.Default(target, valueFilter);
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
        assertEquals(Void.class, bar.getAnnotation(Baz.class).type());
    }

    private Class<?> makeTypeWithAnnotation(Annotation annotation) throws Exception {
        when(valueFilter.isRelevant(any(AnnotationDescription.class), any(MethodDescription.InDefinedShape.class))).thenReturn(true);
        ClassWriter classWriter = new ClassWriter(ASM_MANUAL);
        classWriter.visit(ClassFileVersion.forCurrentJavaVersion().getVersion(),
                Opcodes.ACC_PUBLIC,
                BAR.replace('.', '/'),
                null,
                Type.getInternalName(Object.class),
                null);
        AnnotationVisitor annotationVisitor = classWriter.visitAnnotation(Type.getDescriptor(annotation.annotationType()), true);
        when(target.visit(any(String.class), anyBoolean())).thenReturn(annotationVisitor);
        AnnotationDescription annotationDescription = AnnotationDescription.ForLoadedAnnotation.of(annotation);
        AnnotationAppender.AnnotationVisibility annotationVisibility = AnnotationAppender.AnnotationVisibility.of(annotationDescription);
        annotationAppender.append(annotationDescription, annotationVisibility);
        switch (annotationVisibility) {
            case RUNTIME:
            case CLASS_FILE:
                verify(target).visit(Type.getDescriptor(annotation.annotationType()), annotationVisibility == AnnotationAppender.AnnotationVisibility.RUNTIME);
                verifyNoMoreInteractions(target);
                for (MethodDescription.InDefinedShape methodDescription : annotationDescription.getAnnotationType().getDeclaredMethods()) {
                    verify(valueFilter).isRelevant(annotationDescription, methodDescription);
                }
                verifyNoMoreInteractions(valueFilter);
                break;
            case INVISIBLE:
                verifyZeroInteractions(target);
                break;
            default:
                fail("Unknown annotation visibility");
        }
        classWriter.visitEnd();
        Class<?> bar = new ByteArrayClassLoader(getClass().getClassLoader(),
                Collections.singletonMap(BAR, classWriter.toByteArray()),
                DEFAULT_PROTECTION_DOMAIN,
                AccessController.getContext(),
                ByteArrayClassLoader.PersistenceHandler.LATENT,
                PackageDefinitionStrategy.NoOp.INSTANCE).loadClass(BAR);
        assertThat(bar.getName(), is(BAR));
        assertEquals(Object.class, bar.getSuperclass());
        return bar;
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSkipValues() throws Exception {
        when(valueFilter.isRelevant(any(AnnotationDescription.class), any(MethodDescription.InDefinedShape.class))).thenReturn(false);
        MethodDescription.InDefinedShape methodDescription = mock(MethodDescription.InDefinedShape.class);
        TypeDescription annotationType = mock(TypeDescription.class);
        when(annotationType.getDeclaredMethods())
                .thenReturn((MethodList) new MethodList.Explicit<MethodDescription>(Collections.singletonList(methodDescription)));
        AnnotationDescription annotationDescription = mock(AnnotationDescription.class);
        when(annotationDescription.getAnnotationType()).thenReturn(annotationType);
        AnnotationVisitor annotationVisitor = mock(AnnotationVisitor.class);
        when(target.visit(anyString(), anyBoolean())).thenReturn(annotationVisitor);
        annotationAppender.append(annotationDescription, AnnotationAppender.AnnotationVisibility.RUNTIME);
        verify(valueFilter).isRelevant(annotationDescription, methodDescription);
        verifyNoMoreInteractions(valueFilter);
        verify(annotationVisitor).visitEnd();
        verifyNoMoreInteractions(annotationVisitor);

    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(AnnotationAppender.Default.class).apply();
        ObjectPropertyAssertion.of(AnnotationAppender.AnnotationVisibility.class).apply();
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Foo {

        class Instance implements Foo {

            @Override
            public Class<? extends Annotation> annotationType() {
                return Foo.class;
            }
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface FooSourceCodeRetention {

        class Instance implements FooSourceCodeRetention {

            @Override
            public Class<? extends Annotation> annotationType() {
                return FooSourceCodeRetention.class;
            }
        }
    }

    @Retention(RetentionPolicy.CLASS)
    public @interface FooByteCodeRetention {

        class Instance implements FooByteCodeRetention {

            @Override
            public Class<? extends Annotation> annotationType() {
                return FooByteCodeRetention.class;
            }
        }
    }

    public @interface FooNoRetention {

        class Instance implements FooNoRetention {

            @Override
            public Class<? extends Annotation> annotationType() {
                return FooNoRetention.class;
            }
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Qux {

        String value();

        class Instance implements Qux {

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
    }
}
