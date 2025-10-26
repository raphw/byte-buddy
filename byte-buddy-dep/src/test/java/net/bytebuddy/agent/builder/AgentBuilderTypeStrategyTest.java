package net.bytebuddy.agent.builder;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.VisibilityBridgeStrategy;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.dynamic.scaffold.inline.MethodNameTransformer;
import net.bytebuddy.matcher.LatentMatcher;
import net.bytebuddy.utility.JavaModule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import java.security.ProtectionDomain;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class AgentBuilderTypeStrategyTest {

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private TypeDescription typeDescription;

    @Mock
    private ByteBuddy byteBuddy;

    @Mock
    private ClassFileLocator classFileLocator;

    @Mock
    private MethodNameTransformer methodNameTransformer;

    @Mock
    private ClassLoader classLoader;

    @Mock
    private JavaModule module;

    @Mock
    private ProtectionDomain protectionDomain;

    @Mock
    private DynamicType.Builder<?> dynamicTypeBuilder;

    @Test
    @SuppressWarnings("unchecked")
    public void testRebase() throws Exception {
        when(byteBuddy.rebase(typeDescription, classFileLocator, methodNameTransformer)).thenReturn((DynamicType.Builder) dynamicTypeBuilder);
        assertThat(AgentBuilder.TypeStrategy.Default.REBASE.builder(typeDescription,
                byteBuddy,
                classFileLocator,
                methodNameTransformer,
                classLoader,
                module,
                protectionDomain), is((DynamicType.Builder) dynamicTypeBuilder));
        verify(byteBuddy).rebase(typeDescription, classFileLocator, methodNameTransformer);
        verifyNoMoreInteractions(byteBuddy);
        verifyNoMoreInteractions(dynamicTypeBuilder);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRedefine() throws Exception {
        when(byteBuddy.redefine(typeDescription, classFileLocator)).thenReturn((DynamicType.Builder) dynamicTypeBuilder);
        assertThat(AgentBuilder.TypeStrategy.Default.REDEFINE.builder(typeDescription,
                byteBuddy,
                classFileLocator,
                methodNameTransformer,
                classLoader,
                module,
                protectionDomain), is((DynamicType.Builder) dynamicTypeBuilder));
        verify(byteBuddy).redefine(typeDescription, classFileLocator);
        verifyNoMoreInteractions(byteBuddy);
        verifyNoMoreInteractions(dynamicTypeBuilder);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRedefineFrozen() throws Exception {
        when(byteBuddy.with(InstrumentedType.Factory.Default.FROZEN)).thenReturn(byteBuddy);
        when(byteBuddy.with(VisibilityBridgeStrategy.Default.NEVER)).thenReturn(byteBuddy);
        when(byteBuddy.redefine(typeDescription, classFileLocator)).thenReturn((DynamicType.Builder) dynamicTypeBuilder);
        when(dynamicTypeBuilder.ignoreAlso(LatentMatcher.ForSelfDeclaredMethod.NOT_DECLARED)).thenReturn((DynamicType.Builder) dynamicTypeBuilder);
        assertThat(AgentBuilder.TypeStrategy.Default.REDEFINE_FROZEN.builder(typeDescription,
                byteBuddy,
                classFileLocator,
                methodNameTransformer,
                classLoader,
                module,
                protectionDomain), is((DynamicType.Builder) dynamicTypeBuilder));
        verify(byteBuddy).with(InstrumentedType.Factory.Default.FROZEN);
        verify(byteBuddy).with(VisibilityBridgeStrategy.Default.NEVER);
        verify(byteBuddy).redefine(typeDescription, classFileLocator);
        verifyNoMoreInteractions(byteBuddy);
        verify(dynamicTypeBuilder).ignoreAlso(LatentMatcher.ForSelfDeclaredMethod.NOT_DECLARED);
        verifyNoMoreInteractions(dynamicTypeBuilder);
    }
}
