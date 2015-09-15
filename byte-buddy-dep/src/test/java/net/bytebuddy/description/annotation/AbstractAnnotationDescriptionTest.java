package net.bytebuddy.description.annotation;

import net.bytebuddy.description.enumeration.EnumerationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import net.bytebuddy.utility.PropertyDispatcher;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.objectweb.asm.Opcodes;

import java.lang.annotation.*;
import java.lang.reflect.Method;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class AbstractAnnotationDescriptionTest {

    private static final boolean BOOLEAN = true;

    private static final boolean[] BOOLEAN_ARRAY = new boolean[]{BOOLEAN};

    private static final byte BYTE = 42;

    private static final byte[] BYTE_ARRAY = new byte[]{BYTE};

    private static final short SHORT = 42;

    private static final short[] SHORT_ARRAY = new short[]{SHORT};

    private static final char CHARACTER = 42;

    private static final char[] CHARACTER_ARRAY = new char[]{CHARACTER};

    private static final int INTEGER = 42;

    private static final int[] INTEGER_ARRAY = new int[]{INTEGER};

    private static final long LONG = 42L;

    private static final long[] LONG_ARRAY = new long[]{LONG};

    private static final float FLOAT = 42f;

    private static final float[] FLOAT_ARRAY = new float[]{FLOAT};

    private static final double DOUBLE = 42d;

    private static final double[] DOUBLE_ARRAY = new double[]{DOUBLE};

    private static final String FOO = "foo", BAR = "bar";

    private static final String[] STRING_ARRAY = new String[]{FOO};

    private static final SampleEnumeration ENUMERATION = SampleEnumeration.VALUE;

    private static final SampleEnumeration[] ENUMERATION_ARRAY = new SampleEnumeration[]{ENUMERATION};

    private static final Class<?> CLASS = Void.class;

    private static final Class<?>[] CLASS_ARRAY = new Class<?>[]{CLASS};

    private static final Class<?> ARRAY_CLASS = Void[].class;

    private static final Other ANNOTATION = EnumerationCarrier.class.getAnnotation(Other.class);

    private static final Other[] ANNOTATION_ARRAY = new Other[]{ANNOTATION};

    private static final boolean OTHER_BOOLEAN = false;

    private static final boolean[] OTHER_BOOLEAN_ARRAY = new boolean[]{OTHER_BOOLEAN};

    private static final byte OTHER_BYTE = 42 * 2;

    private static final byte[] OTHER_BYTE_ARRAY = new byte[]{OTHER_BYTE};

    private static final short OTHER_SHORT = 42 * 2;

    private static final short[] OTHER_SHORT_ARRAY = new short[]{OTHER_SHORT};

    private static final char OTHER_CHARACTER = 42 * 2;

    private static final char[] OTHER_CHARACTER_ARRAY = new char[]{OTHER_CHARACTER};

    private static final int OTHER_INTEGER = 42 * 2;

    private static final int[] OTHER_INTEGER_ARRAY = new int[]{OTHER_INTEGER};

    private static final long OTHER_LONG = 42L * 2;

    private static final long[] OTHER_LONG_ARRAY = new long[]{OTHER_LONG};

    private static final float OTHER_FLOAT = 42f * 2;

    private static final float[] OTHER_FLOAT_ARRAY = new float[]{OTHER_FLOAT};

    private static final double OTHER_DOUBLE = 42d * 2;

    private static final double[] OTHER_DOUBLE_ARRAY = new double[]{OTHER_DOUBLE};

    private static final SampleEnumeration OTHER_ENUMERATION = SampleEnumeration.OTHER;

    private static final SampleEnumeration[] OTHER_ENUMERATION_ARRAY = new SampleEnumeration[]{OTHER_ENUMERATION};

    private static final Class<?> OTHER_CLASS = Object.class;

    private static final Class<?>[] OTHER_CLASS_ARRAY = new Class<?>[]{OTHER_CLASS};

    private static final Class<?> OTHER_ARRAY_CLASS = Object[].class;

    private static final Other OTHER_ANNOTATION = OtherEnumerationCarrier.class.getAnnotation(Other.class);

    private static final Other[] OTHER_ANNOTATION_ARRAY = new Other[]{OTHER_ANNOTATION};

    private static final String[] OTHER_STRING_ARRAY = new String[]{BAR};

    private Annotation first, second, defaultFirst, defaultSecond;

    protected abstract AnnotationDescription describe(Annotation annotation, Class<?> declaringType);

    private AnnotationDescription describe(Annotation annotation) {
        Class<?> carrier;
        if (annotation == first) {
            carrier = FooSample.class;
        } else if (annotation == second) {
            carrier = BarSample.class;
        } else if (annotation == defaultFirst) {
            carrier = DefaultSample.class;
        } else if (annotation == defaultSecond) {
            carrier = NonDefaultSample.class;
        } else {
            throw new AssertionError();
        }
        return describe(annotation, carrier);
    }

    @Before
    public void setUp() throws Exception {
        first = FooSample.class.getAnnotation(Sample.class);
        second = BarSample.class.getAnnotation(Sample.class);
        defaultFirst = DefaultSample.class.getAnnotation(SampleDefault.class);
        defaultSecond = NonDefaultSample.class.getAnnotation(SampleDefault.class);
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
        when(equalFirst.getValue(Mockito.any(MethodDescription.InDefinedShape.class))).then(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                MethodDescription.InDefinedShape method = (MethodDescription.InDefinedShape) invocation.getArguments()[0];
                return AnnotationDescription.ForLoadedAnnotation.of(first).getValue(method);
            }
        });
        assertThat(describe(first), equalTo(equalFirst));
        AnnotationDescription equalSecond = mock(AnnotationDescription.class);
        when(equalSecond.getAnnotationType()).thenReturn(new TypeDescription.ForLoadedType(first.annotationType()));
        when(equalSecond.getValue(Mockito.any(MethodDescription.InDefinedShape.class))).then(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                MethodDescription.InDefinedShape method = (MethodDescription.InDefinedShape) invocation.getArguments()[0];
                return AnnotationDescription.ForLoadedAnnotation.of(second).getValue(method);
            }
        });
        assertThat(describe(second), equalTo(equalSecond));
        AnnotationDescription equalFirstTypeOnly = mock(AnnotationDescription.class);
        when(equalFirstTypeOnly.getAnnotationType()).thenReturn(new TypeDescription.ForLoadedType(Other.class));
        when(equalFirstTypeOnly.getValue(Mockito.any(MethodDescription.InDefinedShape.class))).then(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                MethodDescription.InDefinedShape method = (MethodDescription.InDefinedShape) invocation.getArguments()[0];
                return AnnotationDescription.ForLoadedAnnotation.of(first).getValue(method);
            }
        });
        assertThat(describe(first), not(equalTo(equalFirstTypeOnly)));
        AnnotationDescription equalFirstNameOnly = mock(AnnotationDescription.class);
        when(equalFirstNameOnly.getAnnotationType()).thenReturn(new TypeDescription.ForLoadedType(first.annotationType()));
        when(equalFirstNameOnly.getValue(Mockito.any(MethodDescription.InDefinedShape.class))).thenReturn(null);
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
        assertThat(describe(first).prepare(Sample.class).load(), equalTo(describe(first).prepare(Sample.class).load()));
        assertThat(describe(first).prepare(Sample.class).load(), not(equalTo(describe(second).prepare(Sample.class).load())));
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

    @Test
    public void testLoadedAnnotationType() throws Exception {
        assertEquals(Sample.class, describe(first).prepare(Sample.class).load().annotationType());
        assertEquals(Sample.class, describe(second).prepare(Sample.class).load().annotationType());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalPreparation() throws Exception {
        describe(first).prepare(Other.class);
    }

    @Test
    public void testValues() throws Exception {
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
        assertValue(first, "enumValue", new EnumerationDescription.ForLoadedEnumeration(ENUMERATION), ENUMERATION);
        assertValue(second, "enumValue", new EnumerationDescription.ForLoadedEnumeration(ENUMERATION), ENUMERATION);
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
        assertValue(first, "enumArrayValue", new EnumerationDescription[]{new EnumerationDescription.ForLoadedEnumeration(ENUMERATION)}, ENUMERATION_ARRAY);
        assertValue(second, "enumArrayValue", new EnumerationDescription[]{new EnumerationDescription.ForLoadedEnumeration(ENUMERATION)}, ENUMERATION_ARRAY);
        assertValue(first, "annotationArrayValue", new AnnotationDescription[]{AnnotationDescription.ForLoadedAnnotation.of(ANNOTATION)}, ANNOTATION_ARRAY);
        assertValue(second, "annotationArrayValue", new AnnotationDescription[]{AnnotationDescription.ForLoadedAnnotation.of(ANNOTATION)}, ANNOTATION_ARRAY);
    }

    @Test
    public void testValuesDefaults() throws Exception {
        assertValue(defaultFirst, "booleanValue", BOOLEAN, BOOLEAN);
        assertValue(defaultSecond, "booleanValue", OTHER_BOOLEAN, OTHER_BOOLEAN);
        assertValue(defaultFirst, "byteValue", BYTE, BYTE);
        assertValue(defaultSecond, "byteValue", OTHER_BYTE, OTHER_BYTE);
        assertValue(defaultFirst, "shortValue", SHORT, SHORT);
        assertValue(defaultSecond, "shortValue", OTHER_SHORT, OTHER_SHORT);
        assertValue(defaultFirst, "charValue", CHARACTER, CHARACTER);
        assertValue(defaultSecond, "charValue", OTHER_CHARACTER, OTHER_CHARACTER);
        assertValue(defaultFirst, "intValue", INTEGER, INTEGER);
        assertValue(defaultSecond, "intValue", OTHER_INTEGER, OTHER_INTEGER);
        assertValue(defaultFirst, "longValue", LONG, LONG);
        assertValue(defaultSecond, "longValue", OTHER_LONG, OTHER_LONG);
        assertValue(defaultFirst, "floatValue", FLOAT, FLOAT);
        assertValue(defaultSecond, "floatValue", OTHER_FLOAT, OTHER_FLOAT);
        assertValue(defaultFirst, "doubleValue", DOUBLE, DOUBLE);
        assertValue(defaultSecond, "doubleValue", OTHER_DOUBLE, OTHER_DOUBLE);
        assertValue(defaultFirst, "stringValue", FOO, FOO);
        assertValue(defaultSecond, "stringValue", BAR, BAR);
        assertValue(defaultFirst, "classValue", new TypeDescription.ForLoadedType(CLASS), CLASS);
        assertValue(defaultSecond, "classValue", new TypeDescription.ForLoadedType(OTHER_CLASS), OTHER_CLASS);
        assertValue(defaultFirst, "arrayClassValue", new TypeDescription.ForLoadedType(ARRAY_CLASS), ARRAY_CLASS);
        assertValue(defaultSecond, "arrayClassValue", new TypeDescription.ForLoadedType(OTHER_ARRAY_CLASS), OTHER_ARRAY_CLASS);
        assertValue(defaultFirst, "enumValue", new EnumerationDescription.ForLoadedEnumeration(ENUMERATION), ENUMERATION);
        assertValue(defaultSecond, "enumValue", new EnumerationDescription.ForLoadedEnumeration(OTHER_ENUMERATION), OTHER_ENUMERATION);
        assertValue(defaultFirst, "annotationValue", AnnotationDescription.ForLoadedAnnotation.of(ANNOTATION), ANNOTATION);
        assertValue(defaultSecond, "annotationValue", AnnotationDescription.ForLoadedAnnotation.of(OTHER_ANNOTATION), OTHER_ANNOTATION);
        assertValue(defaultFirst, "booleanArrayValue", BOOLEAN_ARRAY, BOOLEAN_ARRAY);
        assertValue(defaultSecond, "booleanArrayValue", OTHER_BOOLEAN_ARRAY, OTHER_BOOLEAN_ARRAY);
        assertValue(defaultFirst, "byteArrayValue", BYTE_ARRAY, BYTE_ARRAY);
        assertValue(defaultSecond, "byteArrayValue", OTHER_BYTE_ARRAY, OTHER_BYTE_ARRAY);
        assertValue(defaultFirst, "shortArrayValue", SHORT_ARRAY, SHORT_ARRAY);
        assertValue(defaultSecond, "shortArrayValue", OTHER_SHORT_ARRAY, OTHER_SHORT_ARRAY);
        assertValue(defaultFirst, "charArrayValue", CHARACTER_ARRAY, CHARACTER_ARRAY);
        assertValue(defaultSecond, "charArrayValue", OTHER_CHARACTER_ARRAY, OTHER_CHARACTER_ARRAY);
        assertValue(defaultFirst, "intArrayValue", INTEGER_ARRAY, INTEGER_ARRAY);
        assertValue(defaultSecond, "intArrayValue", OTHER_INTEGER_ARRAY, OTHER_INTEGER_ARRAY);
        assertValue(defaultFirst, "longArrayValue", LONG_ARRAY, LONG_ARRAY);
        assertValue(defaultSecond, "longArrayValue", OTHER_LONG_ARRAY, OTHER_LONG_ARRAY);
        assertValue(defaultFirst, "floatArrayValue", FLOAT_ARRAY, FLOAT_ARRAY);
        assertValue(defaultSecond, "floatArrayValue", OTHER_FLOAT_ARRAY, OTHER_FLOAT_ARRAY);
        assertValue(defaultFirst, "doubleArrayValue", DOUBLE_ARRAY, DOUBLE_ARRAY);
        assertValue(defaultSecond, "doubleArrayValue", OTHER_DOUBLE_ARRAY, OTHER_DOUBLE_ARRAY);
        assertValue(defaultFirst, "stringArrayValue", STRING_ARRAY, STRING_ARRAY);
        assertValue(defaultSecond, "stringArrayValue", OTHER_STRING_ARRAY, OTHER_STRING_ARRAY);
        assertValue(defaultFirst, "classArrayValue", new TypeDescription[]{new TypeDescription.ForLoadedType(CLASS)}, CLASS_ARRAY);
        assertValue(defaultSecond, "classArrayValue", new TypeDescription[]{new TypeDescription.ForLoadedType(OTHER_CLASS)}, OTHER_CLASS_ARRAY);
        assertValue(defaultFirst, "enumArrayValue", new EnumerationDescription[]{new EnumerationDescription.ForLoadedEnumeration(ENUMERATION)}, ENUMERATION_ARRAY);
        assertValue(defaultSecond, "enumArrayValue", new EnumerationDescription[]{new EnumerationDescription.ForLoadedEnumeration(OTHER_ENUMERATION)}, OTHER_ENUMERATION_ARRAY);
        assertValue(defaultFirst, "annotationArrayValue", new AnnotationDescription[]{AnnotationDescription.ForLoadedAnnotation.of(ANNOTATION)}, ANNOTATION_ARRAY);
        assertValue(defaultSecond, "annotationArrayValue", new AnnotationDescription[]{AnnotationDescription.ForLoadedAnnotation.of(OTHER_ANNOTATION)}, OTHER_ANNOTATION_ARRAY);
    }

    @Test
    public void testRetention() throws Exception {
        assertThat(describe(first).getRetention(), is(RetentionPolicy.RUNTIME));
    }

    @Test
    public void testInheritance() throws Exception {
        assertThat(describe(first).isInherited(), is(false));
        assertThat(describe(defaultFirst).isInherited(), is(true));
    }

    @Test
    public void testDocumented() throws Exception {
        assertThat(describe(first).isDocumented(), is(false));
        assertThat(describe(defaultFirst).isDocumented(), is(true));
    }

    private void assertValue(Annotation annotation, String methodName, Object rawValue, Object loadedValue) throws Exception {
        assertThat(describe(annotation).getValue(new MethodDescription
                .ForLoadedMethod(annotation.annotationType().getDeclaredMethod(methodName))), is(rawValue));
        assertThat(describe(annotation).getValue(new MethodDescription.Latent(new TypeDescription.ForLoadedType(annotation.annotationType()),
                methodName,
                Opcodes.ACC_PUBLIC,
                Collections.<GenericTypeDescription>emptyList(),
                new TypeDescription.ForLoadedType(annotation.annotationType().getDeclaredMethod(methodName).getReturnType()),
                Collections.<ParameterDescription.Token>emptyList(),
                Collections.<GenericTypeDescription>emptyList(),
                Collections.<AnnotationDescription>emptyList(),
                MethodDescription.NO_DEFAULT_VALUE)), is(rawValue));
        assertThat(annotation.annotationType().getDeclaredMethod(methodName)
                .invoke(describe(annotation).prepare(annotation.annotationType()).load()), is(loadedValue));
    }

    public enum SampleEnumeration {
        VALUE,
        OTHER
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Sample {

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

    @Documented
    @Inherited
    @Retention(RetentionPolicy.RUNTIME)
    public @interface SampleDefault {

        boolean booleanValue() default BOOLEAN;

        byte byteValue() default BYTE;

        short shortValue() default SHORT;

        char charValue() default CHARACTER;

        int intValue() default INTEGER;

        long longValue() default LONG;

        float floatValue() default FLOAT;

        double doubleValue() default DOUBLE;

        String stringValue() default FOO;

        Class<?> classValue() default Void.class;

        Class<?> arrayClassValue() default Void[].class;

        SampleEnumeration enumValue() default SampleEnumeration.VALUE;

        Other annotationValue() default @Other;

        boolean[] booleanArrayValue() default BOOLEAN;

        byte[] byteArrayValue() default BYTE;

        short[] shortArrayValue() default SHORT;

        char[] charArrayValue() default CHARACTER;

        int[] intArrayValue() default INTEGER;

        long[] longArrayValue() default LONG;

        float[] floatArrayValue() default FLOAT;

        double[] doubleArrayValue() default DOUBLE;

        String[] stringArrayValue() default FOO;

        Class<?>[] classArrayValue() default Void.class;

        SampleEnumeration[] enumArrayValue() default SampleEnumeration.VALUE;

        Other[] annotationArrayValue() default @Other;
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Other {

        String value() default FOO;
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

    @SampleDefault
    private static class DefaultSample {

    }

    @SampleDefault(booleanValue = !BOOLEAN,
            byteValue = BYTE * 2,
            charValue = CHARACTER * 2,
            shortValue = SHORT * 2,
            intValue = INTEGER * 2,
            longValue = LONG * 2,
            floatValue = FLOAT * 2,
            doubleValue = DOUBLE * 2,
            stringValue = BAR,
            classValue = Object.class,
            arrayClassValue = Object[].class,
            enumValue = SampleEnumeration.OTHER,
            annotationValue = @Other(BAR),
            booleanArrayValue = !BOOLEAN,
            byteArrayValue = OTHER_BYTE,
            shortArrayValue = OTHER_SHORT,
            charArrayValue = OTHER_CHARACTER,
            intArrayValue = OTHER_INTEGER,
            longArrayValue = OTHER_LONG,
            floatArrayValue = OTHER_FLOAT,
            doubleArrayValue = OTHER_DOUBLE,
            stringArrayValue = BAR,
            classArrayValue = Object.class,
            enumArrayValue = SampleEnumeration.OTHER,
            annotationArrayValue = @Other(BAR))
    private static class NonDefaultSample {

    }

    @Other
    private static class EnumerationCarrier {

    }

    @Other(BAR)
    private static class OtherEnumerationCarrier {

    }
}
