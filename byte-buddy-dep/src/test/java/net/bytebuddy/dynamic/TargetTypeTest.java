package net.bytebuddy.dynamic;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.test.utility.MockitoRule;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

public class TargetTypeTest {

    private static final String FOO = "foo";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeDescription leftType, rightType, arrayType;

    @Test
    public void testIsFinal() throws Exception {
        assertThat(Modifier.isFinal(TargetType.class.getModifiers()), is(true));
    }

    @Test
    public void testConstructorIsHidden() throws Exception {
        MatcherAssert.assertThat(TargetType.class.getDeclaredConstructors().length, is(1));
        Constructor<?> constructor = TargetType.class.getDeclaredConstructor();
        MatcherAssert.assertThat(Modifier.isPrivate(constructor.getModifiers()), is(true));
        constructor.setAccessible(true);
        try {
            constructor.newInstance();
            fail();
        } catch (InvocationTargetException e) {
            assertEquals(UnsupportedOperationException.class, e.getCause().getClass());
        }
    }

    @Test
    public void testTypeResolution() throws Exception {
        assertThat(TargetType.resolve(leftType, rightType), is(leftType));
        assertThat(TargetType.resolve(TargetType.DESCRIPTION, rightType), is(rightType));
    }

    @Test
    public void testTypeListResolution() throws Exception {
        assertThat(TargetType.resolve(new TypeList.Explicit(Collections.singletonList(leftType)), rightType),
                is((TypeList) new TypeList.Explicit(Collections.singletonList(leftType))));
        assertThat(TargetType.resolve(new TypeList.Explicit(Arrays.asList(TargetType.DESCRIPTION, leftType)), rightType),
                is((TypeList) new TypeList.Explicit(Arrays.asList(rightType, leftType))));
    }

    @Test
    public void testDescription() throws Exception {
        assertThat(TargetType.DESCRIPTION.represents(TargetType.class), is(true));
    }

    @Test
    public void testArrayTypeResolutionNonTargetType() throws Exception {
        when(arrayType.isArray()).thenReturn(true);
        when(arrayType.getComponentType()).thenReturn(leftType);
        assertThat(TargetType.resolve(arrayType, rightType), is(arrayType));
    }

    @Test
    public void testArrayTypeResolutionTargetType() throws Exception {
        when(rightType.getDescriptor()).thenReturn(FOO);
        when(arrayType.isArray()).thenReturn(true);
        when(arrayType.getComponentType()).thenReturn(TargetType.DESCRIPTION);
        assertThat(TargetType.resolve(arrayType, rightType), is(TypeDescription.ArrayProjection.of(rightType, 1)));
    }
}
