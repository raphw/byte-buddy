package net.bytebuddy.implementation.bind;

import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.type.TypeDescription;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.Mockito.*;

@RunWith(Parameterized.class)
public class ArgumentTypeResolverPrimitiveTest extends AbstractArgumentTypeResolverTest {

    private final Class<?> firstType;

    private final Class<?> secondType;

    @Mock
    private TypeDescription.Generic firstPrimitive, secondPrimitive;

    @Mock
    private TypeDescription firstRawPrimitive, secondRawPrimitive;

    public ArgumentTypeResolverPrimitiveTest(Class<?> firstType, Class<?> secondType) {
        this.firstType = firstType;
        this.secondType = secondType;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {boolean.class, byte.class},
                {boolean.class, short.class},
                {boolean.class, char.class},
                {boolean.class, int.class},
                {boolean.class, long.class},
                {boolean.class, float.class},
                {boolean.class, double.class},
                {byte.class, short.class},
                {byte.class, char.class},
                {byte.class, int.class},
                {byte.class, long.class},
                {byte.class, float.class},
                {byte.class, double.class},
                {short.class, char.class},
                {short.class, int.class},
                {short.class, long.class},
                {short.class, float.class},
                {short.class, double.class},
                {char.class, long.class},
                {char.class, float.class},
                {char.class, double.class},
                {int.class, char.class},
                {int.class, long.class},
                {int.class, float.class},
                {int.class, double.class},
                {long.class, float.class},
                {long.class, double.class},
                {float.class, double.class},
        });
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        when(firstPrimitive.asErasure()).thenReturn(firstRawPrimitive);
        when(secondPrimitive.asErasure()).thenReturn(secondRawPrimitive);
        when(sourceType.isPrimitive()).thenReturn(true);
        when(firstRawPrimitive.isPrimitive()).thenReturn(true);
        when(firstRawPrimitive.represents(firstType)).thenReturn(true);
        when(secondRawPrimitive.isPrimitive()).thenReturn(true);
        when(secondRawPrimitive.represents(secondType)).thenReturn(true);
    }

    @Test
    public void testLeftDominance() throws Exception {
        testDominance(firstPrimitive, secondPrimitive, MethodDelegationBinder.AmbiguityResolver.Resolution.LEFT);
    }

    @Test
    public void testRightDominance() throws Exception {
        testDominance(secondPrimitive, firstPrimitive, MethodDelegationBinder.AmbiguityResolver.Resolution.RIGHT);
    }

    @Test
    public void testLeftNonDominance() throws Exception {
        testDominance(secondPrimitive, firstPrimitive, MethodDelegationBinder.AmbiguityResolver.Resolution.RIGHT);
    }

    @Test
    public void testRightNonDominance() throws Exception {
        testDominance(firstPrimitive, secondPrimitive, MethodDelegationBinder.AmbiguityResolver.Resolution.LEFT);
    }

    @Test
    public void testNonDominance() throws Exception {
        testDominance(firstPrimitive, firstPrimitive, MethodDelegationBinder.AmbiguityResolver.Resolution.AMBIGUOUS);
    }

    private void testDominance(TypeDescription.Generic leftPrimitive,
                               TypeDescription.Generic rightPrimitive,
                               MethodDelegationBinder.AmbiguityResolver.Resolution expected) throws Exception {
        when(sourceParameterList.size()).thenReturn(2);
        when(sourceType.isPrimitive()).thenReturn(true);
        ParameterDescription leftParameter = mock(ParameterDescription.class);
        when(leftParameter.getType()).thenReturn(leftPrimitive);
        when(leftParameterList.get(0)).thenReturn(leftParameter);
        when(left.getTargetParameterIndex(any(ArgumentTypeResolver.ParameterIndexToken.class)))
                .thenAnswer(new TokenAnswer(new int[][]{{0, 0}}));
        ParameterDescription rightParameter = mock(ParameterDescription.class);
        when(rightParameter.getType()).thenReturn(rightPrimitive);
        when(rightParameterList.get(0)).thenReturn(rightParameter);
        when(right.getTargetParameterIndex(any(ArgumentTypeResolver.ParameterIndexToken.class)))
                .thenAnswer(new TokenAnswer(new int[][]{{0, 0}}));
        MethodDelegationBinder.AmbiguityResolver.Resolution resolution =
                ArgumentTypeResolver.INSTANCE.resolve(source, left, right);
        assertThat(resolution, is(expected));
        verify(source, atLeast(1)).getParameters();
        verify(leftMethod, atLeast(1)).getParameters();
        verify(rightMethod, atLeast(1)).getParameters();
        verify(left, atLeast(1)).getTargetParameterIndex(argThat(describesArgument(0)));
        verify(left, atLeast(1)).getTargetParameterIndex(argThat(describesArgument(1)));
        verify(left, never()).getTargetParameterIndex(not(argThat(describesArgument(0, 1))));
        verify(right, atLeast(1)).getTargetParameterIndex(argThat(describesArgument(0)));
        verify(right, atLeast(1)).getTargetParameterIndex(argThat(describesArgument(1)));
        verify(right, never()).getTargetParameterIndex(not(argThat(describesArgument(0, 1))));
    }
}
