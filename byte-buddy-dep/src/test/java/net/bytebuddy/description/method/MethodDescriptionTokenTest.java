package net.bytebuddy.description.method;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationValue;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeVariableToken;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.util.Collections;

import static net.bytebuddy.test.utility.FieldByFieldComparison.matchesPrototype;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class MethodDescriptionTokenTest {

    private static final String FOO = "foo";

    private static final int MODIFIERS = 42;

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeDescription.Generic returnType, visitedReturnType, exceptionType, visitedExceptionType, parameterType, receiverType, visitedReceiverType;

    @Mock
    private ParameterDescription.Token parameterToken, visitedParameterToken;

    @Mock
    private TypeVariableToken typeVariableToken, visitedTypeVariableToken;

    @Mock
    private TypeDescription typeDescription, rawReturnType, rawParameterType;

    @Mock
    private AnnotationDescription annotation;

    @Mock
    private AnnotationValue<?, ?> defaultValue;

    @Mock
    private TypeDescription.Generic.Visitor<? extends TypeDescription.Generic> visitor;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        when(typeVariableToken.accept(visitor)).thenReturn(visitedTypeVariableToken);
        when(returnType.asGenericType()).thenReturn(returnType);
        when(visitedReturnType.asGenericType()).thenReturn(visitedReturnType);
        when(returnType.accept((TypeDescription.Generic.Visitor) visitor)).thenReturn(visitedReturnType);
        when(exceptionType.asGenericType()).thenReturn(exceptionType);
        when(visitedExceptionType.asGenericType()).thenReturn(visitedExceptionType);
        when(exceptionType.accept((TypeDescription.Generic.Visitor) visitor)).thenReturn(visitedExceptionType);
        when(parameterToken.accept(visitor)).thenReturn(visitedParameterToken);
        when(receiverType.accept((TypeDescription.Generic.Visitor) visitor)).thenReturn(visitedReceiverType);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testProperties() throws Exception {
        MethodDescription.Token token = new MethodDescription.Token(FOO,
                MODIFIERS,
                Collections.singletonList(typeVariableToken),
                returnType,
                Collections.singletonList(parameterToken),
                Collections.singletonList(exceptionType),
                Collections.singletonList(annotation),
                defaultValue,
                receiverType);
        assertThat(token.getName(), is(FOO));
        assertThat(token.getModifiers(), is(MODIFIERS));
        assertThat(token.getTypeVariableTokens(), is(Collections.singletonList(typeVariableToken)));
        assertThat(token.getReturnType(), is(returnType));
        assertThat(token.getParameterTokens(), is(Collections.singletonList(parameterToken)));
        assertThat(token.getExceptionTypes(), is(Collections.singletonList(exceptionType)));
        assertThat(token.getAnnotations(), is(Collections.singletonList(annotation)));
        assertThat(token.getDefaultValue(), is((AnnotationValue) defaultValue));
        assertThat(token.getReceiverType(), is(receiverType));
    }

    @Test
    public void testVisitor() throws Exception {
        assertThat(new MethodDescription.Token(FOO,
                        MODIFIERS,
                        Collections.singletonList(typeVariableToken),
                        returnType,
                        Collections.singletonList(parameterToken),
                        Collections.singletonList(exceptionType),
                        Collections.singletonList(annotation),
                        defaultValue,
                        receiverType).accept(visitor),
                is(new MethodDescription.Token(FOO,
                        MODIFIERS,
                        Collections.singletonList(visitedTypeVariableToken),
                        visitedReturnType,
                        Collections.singletonList(visitedParameterToken),
                        Collections.singletonList(visitedExceptionType),
                        Collections.singletonList(annotation),
                        defaultValue,
                        visitedReceiverType)));
    }

    @Test
    public void testSignatureTokenTransformation() throws Exception {
        when(returnType.accept(matchesPrototype(new TypeDescription.Generic.Visitor.Reducing(typeDescription, typeVariableToken))))
                .thenReturn(rawReturnType);
        when(parameterToken.getType()).thenReturn(parameterType);
        when(parameterType.accept(matchesPrototype(new TypeDescription.Generic.Visitor.Reducing(typeDescription, typeVariableToken))))
                .thenReturn(rawParameterType);
        assertThat(new MethodDescription.Token(FOO,
                        MODIFIERS,
                        Collections.singletonList(typeVariableToken),
                        returnType,
                        Collections.singletonList(parameterToken),
                        Collections.singletonList(exceptionType),
                        Collections.singletonList(annotation),
                        defaultValue,
                        receiverType).asSignatureToken(typeDescription),
                is(new MethodDescription.SignatureToken(FOO, rawReturnType, Collections.singletonList(rawParameterType))));
    }
}
