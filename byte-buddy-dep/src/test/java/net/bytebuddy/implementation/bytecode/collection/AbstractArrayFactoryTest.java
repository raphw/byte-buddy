package net.bytebuddy.implementation.bytecode.collection;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.StackSize;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.mockito.asm.Type;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public abstract class AbstractArrayFactoryTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeDescription.Generic componentType;

    @Mock
    private TypeDescription rawComponentType;

    @Mock
    private MethodVisitor methodVisitor;

    @Mock
    private StackManipulation stackManipulation;

    @Mock
    private Implementation.Context implementationContext;

    @Before
    public void setUp() throws Exception {
        when(stackManipulation.isValid()).thenReturn(true);
        when(componentType.asErasure()).thenReturn(rawComponentType);
    }

    @After
    public void tearDown() throws Exception {
        verifyZeroInteractions(implementationContext);
    }

    protected void testCreationUsing(Class<?> componentType, int storageOpcode) throws Exception {
        defineComponentType(componentType);
        CollectionFactory arrayFactory = ArrayFactory.forType(this.componentType);
        StackManipulation arrayStackManipulation = arrayFactory.withValues(Collections.singletonList(stackManipulation));
        assertThat(arrayStackManipulation.isValid(), is(true));
        verify(stackManipulation, atLeast(1)).isValid();
        StackManipulation.Size size = arrayStackManipulation.apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(3 + StackSize.of(componentType).toIncreasingSize().getSizeImpact()));
        verify(methodVisitor).visitInsn(Opcodes.ICONST_1);
        verifyArrayCreation(methodVisitor);
        verify(methodVisitor).visitInsn(Opcodes.DUP);
        verify(methodVisitor).visitInsn(Opcodes.ICONST_0);
        verify(stackManipulation).apply(methodVisitor, implementationContext);
        verify(methodVisitor).visitInsn(storageOpcode);
        verifyNoMoreInteractions(methodVisitor);
        verifyNoMoreInteractions(stackManipulation);
    }

    protected abstract void verifyArrayCreation(MethodVisitor methodVisitor);

    private void defineComponentType(Class<?> type) {
        when(componentType.isPrimitive()).thenReturn(type.isPrimitive());
        when(componentType.represents(type)).thenReturn(true);
        when(rawComponentType.getInternalName()).thenReturn(Type.getInternalName(type));
        when(componentType.getStackSize()).thenReturn(StackSize.of(type));
        when(stackManipulation.apply(any(MethodVisitor.class), any(Implementation.Context.class))).thenReturn(StackSize.of(type).toIncreasingSize());
    }
}
