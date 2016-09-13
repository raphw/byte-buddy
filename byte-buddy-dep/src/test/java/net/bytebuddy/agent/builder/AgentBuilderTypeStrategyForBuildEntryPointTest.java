package net.bytebuddy.agent.builder;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.build.EntryPoint;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.inline.MethodNameTransformer;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class AgentBuilderTypeStrategyForBuildEntryPointTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private EntryPoint entryPoint;

    @Mock
    private ByteBuddy byteBuddy;

    @Mock
    private TypeDescription typeDescription;

    @Mock
    private ClassFileLocator classFileLocator;

    @Mock
    private MethodNameTransformer methodNameTransformer;

    @Mock
    private DynamicType.Builder<?> builder;

    @Test
    @SuppressWarnings("unchecked")
    public void testApplication() throws Exception {
        when(entryPoint.transform(typeDescription, byteBuddy, classFileLocator, methodNameTransformer)).thenReturn((DynamicType.Builder) builder);
        assertThat(new AgentBuilder.TypeStrategy.ForBuildEntryPoint(entryPoint).builder(typeDescription, byteBuddy, classFileLocator, methodNameTransformer),
                is((DynamicType.Builder) builder));
        verify(entryPoint).transform(typeDescription, byteBuddy, classFileLocator, methodNameTransformer);
        verifyNoMoreInteractions(entryPoint);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(AgentBuilder.TypeStrategy.ForBuildEntryPoint.class).apply();
    }
}
