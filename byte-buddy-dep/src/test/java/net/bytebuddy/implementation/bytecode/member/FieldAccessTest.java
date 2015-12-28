package net.bytebuddy.implementation.bytecode.member;

import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.StackSize;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.asm.Opcodes;
import org.objectweb.asm.MethodVisitor;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(Parameterized.class)
public class FieldAccessTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux";

    private final boolean isStatic;

    private final StackSize fieldSize;

    private final int getterChange, getterMaximum, getterOpcode;

    private final int putterChange, putterMaximum, putterOpcode;

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private FieldDescription.InDefinedShape fieldDescription;

    @Mock
    private TypeDescription declaringType, fieldType;

    @Mock
    private TypeDescription.Generic genericFieldType;

    @Mock
    private MethodVisitor methodVisitor;

    @Mock
    private Implementation.Context implementationContext;

    public FieldAccessTest(boolean isStatic,
                           StackSize fieldSize,
                           int getterChange,
                           int getterMaximum,
                           int getterOpcode,
                           int putterChange,
                           int putterMaximum,
                           int putterOpcode) {
        this.isStatic = isStatic;
        this.fieldSize = fieldSize;
        this.getterChange = getterChange;
        this.getterMaximum = getterMaximum;
        this.getterOpcode = getterOpcode;
        this.putterChange = putterChange;
        this.putterMaximum = putterMaximum;
        this.putterOpcode = putterOpcode;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {true, StackSize.SINGLE, 1, 1, Opcodes.GETSTATIC, -1, 0, Opcodes.PUTSTATIC},
                {true, StackSize.DOUBLE, 2, 2, Opcodes.GETSTATIC, -2, 0, Opcodes.PUTSTATIC},
                {false, StackSize.SINGLE, 0, 0, Opcodes.GETFIELD, -2, 0, Opcodes.PUTFIELD},
                {false, StackSize.DOUBLE, 1, 1, Opcodes.GETFIELD, -3, 0, Opcodes.PUTFIELD}
        });
    }

    @Before
    public void setUp() throws Exception {
        when(declaringType.asErasure()).thenReturn(declaringType);
        when(fieldDescription.getDeclaringType()).thenReturn(declaringType);
        when(fieldDescription.getType()).thenReturn(genericFieldType);
        when(genericFieldType.asErasure()).thenReturn(fieldType);
        when(genericFieldType.getStackSize()).thenReturn(fieldSize);
        when(declaringType.getInternalName()).thenReturn(FOO);
        when(fieldDescription.getInternalName()).thenReturn(BAR);
        when(fieldDescription.getDescriptor()).thenReturn(QUX);
        when(fieldDescription.isStatic()).thenReturn(isStatic);
    }

    @After
    public void tearDown() throws Exception {
        verifyZeroInteractions(implementationContext);
    }

    @Test
    public void testGetter() throws Exception {
        FieldAccess.Defined getter = FieldAccess.forField(fieldDescription);
        assertThat(getter.getter().isValid(), is(true));
        StackManipulation.Size size = getter.getter().apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(getterChange));
        assertThat(size.getMaximalSize(), is(getterMaximum));
        verify(methodVisitor).visitFieldInsn(getterOpcode, FOO, BAR, QUX);
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testPutter() throws Exception {
        FieldAccess.Defined setter = FieldAccess.forField(fieldDescription);
        assertThat(setter.putter().isValid(), is(true));
        StackManipulation.Size size = setter.putter().apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(putterChange));
        assertThat(size.getMaximalSize(), is(putterMaximum));
        verify(methodVisitor).visitFieldInsn(putterOpcode, FOO, BAR, QUX);
        verifyNoMoreInteractions(methodVisitor);
    }
}
