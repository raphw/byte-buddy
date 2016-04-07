package net.bytebuddy.agent.builder;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.inline.MethodNameTransformer;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class AgentBuilderTypeStrategyTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeDescription typeDescription;

    @Mock
    private ByteBuddy byteBuddy;

    @Mock
    private ClassFileLocator classFileLocator;

    @Mock
    private MethodNameTransformer methodNameTransformer;

    @Mock
    private DynamicType.Builder<?> dynamicTypeBuilder;

    @Test
    @SuppressWarnings("unchecked")
    public void testRebase() throws Exception {
        when(byteBuddy.rebase(typeDescription, classFileLocator, methodNameTransformer)).thenReturn((DynamicType.Builder) dynamicTypeBuilder);
        assertThat(AgentBuilder.TypeStrategy.Default.REBASE.builder(typeDescription, byteBuddy, classFileLocator, methodNameTransformer),
                is((DynamicType.Builder) dynamicTypeBuilder));
        verify(byteBuddy).rebase(typeDescription, classFileLocator, methodNameTransformer);
        verifyNoMoreInteractions(byteBuddy);
        verifyZeroInteractions(dynamicTypeBuilder);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRedefine() throws Exception {
        when(byteBuddy.redefine(typeDescription, classFileLocator)).thenReturn((DynamicType.Builder) dynamicTypeBuilder);
        assertThat(AgentBuilder.TypeStrategy.Default.REDEFINE.builder(typeDescription, byteBuddy, classFileLocator, methodNameTransformer),
                is((DynamicType.Builder) dynamicTypeBuilder));
        verify(byteBuddy).redefine(typeDescription, classFileLocator);
        verifyNoMoreInteractions(byteBuddy);
        verifyZeroInteractions(dynamicTypeBuilder);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRedefineDeclaredOnly() throws Exception {
        when(byteBuddy.redefine(typeDescription, classFileLocator)).thenReturn((DynamicType.Builder) dynamicTypeBuilder);
        when(dynamicTypeBuilder.ignoreAlso(ElementMatchers.not(isDeclaredBy(typeDescription)))).thenReturn((DynamicType.Builder) dynamicTypeBuilder);
        assertThat(AgentBuilder.TypeStrategy.Default.REDEFINE_DECLARED_ONLY.builder(typeDescription, byteBuddy, classFileLocator, methodNameTransformer),
                is((DynamicType.Builder) dynamicTypeBuilder));
        verify(byteBuddy).redefine(typeDescription, classFileLocator);
        verifyNoMoreInteractions(byteBuddy);
        verify(dynamicTypeBuilder).ignoreAlso(ElementMatchers.not(isDeclaredBy(typeDescription)));
        verifyNoMoreInteractions(dynamicTypeBuilder);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(AgentBuilder.TypeStrategy.Default.class).apply();
    }
}
