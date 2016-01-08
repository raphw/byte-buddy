package net.bytebuddy.implementation.bytecode.assign.primitive;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.StackSize;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.asm.Type;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(Parameterized.class)
public class PrimitiveUnboxingDelegateDirectTest {

    private final Class<?> primitiveType;

    private final Class<?> wrapperType;

    private final String unboxingMethodName;

    private final String unboxingMethodDescriptor;

    private final int sizeChange;

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeDescription.Generic primitiveTypeDescription, wrapperTypeDescription;

    @Mock
    private TypeDescription rawPrimitiveTypeDescription, rawWrapperTypeDescription;

    @Mock
    private Assigner chainedAssigner;

    @Mock
    private StackManipulation stackManipulation;

    @Mock
    private MethodVisitor methodVisitor;

    @Mock
    private Implementation.Context implementationContext;

    public PrimitiveUnboxingDelegateDirectTest(Class<?> primitiveType,
                                               Class<?> wrapperType,
                                               String unboxingMethodName,
                                               String unboxingMethodDescriptor,
                                               int sizeChange) {
        this.primitiveType = primitiveType;
        this.wrapperType = wrapperType;
        this.unboxingMethodName = unboxingMethodName;
        this.unboxingMethodDescriptor = unboxingMethodDescriptor;
        this.sizeChange = sizeChange;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {boolean.class, Boolean.class, "booleanValue", "()Z", 0},
                {byte.class, Byte.class, "byteValue", "()B", 0},
                {short.class, Short.class, "shortValue", "()S", 0},
                {char.class, Character.class, "charValue", "()C", 0},
                {int.class, Integer.class, "intValue", "()I", 0},
                {long.class, Long.class, "longValue", "()J", 1},
                {float.class, Float.class, "floatValue", "()F", 0},
                {double.class, Double.class, "doubleValue", "()D", 1},
        });
    }

    @Before
    public void setUp() throws Exception {
        when(primitiveTypeDescription.isPrimitive()).thenReturn(true);
        when(primitiveTypeDescription.represents(primitiveType)).thenReturn(true);
        when(primitiveTypeDescription.asErasure()).thenReturn(rawPrimitiveTypeDescription);
        when(rawPrimitiveTypeDescription.getInternalName()).thenReturn(Type.getInternalName(primitiveType));
        when(wrapperTypeDescription.isPrimitive()).thenReturn(false);
        when(wrapperTypeDescription.represents(wrapperType)).thenReturn(true);
        when(wrapperTypeDescription.asErasure()).thenReturn(rawWrapperTypeDescription);
        when(rawWrapperTypeDescription.getInternalName()).thenReturn(Type.getInternalName(wrapperType));
        when(chainedAssigner.assign(any(TypeDescription.Generic.class), any(TypeDescription.Generic.class), any(Assigner.Typing.class))).thenReturn(stackManipulation);
        when(stackManipulation.isValid()).thenReturn(true);
        when(stackManipulation.apply(any(MethodVisitor.class), any(Implementation.Context.class))).thenReturn(StackSize.ZERO.toIncreasingSize());
    }

    @After
    public void tearDown() throws Exception {
        verifyZeroInteractions(implementationContext);
    }

    @Test
    public void testTrivialBoxing() throws Exception {
        StackManipulation stackManipulation = PrimitiveUnboxingDelegate.forReferenceType(wrapperTypeDescription)
                .assignUnboxedTo(primitiveTypeDescription, chainedAssigner, Assigner.Typing.STATIC);
        assertThat(stackManipulation.isValid(), is(true));
        StackManipulation.Size size = stackManipulation.apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(sizeChange));
        assertThat(size.getMaximalSize(), is(sizeChange));
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                Type.getInternalName(wrapperType),
                unboxingMethodName,
                unboxingMethodDescriptor,
                false);
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(chainedAssigner);
        verifyZeroInteractions(this.stackManipulation);
    }

    @Test
    public void testImplicitBoxing() throws Exception {
        TypeDescription.Generic referenceTypeDescription = mock(TypeDescription.Generic.class);
        when(referenceTypeDescription.asGenericType()).thenReturn(referenceTypeDescription);
        StackManipulation primitiveStackManipulation = PrimitiveUnboxingDelegate.forReferenceType(referenceTypeDescription)
                .assignUnboxedTo(primitiveTypeDescription, chainedAssigner, Assigner.Typing.DYNAMIC);
        assertThat(primitiveStackManipulation.isValid(), is(true));
        StackManipulation.Size size = primitiveStackManipulation.apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(sizeChange));
        assertThat(size.getMaximalSize(), is(sizeChange));
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                Type.getInternalName(wrapperType),
                unboxingMethodName,
                unboxingMethodDescriptor,
                false);
        verifyNoMoreInteractions(methodVisitor);
        verify(chainedAssigner).assign(referenceTypeDescription, new TypeDescription.Generic.OfNonGenericType.ForLoadedType(wrapperType), Assigner.Typing.DYNAMIC);
        verifyNoMoreInteractions(chainedAssigner);
        verify(stackManipulation, atLeast(1)).isValid();
        verify(stackManipulation).apply(methodVisitor, implementationContext);
        verifyNoMoreInteractions(stackManipulation);
    }
}
