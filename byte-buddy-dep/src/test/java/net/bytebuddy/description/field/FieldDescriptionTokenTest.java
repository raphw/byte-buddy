package net.bytebuddy.description.field;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.type.TypeDescription;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import java.util.Collections;

import static net.bytebuddy.test.utility.FieldByFieldComparison.matchesPrototype;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class FieldDescriptionTokenTest {

    private static final String FOO = "foo";

    private static final int MODIFIERS = 42;

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private TypeDescription.Generic type, visitedType;

    @Mock
    private TypeDescription typeDescription, rawType;

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
        FieldDescription.Token token = new FieldDescription.Token(FOO, MODIFIERS, type, Collections.singletonList(annotation));
        assertThat(token.getName(), is(FOO));
        assertThat(token.getModifiers(), is(MODIFIERS));
        assertThat(token.getType(), is(type));
        assertThat(token.getAnnotations(), is(Collections.singletonList(annotation)));
    }

    @Test
    public void testVisitor() throws Exception {
        assertThat(new FieldDescription.Token(FOO, MODIFIERS, type, Collections.singletonList(annotation)).accept(visitor),
                is(new FieldDescription.Token(FOO, MODIFIERS, visitedType, Collections.singletonList(annotation))));
    }

    @Test
    public void testSignatureTokenTransformation() throws Exception {
        when(type.accept(matchesPrototype(new TypeDescription.Generic.Visitor.Reducing(typeDescription)))).thenReturn(rawType);
        assertThat(new FieldDescription.Token(FOO, MODIFIERS, type, Collections.singletonList(annotation)).asSignatureToken(typeDescription),
                is(new FieldDescription.SignatureToken(FOO, rawType)));
    }
}
