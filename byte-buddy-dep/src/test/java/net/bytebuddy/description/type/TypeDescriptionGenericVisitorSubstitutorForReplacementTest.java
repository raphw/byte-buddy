package net.bytebuddy.description.type;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.annotation.AnnotationSource;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TypeDescriptionGenericVisitorSubstitutorForReplacementTest {

    private static final String FOO = "foo";

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private TypeDescription target;

    @Mock
    private TypeDescription.Generic generic;

    @Mock
    private AnnotationDescription annotationDescription;

    @Before
    public void setUp() throws Exception {
        when(generic.getDeclaredAnnotations()).thenReturn(new AnnotationList.Explicit(annotationDescription));
    }

    @Test
    public void testReplacement() throws Exception {
        when(generic.asErasure()).thenReturn(target);
        TypeDescription.Generic typeDescription = new TypeDescription.Generic.Visitor.Substitutor.ForReplacement(target)
                .onSimpleType(new TypeDescription.Generic.OfNonGenericType.Latent(target, new AnnotationSource.Explicit(annotationDescription)));
        assertThat(typeDescription.asErasure(), is(target));
        assertThat(typeDescription.getDeclaredAnnotations(), is(Collections.singletonList(annotationDescription)));
    }

    @Test
    public void testReplacementNoMatch() throws Exception {
        when(generic.asErasure()).thenReturn(mock(TypeDescription.class));
        TypeDescription.Generic typeDescription = new TypeDescription.Generic.Visitor.Substitutor.ForReplacement(target).onSimpleType(generic);
        assertThat(typeDescription, is(generic));
    }

    @Test
    public void testTypeVariable() throws Exception {
        TypeDescription.Generic typeDescription = new TypeDescription.Generic.Visitor.Substitutor.ForReplacement(target).onTypeVariable(generic);
        assertThat(typeDescription, is(generic));
    }
}
