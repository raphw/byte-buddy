package net.bytebuddy.dynamic.scaffold.subclass;

import net.bytebuddy.description.ByteCodeElement;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.annotation.AnnotationValue;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.description.type.TypeVariableToken;
import net.bytebuddy.dynamic.Transformer;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.dynamic.scaffold.MethodRegistry;
import net.bytebuddy.implementation.attribute.MethodAttributeAppender;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.matcher.LatentMatcher;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.objectweb.asm.Opcodes;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class ConstructorStrategyForDefaultConstructorTest {

    private static final String FOO = "foo";

    private static final int MODIFIERS = 42;

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodRegistry methodRegistry;

    @Mock
    private InstrumentedType instrumentedType;

    @Mock
    private TypeDescription.Generic superClass, typeDescription;

    @Mock
    private MethodDescription.InGenericShape methodDescription;

    @Mock
    private MethodDescription.Token token;

    @Mock
    private AnnotationValue<?, ?> defaultValue;

    private MethodDescription.Token stripped;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        when(methodRegistry.append(any(LatentMatcher.class),
                any(MethodRegistry.Handler.class),
                any(MethodAttributeAppender.Factory.class),
                any(Transformer.class))).thenReturn(methodRegistry);
        when(instrumentedType.getSuperClass()).thenReturn(superClass);
        when(superClass.getDeclaredMethods()).thenReturn(new MethodList.Explicit<MethodDescription.InGenericShape>(methodDescription));
        when(methodDescription.isConstructor()).thenReturn(true);
        when(methodDescription.isVisibleTo(instrumentedType)).thenReturn(true);
        when(methodDescription.asToken(ElementMatchers.is(instrumentedType))).thenReturn(token);
        when(token.getName()).thenReturn(FOO);
        when(token.getModifiers()).thenReturn(MODIFIERS);
        when(token.getTypeVariableTokens()).thenReturn(new ByteCodeElement.Token.TokenList<TypeVariableToken>());
        when(token.getReturnType()).thenReturn(typeDescription);
        when(token.getParameterTokens()).thenReturn(new ByteCodeElement.Token.TokenList<ParameterDescription.Token>());
        when(token.getExceptionTypes()).thenReturn(new TypeList.Generic.Empty());
        when(token.getAnnotations()).thenReturn(new AnnotationList.Empty());
        when(token.getDefaultValue()).thenReturn((AnnotationValue) defaultValue);
        when(token.getReceiverType()).thenReturn(typeDescription);
        stripped = new MethodDescription.Token(FOO,
                MODIFIERS,
                Collections.<TypeVariableToken>emptyList(),
                typeDescription,
                Collections.<ParameterDescription.Token>emptyList(),
                Collections.<TypeDescription.Generic>emptyList(),
                Collections.<AnnotationDescription>emptyList(),
                defaultValue,
                TypeDescription.Generic.UNDEFINED);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSingleConstructorsStrategy() throws Exception {
        assertThat(new ConstructorStrategy.ForDefaultConstructor().extractConstructors(instrumentedType),
                is(Collections.singletonList(new MethodDescription.Token(Opcodes.ACC_PUBLIC))));
        when(methodDescription.getParameters()).thenReturn(new ParameterList.Empty());
        assertThat(new ConstructorStrategy.ForDefaultConstructor().inject(instrumentedType, methodRegistry), is(methodRegistry));
    }

    @Test(expected = IllegalStateException.class)
    @SuppressWarnings("unchecked")
    public void testSingleConstructorsStrategyNoSuperConstuctorExtract() throws Exception {
        TypeDescription noConstructor = mock(TypeDescription.class);
        TypeDescription.Generic noConstructorSuper = mock(TypeDescription.Generic.class);
        when(noConstructor.getSuperClass()).thenReturn(noConstructorSuper);
        when(noConstructorSuper.getDeclaredMethods()).thenReturn(new MethodList.Empty());
        new ConstructorStrategy.ForDefaultConstructor().extractConstructors(noConstructor);
    }

    @Test(expected = IllegalStateException.class)
    @SuppressWarnings("unchecked")
    public void testSingleConstructorsStrategyNoSuperConstuctorInject() throws Exception {
        TypeDescription noConstructor = mock(TypeDescription.class);
        TypeDescription.Generic noConstructorSuper = mock(TypeDescription.Generic.class);
        when(noConstructor.getSuperClass()).thenReturn(noConstructorSuper);
        when(noConstructorSuper.getDeclaredMethods()).thenReturn(new MethodList.Empty());
        new ConstructorStrategy.ForDefaultConstructor().inject(noConstructor, methodRegistry);
    }

    @Test(expected = IllegalStateException.class)
    @SuppressWarnings("unchecked")
    public void testSingleConstructorsStrategyMultipleSuperConstuctorInject() throws Exception {
        TypeDescription noConstructor = mock(TypeDescription.class);
        TypeDescription.Generic noConstructorSuper = mock(TypeDescription.Generic.class);
        when(noConstructor.getSuperClass()).thenReturn(noConstructorSuper);
        when(noConstructorSuper.getDeclaredMethods()).thenReturn(new MethodList.Explicit(methodDescription, methodDescription));
        when(methodDescription.getParameters()).thenReturn(new ParameterList.Empty());
        new ConstructorStrategy.ForDefaultConstructor().inject(noConstructor, methodRegistry);
    }
}
