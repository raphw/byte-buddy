package net.bytebuddy.instrumentation.attribute.annotation;

import net.bytebuddy.instrumentation.type.TypeDescription;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class AbstractEnumerationValueTest<T extends Enum<T>> {

    protected abstract T first();

    protected abstract T second();

    protected abstract AnnotationDescription.EnumerationValue firstValue();

    protected abstract AnnotationDescription.EnumerationValue secondValue();

    @Test
    public void testPrecondition() throws Exception {
        assertThat(first(), not(equalTo(second())));
        assertThat(first(), equalTo(first()));
        assertThat(second(), equalTo(second()));
        assertThat(first().getDeclaringClass(), equalTo(second().getDeclaringClass()));
        assertThat(firstValue().getEnumerationType(), equalTo(secondValue().getEnumerationType()));
        assertThat(firstValue().getEnumerationType().represents(first().getDeclaringClass()), is(true));
        assertThat(secondValue().getEnumerationType().represents(second().getDeclaringClass()), is(true));
    }

    @Test
    public void assertValue() throws Exception {
        assertThat(firstValue().getValue(), is(first().name()));
        assertThat(secondValue().getValue(), is(second().name()));
    }

    @Test
    public void assertToString() throws Exception {
        assertThat(firstValue().toString(), is(first().toString()));
        assertThat(secondValue().toString(), is(second().toString()));
    }

    @Test
    public void assertType() throws Exception {
        assertThat(firstValue().getEnumerationType(), is((TypeDescription) new TypeDescription.ForLoadedType(first().getDeclaringClass())));
        assertThat(secondValue().getEnumerationType(), is((TypeDescription) new TypeDescription.ForLoadedType(first().getDeclaringClass())));
    }

    @Test
    public void assertHashCode() throws Exception {
        assertThat(firstValue().hashCode(), is(first().name().hashCode() + 31 * firstValue().getEnumerationType().hashCode()));
        assertThat(firstValue().hashCode(), not(is(second().name().hashCode() + 31 * firstValue().getEnumerationType().hashCode())));
        assertThat(secondValue().hashCode(), is(second().name().hashCode() + 31 * firstValue().getEnumerationType().hashCode()));
    }

    @Test
    public void assertEquals() throws Exception {
        AnnotationDescription.EnumerationValue enumerationValue = firstValue();
        assertThat(enumerationValue, equalTo(enumerationValue));
        AnnotationDescription.EnumerationValue otherType = mock(AnnotationDescription.EnumerationValue.class);
        when(otherType.getEnumerationType()).thenReturn(mock(TypeDescription.class));
        when(otherType.getValue()).thenReturn(first().name());
        assertThat(firstValue(), not(equalTo(otherType)));
        assertThat(firstValue(), equalTo((AnnotationDescription.EnumerationValue) new AnnotationDescription.EnumerationValue.ForLoadedEnumeration(first())));
        assertThat(firstValue(), not(equalTo((AnnotationDescription.EnumerationValue) new AnnotationDescription.EnumerationValue.ForLoadedEnumeration(second()))));
        assertThat(firstValue(), not(equalTo(new Object())));
        assertThat(firstValue(), not(equalTo(null)));
        assertThat(firstValue(), not(equalTo(secondValue())));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIncompatible() throws Exception {
        firstValue().load(Other.class);
    }

    private static enum Other {
        INSTANCE
    }
}
