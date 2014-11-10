package net.bytebuddy.instrumentation.attribute.annotation;

import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;
import net.bytebuddy.utility.PropertyDispatcher;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.objectweb.asm.Opcodes;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class AbstractAnnotationDescriptionTest {

    private static final boolean BOOLEAN = true;
    private static final byte BYTE = 42;
    private static final short SHORT = 42;
    private static final char CHARACTER = 42;
    private static final int INTEGER = 42;
    private static final long LONG = 42L;
    private static final float FLOAT = 42f;
    private static final double DOUBLE = 42d;
    private static final String FOO = "foo", BAR = "BAR";
    private static final SampleEnumeration ENUMERATION = SampleEnumeration.VALUE;
    private static final Class<?> CLASS = Void.class;
    private static final Class<?> ARRAY_CLASS = Void[].class;
    private static final Other ANNOTATION = Other.Implementation.INSTANCE;

    private static final boolean[] BOOLEAN_ARRAY = new boolean[]{BOOLEAN};
    private static final byte[] BYTE_ARRAY = new byte[]{BYTE};
    private static final short[] SHORT_ARRAY = new short[]{SHORT};
    private static final char[] CHARACTER_ARRAY = new char[]{CHARACTER};
    private static final int[] INTEGER_ARRAY = new int[]{INTEGER};
    private static final long[] LONG_ARRAY = new long[]{LONG};
    private static final float[] FLOAT_ARRAY = new float[]{FLOAT};
    private static final double[] DOUBLE_ARRAY = new double[]{DOUBLE};
    private static final String[] STRING_ARRAY = new String[]{FOO};
    private static final SampleEnumeration[] ENUMERATION_ARRAY = new SampleEnumeration[]{ENUMERATION};
    private static final Class<?>[] CLASS_ARRAY = new Class<?>[]{CLASS};
    private static final Other[] ANNOTATION_ARRAY = new Other[]{ANNOTATION};

    protected abstract AnnotationDescription describe(Annotation annotation, Class<?> declaringType);

    protected AnnotationDescription describe(Annotation annotation) {
        return describe(annotation, annotation == first ? FooSample.class : BarSample.class);
    }

    private Annotation first, second;

    @Before
    public void setUp() throws Exception {
        first = FooSample.class.getAnnotation(Sample.class);
        second = BarSample.class.getAnnotation(Sample.class);
    }

    @Test
    public void testPrecondition() throws Exception {
        assertThat(describe(first), equalTo(describe(first)));
        assertThat(describe(second), equalTo(describe(second)));
        assertThat(describe(first), not(equalTo(describe(second))));
        assertThat(describe(first).getAnnotationType(), equalTo(describe(second).getAnnotationType()));
        assertThat(describe(first).getAnnotationType(), not(equalTo((TypeDescription) new TypeDescription.ForLoadedType(Other.class))));
        assertThat(describe(second).getAnnotationType(), not(equalTo((TypeDescription) new TypeDescription.ForLoadedType(Other.class))));
        assertThat(describe(first).getAnnotationType().represents(first.annotationType()), is(true));
        assertThat(describe(second).getAnnotationType().represents(second.annotationType()), is(true));
    }

    @Test
    public void assertToString() throws Exception {
        assertToString(describe(first).toString(), first);
        assertToString(describe(second).toString(), second);
    }

    private void assertToString(String actual, Annotation loaded) throws Exception {
        assertThat(actual, startsWith("@" + loaded.annotationType().getName()));
        for (Method method : loaded.annotationType().getDeclaredMethods()) {
            assertThat(actual, containsString(method.getName() + "="
                    + PropertyDispatcher.of(method.getReturnType()).toString(method.invoke(loaded))));
        }
    }

    @Test
    public void testHashCode() throws Exception {
        assertThat(describe(first).hashCode(), is(describe(first).hashCode()));
        assertThat(describe(second).hashCode(), is(describe(second).hashCode()));
        assertThat(describe(first).hashCode(), not(is(describe(second).hashCode())));
    }

    @Test
    public void testEquals() throws Exception {
        AnnotationDescription identical = describe(first);
        assertThat(identical, equalTo(identical));
        AnnotationDescription equalFirst = mock(AnnotationDescription.class);
        when(equalFirst.getAnnotationType()).thenReturn(new TypeDescription.ForLoadedType(first.annotationType()));
        when(equalFirst.getValue(Mockito.any(MethodDescription.class))).then(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                MethodDescription method = (MethodDescription) invocation.getArguments()[0];
                return AnnotationDescription.ForLoadedAnnotation.of(first).getValue(method);
            }
        });
        assertThat(describe(first), equalTo(equalFirst));
        AnnotationDescription equalSecond = mock(AnnotationDescription.class);
        when(equalSecond.getAnnotationType()).thenReturn(new TypeDescription.ForLoadedType(first.annotationType()));
        when(equalSecond.getValue(Mockito.any(MethodDescription.class))).then(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                MethodDescription method = (MethodDescription) invocation.getArguments()[0];
                return AnnotationDescription.ForLoadedAnnotation.of(second).getValue(method);
            }
        });
        assertThat(describe(second), equalTo(equalSecond));
        AnnotationDescription equalFirstTypeOnly = mock(AnnotationDescription.class);
        when(equalFirstTypeOnly.getAnnotationType()).thenReturn(new TypeDescription.ForLoadedType(Other.class));
        when(equalFirstTypeOnly.getValue(Mockito.any(MethodDescription.class))).then(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                MethodDescription method = (MethodDescription) invocation.getArguments()[0];
                return AnnotationDescription.ForLoadedAnnotation.of(first).getValue(method);
            }
        });
        assertThat(describe(first), not(equalTo(equalFirstTypeOnly)));
        AnnotationDescription equalFirstNameOnly = mock(AnnotationDescription.class);
        when(equalFirstNameOnly.getAnnotationType()).thenReturn(new TypeDescription.ForLoadedType(first.annotationType()));
        when(equalFirstNameOnly.getValue(Mockito.any(MethodDescription.class))).thenReturn(null);
        assertThat(describe(first), not(equalTo(equalFirstNameOnly)));
        assertThat(describe(first), not(equalTo(equalSecond)));
        assertThat(describe(first), not(equalTo(new Object())));
        assertThat(describe(first), not(equalTo(null)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalMethod() throws Exception {
        describe(first).getValue(new MethodDescription.ForLoadedMethod(Object.class.getMethod("toString")));
    }

    @Test
    public void testLoadedEquals() throws Exception {
        assertThat(describe(first).prepare(Sample.class).load(), equalTo(first));
        assertThat(describe(second).prepare(Sample.class).load(), equalTo(second));
        assertThat(describe(first).prepare(Sample.class).load(), not(equalTo(second)));
    }

    @Test
    public void testLoadedHashCode() throws Exception {
        assertThat(describe(first).prepare(Sample.class).load().hashCode(), equalTo(first.hashCode()));
        assertThat(describe(second).prepare(Sample.class).load().hashCode(), equalTo(second.hashCode()));
        assertThat(describe(first).prepare(Sample.class).load().hashCode(), not(equalTo(second.hashCode())));
    }

    @Test
    public void testLoadedToString() throws Exception {
        assertToString(describe(first).prepare(Sample.class).load().toString(), first);
        assertToString(describe(second).prepare(Sample.class).load().toString(), second);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalPreparation() throws Exception {
        describe(first).prepare(Other.class);
    }

    @Test
    public void testValuesNonLoaded() throws Exception {
        assertValue(first, "booleanValue", BOOLEAN, BOOLEAN);
        assertValue(second, "booleanValue", BOOLEAN, BOOLEAN);
        assertValue(first, "byteValue", BYTE, BYTE);
        assertValue(second, "byteValue", BYTE, BYTE);
        assertValue(first, "shortValue", SHORT, SHORT);
        assertValue(second, "shortValue", SHORT, SHORT);
        assertValue(first, "charValue", CHARACTER, CHARACTER);
        assertValue(second, "charValue", CHARACTER, CHARACTER);
        assertValue(first, "intValue", INTEGER, INTEGER);
        assertValue(second, "intValue", INTEGER, INTEGER);
        assertValue(first, "longValue", LONG, LONG);
        assertValue(second, "longValue", LONG, LONG);
        assertValue(first, "floatValue", FLOAT, FLOAT);
        assertValue(second, "floatValue", FLOAT, FLOAT);
        assertValue(first, "doubleValue", DOUBLE, DOUBLE);
        assertValue(second, "doubleValue", DOUBLE, DOUBLE);
        assertValue(first, "stringValue", FOO, FOO);
        assertValue(second, "stringValue", BAR, BAR);
        assertValue(first, "classValue", new TypeDescription.ForLoadedType(CLASS), CLASS);
        assertValue(second, "classValue", new TypeDescription.ForLoadedType(CLASS), CLASS);
        assertValue(first, "arrayClassValue", new TypeDescription.ForLoadedType(ARRAY_CLASS), ARRAY_CLASS);
        assertValue(second, "arrayClassValue", new TypeDescription.ForLoadedType(ARRAY_CLASS), ARRAY_CLASS);
        assertValue(first, "enumValue", new AnnotationDescription.EnumerationValue.ForLoadedEnumeration(ENUMERATION), ENUMERATION);
        assertValue(second, "enumValue", new AnnotationDescription.EnumerationValue.ForLoadedEnumeration(ENUMERATION), ENUMERATION);
        assertValue(first, "annotationValue", AnnotationDescription.ForLoadedAnnotation.of(ANNOTATION), ANNOTATION);
        assertValue(second, "annotationValue", AnnotationDescription.ForLoadedAnnotation.of(ANNOTATION), ANNOTATION);
        assertValue(first, "booleanArrayValue", BOOLEAN_ARRAY, BOOLEAN_ARRAY);
        assertValue(second, "booleanArrayValue", BOOLEAN_ARRAY, BOOLEAN_ARRAY);
        assertValue(first, "byteArrayValue", BYTE_ARRAY, BYTE_ARRAY);
        assertValue(second, "byteArrayValue", BYTE_ARRAY, BYTE_ARRAY);
        assertValue(first, "shortArrayValue", SHORT_ARRAY, SHORT_ARRAY);
        assertValue(second, "shortArrayValue", SHORT_ARRAY, SHORT_ARRAY);
        assertValue(first, "charArrayValue", CHARACTER_ARRAY, CHARACTER_ARRAY);
        assertValue(second, "charArrayValue", CHARACTER_ARRAY, CHARACTER_ARRAY);
        assertValue(first, "intArrayValue", INTEGER_ARRAY, INTEGER_ARRAY);
        assertValue(second, "intArrayValue", INTEGER_ARRAY, INTEGER_ARRAY);
        assertValue(first, "longArrayValue", LONG_ARRAY, LONG_ARRAY);
        assertValue(second, "longArrayValue", LONG_ARRAY, LONG_ARRAY);
        assertValue(first, "floatArrayValue", FLOAT_ARRAY, FLOAT_ARRAY);
        assertValue(second, "floatArrayValue", FLOAT_ARRAY, FLOAT_ARRAY);
        assertValue(first, "doubleArrayValue", DOUBLE_ARRAY, DOUBLE_ARRAY);
        assertValue(second, "doubleArrayValue", DOUBLE_ARRAY, DOUBLE_ARRAY);
        assertValue(first, "stringArrayValue", STRING_ARRAY, STRING_ARRAY);
        assertValue(second, "stringArrayValue", STRING_ARRAY, STRING_ARRAY);
        assertValue(first, "classArrayValue", new TypeDescription[]{new TypeDescription.ForLoadedType(CLASS)}, CLASS_ARRAY);
        assertValue(second, "classArrayValue", new TypeDescription[]{new TypeDescription.ForLoadedType(CLASS)}, CLASS_ARRAY);
        assertValue(first, "enumArrayValue", new AnnotationDescription.EnumerationValue[]{new AnnotationDescription.EnumerationValue.ForLoadedEnumeration(ENUMERATION)}, ENUMERATION_ARRAY);
        assertValue(second, "enumArrayValue", new AnnotationDescription.EnumerationValue[]{new AnnotationDescription.EnumerationValue.ForLoadedEnumeration(ENUMERATION)}, ENUMERATION_ARRAY);
        assertValue(first, "annotationArrayValue", new AnnotationDescription[]{AnnotationDescription.ForLoadedAnnotation.of(ANNOTATION)}, ANNOTATION_ARRAY);
        assertValue(second, "annotationArrayValue", new AnnotationDescription[]{AnnotationDescription.ForLoadedAnnotation.of(ANNOTATION)}, ANNOTATION_ARRAY);
    }

    private void assertValue(Annotation annotation, String methodName, Object rawValue, Object loadedValue) throws Exception {
        assertThat(describe(annotation).getValue(new MethodDescription
                .ForLoadedMethod(annotation.annotationType().getDeclaredMethod(methodName))), is(rawValue));
        assertThat(describe(annotation).getValue(new MethodDescription.Latent(methodName,
                new TypeDescription.ForLoadedType(annotation.annotationType()),
                new TypeDescription.ForLoadedType(annotation.annotationType().getDeclaredMethod(methodName).getReturnType()),
                new TypeList.Empty(),
                Opcodes.ACC_PUBLIC,
                new TypeList.Empty())), is(rawValue));
        assertThat(annotation.annotationType().getDeclaredMethod(methodName)
                .invoke(describe(annotation).prepare(annotation.annotationType()).load()), is(loadedValue));
    }

    @Sample(booleanValue = BOOLEAN,
            byteValue = BYTE,
            charValue = CHARACTER,
            shortValue = SHORT,
            intValue = INTEGER,
            longValue = LONG,
            floatValue = FLOAT,
            doubleValue = DOUBLE,
            stringValue = FOO,
            classValue = Void.class,
            arrayClassValue = Void[].class,
            enumValue = SampleEnumeration.VALUE,
            annotationValue = @Other,
            booleanArrayValue = BOOLEAN,
            byteArrayValue = BYTE,
            shortArrayValue = SHORT,
            charArrayValue = CHARACTER,
            intArrayValue = INTEGER,
            longArrayValue = LONG,
            floatArrayValue = FLOAT,
            doubleArrayValue = DOUBLE,
            stringArrayValue = FOO,
            classArrayValue = Void.class,
            enumArrayValue = SampleEnumeration.VALUE,
            annotationArrayValue = @Other)
    private static class FooSample {
    }

    @Sample(booleanValue = BOOLEAN,
            byteValue = BYTE,
            charValue = CHARACTER,
            shortValue = SHORT,
            intValue = INTEGER,
            longValue = LONG,
            floatValue = FLOAT,
            doubleValue = DOUBLE,
            stringValue = BAR,
            classValue = Void.class,
            arrayClassValue = Void[].class,
            enumValue = SampleEnumeration.VALUE,
            annotationValue = @Other,
            booleanArrayValue = BOOLEAN,
            byteArrayValue = BYTE,
            shortArrayValue = SHORT,
            charArrayValue = CHARACTER,
            intArrayValue = INTEGER,
            longArrayValue = LONG,
            floatArrayValue = FLOAT,
            doubleArrayValue = DOUBLE,
            stringArrayValue = FOO,
            classArrayValue = Void.class,
            enumArrayValue = SampleEnumeration.VALUE,
            annotationArrayValue = @Other)
    private static class BarSample {
    }

    @Retention(RetentionPolicy.RUNTIME)
    public static @interface Sample {

        boolean booleanValue();

        byte byteValue();

        short shortValue();

        char charValue();

        int intValue();

        long longValue();

        float floatValue();

        double doubleValue();

        String stringValue();

        Class<?> classValue();

        Class<?> arrayClassValue();

        SampleEnumeration enumValue();

        Other annotationValue();

        boolean[] booleanArrayValue();

        byte[] byteArrayValue();

        short[] shortArrayValue();

        char[] charArrayValue();

        int[] intArrayValue();

        long[] longArrayValue();

        float[] floatArrayValue();

        double[] doubleArrayValue();

        String[] stringArrayValue();

        Class<?>[] classArrayValue();

        SampleEnumeration[] enumArrayValue();

        Other[] annotationArrayValue();
    }

    public @interface Other {

        static enum Implementation implements Other {
            INSTANCE;

            @Override
            public Class<? extends Annotation> annotationType() {
                return Other.class;
            }
        }
    }

    public static enum SampleEnumeration {
        VALUE
    }
}
