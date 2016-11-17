package net.bytebuddy.implementation.bytecode.collection;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bytecode.Duplication;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.constant.IntegerConstant;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

public class ArrayAccessOtherTest {

    @Test(expected = IllegalArgumentException.class)
    public void testVoidThrowsException() throws Exception {
        ArrayAccess.of(TypeDescription.VOID);
    }

    @Test
    public void testForEach() throws Exception {
        StackManipulation stackManipulation = mock(StackManipulation.class);
        assertThat(ArrayAccess.REFERENCE.forEach(Collections.singletonList(stackManipulation)),
                is((StackManipulation) new StackManipulation.Compound(new StackManipulation.Compound(Duplication.SINGLE,
                        IntegerConstant.forValue(0),
                        ArrayAccess.REFERENCE.new Loader(),
                        stackManipulation))));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(ArrayAccess.class).apply();
        ObjectPropertyAssertion.of(ArrayAccess.Loader.class).apply();
        ObjectPropertyAssertion.of(ArrayAccess.Putter.class).apply();
    }
}
