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

    private static final String FOO = "foo";

    private static final String BAR = "bar";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private GenericTypeDescription first, second;

    @Test
    public void testTokenIdentity() throws Exception {
        ParameterDescription.Token token = new ParameterDescription.Token(mock(GenericTypeDescription.class),
                Collections.singletonList(mock(AnnotationDescription.class)),
                ParameterDescription.Token.NO_NAME,
                ParameterDescription.Token.NO_MODIFIERS);
        assertThat(token.hashCode(), is(token.hashCode()));
        assertThat(token, is(token));
    }

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
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(ParameterDescription.Token.class).create(new ObjectPropertyAssertion.Creator<Integer>() {
            @Override
            public Integer create() {
                return new Random().nextInt();
            }
        }).applyBasic();
    }
}
