package net.bytebuddy.instrumentation.method.bytecode.stack.assign;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.objectweb.asm.MethodVisitor;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.verifyZeroInteractions;

@RunWith(Parameterized.class)
public class AssignerRefusingTest {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[]{false}, new Object[]{true});
    }

    private final boolean dynamicallyTyped;

    public AssignerRefusingTest(boolean dynamicallyTyped) {
        this.dynamicallyTyped = dynamicallyTyped;
    }

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeDescription first, second;

    @Mock
    private MethodVisitor methodVisitor;

    @Mock
    private Instrumentation.Context instrumentationContext;

    @After
    public void tearDown() throws Exception {
        verifyZeroInteractions(methodVisitor);
        verifyZeroInteractions(instrumentationContext);
    }

    @Test
    public void testAssignmentEqual() throws Exception {
        StackManipulation stackManipulation = Assigner.Refusing.INSTANCE.assign(first, first, dynamicallyTyped);
        assertThat(stackManipulation.isValid(), is(false));
    }

    @Test
    public void testAssignmentNotEqual() throws Exception {
        StackManipulation stackManipulation = Assigner.Refusing.INSTANCE.assign(first, second, dynamicallyTyped);
        assertThat(stackManipulation.isValid(), is(false));
    }
}
