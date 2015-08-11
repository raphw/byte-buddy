package net.bytebuddy.dynamic;

import net.bytebuddy.description.ByteCodeElement;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.modifier.ModifierContributor;
import net.bytebuddy.description.type.TypeDescription;
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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

public class MethodTransformerSimpleTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux";

    private static final int MODIFIERS = 42, RANGE = 3, MASK = 1;

    @Rule
    public TestRule mocktioRule = new MockitoRule(this);

    @Mock
    private TypeDescription instrumentedType, rawDeclaringType, rawReturnType, rawParameterType;

    @Mock
    private MethodTransformer.Simple.Transformer transformer;

    @Mock
    private MethodDescription methodDescription;

    @Mock
    private MethodDescription.InDefinedShape definedMethod;

    @Mock
    private MethodDescription.Token methodToken;

    @Mock
    private ParameterDescription.Token parameterToken;

    @Mock
    private ParameterDescription.InDefinedShape definedParameter;

    @Mock
    private GenericTypeDescription returnType, typeVariable, parameterType, exceptionType, declaringType;

    @Mock
    private AnnotationDescription methodAnnotation, parameterAnnotation;

    @Mock
    private ModifierContributor.ForMethod modifierContributor;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        when(returnType.accept(any(GenericTypeDescription.Visitor.class))).thenReturn(returnType);
        when(typeVariable.accept(any(GenericTypeDescription.Visitor.class))).thenReturn(typeVariable);
        when(parameterType.accept(any(GenericTypeDescription.Visitor.class))).thenReturn(parameterType);
        when(exceptionType.accept(any(GenericTypeDescription.Visitor.class))).thenReturn(exceptionType);
        when(typeVariable.getSymbol()).thenReturn(QUX);
        when(typeVariable.getSort()).thenReturn(GenericTypeDescription.Sort.VARIABLE);
        when(methodDescription.asToken()).thenReturn(methodToken);
        when(methodDescription.getDeclaringType()).thenReturn(declaringType);
        when(methodDescription.asDefined()).thenReturn(definedMethod);
        when(methodToken.getInternalName()).thenReturn(FOO);
        when(methodToken.getModifiers()).thenReturn(MODIFIERS);
        when(methodToken.getReturnType()).thenReturn(returnType);
        when(methodToken.getTypeVariables()).thenReturn(new GenericTypeList.Explicit(Collections.singletonList(typeVariable)));
        when(methodToken.getExceptionTypes()).thenReturn(new GenericTypeList.Explicit(Collections.singletonList(exceptionType)));
        when(methodToken.getParameterTokens())
                .thenReturn(new ByteCodeElement.Token.TokenList<ParameterDescription.Token>(Collections.singletonList(parameterToken)));
        when(methodToken.getAnnotations()).thenReturn(new AnnotationList.Explicit(Collections.singletonList(methodAnnotation)));
        when(modifierContributor.getMask()).thenReturn(MASK);
        when(modifierContributor.getRange()).thenReturn(RANGE);
        when(parameterToken.getType()).thenReturn(parameterType);
        when(parameterToken.getAnnotations()).thenReturn(new AnnotationList.Explicit(Collections.singletonList(parameterAnnotation)));
        when(parameterToken.getName()).thenReturn(BAR);
        when(parameterToken.getModifiers()).thenReturn(MODIFIERS * 2);
        when(definedMethod.getParameters())
                .thenReturn(new ParameterList.Explicit<ParameterDescription.InDefinedShape>(Collections.singletonList(definedParameter)));
        when(declaringType.asErasure()).thenReturn(rawDeclaringType);
        when(returnType.asErasure()).thenReturn(rawReturnType);
        when(parameterType.asErasure()).thenReturn(rawParameterType);
    }

    @Test
    public void testSimpleTransformation() throws Exception {
        when(transformer.transform(methodToken)).thenReturn(methodToken);
        MethodDescription transformed = new MethodTransformer.Simple(transformer).transform(instrumentedType, methodDescription);
        assertThat(transformed.getDeclaringType(), is(declaringType));
        assertThat(transformed.getInternalName(), is(FOO));
        assertThat(transformed.getModifiers(), is(MODIFIERS));
        assertThat(transformed.getReturnType(), is(returnType));
        assertThat(transformed.getTypeVariables().size(), is(1));
        assertThat(transformed.getTypeVariables().getOnly().getSymbol(), is(QUX));
        assertThat(transformed.getExceptionTypes().size(), is(1));
        assertThat(transformed.getExceptionTypes().getOnly(), is(exceptionType));
        assertThat(transformed.getParameters().size(), is(1));
        assertThat(transformed.getParameters().getOnly().asToken(), is(parameterToken));
        assertThat(transformed.getParameters().getOnly().asDefined(), is(definedParameter));
        assertThat(transformed.getParameters().getOnly().getDeclaringMethod(), is(transformed));
        assertThat(transformed.asDefined(), is(definedMethod));
    }

    @Test
    public void testModifierTransformation() throws Exception {
        MethodDescription.Token transformed = new MethodTransformer.Simple.Transformer.ForModifierTransformation(Collections.singletonList(modifierContributor))
                .transform(methodToken);
        assertThat(transformed.getInternalName(), is(FOO));
        assertThat(transformed.getModifiers(), is((MODIFIERS & ~RANGE) | MASK));
        assertThat(transformed.getReturnType(), is(returnType));
        assertThat(transformed.getTypeVariables().size(), is(1));
        assertThat(transformed.getTypeVariables().getOnly(), is(typeVariable));
        assertThat(transformed.getExceptionTypes().size(), is(1));
        assertThat(transformed.getExceptionTypes().getOnly(), is(exceptionType));
        assertThat(transformed.getParameterTokens().size(), is(1));
        assertThat(transformed.getParameterTokens().getOnly(), is(parameterToken));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(MethodTransformer.Simple.class).apply();
        ObjectPropertyAssertion.of(MethodTransformer.Simple.Transformer.ForModifierTransformation.class).apply();
    }
}
