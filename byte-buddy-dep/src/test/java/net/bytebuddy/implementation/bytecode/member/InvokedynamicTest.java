package net.bytebuddy.implementation.bytecode.member;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.utility.JavaConstant;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class InvokedynamicTest {

    private static final String FOO = "foo", BAR = "bar";

    private static final int QUX = 42;

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private MethodVisitor methodVisitor;

    @Mock
    private Implementation.Context implementationContext;

    @Test
    public void testDynamicInvocation() throws Exception {
        StackManipulation stackManipulation = new Invokedynamic(
                FOO,
                JavaConstant.MethodType.of(void.class, Object.class),
                new JavaConstant.MethodHandle(JavaConstant.MethodHandle.HandleType.INVOKE_STATIC,
                        TypeDescription.ForLoadedType.of(Object.class),
                        BAR,
                        TypeDescription.ForLoadedType.of(Object.class),
                        Collections.singletonList(TypeDescription.ForLoadedType.of(Object.class))),
                Collections.singletonList(JavaConstant.Simple.ofLoaded(QUX)));
        assertThat(stackManipulation.isValid(), is(true));
        StackManipulation.Size size = stackManipulation.apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(-1));
        assertThat(size.getMaximalSize(), is(0));
        verify(methodVisitor).visitInvokeDynamicInsn(FOO,
                "(Ljava/lang/Object;)V",
                new Handle(Opcodes.H_INVOKESTATIC,
                        "java/lang/Object",
                        BAR,
                        "(Ljava/lang/Object;)Ljava/lang/Object;",
                        false),
                QUX);
        verifyNoMoreInteractions(methodVisitor);
        verifyNoMoreInteractions(implementationContext);
    }
}
