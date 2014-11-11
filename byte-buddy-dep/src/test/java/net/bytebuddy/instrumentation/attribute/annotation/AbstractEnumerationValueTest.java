package net.bytebuddy.instrumentation.attribute.annotation;

import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.type.TypeDescription;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class AbstractEnumerationValueTest {

    private AnnotationDescription.EnumerationValue describe(Enum<?> enumeration) {
        if (enumeration == Sample.FIRST) {
            return describe(enumeration, FirstCarrier.class, new MethodDescription.ForLoadedMethod(annotationMethod));
        } else {
            return describe(enumeration, SecondCarrier.class, new MethodDescription.ForLoadedMethod(annotationMethod));
        }
    }

    protected abstract AnnotationDescription.EnumerationValue describe(Enum<?> enumeration,
                                                                       Class<?> carrierType,
                                                                       MethodDescription annotationMethod);

    private Method annotationMethod;

    @Before
    public void setUp() throws Exception {
        annotationMethod = Carrier.class.getDeclaredMethod("value");
    }

    @Test
    public void testPrecondition() throws Exception {
        assertThat(describe(Sample.FIRST).getEnumerationType().represents(Sample.class), is(true));
        assertThat(describe(Sample.SECOND).getEnumerationType().represents(Sample.class), is(true));
        assertThat(describe(Sample.FIRST).getEnumerationType().represents(Other.class), is(false));
    }

    @Test
    public void assertValue() throws Exception {
        assertThat(describe(Sample.FIRST).getValue(), is(Sample.FIRST.name()));
        assertThat(describe(Sample.SECOND).getValue(), is(Sample.SECOND.name()));
    }

    @Test
    public void assertToString() throws Exception {
        assertThat(describe(Sample.FIRST).toString(), is(Sample.FIRST.toString()));
        assertThat(describe(Sample.SECOND).toString(), is(Sample.SECOND.toString()));
    }

    @Test
    public void assertType() throws Exception {
        assertThat(describe(Sample.FIRST).getEnumerationType(), equalTo((TypeDescription) new TypeDescription.ForLoadedType(Sample.class)));
        assertThat(describe(Sample.SECOND).getEnumerationType(), equalTo((TypeDescription) new TypeDescription.ForLoadedType(Sample.class)));
    }

    @Test
    public void assertHashCode() throws Exception {
        assertThat(describe(Sample.FIRST).hashCode(), is(Sample.FIRST.name().hashCode() + 31 * new TypeDescription.ForLoadedType(Sample.class).hashCode()));
        assertThat(describe(Sample.SECOND).hashCode(), is(Sample.SECOND.name().hashCode() + 31 * new TypeDescription.ForLoadedType(Sample.class).hashCode()));
        assertThat(describe(Sample.FIRST).hashCode(), not(is(describe(Sample.SECOND).hashCode())));
    }

    @Test
    public void assertEquals() throws Exception {
        AnnotationDescription.EnumerationValue identical = describe(Sample.FIRST);
        assertThat(identical, equalTo(identical));
        AnnotationDescription.EnumerationValue equalFirst = mock(AnnotationDescription.EnumerationValue.class);
        when(equalFirst.getValue()).thenReturn(Sample.FIRST.name());
        when(equalFirst.getEnumerationType()).thenReturn(new TypeDescription.ForLoadedType(Sample.class));
        assertThat(describe(Sample.FIRST), equalTo(equalFirst));
        AnnotationDescription.EnumerationValue equalSecond = mock(AnnotationDescription.EnumerationValue.class);
        when(equalSecond.getValue()).thenReturn(Sample.SECOND.name());
        when(equalSecond.getEnumerationType()).thenReturn(new TypeDescription.ForLoadedType(Sample.class));
        assertThat(describe(Sample.SECOND), equalTo(equalSecond));
        AnnotationDescription.EnumerationValue equalFirstTypeOnly = mock(AnnotationDescription.EnumerationValue.class);
        when(equalFirstTypeOnly.getValue()).thenReturn(Sample.SECOND.name());
        when(equalFirstTypeOnly.getEnumerationType()).thenReturn(new TypeDescription.ForLoadedType(Sample.class));
        assertThat(describe(Sample.FIRST), not(equalTo(equalFirstTypeOnly)));
        AnnotationDescription.EnumerationValue equalFirstNameOnly = mock(AnnotationDescription.EnumerationValue.class);
        when(equalFirstNameOnly.getValue()).thenReturn(Sample.FIRST.name());
        when(equalFirstNameOnly.getEnumerationType()).thenReturn(new TypeDescription.ForLoadedType(Other.class));
        assertThat(describe(Sample.FIRST), not(equalTo(equalFirstNameOnly)));
        assertThat(describe(Sample.FIRST), not(equalTo(equalSecond)));
        assertThat(describe(Sample.FIRST), not(equalTo(new Object())));
        assertThat(describe(Sample.FIRST), not(equalTo(null)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIncompatible() throws Exception {
        describe(Sample.FIRST).load(Other.class);
    }

    public static enum Sample {
        FIRST,
        SECOND
    }

    private static enum Other {
        INSTANCE
    }

    @Carrier(Sample.FIRST)
    private static class FirstCarrier {
    }

    @Carrier(Sample.SECOND)
    private static class SecondCarrier {
    }

    public static @interface Carrier {
        Sample value();
    }
}
