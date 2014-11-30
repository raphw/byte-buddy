package net.bytebuddy.instrumentation.type;

import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class InstrumentedTypeAbstractBaseTokenTest {

    private static final String FOO = "foo", BAR = "bar";
    private static final int MODIFIER = 0;

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeDescription typeDescription;
    @Mock
    private InstrumentedType.AbstractBase abstractBase;

    @Test
    public void testFieldTokenHashCodeEquals() throws Exception {
        assertThat(abstractBase.new FieldToken(FOO, typeDescription, MODIFIER).hashCode(),
                is(abstractBase.new FieldToken(FOO, typeDescription, MODIFIER).hashCode()));
        assertThat(abstractBase.new FieldToken(FOO, typeDescription, MODIFIER),
                is(abstractBase.new FieldToken(FOO, typeDescription, MODIFIER)));
        assertThat(abstractBase.new FieldToken(FOO, typeDescription, MODIFIER).hashCode(),
                not(is(abstractBase.new FieldToken(BAR, typeDescription, MODIFIER).hashCode())));
        assertThat(abstractBase.new FieldToken(FOO, typeDescription, MODIFIER),
                not(is(abstractBase.new FieldToken(BAR, typeDescription, MODIFIER))));
    }

    @Test
    public void testMethodTokenHashCodeEquals() throws Exception {
        assertThat(abstractBase.new MethodToken(FOO, typeDescription, Collections.<TypeDescription>emptyList(), Collections.<TypeDescription>emptyList(), MODIFIER).hashCode(),
                is(abstractBase.new MethodToken(FOO, typeDescription, Collections.<TypeDescription>emptyList(), Collections.<TypeDescription>emptyList(), MODIFIER).hashCode()));
        assertThat(abstractBase.new MethodToken(FOO, typeDescription, Collections.<TypeDescription>emptyList(), Collections.<TypeDescription>emptyList(), MODIFIER),
                is(abstractBase.new MethodToken(FOO, typeDescription, Collections.<TypeDescription>emptyList(), Collections.<TypeDescription>emptyList(), MODIFIER)));
        assertThat(abstractBase.new MethodToken(FOO, typeDescription, Collections.<TypeDescription>emptyList(), Collections.<TypeDescription>emptyList(), MODIFIER).hashCode(),
                not(is(abstractBase.new MethodToken(BAR, typeDescription, Collections.<TypeDescription>emptyList(), Collections.<TypeDescription>emptyList(), MODIFIER).hashCode())));
        assertThat(abstractBase.new MethodToken(FOO, typeDescription, Collections.<TypeDescription>emptyList(), Collections.<TypeDescription>emptyList(), MODIFIER),
                not(is(abstractBase.new MethodToken(BAR, typeDescription, Collections.<TypeDescription>emptyList(), Collections.<TypeDescription>emptyList(), MODIFIER))));
    }
}
