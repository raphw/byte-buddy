package net.bytebuddy.agent.builder;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.test.utility.JavaVersionRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mockito;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class AgentBuilderLambdaInstrumentationStrategyTest {

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    @Test
    public void testEnabled() throws Exception {
        assertThat(AgentBuilder.LambdaInstrumentationStrategy.of(true).isEnabled(), is(true));
        assertThat(AgentBuilder.LambdaInstrumentationStrategy.of(false).isEnabled(), is(false));
    }

    @Test
    public void testEnabledStrategyNeverThrowsException() throws Exception {
        ClassFileTransformer initialClassFileTransformer = mock(ClassFileTransformer.class);
        assertThat(LambdaFactory.register(initialClassFileTransformer,
                mock(AgentBuilder.Default.LambdaInstrumentationStrategy.LambdaInstanceFactory.class)), is(true));
        try {
            ByteBuddy byteBuddy = mock(ByteBuddy.class);
            Instrumentation instrumentation = mock(Instrumentation.class);
            ClassFileTransformer classFileTransformer = mock(ClassFileTransformer.class);
            try {
                AgentBuilder.Default.LambdaInstrumentationStrategy.ENABLED.apply(byteBuddy, instrumentation, classFileTransformer);
            } finally {
                assertThat(LambdaFactory.release(classFileTransformer), is(false));
            }
        } finally {
            assertThat(LambdaFactory.release(initialClassFileTransformer), is(true));
        }
    }

    @Test
    public void testDisabledStrategyIsNoOp() throws Exception {
        ByteBuddy byteBuddy = mock(ByteBuddy.class);
        Instrumentation instrumentation = mock(Instrumentation.class);
        ClassFileTransformer classFileTransformer = mock(ClassFileTransformer.class);
        AgentBuilder.Default.LambdaInstrumentationStrategy.DISABLED.apply(byteBuddy, instrumentation, classFileTransformer);
        verifyNoMoreInteractions(byteBuddy);
        verifyNoMoreInteractions(instrumentation);
        verifyNoMoreInteractions(classFileTransformer);
    }

    @Test
    public void testEnabledIsInstrumented() throws Exception {
        assertThat(AgentBuilder.LambdaInstrumentationStrategy.ENABLED.isInstrumented(Object.class), is(true));
        assertThat(AgentBuilder.LambdaInstrumentationStrategy.ENABLED.isInstrumented(null), is(true));
    }

    @Test
    public void testDisabledIsInstrumented() throws Exception {
        assertThat(AgentBuilder.LambdaInstrumentationStrategy.DISABLED.isInstrumented(Object.class), is(true));
        assertThat(AgentBuilder.LambdaInstrumentationStrategy.DISABLED.isInstrumented(null), is(true));
    }

    @Test(expected = IllegalStateException.class)
    public void testLoaderUnavailableThrowsExceptionOnApply() throws Exception {
        AgentBuilder.LambdaInstrumentationStrategy.LambdaMetafactoryFactory.Loader.Unavailable.INSTANCE.apply(mock(MethodVisitor.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testLoaderUnavailableThrowsExceptionOnStackSize() throws Exception {
        AgentBuilder.LambdaInstrumentationStrategy.LambdaMetafactoryFactory.Loader.Unavailable.INSTANCE.getStackSize();
    }

    @Test(expected = IllegalStateException.class)
    public void testLoaderUnavailableThrowsExceptionOnLocalVariableArrayLength() throws Exception {
        AgentBuilder.LambdaInstrumentationStrategy.LambdaMetafactoryFactory.Loader.Unavailable.INSTANCE.getLocalVariableLength();
    }

    @Test
    public void testLoaderMethodHandleLookup() throws Exception {
        MethodVisitor methodVisitor = mock(MethodVisitor.class);
        AgentBuilder.LambdaInstrumentationStrategy.LambdaMetafactoryFactory.Loader.UsingMethodHandleLookup.INSTANCE.apply(methodVisitor);
        verify(methodVisitor, never()).visitCode();
        verify(methodVisitor, never()).visitMaxs(anyInt(), anyInt());
        verify(methodVisitor, never()).visitLineNumber(anyInt(), Mockito.<Label>any());
        assertThat(AgentBuilder.LambdaInstrumentationStrategy.LambdaMetafactoryFactory.Loader.UsingMethodHandleLookup.INSTANCE.getStackSize(), is(8));
        assertThat(AgentBuilder.LambdaInstrumentationStrategy.LambdaMetafactoryFactory.Loader.UsingMethodHandleLookup.INSTANCE.getLocalVariableLength(), is(15));
    }

    @Test
    public void testLoaderSunMiscUnsafe() throws Exception {
        MethodVisitor methodVisitor = mock(MethodVisitor.class);
        AgentBuilder.LambdaInstrumentationStrategy.LambdaMetafactoryFactory.Loader.UsingUnsafe.SUN_MISC_UNSAFE.apply(methodVisitor);
        verify(methodVisitor, never()).visitCode();
        verify(methodVisitor, never()).visitMaxs(anyInt(), anyInt());
        verify(methodVisitor, never()).visitLineNumber(anyInt(), Mockito.<Label>any());
        assertThat(AgentBuilder.LambdaInstrumentationStrategy.LambdaMetafactoryFactory.Loader.UsingUnsafe.SUN_MISC_UNSAFE.getStackSize(), is(4));
        assertThat(AgentBuilder.LambdaInstrumentationStrategy.LambdaMetafactoryFactory.Loader.UsingUnsafe.SUN_MISC_UNSAFE.getLocalVariableLength(), is(13));
    }

    @Test
    public void testLoaderJdkInternalMiscUnsafe() throws Exception {
        MethodVisitor methodVisitor = mock(MethodVisitor.class);
        AgentBuilder.LambdaInstrumentationStrategy.LambdaMetafactoryFactory.Loader.UsingUnsafe.JDK_INTERNAL_MISC_UNSAFE.apply(methodVisitor);
        verify(methodVisitor, never()).visitCode();
        verify(methodVisitor, never()).visitMaxs(anyInt(), anyInt());
        verify(methodVisitor, never()).visitLineNumber(anyInt(), Mockito.<Label>any());
        assertThat(AgentBuilder.LambdaInstrumentationStrategy.LambdaMetafactoryFactory.Loader.UsingUnsafe.JDK_INTERNAL_MISC_UNSAFE.getStackSize(), is(4));
        assertThat(AgentBuilder.LambdaInstrumentationStrategy.LambdaMetafactoryFactory.Loader.UsingUnsafe.JDK_INTERNAL_MISC_UNSAFE.getLocalVariableLength(), is(13));
    }

    @Test
    public void testFactoryRegularPrepare() throws Exception {
        MethodVisitor methodVisitor = mock(MethodVisitor.class);
        AgentBuilder.LambdaInstrumentationStrategy.LambdaMetafactoryFactory.REGULAR.onDispatch(methodVisitor);
        verify(methodVisitor, never()).visitCode();
        verify(methodVisitor, never()).visitMaxs(anyInt(), anyInt());
        verify(methodVisitor, never()).visitLineNumber(anyInt(), Mockito.<Label>any());
    }

    @Test
    public void testFactoryAlternativePrepare() throws Exception {
        MethodVisitor methodVisitor = mock(MethodVisitor.class);
        AgentBuilder.LambdaInstrumentationStrategy.LambdaMetafactoryFactory.ALTERNATIVE.onDispatch(methodVisitor);
        verify(methodVisitor, never()).visitCode();
        verify(methodVisitor, never()).visitMaxs(anyInt(), anyInt());
        verify(methodVisitor, never()).visitLineNumber(anyInt(), Mockito.<Label>any());
    }

    @Test
    @JavaVersionRule.Enforce(7)
    public void testFactoryRegular() throws Exception {
        MethodVisitor methodVisitor = mock(MethodVisitor.class);
        ByteCodeAppender.Size size = AgentBuilder.LambdaInstrumentationStrategy.LambdaMetafactoryFactory.REGULAR.apply(
                methodVisitor,
                mock(Implementation.Context.class),
                mock(MethodDescription.class));
        verify(methodVisitor, never()).visitCode();
        assertThat(size.getOperandStackSize(), not(0));
        assertThat(size.getLocalVariableSize(), not(0));
        verify(methodVisitor, never()).visitLineNumber(anyInt(), Mockito.<Label>any());
    }

    @Test
    @JavaVersionRule.Enforce(7)
    public void testFactoryAlternative() throws Exception {
        MethodVisitor methodVisitor = mock(MethodVisitor.class);
        ByteCodeAppender.Size size = AgentBuilder.LambdaInstrumentationStrategy.LambdaMetafactoryFactory.ALTERNATIVE.apply(
                methodVisitor,
                mock(Implementation.Context.class),
                mock(MethodDescription.class));
        verify(methodVisitor, never()).visitCode();
        assertThat(size.getOperandStackSize(), not(0));
        assertThat(size.getLocalVariableSize(), not(0));
        verify(methodVisitor, never()).visitLineNumber(anyInt(), Mockito.<Label>any());
    }
}
