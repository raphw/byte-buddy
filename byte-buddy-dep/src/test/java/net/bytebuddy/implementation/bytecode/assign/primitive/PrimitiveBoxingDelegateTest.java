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
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(Parameterized.class)
public class PrimitiveBoxingDelegateTest {

    private static final String VALUE_OF = "valueOf";

    private final Class<?> primitiveType;

    private final TypeDescription primitiveTypeDescription;

    private final TypeDescription referenceTypeDescription;

    private final String boxingMethodDescriptor;

    private final int sizeChange;

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeDescription.Generic targetType;

    @Mock
    private Assigner chainedAssigner;

    @Mock
    private StackManipulation stackManipulation;

    @Mock
    private MethodVisitor methodVisitor;

    @Mock
    private Implementation.Context implementationContext;

    public PrimitiveBoxingDelegateTest(Class<?> primitiveType,
                                       Class<?> referenceType,
                                       String boxingMethodDescriptor,
                                       int sizeChange) {
        this.primitiveType = primitiveType;
        primitiveTypeDescription = mock(TypeDescription.class);
        when(primitiveTypeDescription.represents(primitiveType)).thenReturn(true);
        referenceTypeDescription = TypeDescription.ForLoadedType.of(referenceType);
        this.boxingMethodDescriptor = boxingMethodDescriptor;
        this.sizeChange = sizeChange;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {boolean.class, Boolean.class, "(Z)Ljava/lang/Boolean;", 0},
                {byte.class, Byte.class, "(B)Ljava/lang/Byte;", 0},
                {short.class, Short.class, "(S)Ljava/lang/Short;", 0},
                {char.class, Character.class, "(C)Ljava/lang/Character;", 0},
                {int.class, Integer.class, "(I)Ljava/lang/Integer;", 0},
                {long.class, Long.class, "(J)Ljava/lang/Long;", -1},
                {float.class, Float.class, "(F)Ljava/lang/Float;", 0},
                {double.class, Double.class, "(D)Ljava/lang/Double;", -1},
        });
    }

    @Before
    public void setUp() throws Exception {
        when(chainedAssigner.assign(any(TypeDescription.Generic.class), any(TypeDescription.Generic.class), any(Assigner.Typing.class)))
                .thenReturn(stackManipulation);
        when(stackManipulation.isValid())
                .thenReturn(true);
        when(stackManipulation.apply(any(MethodVisitor.class), any(Implementation.Context.class)))
                .thenReturn(StackSize.ZERO.toIncreasingSize());
    }

    @After
    public void tearDown() throws Exception {
        verifyZeroInteractions(targetType);
        verifyZeroInteractions(implementationContext);
    }

    @Test
    public void testBoxing() throws Exception {
        StackManipulation boxingStackManipulation = PrimitiveBoxingDelegate.forPrimitive(primitiveTypeDescription)
                .assignBoxedTo(targetType, chainedAssigner, Assigner.Typing.STATIC);
        assertThat(boxingStackManipulation.isValid(), is(true));
        StackManipulation.Size size = boxingStackManipulation.apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(sizeChange));
        assertThat(size.getMaximalSize(), is(0));
        verify(primitiveTypeDescription).represents(primitiveType);
        verify(primitiveTypeDescription, atLeast(1)).represents(any(Class.class));
        verifyNoMoreInteractions(primitiveTypeDescription);
        verify(chainedAssigner).assign(referenceTypeDescription.asGenericType(), targetType, Assigner.Typing.STATIC);
        verifyNoMoreInteractions(chainedAssigner);
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKESTATIC,
                referenceTypeDescription.getInternalName(),
                VALUE_OF,
                boxingMethodDescriptor,
                false);
        verifyNoMoreInteractions(methodVisitor);
        verify(stackManipulation, atLeast(1)).isValid();
        verify(stackManipulation).apply(methodVisitor, implementationContext);
        verifyNoMoreInteractions(stackManipulation);
    }
}
