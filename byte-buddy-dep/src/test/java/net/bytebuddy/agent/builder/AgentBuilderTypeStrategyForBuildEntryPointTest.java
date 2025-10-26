package net.bytebuddy.agent.builder;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.build.EntryPoint;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.inline.MethodNameTransformer;
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

public class AgentBuilderTypeStrategyForBuildEntryPointTest {

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

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
    private ClassLoader classLoader;

    @Mock
    private JavaModule module;

    @Mock
    private ProtectionDomain protectionDomain;

    @Mock
    private DynamicType.Builder<?> builder;

    @Test
    @SuppressWarnings("unchecked")
    public void testApplication() throws Exception {
        when(entryPoint.transform(typeDescription, byteBuddy, classFileLocator, methodNameTransformer)).thenReturn((DynamicType.Builder) builder);
        assertThat(new AgentBuilder.TypeStrategy.ForBuildEntryPoint(entryPoint).builder(typeDescription,
                byteBuddy,
                classFileLocator,
                methodNameTransformer,
                classLoader,
                module,
                protectionDomain), is((DynamicType.Builder) builder));
        verify(entryPoint).transform(typeDescription, byteBuddy, classFileLocator, methodNameTransformer);
        verifyNoMoreInteractions(entryPoint);
    }
}
