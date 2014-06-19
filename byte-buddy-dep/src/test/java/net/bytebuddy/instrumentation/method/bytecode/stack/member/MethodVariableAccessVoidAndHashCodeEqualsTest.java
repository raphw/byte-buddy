package net.bytebuddy.instrumentation.method.bytecode.stack.member;

import net.bytebuddy.instrumentation.type.TypeDescription;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MethodVariableAccessVoidAndHashCodeEqualsTest {

    @Test(expected = IllegalArgumentException.class)
    public void testVoidArgument() throws Exception {
        TypeDescription voidTypeDescription = mock(TypeDescription.class);
        when(voidTypeDescription.isPrimitive()).thenReturn(true);
        when(voidTypeDescription.represents(void.class)).thenReturn(true);
        MethodVariableAccess.forType(voidTypeDescription);
    }

    @Test
    public void testHashCodeEquals() throws Exception {
        assertThat(MethodVariableAccess.forType(new TypeDescription.ForLoadedType(Object.class)).loadFromIndex(4).hashCode(),
                is(MethodVariableAccess.forType(new TypeDescription.ForLoadedType(Object.class)).loadFromIndex(4).hashCode()));
        assertThat(MethodVariableAccess.forType(new TypeDescription.ForLoadedType(Object.class)).loadFromIndex(4),
                is(MethodVariableAccess.forType(new TypeDescription.ForLoadedType(Object.class)).loadFromIndex(4)));
        assertThat(MethodVariableAccess.forType(new TypeDescription.ForLoadedType(Object.class)).loadFromIndex(3).hashCode(),
                not(is(MethodVariableAccess.forType(new TypeDescription.ForLoadedType(Object.class)).loadFromIndex(4).hashCode())));
        assertThat(MethodVariableAccess.forType(new TypeDescription.ForLoadedType(Object.class)).loadFromIndex(3),
                not(is(MethodVariableAccess.forType(new TypeDescription.ForLoadedType(Object.class)).loadFromIndex(4))));
    }
}
