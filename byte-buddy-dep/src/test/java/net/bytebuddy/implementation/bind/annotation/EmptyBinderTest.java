package net.bytebuddy.implementation.bind.annotation;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(Parameterized.class)
public class EmptyBinderTest extends AbstractAnnotationBinderTest<Empty> {

    private final TypeDescription typeDescription;

    private final int opcode;

    @Mock
    private MethodVisitor methodVisitor;

    @Mock
    private Implementation.Context implementationContext;

    public EmptyBinderTest(Class<?> type, int opcode) {
        super(Empty.class);
        typeDescription = new TypeDescription.ForLoadedType(type);
        this.opcode = opcode;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {boolean.class, Opcodes.ICONST_0},
                {byte.class, Opcodes.ICONST_0},
                {short.class, Opcodes.ICONST_0},
                {char.class, Opcodes.ICONST_0},
                {int.class, Opcodes.ICONST_0},
                {long.class, Opcodes.LCONST_0},
                {float.class, Opcodes.FCONST_0},
                {double.class, Opcodes.DCONST_0},
                {Object.class, Opcodes.ACONST_NULL}
        });
    }

    @Override
    protected TargetMethodAnnotationDrivenBinder.ParameterBinder<Empty> getSimpleBinder() {
        return Empty.Binder.INSTANCE;
    }

    @Test
    public void testEmptyValue() throws Exception {
        when(target.getType()).thenReturn(typeDescription);
        TargetMethodAnnotationDrivenBinder.ParameterBinding<?> binding = Empty.Binder
                .INSTANCE.bind(annotationDescription,
                        source,
                        target,
                        implementationTarget,
                        assigner);
        assertThat(binding.isValid(), is(true));
        StackManipulation.Size size = binding.apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(typeDescription.getStackSize().getSize()));
        assertThat(size.getMaximalSize(), is(typeDescription.getStackSize().getSize()));
        verify(methodVisitor).visitInsn(opcode);
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(implementationContext);
    }
}
