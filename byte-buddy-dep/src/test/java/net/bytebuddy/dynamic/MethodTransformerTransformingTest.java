package net.bytebuddy.dynamic;

import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.modifier.ModifierContributor;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeList;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;

public class MethodTransformerTransformingTest {

    private static final String FOO = "foo";

    private static final int MODIFIERS = 42, RANGE = 3, MASK = 1;

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private ModifierContributor.ForMethod modifierContributor;

    @Mock
    private MethodDescription methodDescription;

    @Mock
    private MethodDescription.InDefinedShape definedShape;

    @Mock
    private AnnotationList annotationList;

    @Mock
    private GenericTypeDescription returnType;

    @Mock
    private ParameterList<?> parameterList;

    @Mock
    private GenericTypeList exceptionTypes;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        when(methodDescription.getModifiers()).thenReturn(MODIFIERS);
        when(methodDescription.getDeclaredAnnotations()).thenReturn(annotationList);
        when(methodDescription.getReturnType()).thenReturn(returnType);
        when(methodDescription.getParameters()).thenReturn((ParameterList) parameterList);
        when(methodDescription.getExceptionTypes()).thenReturn(exceptionTypes);
        when(definedShape.getDeclaredAnnotations()).thenReturn(annotationList);
        when(definedShape.getReturnType()).thenReturn(returnType);
        when(definedShape.getParameters()).thenReturn((ParameterList) parameterList);
        when(definedShape.getExceptionTypes()).thenReturn(exceptionTypes);
        when(modifierContributor.getRange()).thenReturn(RANGE);
        when(modifierContributor.getMask()).thenReturn(MASK);
    }

    @Test
    public void testModifierTransformation() throws Exception {
        MethodTransformer.Transforming.Transformer transformer = new MethodTransformer.Transforming.Transformer
                .ForModifierTransformation(Collections.singletonList(modifierContributor));
        assertThat(transformer.resolveModifiers(methodDescription), is((MODIFIERS & ~RANGE) | MASK));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testNoOpTransformer() throws Exception {
        MethodTransformer.Transforming.Transformer transformer = new NoOpTransformer();
        assertThat(transformer.resolveModifiers(methodDescription), is(MODIFIERS));
        assertThat(transformer.resolveReturnType(methodDescription), is(returnType));
        assertThat(transformer.resolveExceptionTypes(methodDescription), is(exceptionTypes));
        assertThat(transformer.resolveParameters(methodDescription), is((ParameterList) parameterList));
        assertThat(transformer.resolveAnnotations(methodDescription), is(annotationList));
        assertThat(transformer.resolveReturnType(definedShape), is(returnType));
        assertThat(transformer.resolveExceptionTypes(definedShape), is(exceptionTypes));
        assertThat(transformer.resolveParameters(definedShape), is((ParameterList) parameterList));
        assertThat(transformer.resolveAnnotations(definedShape), is(annotationList));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(MethodTransformer.Transforming.class).apply();
        ObjectPropertyAssertion.of(MethodTransformer.Transforming.Transformer.ForModifierTransformation.class).apply();
    }

    private static class NoOpTransformer extends MethodTransformer.Transforming.Transformer.AbstractBase {
        /* empty */
    }
}
