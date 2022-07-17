package net.bytebuddy.implementation.bytecode.assign;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.objectweb.asm.MethodVisitor;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(Parameterized.class)
public class AssignerEqualTypesOnlyTest {

    private final boolean dynamicallyTyped;

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private TypeDescription.Generic first, second;

    @Mock
    private TypeDescription firstRaw, secondRaw;

    @Mock
    private MethodVisitor methodVisitor;

    @Mock
    private Implementation.Context implementationContext;

    public AssignerEqualTypesOnlyTest(boolean dynamicallyTyped) {
        this.dynamicallyTyped = dynamicallyTyped;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[]{false}, new Object[]{true});
    }

    @Before
    public void setUp() throws Exception {
        when(first.asErasure()).thenReturn(firstRaw);
        when(second.asErasure()).thenReturn(secondRaw);
    }

    @After
    public void tearDown() throws Exception {
        verifyNoMoreInteractions(methodVisitor);
        verifyNoMoreInteractions(implementationContext);
    }

    @Test
    public void testAssignmentGenericEqual() throws Exception {
        StackManipulation stackManipulation = Assigner.EqualTypesOnly.GENERIC.assign(first, first, Assigner.Typing.of(dynamicallyTyped));
        assertThat(stackManipulation.isValid(), is(true));
        StackManipulation.Size size = stackManipulation.apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
        verifyNoMoreInteractions(first);
    }

    @Test
    public void testAssignmentGenericNotEqual() throws Exception {
        StackManipulation stackManipulation = Assigner.EqualTypesOnly.GENERIC.assign(first, second, Assigner.Typing.of(dynamicallyTyped));
        assertThat(stackManipulation.isValid(), is(false));
        verifyNoMoreInteractions(first);
        verifyNoMoreInteractions(second);
    }

    @Test
    public void testAssignmentErausreEqual() throws Exception {
        StackManipulation stackManipulation = Assigner.EqualTypesOnly.ERASURE.assign(first, first, Assigner.Typing.of(dynamicallyTyped));
        assertThat(stackManipulation.isValid(), is(true));
        StackManipulation.Size size = stackManipulation.apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
        verify(first, times(2)).asErasure();
        verifyNoMoreInteractions(first);
    }

    @Test
    public void testAssignmentErasureNotEqual() throws Exception {
        StackManipulation stackManipulation = Assigner.EqualTypesOnly.ERASURE.assign(first, second, Assigner.Typing.of(dynamicallyTyped));
        assertThat(stackManipulation.isValid(), is(false));
        verify(first).asErasure();
        verifyNoMoreInteractions(first);
        verify(second).asErasure();
        verifyNoMoreInteractions(second);
    }
}
