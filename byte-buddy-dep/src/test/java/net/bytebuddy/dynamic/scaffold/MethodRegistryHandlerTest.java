package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.attribute.MethodAttributeAppender;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.type.InstrumentedType;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.objectweb.asm.ClassVisitor;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

public class MethodRegistryHandlerTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private InstrumentedType instrumentedType, preparedInstrumentedType;

    @Mock
    private Instrumentation instrumentation;

    @Mock
    private Object annotationValue;

    @Mock
    private Instrumentation.Target instrumentationTarget;

    @Mock
    private MethodAttributeAppender attributeAppender;

    @Mock
    private ClassVisitor classVisitor;

    @Mock
    private Instrumentation.Context instrumentationContext;

    @Mock
    private MethodDescription methodDescription;

    @Before
    public void setUp() throws Exception {
        when(instrumentation.prepare(instrumentedType)).thenReturn(preparedInstrumentedType);
    }

    @Test
    public void testHandlerForAbstractMethod() throws Exception {
        assertThat(MethodRegistry.Handler.ForAbstractMethod.INSTANCE.prepare(instrumentedType), is(instrumentedType));
        TypeWriter.MethodPool.Entry entry = MethodRegistry.Handler.ForAbstractMethod.INSTANCE.compile(instrumentationTarget).assemble(attributeAppender);
        assertThat(entry.getSort(), is(TypeWriter.MethodPool.Entry.Sort.DEFINE));
    }

    @Test
    public void testHandlerForInstrumentation() throws Exception {
        MethodRegistry.Handler handler = new MethodRegistry.Handler.ForInstrumentation(instrumentation);
        assertThat(handler.prepare(instrumentedType), is(preparedInstrumentedType));
        TypeWriter.MethodPool.Entry entry = handler.compile(instrumentationTarget).assemble(attributeAppender);
        assertThat(entry.getSort(), is(TypeWriter.MethodPool.Entry.Sort.IMPLEMENT));
    }

    @Test
    public void testHandlerForAnnotationValue() throws Exception {
        MethodRegistry.Handler handler = new MethodRegistry.Handler.ForAnnotationValue(annotationValue);
        assertThat(handler.prepare(instrumentedType), is(instrumentedType));
        TypeWriter.MethodPool.Entry entry = handler.compile(instrumentationTarget).assemble(attributeAppender);
        assertThat(entry.getSort(), is(TypeWriter.MethodPool.Entry.Sort.DEFINE));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(MethodRegistry.Handler.ForAbstractMethod.class).apply();
        ObjectPropertyAssertion.of(MethodRegistry.Handler.ForInstrumentation.class).apply();
        ObjectPropertyAssertion.of(MethodRegistry.Handler.ForInstrumentation.Compiled.class).apply();
        ObjectPropertyAssertion.of(MethodRegistry.Handler.ForAnnotationValue.class).apply();
    }
}
