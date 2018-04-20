package net.bytebuddy.dynamic;

import net.bytebuddy.description.ByteCodeElement;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.modifier.ModifierContributor;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.description.type.TypeVariableToken;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.util.Collections;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.none;
import static net.bytebuddy.test.utility.FieldByFieldComparison.matchesPrototype;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

public class TransformerForMethodTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux";

    private static final int MODIFIERS = 42, RANGE = 3, MASK = 1;

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeDescription instrumentedType, rawDeclaringType, rawReturnType, rawParameterType;

    @Mock
    private Transformer<MethodDescription.Token> tokenTransformer;

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
    private TypeDescription.Generic returnType, typeVariableBound, parameterType, exceptionType, declaringType;

    @Mock
    private AnnotationDescription methodAnnotation, parameterAnnotation;

    @Mock
    private ModifierContributor.ForMethod modifierContributor;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        when(returnType.accept(any(TypeDescription.Generic.Visitor.class))).thenReturn(returnType);
        when(typeVariableBound.accept(any(TypeDescription.Generic.Visitor.class))).thenReturn(typeVariableBound);
        when(parameterType.accept(any(TypeDescription.Generic.Visitor.class))).thenReturn(parameterType);
        when(exceptionType.accept(any(TypeDescription.Generic.Visitor.class))).thenReturn(exceptionType);
        when(typeVariableBound.getSymbol()).thenReturn(QUX);
        when(typeVariableBound.getSort()).thenReturn(TypeDefinition.Sort.VARIABLE);
        when(typeVariableBound.asGenericType()).thenReturn(typeVariableBound);
        when(methodDescription.asToken(matchesPrototype(none()))).thenReturn(methodToken);
        when(methodDescription.getDeclaringType()).thenReturn(declaringType);
        when(methodDescription.asDefined()).thenReturn(definedMethod);
        when(methodToken.getName()).thenReturn(FOO);
        when(methodToken.getModifiers()).thenReturn(MODIFIERS);
        when(methodToken.getReturnType()).thenReturn(returnType);
        when(methodToken.getTypeVariableTokens())
                .thenReturn(new ByteCodeElement.Token.TokenList<TypeVariableToken>(new TypeVariableToken(QUX, new TypeList.Generic.Explicit(typeVariableBound))));
        when(methodToken.getExceptionTypes()).thenReturn(new TypeList.Generic.Explicit(exceptionType));
        when(methodToken.getParameterTokens())
                .thenReturn(new ByteCodeElement.Token.TokenList<ParameterDescription.Token>(parameterToken));
        when(methodToken.getAnnotations()).thenReturn(new AnnotationList.Explicit(methodAnnotation));
        when(modifierContributor.getMask()).thenReturn(MASK);
        when(modifierContributor.getRange()).thenReturn(RANGE);
        when(parameterToken.getType()).thenReturn(parameterType);
        when(parameterToken.getAnnotations()).thenReturn(new AnnotationList.Explicit(parameterAnnotation));
        when(parameterToken.getName()).thenReturn(BAR);
        when(parameterToken.getModifiers()).thenReturn(MODIFIERS * 2);
        when(definedMethod.getParameters())
                .thenReturn(new ParameterList.Explicit<ParameterDescription.InDefinedShape>(definedParameter));
        when(declaringType.asErasure()).thenReturn(rawDeclaringType);
        when(returnType.asErasure()).thenReturn(rawReturnType);
        when(parameterType.asErasure()).thenReturn(rawParameterType);
        when(exceptionType.asGenericType()).thenReturn(exceptionType);
    }

    @Test
    public void testSimpleTransformation() throws Exception {
        when(tokenTransformer.transform(instrumentedType, methodToken)).thenReturn(methodToken);
        MethodDescription transformed = new Transformer.ForMethod(tokenTransformer).transform(instrumentedType, methodDescription);
        assertThat(transformed.getDeclaringType(), is((TypeDefinition) declaringType));
        assertThat(transformed.getInternalName(), is(FOO));
        assertThat(transformed.getModifiers(), is(MODIFIERS));
        assertThat(transformed.getReturnType(), is(returnType));
        assertThat(transformed.getTypeVariables().size(), is(1));
        assertThat(transformed.getTypeVariables().getOnly().getSymbol(), is(QUX));
        assertThat(transformed.getExceptionTypes().size(), is(1));
        assertThat(transformed.getExceptionTypes().getOnly(), is(exceptionType));
        assertThat(transformed.getDeclaredAnnotations(), is(Collections.singletonList(methodAnnotation)));
        assertThat(transformed.getParameters().size(), is(1));
        assertThat(transformed.getParameters().getOnly().getType(), is(parameterType));
        assertThat(transformed.getParameters().getOnly().getName(), is(BAR));
        assertThat(transformed.getParameters().getOnly().getModifiers(), is(MODIFIERS * 2));
        assertThat(transformed.getParameters().getOnly().getDeclaredAnnotations().size(), is(1));
        assertThat(transformed.getParameters().getOnly().getDeclaredAnnotations().getOnly(), is(parameterAnnotation));
        assertThat(transformed.getParameters().getOnly().asDefined(), is(definedParameter));
        assertThat(transformed.getParameters().getOnly().getDeclaredAnnotations(), is(Collections.singletonList(parameterAnnotation)));
        assertThat(transformed.getParameters().getOnly().getDeclaringMethod(), is(transformed));
        assertThat(transformed.asDefined(), is(definedMethod));
    }

    @Test
    public void testModifierTransformation() throws Exception {
        MethodDescription.Token transformed = new Transformer.ForMethod.MethodModifierTransformer(ModifierContributor.Resolver.of(modifierContributor))
                .transform(instrumentedType, methodToken);
        assertThat(transformed.getName(), is(FOO));
        assertThat(transformed.getModifiers(), is((MODIFIERS & ~RANGE) | MASK));
        assertThat(transformed.getReturnType(), is(returnType));
        assertThat(transformed.getTypeVariableTokens().size(), is(1));
        assertThat(transformed.getTypeVariableTokens().get(0), is(new TypeVariableToken(QUX, Collections.singletonList(typeVariableBound))));
        assertThat(transformed.getExceptionTypes().size(), is(1));
        assertThat(transformed.getExceptionTypes().getOnly(), is(exceptionType));
        assertThat(transformed.getParameterTokens().size(), is(1));
        assertThat(transformed.getParameterTokens().getOnly(), is(parameterToken));
    }

    @Test
    public void testNoChangesUnlessSpecified() throws Exception {
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(Bar.class);
        MethodDescription methodDescription = typeDescription.getSuperClass().getDeclaredMethods().filter(named(FOO)).getOnly();
        MethodDescription transformed = Transformer.ForMethod.withModifiers().transform(typeDescription, methodDescription);
        assertThat(transformed, is(methodDescription));
        assertThat(transformed.getModifiers(), is(methodDescription.getModifiers()));
    }

    @Test
    public void testRetainsInstrumentedType() throws Exception {
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(Bar.class);
        MethodDescription methodDescription = typeDescription.getSuperClass().getDeclaredMethods().filter(named(BAR)).getOnly();
        MethodDescription transformed = Transformer.ForMethod.withModifiers().transform(typeDescription, methodDescription);
        assertThat(transformed, is(methodDescription));
        assertThat(transformed.getModifiers(), is(methodDescription.getModifiers()));
        assertThat(transformed.getReturnType().asErasure(), is(typeDescription));
        assertThat(transformed.getReturnType().getSort(), is(TypeDefinition.Sort.PARAMETERIZED));
        assertThat(transformed.getReturnType().getTypeArguments().size(), is(1));
        assertThat(transformed.getReturnType().getTypeArguments().getOnly(), is(typeDescription.getSuperClass().getDeclaredMethods().filter(named(FOO)).getOnly().getReturnType()));
    }

    private static class Foo<T> {

        T foo() {
            return null;
        }

        Bar<T> bar() {
            return null;
        }
    }

    private static class Bar<S> extends Foo<S> {
        /* empty */
    }
}
