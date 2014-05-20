package net.bytebuddy.instrumentation;

import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.type.InstrumentedType;
import net.bytebuddy.utility.MockitoRule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.objectweb.asm.MethodVisitor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class InstrumentationForAbstractMethodTest {

    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private InstrumentedType instrumentedType;
    @Mock
    private Instrumentation.Target instrumentationTarget;
    @Mock
    private Instrumentation.Context instrumentationContext;
    @Mock
    private MethodVisitor methodVisitor;
    @Mock
    private MethodDescription methodDescription;

    @Test
    public void testAppendsCode() throws Exception {
        assertThat(Instrumentation.ForAbstractMethod.INSTANCE.appender(instrumentationTarget).appendsCode(), is(false));
    }

    @Test(expected = IllegalStateException.class)
    public void testAppender() throws Exception {
        Instrumentation.ForAbstractMethod.INSTANCE.appender(instrumentationTarget)
                .apply(methodVisitor, instrumentationContext, methodDescription);
    }

    @Test
    public void testPrepare() throws Exception {
        assertThat(Instrumentation.ForAbstractMethod.INSTANCE.prepare(instrumentedType), is(instrumentedType));
    }
}
