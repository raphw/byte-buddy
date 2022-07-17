package net.bytebuddy.description.type;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.annotation.AnnotationSource;
import net.bytebuddy.dynamic.TargetType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class TypeDescriptionGenericVisitorSubstitutorForTokenNormalizationTest {

    private static final String FOO = "foo";

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private TypeDescription target;

    @Mock
    private TypeDescription.Generic source;

    @Mock
    private AnnotationDescription annotationDescription;

    @Before
    public void setUp() throws Exception {
        when(source.getSymbol()).thenReturn(FOO);
        when(source.getDeclaredAnnotations()).thenReturn(new AnnotationList.Explicit(annotationDescription));
    }

    @Test
    public void testTargetType() throws Exception {
        TypeDescription.Generic typeDescription = new TypeDescription.Generic.Visitor.Substitutor.ForTokenNormalization(target)
                .onSimpleType(new TypeDescription.Generic.OfNonGenericType.Latent(TargetType.DESCRIPTION, new AnnotationSource.Explicit(annotationDescription)));
        assertThat(typeDescription.asErasure(), is(target));
        assertThat(typeDescription.getDeclaredAnnotations(), is(Collections.singletonList(annotationDescription)));
    }

    @Test
    public void testNotTargetType() throws Exception {
        assertThat(new TypeDescription.Generic.Visitor.Substitutor.ForTokenNormalization(target).onSimpleType(source), sameInstance(source));
    }

    @Test
    public void testTypeVariable() throws Exception {
        TypeDescription.Generic typeDescription = new TypeDescription.Generic.Visitor.Substitutor.ForTokenNormalization(target).onTypeVariable(source);
        assertThat(typeDescription, is((TypeDescription.Generic) new TypeDescription.Generic.OfTypeVariable.Symbolic(FOO, new AnnotationSource.Explicit(annotationDescription))));
        assertThat(typeDescription.getDeclaredAnnotations(), is(Collections.singletonList(annotationDescription)));
    }
}
