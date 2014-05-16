package net.bytebuddy.instrumentation.method.bytecode.stack.constant;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;
import net.bytebuddy.utility.MockitoRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.mockito.asm.Opcodes;
import org.mockito.asm.Type;
import org.objectweb.asm.MethodVisitor;

import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;

public class MethodConstantTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodDescription methodDescription;
    @Mock
    private TypeDescription declaringType, parameterType;
    @Mock
    private TypeList typeList;
    @Mock
    private MethodVisitor methodVisitor;
    @Mock
    private Instrumentation.Context instrumentationContext;

    @Before
    public void setUp() throws Exception {
        when(methodDescription.getDeclaringType()).thenReturn(declaringType);
        when(methodDescription.getInternalName()).thenReturn(FOO);
        when(methodDescription.getParameterTypes()).thenReturn(typeList);
        when(declaringType.getDescriptor()).thenReturn(BAR);
        when(typeList.iterator()).thenReturn(Arrays.asList(parameterType).iterator());
        when(parameterType.getDescriptor()).thenReturn(QUX);
    }

    @After
    public void tearDown() throws Exception {
        verifyZeroInteractions(instrumentationContext);
    }

    @Test
    public void testMethod() throws Exception {
        StackManipulation.Size size = MethodConstant.forMethod(methodDescription).apply(methodVisitor, instrumentationContext);
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(6));
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                Type.getInternalName(Class.class),
                "getDeclaredMethod",
                "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;",
                false);
    }

    @Test
    public void testConstructor() throws Exception {
        when(methodDescription.isConstructor()).thenReturn(true);
        StackManipulation.Size size = MethodConstant.forMethod(methodDescription).apply(methodVisitor, instrumentationContext);
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(5));
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                Type.getInternalName(Class.class),
                "getDeclaredConstructor",
                "([Ljava/lang/Class;)Ljava/lang/reflect/Constructor;",
                false);
    }
}
