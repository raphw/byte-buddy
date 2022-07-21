package net.bytebuddy.agent.builder;

import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.test.utility.JavaVersionRule;
import net.bytebuddy.utility.JavaModule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.objectweb.asm.Type;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class AgentBuilderDefaultWarmupStrategyTest {

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    @Mock
    private ResettableClassFileTransformer classFileTransformer;

    @Mock
    private AgentBuilder.LocationStrategy locationStrategy;

    @Mock
    private AgentBuilder.CircularityLock circularityLock;

    @Mock
    private AgentBuilder.InstallationListener listener;

    @Mock
    private RuntimeException throwable;

    @Test
    public void testNoOp() throws Exception {
        AgentBuilder.Default.WarmupStrategy.NoOp.INSTANCE.apply(classFileTransformer,
                locationStrategy,
                AgentBuilder.RedefinitionStrategy.DISABLED,
                circularityLock,
                listener);
        verifyNoMoreInteractions(classFileTransformer);
        verifyNoMoreInteractions(locationStrategy);
        verifyNoMoreInteractions(circularityLock);
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void testEnabledNoRedefinition() throws Exception {
        when(locationStrategy.classFileLocator(null, JavaModule.ofType(Object.class)))
                .thenReturn(new ClassFileLocator.Simple(Collections.singletonMap(Object.class.getName(), new byte[0])));
        new AgentBuilder.Default.WarmupStrategy.Enabled(Collections.<Class<?>>singleton(Object.class)).apply(classFileTransformer,
                locationStrategy,
                AgentBuilder.RedefinitionStrategy.DISABLED,
                circularityLock,
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
        verify(circularityLock).release();
        verify(circularityLock).acquire();
        verifyNoMoreInteractions(circularityLock);
        verify(listener).onBeforeWarmUp(Collections.<Class<?>>singleton(Object.class), classFileTransformer);
        verify(listener).onAfterWarmUp(Collections.<Class<?>, byte[]>singletonMap(Object.class, null), classFileTransformer, false);
        verifyNoMoreInteractions(listener);
    }

    @Test
    @JavaVersionRule.Enforce(7) // Error in generic processing on Java 6
    public void testEnabledEffect() throws Exception {
        when(locationStrategy.classFileLocator(null, JavaModule.ofType(Object.class)))
                .thenReturn(new ClassFileLocator.Simple(Collections.singletonMap(Object.class.getName(), new byte[0])));
        when(transform(classFileTransformer,
                JavaModule.ofType(Object.class),
                null,
                Type.getInternalName(Object.class),
                null,
                Object.class.getProtectionDomain(),
                new byte[0])).thenReturn(new byte[]{4, 5, 6});
        new AgentBuilder.Default.WarmupStrategy.Enabled(Collections.<Class<?>>singleton(Object.class)).apply(classFileTransformer,
                locationStrategy,
                AgentBuilder.RedefinitionStrategy.DISABLED,
                circularityLock,
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
        verify(circularityLock).release();
        verify(circularityLock).acquire();
        verifyNoMoreInteractions(circularityLock);
        verify(listener).onBeforeWarmUp(Collections.<Class<?>>singleton(Object.class), classFileTransformer);
        verify(listener).onAfterWarmUp(argThat(new ArgumentMatcher<Map<Class<?>, byte[]>>() {
            public boolean matches(Map<Class<?>, byte[]> argument) {
                return argument.size() == 1
                        && argument.containsKey(Object.class)
                        && Arrays.equals(argument.get(Object.class), new byte[]{4, 5, 6});
            }
        }), eq(classFileTransformer), eq(true));
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void testEnabledWithRedefinition() throws Exception {
        when(locationStrategy.classFileLocator(null, JavaModule.ofType(Object.class)))
                .thenReturn(new ClassFileLocator.Simple(Collections.singletonMap(Object.class.getName(), new byte[0])));
        new AgentBuilder.Default.WarmupStrategy.Enabled(Collections.<Class<?>>singleton(Object.class)).apply(classFileTransformer,
                locationStrategy,
                AgentBuilder.RedefinitionStrategy.REDEFINITION,
                circularityLock,
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
        verify(circularityLock).release();
        verify(circularityLock).acquire();
        verifyNoMoreInteractions(circularityLock);
        verify(listener).onBeforeWarmUp(Collections.<Class<?>>singleton(Object.class), classFileTransformer);
        verify(listener).onAfterWarmUp(Collections.<Class<?>, byte[]>singletonMap(Object.class, null), classFileTransformer, false);
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
                circularityLock,
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
        verify(circularityLock).release();
        verify(circularityLock).acquire();
        verifyNoMoreInteractions(circularityLock);
        verify(listener).onBeforeWarmUp(Collections.<Class<?>>singleton(Object.class), classFileTransformer);
        verify(listener).onWarmUpError(Object.class, classFileTransformer, throwable);
        verify(listener).onAfterWarmUp(Collections.<Class<?>, byte[]>singletonMap(Object.class, null), classFileTransformer, false);
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
