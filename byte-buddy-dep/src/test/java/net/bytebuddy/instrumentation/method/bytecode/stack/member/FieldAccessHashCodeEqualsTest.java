package net.bytebuddy.instrumentation.method.bytecode.stack.member;

import net.bytebuddy.instrumentation.field.FieldDescription;
import net.bytebuddy.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class FieldAccessHashCodeEqualsTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);
    @Mock
    private FieldDescription instanceField, otherInstanceField, staticField;

    @Before
    public void setUp() throws Exception {
        when(staticField.isStatic()).thenReturn(true);
    }

    @Test
    public void testBasicHashCodeEquals() throws Exception {
        assertThat(FieldAccess.forField(instanceField).hashCode(), is(FieldAccess.forField(instanceField).hashCode()));
        assertThat(FieldAccess.forField(instanceField), is(FieldAccess.forField(instanceField)));
        assertThat(FieldAccess.forField(instanceField).hashCode(), not(is(FieldAccess.forField(otherInstanceField).hashCode())));
        assertThat(FieldAccess.forField(instanceField), not(is(FieldAccess.forField(otherInstanceField))));
        assertThat(FieldAccess.forField(staticField).hashCode(), not(is(FieldAccess.forField(otherInstanceField).hashCode())));
        assertThat(FieldAccess.forField(staticField), not(is(FieldAccess.forField(otherInstanceField))));
    }

    @Test
    public void testGetterHashCodeEquals() throws Exception {
        assertThat(FieldAccess.forField(instanceField).getter().hashCode(), is(FieldAccess.forField(instanceField).getter().hashCode()));
        assertThat(FieldAccess.forField(instanceField).getter(), is(FieldAccess.forField(instanceField).getter()));
        assertThat(FieldAccess.forField(instanceField).getter().hashCode(), not(is(FieldAccess.forField(otherInstanceField).getter().hashCode())));
        assertThat(FieldAccess.forField(instanceField).getter(), not(is(FieldAccess.forField(otherInstanceField).getter())));
        assertThat(FieldAccess.forField(staticField).getter().hashCode(), not(is(FieldAccess.forField(otherInstanceField).getter().hashCode())));
        assertThat(FieldAccess.forField(staticField).getter(), not(is(FieldAccess.forField(otherInstanceField).getter())));
    }

    @Test
    public void testPutterHashCodeEquals() throws Exception {
        assertThat(FieldAccess.forField(instanceField).putter().hashCode(), is(FieldAccess.forField(instanceField).putter().hashCode()));
        assertThat(FieldAccess.forField(instanceField).putter(), is(FieldAccess.forField(instanceField).putter()));
        assertThat(FieldAccess.forField(instanceField).putter().hashCode(), not(is(FieldAccess.forField(otherInstanceField).putter().hashCode())));
        assertThat(FieldAccess.forField(instanceField).putter(), not(is(FieldAccess.forField(otherInstanceField).putter())));
        assertThat(FieldAccess.forField(staticField).putter().hashCode(), not(is(FieldAccess.forField(otherInstanceField).putter().hashCode())));
        assertThat(FieldAccess.forField(staticField).putter(), not(is(FieldAccess.forField(otherInstanceField).putter())));
    }
}
