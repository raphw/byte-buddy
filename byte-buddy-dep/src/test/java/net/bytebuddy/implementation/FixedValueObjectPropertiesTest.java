package net.bytebuddy.implementation;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class FixedValueObjectPropertiesTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux";

    @Test
    public void testConstantPoolValue() throws Exception {
        assertThat(FixedValue.value(FOO).hashCode(), is(FixedValue.value(FOO).hashCode()));
        assertThat(FixedValue.value(FOO), is(FixedValue.value(FOO)));
        assertThat(FixedValue.value(FOO).hashCode(), not(is(FixedValue.value(BAR).hashCode())));
        assertThat(FixedValue.value(FOO), not(is(FixedValue.value(BAR))));
        assertThat(FixedValue.value(FOO).hashCode(), not(is(FixedValue.reference(FOO).hashCode())));
        assertThat(FixedValue.value(FOO), not(is(FixedValue.reference(FOO))));
    }

    @Test
    public void testReferenceValue() throws Exception {
        assertThat(FixedValue.reference(FOO).hashCode(), is(FixedValue.reference(FOO).hashCode()));
        assertThat(FixedValue.reference(FOO), is(FixedValue.reference(FOO)));
        assertThat(FixedValue.reference(FOO).hashCode(), not(is(FixedValue.value(FOO).hashCode())));
        assertThat(FixedValue.reference(FOO), not(is(FixedValue.value(FOO))));
        assertThat(FixedValue.reference(FOO).hashCode(), not(is(FixedValue.reference(BAR).hashCode())));
        assertThat(FixedValue.reference(FOO), not(is(FixedValue.reference(BAR))));
    }

    @Test
    public void testReferenceValueWithExplicitFieldName() throws Exception {
        assertThat(FixedValue.reference(FOO, QUX).hashCode(), is(FixedValue.reference(FOO, QUX).hashCode()));
        assertThat(FixedValue.reference(FOO, QUX), is(FixedValue.reference(FOO, QUX)));
        assertThat(FixedValue.reference(FOO, QUX).hashCode(), not(is(FixedValue.reference(BAR, QUX).hashCode())));
        assertThat(FixedValue.reference(FOO, QUX), not(is(FixedValue.reference(BAR, QUX))));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(FixedValue.ForPoolValue.class).skipSynthetic().apply();
        ObjectPropertyAssertion.of(FixedValue.ForStaticField.class).apply();
    }
}
