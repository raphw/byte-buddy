package net.bytebuddy.description.method;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.util.Collections;
import java.util.Random;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

public class ParameterDescriptionTokenTest {

    private static final String FOO = "foo", BAR = "bar";

    private static final int MODIFIERS = 42, MASK = 12, UPDATE = 7;

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private GenericTypeDescription first, second;

    @Mock
    private AnnotationDescription firstAnnotation, secondAnnotation;

    @Test
    public void testTokenInequalityHashCode() throws Exception {
        assertThat(new ParameterDescription.Token(mock(GenericTypeDescription.class),
                        Collections.singletonList(mock(AnnotationDescription.class)),
                        ParameterDescription.Token.NO_NAME,
                        ParameterDescription.Token.NO_MODIFIERS).hashCode(),
                not(new ParameterDescription.Token(mock(GenericTypeDescription.class),
                        Collections.singletonList(mock(AnnotationDescription.class)),
                        ParameterDescription.Token.NO_NAME,
                        ParameterDescription.Token.NO_MODIFIERS).hashCode()));
    }

    @Test
    public void testTokenInequality() throws Exception {
        assertThat(new ParameterDescription.Token(mock(GenericTypeDescription.class),
                        Collections.singletonList(mock(AnnotationDescription.class)),
                        ParameterDescription.Token.NO_NAME,
                        ParameterDescription.Token.NO_MODIFIERS),
                not(new ParameterDescription.Token(mock(GenericTypeDescription.class),
                        Collections.singletonList(mock(AnnotationDescription.class)),
                        ParameterDescription.Token.NO_NAME,
                        ParameterDescription.Token.NO_MODIFIERS)));
    }

    @Test
    public void testNameEqualityHashCode() throws Exception {
        assertThat(new ParameterDescription.Token(mock(GenericTypeDescription.class),
                        Collections.singletonList(mock(AnnotationDescription.class)),
                        FOO,
                        ParameterDescription.Token.NO_MODIFIERS).hashCode(),
                is(new ParameterDescription.Token(mock(GenericTypeDescription.class),
                        Collections.singletonList(mock(AnnotationDescription.class)),
                        FOO,
                        ParameterDescription.Token.NO_MODIFIERS).hashCode()));
    }

    @Test
    public void testNameInequalityHashCode() throws Exception {
        assertThat(new ParameterDescription.Token(mock(GenericTypeDescription.class),
                        Collections.singletonList(mock(AnnotationDescription.class)),
                        FOO,
                        ParameterDescription.Token.NO_MODIFIERS).hashCode(),
                not(new ParameterDescription.Token(mock(GenericTypeDescription.class),
                        Collections.singletonList(mock(AnnotationDescription.class)),
                        BAR,
                        ParameterDescription.Token.NO_MODIFIERS).hashCode()));
    }

    @Test
    public void testNameEquality() throws Exception {
        assertThat(new ParameterDescription.Token(mock(GenericTypeDescription.class),
                        Collections.singletonList(mock(AnnotationDescription.class)),
                        FOO,
                        ParameterDescription.Token.NO_MODIFIERS),
                is(new ParameterDescription.Token(mock(GenericTypeDescription.class),
                        Collections.singletonList(mock(AnnotationDescription.class)),
                        FOO,
                        ParameterDescription.Token.NO_MODIFIERS)));
    }

    @Test
    public void testNameInequality() throws Exception {
        assertThat(new ParameterDescription.Token(mock(GenericTypeDescription.class),
                        Collections.singletonList(mock(AnnotationDescription.class)),
                        FOO,
                        ParameterDescription.Token.NO_MODIFIERS),
                not(new ParameterDescription.Token(mock(GenericTypeDescription.class),
                        Collections.singletonList(mock(AnnotationDescription.class)),
                        BAR,
                        ParameterDescription.Token.NO_MODIFIERS)));
    }

    @Test
    public void testTokenIdentity() throws Exception {
        assertThat(new ParameterDescription.Token(first,
                Collections.singletonList(firstAnnotation),
                ParameterDescription.Token.NO_NAME,
                ParameterDescription.Token.NO_MODIFIERS)
                .isIdenticalTo(new ParameterDescription.Token(first,
                        Collections.singletonList(firstAnnotation),
                        ParameterDescription.Token.NO_NAME,
                        ParameterDescription.Token.NO_MODIFIERS)), is(true));
    }

    @Test
    public void testTokenNoIdentityType() throws Exception {
        assertThat(new ParameterDescription.Token(first,
                Collections.singletonList(firstAnnotation),
                ParameterDescription.Token.NO_NAME,
                ParameterDescription.Token.NO_MODIFIERS)
                .isIdenticalTo(new ParameterDescription.Token(second,
                        Collections.singletonList(firstAnnotation),
                        ParameterDescription.Token.NO_NAME,
                        ParameterDescription.Token.NO_MODIFIERS)), is(false));
    }

    @Test
    public void testTokenNoIdentityAnnotations() throws Exception {
        assertThat(new ParameterDescription.Token(first,
                Collections.singletonList(firstAnnotation),
                ParameterDescription.Token.NO_NAME,
                ParameterDescription.Token.NO_MODIFIERS)
                .isIdenticalTo(new ParameterDescription.Token(first,
                        Collections.singletonList(secondAnnotation),
                        ParameterDescription.Token.NO_NAME,
                        ParameterDescription.Token.NO_MODIFIERS)), is(false));
    }

    @Test
    public void testTokenNoIdentityName() throws Exception {
        assertThat(new ParameterDescription.Token(first,
                Collections.singletonList(firstAnnotation),
                FOO,
                ParameterDescription.Token.NO_MODIFIERS)
                .isIdenticalTo(new ParameterDescription.Token(first,
                        Collections.singletonList(firstAnnotation),
                        BAR,
                        ParameterDescription.Token.NO_MODIFIERS)), is(false));
    }

    @Test
    public void testTokenNoIdentityNameLeftNull() throws Exception {
        assertThat(new ParameterDescription.Token(first,
                Collections.singletonList(firstAnnotation),
                FOO,
                ParameterDescription.Token.NO_MODIFIERS)
                .isIdenticalTo(new ParameterDescription.Token(first,
                        Collections.singletonList(firstAnnotation),
                        ParameterDescription.Token.NO_NAME,
                        ParameterDescription.Token.NO_MODIFIERS)), is(false));
    }

    @Test
    public void testTokenNoIdentityNameRightNull() throws Exception {
        assertThat(new ParameterDescription.Token(first,
                Collections.singletonList(firstAnnotation),
                FOO,
                ParameterDescription.Token.NO_MODIFIERS)
                .isIdenticalTo(new ParameterDescription.Token(first,
                        Collections.singletonList(firstAnnotation),
                        ParameterDescription.Token.NO_NAME,
                        ParameterDescription.Token.NO_MODIFIERS)), is(false));
    }

    @Test
    public void testTokenNoIdentityModifiers() throws Exception {
        assertThat(new ParameterDescription.Token(first,
                Collections.singletonList(firstAnnotation),
                ParameterDescription.Token.NO_NAME,
                MODIFIERS)
                .isIdenticalTo(new ParameterDescription.Token(first,
                        Collections.singletonList(firstAnnotation),
                        ParameterDescription.Token.NO_NAME,
                        MODIFIERS * 2)), is(false));
    }

    @Test
    public void testTokenNoIdentityModifiersLeftNull() throws Exception {
        assertThat(new ParameterDescription.Token(first,
                Collections.singletonList(firstAnnotation),
                ParameterDescription.Token.NO_NAME,
                MODIFIERS)
                .isIdenticalTo(new ParameterDescription.Token(first,
                        Collections.singletonList(firstAnnotation),
                        ParameterDescription.Token.NO_NAME,
                        ParameterDescription.Token.NO_MODIFIERS)), is(false));
    }

    @Test
    public void testTokenNoIdentityModifiersRightNull() throws Exception {
        assertThat(new ParameterDescription.Token(first,
                Collections.singletonList(firstAnnotation),
                ParameterDescription.Token.NO_NAME,
                ParameterDescription.Token.NO_MODIFIERS)
                .isIdenticalTo(new ParameterDescription.Token(first,
                        Collections.singletonList(firstAnnotation),
                        ParameterDescription.Token.NO_NAME,
                        MODIFIERS)), is(false));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(ParameterDescription.Token.class).create(new ObjectPropertyAssertion.Creator<Integer>() {
            @Override
            public Integer create() {
                return new Random().nextInt();
            }
        }).applyBasic();
    }
}
