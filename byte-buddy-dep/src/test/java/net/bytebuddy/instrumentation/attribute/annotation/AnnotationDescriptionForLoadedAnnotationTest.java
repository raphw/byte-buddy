package net.bytebuddy.instrumentation.attribute.annotation;

import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.matcher.MethodMatcher;
import net.bytebuddy.instrumentation.type.TypeDescription;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;

import static net.bytebuddy.instrumentation.method.matcher.MethodMatchers.*;
import static net.bytebuddy.instrumentation.method.matcher.MethodMatchers.not;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class AnnotationDescriptionForLoadedAnnotationTest {

    private static final boolean BOOLEAN = true;
    private static final byte BYTE = 42;
    private static final short SHORT = 42;
    private static final char CHAR = 42;
    private static final int INT = 42;
    private static final long LONG = 42L;
    private static final float FLOAT = 42f;
    private static final double DOUBLE = 42d;
    private static final String STRING = "foo";
    private static final SampleEnumeration ENUM = SampleEnumeration.VALUE;
    private static final Class<?> CLASS = Void.class;
    private static final Other ANNOTATION = Other.Instance.INSTANCE;

    private static final boolean[] BOOLEAN_ARRAY = new boolean[]{BOOLEAN};
    private static final byte[] BYTE_ARRAY = new byte[]{BYTE};
    private static final short[] SHORT_ARRAY = new short[]{SHORT};
    private static final char[] CHAR_ARRAY = new char[]{CHAR};
    private static final int[] INT_ARRAY = new int[]{INT};
    private static final long[] LONG_ARRAY = new long[]{LONG};
    private static final float[] FLOAT_ARRAY = new float[]{FLOAT};
    private static final double[] DOUBLE_ARRAY = new double[]{DOUBLE};
    private static final String[] STRING_ARRAY = new String[]{STRING};
    private static final SampleEnumeration[] ENUM_ARRAY = new SampleEnumeration[]{ENUM};
    private static final Class<?>[] CLASS_ARRAY = new Class<?>[]{CLASS};
    private static final Other[] ANNOTATION_ARRAY = new Other[]{ANNOTATION};

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {boolean.class, Boolean.class, BOOLEAN, BOOLEAN},
                {byte.class, Byte.class, BYTE, BYTE},
                {short.class, Short.class, SHORT, SHORT},
                {char.class, Character.class, CHAR, CHAR},
                {int.class, Integer.class, INT, INT},
                {long.class, Long.class, LONG, LONG},
                {float.class, Float.class, FLOAT, FLOAT},
                {double.class, Double.class, DOUBLE, DOUBLE},
                {String.class, String.class, STRING, STRING},
                {Enum.class, AnnotationDescription.EnumerationValue.class, ENUM, new AnnotationDescription.EnumerationValue.ForLoadedEnumeration(ENUM)},
                {Class.class, TypeDescription.class, CLASS, new TypeDescription.ForLoadedType(CLASS)},
                {Annotation.class, AnnotationDescription.class, ANNOTATION, AnnotationDescription.ForLoadedAnnotation.of(ANNOTATION)},
                {boolean[].class, boolean[].class, BOOLEAN_ARRAY, BOOLEAN_ARRAY},
                {byte[].class, byte[].class, BYTE_ARRAY, BYTE_ARRAY},
                {short[].class, short[].class, SHORT_ARRAY, SHORT_ARRAY},
                {char[].class, char[].class, CHAR_ARRAY, CHAR_ARRAY},
                {int[].class, int[].class, INT_ARRAY, INT_ARRAY},
                {long[].class, long[].class, LONG_ARRAY, LONG_ARRAY},
                {float[].class, float[].class, FLOAT_ARRAY, FLOAT_ARRAY},
                {double[].class, double[].class, DOUBLE_ARRAY, DOUBLE_ARRAY},
                {String[].class, String[].class, STRING_ARRAY, STRING_ARRAY},
                {Enum[].class, AnnotationDescription.EnumerationValue[].class, ENUM_ARRAY, new AnnotationDescription.EnumerationValue[]{new AnnotationDescription.EnumerationValue.ForLoadedEnumeration(ENUM)}},
                {Class[].class, TypeDescription[].class, CLASS_ARRAY, new TypeDescription[]{new TypeDescription.ForLoadedType(CLASS)}},
                {Annotation[].class, AnnotationDescription[].class, ANNOTATION_ARRAY, new AnnotationDescription[]{AnnotationDescription.ForLoadedAnnotation.of(ANNOTATION)}}
        });
    }

    private final MethodDescription methodDescription;

    private final Class<?> castType;

    private final Object expected;

    private final Object expectedLoaded;

    public AnnotationDescriptionForLoadedAnnotationTest(Class<?> type,
                                                        Class<?> castType,
                                                        Object expectedLoaded,
                                                        Object expected) {
        MethodMatcher arrayMatcher = nameContains("Array");
        if (type.isArray()) {
            type = type.getComponentType();
        } else {
            arrayMatcher = not(arrayMatcher);
        }
        methodDescription = new TypeDescription.ForLoadedType(Sample.class).getDeclaredMethods()
                .filter(nameStartsWith(type.getSimpleName().toLowerCase()).and(arrayMatcher)).getOnly();
        this.castType = castType;
        this.expectedLoaded = expectedLoaded;
        this.expected = expected;
    }

    private AnnotationDescription annotationDescription;

    @Before
    public void setUp() throws Exception {
        annotationDescription = AnnotationDescription.ForLoadedAnnotation.of(Sample.Instance.INSTANCE);
    }

    @Test
    public void testPropertyRetrieval() throws Exception {
        assertThat(annotationDescription.getValue(methodDescription), is(expected));
    }

    @Test
    public void testPropertyRetrievalCasted() throws Exception {
        assertThat(castType.isInstance(annotationDescription.getValue(methodDescription, castType)), is(true));
        assertThat(annotationDescription.getValue(methodDescription, castType), is(expected));
    }

    @Test
    public void testPropertyRetrievalLoaded() throws Exception {
        Sample sample = annotationDescription.prepare(Sample.class).load();
        assertThat(sample, notNullValue(Sample.class));
        assertEquals(Sample.class, sample.annotationType());
        assertThat(Sample.class.getDeclaredMethod(methodDescription.getName()).invoke(sample), is(expectedLoaded));
    }

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

        static enum Instance implements Sample {

            INSTANCE;

            @Override
            public boolean booleanValue() {
                return BOOLEAN;
            }

            @Override
            public byte byteValue() {
                return BYTE;
            }

            @Override
            public short shortValue() {
                return SHORT;
            }

            @Override
            public char charValue() {
                return CHAR;
            }

            @Override
            public int intValue() {
                return INT;
            }

            @Override
            public long longValue() {
                return LONG;
            }

            @Override
            public float floatValue() {
                return FLOAT;
            }

            @Override
            public double doubleValue() {
                return DOUBLE;
            }

            @Override
            public String stringValue() {
                return STRING;
            }

            @Override
            public Class<?> classValue() {
                return CLASS;
            }

            @Override
            public SampleEnumeration enumValue() {
                return ENUM;
            }

            @Override
            public Other annotationValue() {
                return ANNOTATION;
            }

            @Override
            public boolean[] booleanArrayValue() {
                return BOOLEAN_ARRAY;
            }

            @Override
            public byte[] byteArrayValue() {
                return BYTE_ARRAY;
            }

            @Override
            public short[] shortArrayValue() {
                return SHORT_ARRAY;
            }

            @Override
            public char[] charArrayValue() {
                return CHAR_ARRAY;
            }

            @Override
            public int[] intArrayValue() {
                return INT_ARRAY;
            }

            @Override
            public long[] longArrayValue() {
                return LONG_ARRAY;
            }

            @Override
            public float[] floatArrayValue() {
                return FLOAT_ARRAY;
            }

            @Override
            public double[] doubleArrayValue() {
                return DOUBLE_ARRAY;
            }

            @Override
            public String[] stringArrayValue() {
                return STRING_ARRAY;
            }

            @Override
            public Class<?>[] classArrayValue() {
                return CLASS_ARRAY;
            }

            @Override
            public SampleEnumeration[] enumArrayValue() {
                return ENUM_ARRAY;
            }

            @Override
            public Other[] annotationArrayValue() {
                return ANNOTATION_ARRAY;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return Sample.class;
            }
        }
    }

    public @interface Other {

        static enum Instance implements Other {

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
