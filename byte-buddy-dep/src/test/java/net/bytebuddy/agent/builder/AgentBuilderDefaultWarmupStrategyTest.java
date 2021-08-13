package net.bytebuddy.agent.builder;

import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.utility.JavaModule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.objectweb.asm.Type;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class AgentBuilderDefaultWarmupStrategyTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private ResettableClassFileTransformer classFileTransformer;

    @Mock
    private AgentBuilder.LocationStrategy locationStrategy;

    @Mock
    private AgentBuilder.InstallationListener listener;

    @Mock
    private RuntimeException throwable;

    @Test
    public void testNoOp() throws Exception {
        AgentBuilder.Default.WarmupStrategy.NoOp.INSTANCE.apply(classFileTransformer,
                locationStrategy,
                AgentBuilder.RedefinitionStrategy.DISABLED,
                listener);
        verifyZeroInteractions(classFileTransformer);
        verifyZeroInteractions(locationStrategy);
        verifyZeroInteractions(listener);
    }

    @Test
    public void testEnabledNoRedefinition() throws Exception {
        when(locationStrategy.classFileLocator(null, JavaModule.ofType(Object.class)))
                .thenReturn(new ClassFileLocator.Simple(Collections.singletonMap(Object.class.getName(), new byte[0])));
        new AgentBuilder.Default.WarmupStrategy.Enabled(Collections.<Class<?>>singleton(Object.class)).apply(classFileTransformer,
                locationStrategy,
                AgentBuilder.RedefinitionStrategy.DISABLED,
                listener);
        transform(verify(classFileTransformer),
                JavaModule.ofType(Object.class),
                null,
                Type.getInternalName(Object.class),
                null,
                Object.class.getProtectionDomain(),
                new byte[0]);
        verifyNoMoreInteractions(classFileTransformer);
        verify(locationStrategy).classFileLocator(null, JavaModule.ofType(Object.class));
        verifyNoMoreInteractions(locationStrategy);
        verify(listener).onBeforeWarmUp(Collections.<Class<?>>singleton(Object.class), classFileTransformer);
        verify(listener).onAfterWarmUp(Collections.<Class<?>>singleton(Object.class), classFileTransformer, false);
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void testEnabledEffect() throws Exception {
        when(locationStrategy.classFileLocator(null, JavaModule.ofType(Object.class)))
                .thenReturn(new ClassFileLocator.Simple(Collections.singletonMap(Object.class.getName(), new byte[0])));
        when(transform(classFileTransformer,
                JavaModule.ofType(Object.class),
                null,
                Type.getInternalName(Object.class),
                null,
                Object.class.getProtectionDomain(),
                new byte[0])).thenReturn(new byte[0]);
        new AgentBuilder.Default.WarmupStrategy.Enabled(Collections.<Class<?>>singleton(Object.class)).apply(classFileTransformer,
                locationStrategy,
                AgentBuilder.RedefinitionStrategy.DISABLED,
                listener);
        transform(verify(classFileTransformer),
                JavaModule.ofType(Object.class),
                null,
                Type.getInternalName(Object.class),
                null,
                Object.class.getProtectionDomain(),
                new byte[0]);
        verifyNoMoreInteractions(classFileTransformer);
        verify(locationStrategy).classFileLocator(null, JavaModule.ofType(Object.class));
        verifyNoMoreInteractions(locationStrategy);
        verify(listener).onBeforeWarmUp(Collections.<Class<?>>singleton(Object.class), classFileTransformer);
        verify(listener).onAfterWarmUp(Collections.<Class<?>>singleton(Object.class), classFileTransformer, true);
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void testEnabledWithRedefinition() throws Exception {
        when(locationStrategy.classFileLocator(null, JavaModule.ofType(Object.class)))
                .thenReturn(new ClassFileLocator.Simple(Collections.singletonMap(Object.class.getName(), new byte[0])));
        new AgentBuilder.Default.WarmupStrategy.Enabled(Collections.<Class<?>>singleton(Object.class)).apply(classFileTransformer,
                locationStrategy,
                AgentBuilder.RedefinitionStrategy.REDEFINITION,
                listener);
        transform(verify(classFileTransformer),
                JavaModule.ofType(Object.class),
                null,
                Type.getInternalName(Object.class),
                null,
                Object.class.getProtectionDomain(),
                new byte[0]);
        transform(verify(classFileTransformer),
                JavaModule.ofType(Object.class),
                null,
                Type.getInternalName(Object.class),
                Object.class,
                Object.class.getProtectionDomain(),
                new byte[0]);
        verifyNoMoreInteractions(classFileTransformer);
        verify(locationStrategy).classFileLocator(null, JavaModule.ofType(Object.class));
        verifyNoMoreInteractions(locationStrategy);
        verify(listener).onBeforeWarmUp(Collections.<Class<?>>singleton(Object.class), classFileTransformer);
        verify(listener).onAfterWarmUp(Collections.<Class<?>>singleton(Object.class), classFileTransformer, false);
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void testEnabledError() throws Exception {
        when(locationStrategy.classFileLocator(null, JavaModule.ofType(Object.class)))
                .thenReturn(new ClassFileLocator.Simple(Collections.singletonMap(Object.class.getName(), new byte[0])));
        when(transform(classFileTransformer,
                JavaModule.ofType(Object.class),
                null,
                Type.getInternalName(Object.class),
                null,
                Object.class.getProtectionDomain(),
                new byte[0])).thenThrow(throwable);
        new AgentBuilder.Default.WarmupStrategy.Enabled(Collections.<Class<?>>singleton(Object.class)).apply(classFileTransformer,
                locationStrategy,
                AgentBuilder.RedefinitionStrategy.DISABLED,
                listener);
        transform(verify(classFileTransformer),
                JavaModule.ofType(Object.class),
                null,
                Type.getInternalName(Object.class),
                null,
                Object.class.getProtectionDomain(),
                new byte[0]);
        verifyNoMoreInteractions(classFileTransformer);
        verify(locationStrategy).classFileLocator(null, JavaModule.ofType(Object.class));
        verifyNoMoreInteractions(locationStrategy);
        verify(listener).onBeforeWarmUp(Collections.<Class<?>>singleton(Object.class), classFileTransformer);
        verify(listener).onWarmUpError(Object.class, classFileTransformer, throwable);
        verify(listener).onAfterWarmUp(Collections.<Class<?>>singleton(Object.class), classFileTransformer, false);
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void testNoOpWarmupChained() throws Exception {
        assertThat(AgentBuilder.Default.WarmupStrategy.NoOp.INSTANCE.with(Collections.<Class<?>>singleton(Object.class)),
                instanceOf(AgentBuilder.Default.WarmupStrategy.Enabled.class));
    }

    @Test
    public void testEnabledChained() throws Exception {
        assertThat(new AgentBuilder.Default.WarmupStrategy.Enabled(Collections.<Class<?>>emptySet()).with(Collections.<Class<?>>singleton(Object.class)),
                instanceOf(AgentBuilder.Default.WarmupStrategy.Enabled.class));
    }

    private static byte[] transform(ClassFileTransformer classFileTransformer,
                                    JavaModule javaModule,
                                    ClassLoader classLoader,
                                    String typeName,
                                    Class<?> type,
                                    ProtectionDomain protectionDomain,
                                    byte[] binaryRepresentation) throws Exception {
        try {
            return (byte[]) ClassFileTransformer.class.getDeclaredMethod("transform", Class.forName("java.lang.Module"), ClassLoader.class, String.class, Class.class, ProtectionDomain.class, byte[].class)
                    .invoke(classFileTransformer, javaModule.unwrap(), classLoader, typeName, type, protectionDomain, binaryRepresentation);
        } catch (Exception ignored) {
            return classFileTransformer.transform(classLoader, typeName, type, protectionDomain, binaryRepresentation);
        }
    }
}
