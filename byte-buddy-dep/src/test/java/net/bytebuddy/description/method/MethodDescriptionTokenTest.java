package net.bytebuddy.description.method;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MethodDescriptionTokenTest {

    private static final String FOO = "foo";

    private static final String BAR = "bar";

    private static final int MODIFIERS = 42;

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private GenericTypeDescription first, second;

    @Mock
    private TypeDescription firstRaw, secondRaw;

    @Mock
    private ParameterDescription.Token firstParameter, secondParameter;

    @Before
    public void setUp() throws Exception {
        when(first.asRawType()).thenReturn(firstRaw);
        when(second.asRawType()).thenReturn(secondRaw);
        when(firstParameter.getType()).thenReturn(first);
        when(secondParameter.getType()).thenReturn(second);
    }

    @Test
    public void testMethodEquality() throws Exception {
        assertThat(new MethodDescription.Token(FOO,
                        MODIFIERS,
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        first,
                        Collections.singletonList(firstParameter),
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        Collections.singletonList(mock(AnnotationDescription.class)),
                        null),
                is(new MethodDescription.Token(FOO,
                        MODIFIERS,
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        first,
                        Collections.singletonList(firstParameter),
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        Collections.singletonList(mock(AnnotationDescription.class)),
                        null)));
    }

    @Test
    public void testMethodNameInequality() throws Exception {
        assertThat(new MethodDescription.Token(FOO,
                        MODIFIERS,
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        first,
                        Collections.singletonList(firstParameter),
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        Collections.singletonList(mock(AnnotationDescription.class)),
                        null),
                not(new MethodDescription.Token(BAR,
                        MODIFIERS,
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        first,
                        Collections.singletonList(firstParameter),
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        Collections.singletonList(mock(AnnotationDescription.class)),
                        null)));
    }

    @Test
    public void testReturnTypeInequality() throws Exception {
        assertThat(new MethodDescription.Token(FOO,
                        MODIFIERS,
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        first,
                        Collections.singletonList(firstParameter),
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        Collections.singletonList(mock(AnnotationDescription.class)),
                        null),
                not(new MethodDescription.Token(FOO,
                        MODIFIERS,
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        second,
                        Collections.singletonList(firstParameter),
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        Collections.singletonList(mock(AnnotationDescription.class)),
                        null)));
    }

    @Test
    public void testParameterTypeInequality() throws Exception {
        assertThat(new MethodDescription.Token(FOO,
                        MODIFIERS,
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        first,
                        Collections.singletonList(firstParameter),
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        Collections.singletonList(mock(AnnotationDescription.class)),
                        null),
                not(new MethodDescription.Token(FOO,
                        MODIFIERS,
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        second,
                        Collections.singletonList(secondParameter),
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        Collections.singletonList(mock(AnnotationDescription.class)),
                        null)));
    }

    @Test
    public void testParameterTypeLengthInequality() throws Exception {
        assertThat(new MethodDescription.Token(FOO,
                        MODIFIERS,
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        first,
                        Collections.singletonList(firstParameter),
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        Collections.singletonList(mock(AnnotationDescription.class)),
                        null),
                not(new MethodDescription.Token(FOO,
                        MODIFIERS,
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        second,
                        Collections.<ParameterDescription.Token>emptyList(),
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        Collections.singletonList(mock(AnnotationDescription.class)),
                        null)));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(MethodDescription.Token.class).applyCustom();
    }
}
