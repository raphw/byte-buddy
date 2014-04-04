package net.bytebuddy.instrumentation.method.bytecode.bind;

import net.bytebuddy.instrumentation.type.TypeDescription;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

@RunWith(Parameterized.class)
public class MostSpecificTypeResolverPrimitiveTest extends AbstractMostSpecificTypeResolverTest {

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

    private final Class<?> firstType;
    private final Class<?> secondType;

    public MostSpecificTypeResolverPrimitiveTest(Class<?> firstType, Class<?> secondType) {
        this.firstType = firstType;
        this.secondType = secondType;
    }

    @Mock
    private TypeDescription firstPrimitive;
    @Mock
    private TypeDescription secondPrimitive;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        when(sourceType.isPrimitive()).thenReturn(true);
        when(firstPrimitive.isPrimitive()).thenReturn(true);
        when(firstPrimitive.represents(firstType)).thenReturn(true);
        when(secondPrimitive.isPrimitive()).thenReturn(true);
        when(secondPrimitive.represents(secondType)).thenReturn(true);
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

    private void testDominance(TypeDescription leftPrimitive,
                               TypeDescription rightPrimitive,
                               MethodDelegationBinder.AmbiguityResolver.Resolution expected) throws Exception {
        when(sourceTypeList.size()).thenReturn(2);
        when(sourceType.isPrimitive()).thenReturn(true);
        when(leftTypeList.get(0)).thenReturn(leftPrimitive);
        when(left.getTargetParameterIndex(any(MostSpecificTypeResolver.ParameterIndexToken.class)))
                .thenAnswer(new TokenAnswer(new int[][]{{0, 0}}));
        when(rightTypeList.get(0)).thenReturn(rightPrimitive);
        when(right.getTargetParameterIndex(any(MostSpecificTypeResolver.ParameterIndexToken.class)))
                .thenAnswer(new TokenAnswer(new int[][]{{0, 0}}));
        MethodDelegationBinder.AmbiguityResolver.Resolution resolution =
                MostSpecificTypeResolver.INSTANCE.resolve(source, left, right);
        assertThat(resolution, is(expected));
        verify(source, atLeast(1)).getParameterTypes();
        verify(leftMethod, atLeast(1)).getParameterTypes();
        verify(rightMethod, atLeast(1)).getParameterTypes();
        verify(left, atLeast(1)).getTargetParameterIndex(argThat(describesArgument(0)));
        verify(left, atLeast(1)).getTargetParameterIndex(argThat(describesArgument(1)));
        verify(left, never()).getTargetParameterIndex(argThat(not(describesArgument(0, 1))));
        verify(right, atLeast(1)).getTargetParameterIndex(argThat(describesArgument(0)));
        verify(right, atLeast(1)).getTargetParameterIndex(argThat(describesArgument(1)));
        verify(right, never()).getTargetParameterIndex(argThat(not(describesArgument(0, 1))));
    }
}
