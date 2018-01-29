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
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
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

public class ConstructorStrategyDefaultTest {

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
    public void testNoConstructorsStrategy() throws Exception {
        assertThat(ConstructorStrategy.Default.NO_CONSTRUCTORS.extractConstructors(instrumentedType).size(), is(0));
        assertThat(ConstructorStrategy.Default.NO_CONSTRUCTORS.inject(instrumentedType, methodRegistry), is(methodRegistry));
        verifyZeroInteractions(methodRegistry);
        verifyZeroInteractions(instrumentedType);
    }

    @Test
    public void testNoConstructorsStrategyWithAttributeAppender() throws Exception {
        MethodAttributeAppender.Factory methodAttributeAppenderFactory = mock(MethodAttributeAppender.Factory.class);
        ConstructorStrategy constructorStrategy = ConstructorStrategy.Default.NO_CONSTRUCTORS.with(methodAttributeAppenderFactory);
        assertThat(constructorStrategy.extractConstructors(instrumentedType).size(), is(0));
        assertThat(constructorStrategy.inject(instrumentedType, methodRegistry), is(methodRegistry));
        verifyZeroInteractions(methodRegistry);
        verifyZeroInteractions(instrumentedType);
    }

    @Test
    public void testNoConstructorsStrategyWithInheritedAnnotations() throws Exception {
        ConstructorStrategy constructorStrategy = ConstructorStrategy.Default.NO_CONSTRUCTORS.withInheritedAnnotations();
        assertThat(constructorStrategy.extractConstructors(instrumentedType).size(), is(0));
        assertThat(constructorStrategy.inject(instrumentedType, methodRegistry), is(methodRegistry));
        verifyZeroInteractions(methodRegistry);
        verifyZeroInteractions(instrumentedType);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testImitateSuperClassStrategy() throws Exception {
        assertThat(ConstructorStrategy.Default.IMITATE_SUPER_CLASS.extractConstructors(instrumentedType), is(Collections.singletonList(stripped)));
        assertThat(ConstructorStrategy.Default.IMITATE_SUPER_CLASS.inject(instrumentedType, methodRegistry), is(methodRegistry));
        verify(methodRegistry).append(any(LatentMatcher.class),
                any(MethodRegistry.Handler.class),
                eq(MethodAttributeAppender.NoOp.INSTANCE),
                eq(Transformer.NoOp.<MethodDescription>make()));
        verifyNoMoreInteractions(methodRegistry);
        verify(instrumentedType, atLeastOnce()).getSuperClass();
        verifyNoMoreInteractions(instrumentedType);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testImitateSuperClassStrategyWithAttributeAppender() throws Exception {
        when(methodDescription.getModifiers()).thenReturn(Opcodes.ACC_PUBLIC);
        MethodAttributeAppender.Factory methodAttributeAppenderFactory = mock(MethodAttributeAppender.Factory.class);
        ConstructorStrategy constructorStrategy = ConstructorStrategy.Default.IMITATE_SUPER_CLASS.with(methodAttributeAppenderFactory);
        assertThat(constructorStrategy.extractConstructors(instrumentedType), is(Collections.singletonList(stripped)));
        assertThat(constructorStrategy.inject(instrumentedType, methodRegistry), is(methodRegistry));
        verify(methodRegistry).append(any(LatentMatcher.class),
                any(MethodRegistry.Handler.class),
                eq(methodAttributeAppenderFactory),
                eq(Transformer.NoOp.<MethodDescription>make()));
        verifyNoMoreInteractions(methodRegistry);
        verify(instrumentedType, atLeastOnce()).getSuperClass();
        verifyNoMoreInteractions(instrumentedType);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testImitateSuperClassStrategyWithInheritedAnnotations() throws Exception {
        when(methodDescription.getModifiers()).thenReturn(Opcodes.ACC_PUBLIC);
        ConstructorStrategy constructorStrategy = ConstructorStrategy.Default.IMITATE_SUPER_CLASS.withInheritedAnnotations();
        assertThat(constructorStrategy.extractConstructors(instrumentedType), is(Collections.singletonList(stripped)));
        assertThat(constructorStrategy.inject(instrumentedType, methodRegistry), is(methodRegistry));
        verify(methodRegistry).append(any(LatentMatcher.class),
                any(MethodRegistry.Handler.class),
                eq(MethodAttributeAppender.ForInstrumentedMethod.EXCLUDING_RECEIVER),
                eq(Transformer.NoOp.<MethodDescription>make()));
        verifyNoMoreInteractions(methodRegistry);
        verify(instrumentedType, atLeastOnce()).getSuperClass();
        verifyNoMoreInteractions(instrumentedType);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testImitateSuperClassPublicStrategy() throws Exception {
        when(methodDescription.getModifiers()).thenReturn(Opcodes.ACC_PUBLIC);
        assertThat(ConstructorStrategy.Default.IMITATE_SUPER_CLASS_PUBLIC.extractConstructors(instrumentedType), is(Collections.singletonList(stripped)));
        assertThat(ConstructorStrategy.Default.IMITATE_SUPER_CLASS_PUBLIC.inject(instrumentedType, methodRegistry), is(methodRegistry));
        verify(methodRegistry).append(any(LatentMatcher.class),
                any(MethodRegistry.Handler.class),
                eq(MethodAttributeAppender.NoOp.INSTANCE),
                eq(Transformer.NoOp.<MethodDescription>make()));
        verifyNoMoreInteractions(methodRegistry);
        verify(instrumentedType, atLeastOnce()).getSuperClass();
        verifyNoMoreInteractions(instrumentedType);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testImitateSuperClassPublicStrategyWithAttributeAppender() throws Exception {
        when(methodDescription.getModifiers()).thenReturn(Opcodes.ACC_PUBLIC);
        MethodAttributeAppender.Factory methodAttributeAppenderFactory = mock(MethodAttributeAppender.Factory.class);
        ConstructorStrategy constructorStrategy = ConstructorStrategy.Default.IMITATE_SUPER_CLASS_PUBLIC.with(methodAttributeAppenderFactory);
        assertThat(constructorStrategy.extractConstructors(instrumentedType), is(Collections.singletonList(stripped)));
        assertThat(constructorStrategy.inject(instrumentedType, methodRegistry), is(methodRegistry));
        verify(methodRegistry).append(any(LatentMatcher.class),
                any(MethodRegistry.Handler.class),
                eq(methodAttributeAppenderFactory),
                eq(Transformer.NoOp.<MethodDescription>make()));
        verifyNoMoreInteractions(methodRegistry);
        verify(instrumentedType, atLeastOnce()).getSuperClass();
        verifyNoMoreInteractions(instrumentedType);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testImitateSuperClassPublicStrategyWithInheritedAnnotations() throws Exception {
        when(methodDescription.getModifiers()).thenReturn(Opcodes.ACC_PUBLIC);
        ConstructorStrategy constructorStrategy = ConstructorStrategy.Default.IMITATE_SUPER_CLASS_PUBLIC.withInheritedAnnotations();
        assertThat(constructorStrategy.extractConstructors(instrumentedType), is(Collections.singletonList(stripped)));
        assertThat(constructorStrategy.inject(instrumentedType, methodRegistry), is(methodRegistry));
        verify(methodRegistry).append(any(LatentMatcher.class),
                any(MethodRegistry.Handler.class),
                eq(MethodAttributeAppender.ForInstrumentedMethod.EXCLUDING_RECEIVER),
                eq(Transformer.NoOp.<MethodDescription>make()));
        verifyNoMoreInteractions(methodRegistry);
        verify(instrumentedType, atLeastOnce()).getSuperClass();
        verifyNoMoreInteractions(instrumentedType);
    }

    @Test
    public void testImitateSuperClassPublicStrategyDoesNotSeeNonPublic() throws Exception {
        when(methodDescription.getModifiers()).thenReturn(0);
        assertThat(ConstructorStrategy.Default.IMITATE_SUPER_CLASS_PUBLIC.extractConstructors(instrumentedType).size(), is(0));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testDefaultConstructorStrategy() throws Exception {
        when(methodDescription.getParameters()).thenReturn(new ParameterList.Empty<ParameterDescription.InGenericShape>());
        assertThat(ConstructorStrategy.Default.DEFAULT_CONSTRUCTOR.extractConstructors(instrumentedType),
                is(Collections.singletonList(new MethodDescription.Token(Opcodes.ACC_PUBLIC))));
        assertThat(ConstructorStrategy.Default.DEFAULT_CONSTRUCTOR.inject(instrumentedType, methodRegistry), is(methodRegistry));
        verify(methodRegistry).append(any(LatentMatcher.class),
                any(MethodRegistry.Handler.class),
                eq(MethodAttributeAppender.NoOp.INSTANCE),
                eq(Transformer.NoOp.<MethodDescription>make()));
        verifyNoMoreInteractions(methodRegistry);
        verify(instrumentedType).getSuperClass();
        verifyNoMoreInteractions(instrumentedType);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testDefaultConstructorStrategyWithAttributeAppender() throws Exception {
        when(methodDescription.getParameters()).thenReturn(new ParameterList.Empty<ParameterDescription.InGenericShape>());
        MethodAttributeAppender.Factory methodAttributeAppenderFactory = mock(MethodAttributeAppender.Factory.class);
        ConstructorStrategy constructorStrategy = ConstructorStrategy.Default.DEFAULT_CONSTRUCTOR.with(methodAttributeAppenderFactory);
        assertThat(constructorStrategy.extractConstructors(instrumentedType),
                is(Collections.singletonList(new MethodDescription.Token(Opcodes.ACC_PUBLIC))));
        assertThat(constructorStrategy.inject(instrumentedType, methodRegistry), is(methodRegistry));
        verify(methodRegistry).append(any(LatentMatcher.class),
                any(MethodRegistry.Handler.class),
                eq(methodAttributeAppenderFactory),
                eq(Transformer.NoOp.<MethodDescription>make()));
        verifyNoMoreInteractions(methodRegistry);
        verify(instrumentedType).getSuperClass();
        verifyNoMoreInteractions(instrumentedType);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testDefaultConstructorStrategyWithInheritedAnnotations() throws Exception {
        when(methodDescription.getParameters()).thenReturn(new ParameterList.Empty<ParameterDescription.InGenericShape>());
        ConstructorStrategy constructorStrategy = ConstructorStrategy.Default.DEFAULT_CONSTRUCTOR.withInheritedAnnotations();
        assertThat(constructorStrategy.extractConstructors(instrumentedType),
                is(Collections.singletonList(new MethodDescription.Token(Opcodes.ACC_PUBLIC))));
        assertThat(constructorStrategy.inject(instrumentedType, methodRegistry), is(methodRegistry));
        verify(methodRegistry).append(any(LatentMatcher.class),
                any(MethodRegistry.Handler.class),
                eq(MethodAttributeAppender.ForInstrumentedMethod.EXCLUDING_RECEIVER),
                eq(Transformer.NoOp.<MethodDescription>make()));
        verifyNoMoreInteractions(methodRegistry);
        verify(instrumentedType).getSuperClass();
        verifyNoMoreInteractions(instrumentedType);
    }


    @Test(expected = IllegalArgumentException.class)
    @SuppressWarnings("unchecked")
    public void testDefaultConstructorStrategyNoDefault() throws Exception {
        when(methodDescription.getParameters())
                .thenReturn(new ParameterList.Explicit<ParameterDescription.InGenericShape>(mock(ParameterDescription.InGenericShape.class)));
        ConstructorStrategy.Default.DEFAULT_CONSTRUCTOR.extractConstructors(instrumentedType);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testImitateSuperClassOpeningStrategyNonVisible() throws Exception {
        when(methodDescription.isVisibleTo(instrumentedType)).thenReturn(false);
        assertThat(ConstructorStrategy.Default.IMITATE_SUPER_CLASS_OPENING.extractConstructors(instrumentedType).isEmpty(), is(true));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testImitateSuperClassOpeningStrategy() throws Exception {
        assertThat(ConstructorStrategy.Default.IMITATE_SUPER_CLASS_OPENING.extractConstructors(instrumentedType), is(Collections.singletonList(new MethodDescription.Token(FOO,
                Opcodes.ACC_PUBLIC,
                Collections.<TypeVariableToken>emptyList(),
                typeDescription,
                Collections.<ParameterDescription.Token>emptyList(),
                Collections.<TypeDescription.Generic>emptyList(),
                Collections.<AnnotationDescription>emptyList(),
                defaultValue,
                TypeDescription.Generic.UNDEFINED))));
        assertThat(ConstructorStrategy.Default.IMITATE_SUPER_CLASS_OPENING.inject(instrumentedType, methodRegistry), is(methodRegistry));
        verify(methodRegistry).append(any(LatentMatcher.class),
                any(MethodRegistry.Handler.class),
                eq(MethodAttributeAppender.NoOp.INSTANCE),
                eq(Transformer.NoOp.<MethodDescription>make()));
        verifyNoMoreInteractions(methodRegistry);
        verify(instrumentedType, atLeastOnce()).getSuperClass();
        verifyNoMoreInteractions(instrumentedType);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(ConstructorStrategy.Default.class).apply();
        ObjectPropertyAssertion.of(ConstructorStrategy.Default.WithMethodAttributeAppenderFactory.class).apply();
    }
}
