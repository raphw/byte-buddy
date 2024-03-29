package net.bytebuddy.description.method;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.type.TypeDescription;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class ParameterDescriptionTokenTest {

    private static final String FOO = "foo";

    private static final int MODIFIERS = 42;

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private TypeDescription.Generic type, visitedType;

    @Mock
    private AnnotationDescription annotation;

    @Mock
    private TypeDescription.Generic.Visitor<? extends TypeDescription.Generic> visitor;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        when(type.asGenericType()).thenReturn(type);
        when(visitedType.asGenericType()).thenReturn(visitedType);
        when(type.accept((TypeDescription.Generic.Visitor) visitor)).thenReturn(visitedType);
    }

    @Test
    public void testProperties() throws Exception {
        ParameterDescription.Token token = new ParameterDescription.Token(type, Collections.singletonList(annotation), FOO, MODIFIERS);
        assertThat(token.getType(), is(type));
        assertThat(token.getAnnotations(), is(Collections.singletonList(annotation)));
        assertThat(token.getName(), is(FOO));
        assertThat(token.getModifiers(), is(MODIFIERS));
    }

    @Test
    public void testVisitor() throws Exception {
        assertThat(new ParameterDescription.Token(type, Collections.singletonList(annotation), FOO, MODIFIERS).accept(visitor),
                is(new ParameterDescription.Token(visitedType, Collections.singletonList(annotation), FOO, MODIFIERS)));
    }
}
