package net.bytebuddy.description.enumeration;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class AbstractEnumerationDescriptionTest {

    private Method annotationMethod;

    private EnumerationDescription describe(Enum<?> enumeration) {
        if (enumeration == Sample.FIRST) {
            return describe(enumeration, FirstCarrier.class, new MethodDescription.ForLoadedMethod(annotationMethod));
        } else {
            return describe(enumeration, SecondCarrier.class, new MethodDescription.ForLoadedMethod(annotationMethod));
        }
    }

    protected abstract EnumerationDescription describe(Enum<?> enumeration,
                                                       Class<?> carrierType,
                                                       MethodDescription.InDefinedShape annotationMethod);

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
        assertThat(describe(Sample.FIRST).getEnumerationType(), is((TypeDescription) new TypeDescription.ForLoadedType(Sample.class)));
        assertThat(describe(Sample.SECOND).getEnumerationType(), is((TypeDescription) new TypeDescription.ForLoadedType(Sample.class)));
    }

    @Test
    public void assertHashCode() throws Exception {
        assertThat(describe(Sample.FIRST).hashCode(), is(Sample.FIRST.name().hashCode() + 31 * new TypeDescription.ForLoadedType(Sample.class).hashCode()));
        assertThat(describe(Sample.SECOND).hashCode(), is(Sample.SECOND.name().hashCode() + 31 * new TypeDescription.ForLoadedType(Sample.class).hashCode()));
        assertThat(describe(Sample.FIRST).hashCode(), not(describe(Sample.SECOND).hashCode()));
    }

    @Test
    public void assertEquals() throws Exception {
        EnumerationDescription identical = describe(Sample.FIRST);
        assertThat(identical, is(identical));
        EnumerationDescription equalFirst = mock(EnumerationDescription.class);
        when(equalFirst.getValue()).thenReturn(Sample.FIRST.name());
        when(equalFirst.getEnumerationType()).thenReturn(new TypeDescription.ForLoadedType(Sample.class));
        assertThat(describe(Sample.FIRST), is(equalFirst));
        EnumerationDescription equalSecond = mock(EnumerationDescription.class);
        when(equalSecond.getValue()).thenReturn(Sample.SECOND.name());
        when(equalSecond.getEnumerationType()).thenReturn(new TypeDescription.ForLoadedType(Sample.class));
        assertThat(describe(Sample.SECOND), is(equalSecond));
        EnumerationDescription equalFirstTypeOnly = mock(EnumerationDescription.class);
        when(equalFirstTypeOnly.getValue()).thenReturn(Sample.SECOND.name());
        when(equalFirstTypeOnly.getEnumerationType()).thenReturn(new TypeDescription.ForLoadedType(Sample.class));
        assertThat(describe(Sample.FIRST), not(equalFirstTypeOnly));
        EnumerationDescription equalFirstNameOnly = mock(EnumerationDescription.class);
        when(equalFirstNameOnly.getValue()).thenReturn(Sample.FIRST.name());
        when(equalFirstNameOnly.getEnumerationType()).thenReturn(new TypeDescription.ForLoadedType(Other.class));
        assertThat(describe(Sample.FIRST), not(equalFirstNameOnly));
        assertThat(describe(Sample.FIRST), not(equalSecond));
        assertThat(describe(Sample.FIRST), not(new Object()));
        assertThat(describe(Sample.FIRST), not(equalTo(null)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIncompatible() throws Exception {
        describe(Sample.FIRST).load(Other.class);
    }

    @Test
    public void testLoad() throws Exception {
        assertThat(describe(Sample.FIRST).load(Sample.class), is(Sample.FIRST));
    }

    public enum Sample {
        FIRST,
        SECOND
    }

    private enum Other {
        INSTANCE
    }

    public @interface Carrier {

        Sample value();
    }

    @Carrier(Sample.FIRST)
    private static class FirstCarrier {

    }

    @Carrier(Sample.SECOND)
    private static class SecondCarrier {

    }
}
