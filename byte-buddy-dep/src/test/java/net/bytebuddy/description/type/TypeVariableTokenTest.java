package net.bytebuddy.description.type;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class TypeVariableTokenTest {

    private static final String FOO = "foo";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeDescription.Generic bound, visitedBound;

    @Mock
    private AnnotationDescription annotation;

    @Mock
    private TypeDescription.Generic.Visitor<? extends TypeDescription.Generic> visitor;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        when(bound.asGenericType()).thenReturn(bound);
        when(visitedBound.asGenericType()).thenReturn(visitedBound);
        when(bound.accept((TypeDescription.Generic.Visitor) visitor)).thenReturn(visitedBound);
    }

    @Test
    public void testPropertiesSimple() throws Exception {
        assertThat(new TypeVariableToken(FOO, Collections.singletonList(bound)).getSymbol(), is(FOO));
        assertThat(new TypeVariableToken(FOO, Collections.singletonList(bound)).getBounds(), is(Collections.singletonList(bound)));
        assertThat(new TypeVariableToken(FOO, Collections.singletonList(bound)).getAnnotations().size(), is(0));
    }

    @Test
    public void testProperties() throws Exception {
        assertThat(new TypeVariableToken(FOO, Collections.singletonList(bound), Collections.singletonList(annotation)).getSymbol(), is(FOO));
        assertThat(new TypeVariableToken(FOO, Collections.singletonList(bound), Collections.singletonList(annotation)).getBounds(),
                is(Collections.singletonList(bound)));
        assertThat(new TypeVariableToken(FOO, Collections.singletonList(bound), Collections.singletonList(annotation)).getAnnotations(),
                is(Collections.singletonList(annotation)));
    }

    @Test
    public void testVisitor() throws Exception {
        assertThat(new TypeVariableToken(FOO, Collections.singletonList(bound)).accept(visitor), is(new TypeVariableToken(FOO, Collections.singletonList(visitedBound))));
    }
}
