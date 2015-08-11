package net.bytebuddy.description.method;

import net.bytebuddy.description.annotation.AnnotationDescription;
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

    private static final String FOO = "foo", BAR = "bar";

    private static final int MODIFIERS = 42, MASK = 12, UPDATE = 7;

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private GenericTypeDescription first, second;

    @Mock
    private AnnotationDescription firstAnnotation, secondAnnotation;

    @Mock
    private TypeDescription firstRaw, secondRaw;

    @Mock
    private ParameterDescription.Token firstParameter, secondParameter;

    @Mock
    private Object firstDefault, secondDefault;

    @Before
    public void setUp() throws Exception {
        when(first.asErasure()).thenReturn(firstRaw);
        when(second.asErasure()).thenReturn(secondRaw);
        when(firstParameter.getType()).thenReturn(first);
        when(secondParameter.getType()).thenReturn(second);
    }

    @Test
    public void testMethodEqualityHashCode() throws Exception {
        assertThat(new MethodDescription.Token(FOO,
                        MODIFIERS,
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        first,
                        Collections.singletonList(firstParameter),
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        Collections.singletonList(mock(AnnotationDescription.class)),
                        MethodDescription.NO_DEFAULT_VALUE).hashCode(),
                is(new MethodDescription.Token(FOO,
                        MODIFIERS,
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        first,
                        Collections.singletonList(firstParameter),
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        Collections.singletonList(mock(AnnotationDescription.class)),
                        MethodDescription.NO_DEFAULT_VALUE).hashCode()));
    }

    @Test
    public void testMethodNameInequalityHashCode() throws Exception {
        assertThat(new MethodDescription.Token(FOO,
                        MODIFIERS,
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        first,
                        Collections.singletonList(firstParameter),
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        Collections.singletonList(mock(AnnotationDescription.class)),
                        MethodDescription.NO_DEFAULT_VALUE).hashCode(),
                not(new MethodDescription.Token(BAR,
                        MODIFIERS,
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        first,
                        Collections.singletonList(firstParameter),
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        Collections.singletonList(mock(AnnotationDescription.class)),
                        MethodDescription.NO_DEFAULT_VALUE).hashCode()));
    }

    @Test
    public void testReturnTypeInequalityHashCode() throws Exception {
        assertThat(new MethodDescription.Token(FOO,
                        MODIFIERS,
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        first,
                        Collections.singletonList(firstParameter),
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        Collections.singletonList(mock(AnnotationDescription.class)),
                        MethodDescription.NO_DEFAULT_VALUE).hashCode(),
                not(new MethodDescription.Token(FOO,
                        MODIFIERS,
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        second,
                        Collections.singletonList(firstParameter),
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        Collections.singletonList(mock(AnnotationDescription.class)),
                        MethodDescription.NO_DEFAULT_VALUE).hashCode()));
    }

    @Test
    public void testParameterTypeInequalityHashCode() throws Exception {
        assertThat(new MethodDescription.Token(FOO,
                        MODIFIERS,
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        first,
                        Collections.singletonList(firstParameter),
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        Collections.singletonList(mock(AnnotationDescription.class)),
                        MethodDescription.NO_DEFAULT_VALUE).hashCode(),
                not(new MethodDescription.Token(FOO,
                        MODIFIERS,
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        first,
                        Collections.singletonList(secondParameter),
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        Collections.singletonList(mock(AnnotationDescription.class)),
                        MethodDescription.NO_DEFAULT_VALUE).hashCode()));
    }

    @Test
    public void testParameterTypeLengthInequalityHashCode() throws Exception {
        assertThat(new MethodDescription.Token(FOO,
                        MODIFIERS,
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        first,
                        Collections.singletonList(firstParameter),
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        Collections.singletonList(mock(AnnotationDescription.class)),
                        MethodDescription.NO_DEFAULT_VALUE).hashCode(),
                not(new MethodDescription.Token(FOO,
                        MODIFIERS,
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        first,
                        Collections.<ParameterDescription.Token>emptyList(),
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        Collections.singletonList(mock(AnnotationDescription.class)),
                        MethodDescription.NO_DEFAULT_VALUE).hashCode()));
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
                        MethodDescription.NO_DEFAULT_VALUE),
                is(new MethodDescription.Token(FOO,
                        MODIFIERS,
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        first,
                        Collections.singletonList(firstParameter),
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        Collections.singletonList(mock(AnnotationDescription.class)),
                        MethodDescription.NO_DEFAULT_VALUE)));
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
                        MethodDescription.NO_DEFAULT_VALUE),
                not(new MethodDescription.Token(BAR,
                        MODIFIERS,
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        first,
                        Collections.singletonList(firstParameter),
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        Collections.singletonList(mock(AnnotationDescription.class)),
                        MethodDescription.NO_DEFAULT_VALUE)));
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
                        MethodDescription.NO_DEFAULT_VALUE),
                not(new MethodDescription.Token(FOO,
                        MODIFIERS,
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        second,
                        Collections.singletonList(firstParameter),
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        Collections.singletonList(mock(AnnotationDescription.class)),
                        MethodDescription.NO_DEFAULT_VALUE)));
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
                        MethodDescription.NO_DEFAULT_VALUE),
                not(new MethodDescription.Token(FOO,
                        MODIFIERS,
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        first,
                        Collections.singletonList(secondParameter),
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        Collections.singletonList(mock(AnnotationDescription.class)),
                        MethodDescription.NO_DEFAULT_VALUE)));
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
                        MethodDescription.NO_DEFAULT_VALUE),
                not(new MethodDescription.Token(FOO,
                        MODIFIERS,
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        first,
                        Collections.<ParameterDescription.Token>emptyList(),
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        Collections.singletonList(mock(AnnotationDescription.class)),
                        MethodDescription.NO_DEFAULT_VALUE)));
    }

    @Test
    public void testTokenIdentity() throws Exception {
        assertThat(new MethodDescription.Token(FOO,
                MODIFIERS,
                Collections.singletonList(first),
                first,
                Collections.singletonList(firstParameter),
                Collections.singletonList(first),
                Collections.singletonList(firstAnnotation),
                MethodDescription.NO_DEFAULT_VALUE)
                .isIdenticalTo(new MethodDescription.Token(FOO,
                        MODIFIERS,
                        Collections.singletonList(first),
                        first,
                        Collections.singletonList(firstParameter),
                        Collections.singletonList(first),
                        Collections.singletonList(firstAnnotation),
                        MethodDescription.NO_DEFAULT_VALUE)), is(true));
    }

    @Test
    public void testTokenNoIdentityName() throws Exception {
        assertThat(new MethodDescription.Token(FOO,
                MODIFIERS,
                Collections.singletonList(first),
                first,
                Collections.singletonList(firstParameter),
                Collections.singletonList(first),
                Collections.singletonList(firstAnnotation),
                MethodDescription.NO_DEFAULT_VALUE)
                .isIdenticalTo(new MethodDescription.Token(BAR,
                        MODIFIERS,
                        Collections.singletonList(first),
                        first,
                        Collections.singletonList(firstParameter),
                        Collections.singletonList(first),
                        Collections.singletonList(firstAnnotation),
                        MethodDescription.NO_DEFAULT_VALUE)), is(false));
    }

    @Test
    public void testTokenNoIdentityModifiers() throws Exception {
        assertThat(new MethodDescription.Token(FOO,
                MODIFIERS,
                Collections.singletonList(first),
                first,
                Collections.singletonList(firstParameter),
                Collections.singletonList(first),
                Collections.singletonList(firstAnnotation),
                MethodDescription.NO_DEFAULT_VALUE)
                .isIdenticalTo(new MethodDescription.Token(FOO,
                        MODIFIERS * 2,
                        Collections.singletonList(first),
                        first,
                        Collections.singletonList(firstParameter),
                        Collections.singletonList(first),
                        Collections.singletonList(firstAnnotation),
                        MethodDescription.NO_DEFAULT_VALUE)), is(false));
    }

    @Test
    public void testTokenNoIdentityTypeVariables() throws Exception {
        assertThat(new MethodDescription.Token(FOO,
                MODIFIERS,
                Collections.singletonList(first),
                first,
                Collections.singletonList(firstParameter),
                Collections.singletonList(first),
                Collections.singletonList(firstAnnotation),
                MethodDescription.NO_DEFAULT_VALUE)
                .isIdenticalTo(new MethodDescription.Token(FOO,
                        MODIFIERS,
                        Collections.singletonList(second),
                        first,
                        Collections.singletonList(firstParameter),
                        Collections.singletonList(first),
                        Collections.singletonList(firstAnnotation),
                        MethodDescription.NO_DEFAULT_VALUE)), is(false));
    }

    @Test
    public void testTokenNoIdentityReturnType() throws Exception {
        assertThat(new MethodDescription.Token(FOO,
                MODIFIERS,
                Collections.singletonList(first),
                first,
                Collections.singletonList(firstParameter),
                Collections.singletonList(first),
                Collections.singletonList(firstAnnotation),
                MethodDescription.NO_DEFAULT_VALUE)
                .isIdenticalTo(new MethodDescription.Token(FOO,
                        MODIFIERS,
                        Collections.singletonList(first),
                        second,
                        Collections.singletonList(firstParameter),
                        Collections.singletonList(first),
                        Collections.singletonList(firstAnnotation),
                        MethodDescription.NO_DEFAULT_VALUE)), is(false));
    }

    @Test
    public void testTokenNoIdentityParameters() throws Exception {
        assertThat(new MethodDescription.Token(FOO,
                MODIFIERS,
                Collections.singletonList(first),
                first,
                Collections.singletonList(firstParameter),
                Collections.singletonList(first),
                Collections.singletonList(firstAnnotation),
                MethodDescription.NO_DEFAULT_VALUE)
                .isIdenticalTo(new MethodDescription.Token(FOO,
                        MODIFIERS,
                        Collections.singletonList(first),
                        first,
                        Collections.singletonList(secondParameter),
                        Collections.singletonList(first),
                        Collections.singletonList(firstAnnotation),
                        MethodDescription.NO_DEFAULT_VALUE)), is(false));
    }

    @Test
    public void testTokenNoIdentityExceptions() throws Exception {
        assertThat(new MethodDescription.Token(FOO,
                MODIFIERS,
                Collections.singletonList(first),
                first,
                Collections.singletonList(firstParameter),
                Collections.singletonList(first),
                Collections.singletonList(firstAnnotation),
                MethodDescription.NO_DEFAULT_VALUE)
                .isIdenticalTo(new MethodDescription.Token(FOO,
                        MODIFIERS,
                        Collections.singletonList(first),
                        first,
                        Collections.singletonList(firstParameter),
                        Collections.singletonList(second),
                        Collections.singletonList(firstAnnotation),
                        MethodDescription.NO_DEFAULT_VALUE)), is(false));
    }

    @Test
    public void testTokenNoIdentityAnnotations() throws Exception {
        assertThat(new MethodDescription.Token(FOO,
                MODIFIERS,
                Collections.singletonList(first),
                first,
                Collections.singletonList(firstParameter),
                Collections.singletonList(first),
                Collections.singletonList(firstAnnotation),
                MethodDescription.NO_DEFAULT_VALUE)
                .isIdenticalTo(new MethodDescription.Token(FOO,
                        MODIFIERS,
                        Collections.singletonList(first),
                        first,
                        Collections.singletonList(firstParameter),
                        Collections.singletonList(first),
                        Collections.singletonList(secondAnnotation),
                        MethodDescription.NO_DEFAULT_VALUE)), is(false));
    }

    @Test
    public void testTokenNoIdentityDefaultValue() throws Exception {
        assertThat(new MethodDescription.Token(FOO,
                MODIFIERS,
                Collections.singletonList(first),
                first,
                Collections.singletonList(firstParameter),
                Collections.singletonList(first),
                Collections.singletonList(firstAnnotation),
                firstDefault)
                .isIdenticalTo(new MethodDescription.Token(FOO,
                        MODIFIERS,
                        Collections.singletonList(first),
                        first,
                        Collections.singletonList(firstParameter),
                        Collections.singletonList(first),
                        Collections.singletonList(firstAnnotation),
                        secondDefault)), is(false));
    }

    @Test
    public void testTokenNoIdentityDefaultValueLeftNull() throws Exception {
        assertThat(new MethodDescription.Token(FOO,
                MODIFIERS,
                Collections.singletonList(first),
                first,
                Collections.singletonList(firstParameter),
                Collections.singletonList(first),
                Collections.singletonList(firstAnnotation),
                MethodDescription.NO_DEFAULT_VALUE)
                .isIdenticalTo(new MethodDescription.Token(FOO,
                        MODIFIERS,
                        Collections.singletonList(first),
                        first,
                        Collections.singletonList(firstParameter),
                        Collections.singletonList(first),
                        Collections.singletonList(firstAnnotation),
                        secondDefault)), is(false));
    }

    @Test
    public void testTokenNoIdentityDefaultValueRightNull() throws Exception {
        assertThat(new MethodDescription.Token(FOO,
                MODIFIERS,
                Collections.singletonList(first),
                first,
                Collections.singletonList(firstParameter),
                Collections.singletonList(first),
                Collections.singletonList(firstAnnotation),
                firstDefault)
                .isIdenticalTo(new MethodDescription.Token(FOO,
                        MODIFIERS,
                        Collections.singletonList(first),
                        first,
                        Collections.singletonList(firstParameter),
                        Collections.singletonList(first),
                        Collections.singletonList(firstAnnotation),
                        MethodDescription.NO_DEFAULT_VALUE)), is(false));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(MethodDescription.Token.class).applyBasic();
    }
}
