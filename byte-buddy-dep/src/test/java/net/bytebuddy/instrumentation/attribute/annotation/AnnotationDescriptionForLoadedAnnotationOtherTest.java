package net.bytebuddy.instrumentation.attribute.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class AnnotationDescriptionForLoadedAnnotationOtherTest extends AbstractAnnotationDescriptionTest<AnnotationDescriptionForLoadedAnnotationOtherTest.Sample> {

    @Override
    protected Sample first() {
        return First.class.getAnnotation(Sample.class);
    }

    @Override
    protected Sample second() {
        return Second.class.getAnnotation(Sample.class);
    }

    @Override
    protected AnnotationDescription firstValue() {
        return AnnotationDescription.ForLoadedAnnotation.of(First.class.getAnnotation(Sample.class));
    }

    @Override
    protected AnnotationDescription secondValue() {
        return AnnotationDescription.ForLoadedAnnotation.of(Second.class.getAnnotation(Sample.class));
    }

    @Sample(booleanValue = true,
            byteValue = 42,
            charValue = 42,
            shortValue = 42,
            intValue = 42,
            longValue = 42L,
            floatValue = 42f,
            doubleValue = 42d,
            stringValue = "foo",
            classValue = Void.class,
            arrayClassValue = Void[].class,
            enumValue = SampleEnumeration.VALUE,
            annotationValue = @Other,
            booleanArrayValue = true,
            byteArrayValue = 42,
            shortArrayValue = 42,
            charArrayValue = 42,
            intArrayValue = 42,
            longArrayValue = 42L,
            floatArrayValue = 42f,
            doubleArrayValue = 42d,
            stringArrayValue = "foo",
            classArrayValue = Void.class,
            enumArrayValue = SampleEnumeration.VALUE,
            annotationArrayValue = @Other)
    private static class First {
    }

    @Sample(booleanValue = true,
            byteValue = 42,
            charValue = 42,
            shortValue = 42,
            intValue = 42,
            longValue = 42L,
            floatValue = 42f,
            doubleValue = 42d,
            stringValue = "foo",
            classValue = Void.class,
            arrayClassValue = Void[].class,
            enumValue = SampleEnumeration.VALUE,
            annotationValue = @Other,
            booleanArrayValue = true,
            byteArrayValue = 42,
            shortArrayValue = 42,
            charArrayValue = 42,
            intArrayValue = 42,
            longArrayValue = 42L,
            floatArrayValue = 42f,
            doubleArrayValue = 42d,
            stringArrayValue = "bar",
            classArrayValue = Void.class,
            enumArrayValue = SampleEnumeration.VALUE,
            annotationArrayValue = @Other)
    private static class Second {
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
    }

    public static enum SampleEnumeration {
        VALUE
    }
}
