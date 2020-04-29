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

public class RecordComponentDescriptionTokenTest {

    private static final String FOO = "foo";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

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
        RecordComponentDescription.Token token = new RecordComponentDescription.Token(FOO, type, Collections.singletonList(annotation));
        assertThat(token.getName(), is(FOO));
        assertThat(token.getType(), is(type));
        assertThat(token.getAnnotations(), is(Collections.singletonList(annotation)));
    }

    @Test
    public void testVisitor() throws Exception {
        assertThat(new RecordComponentDescription.Token(FOO, type, Collections.singletonList(annotation)).accept(visitor),
                is(new RecordComponentDescription.Token(FOO, visitedType, Collections.singletonList(annotation))));
    }
}
