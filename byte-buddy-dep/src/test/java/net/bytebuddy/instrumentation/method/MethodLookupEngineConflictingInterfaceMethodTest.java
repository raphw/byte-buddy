package net.bytebuddy.instrumentation.method;

import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.lang.reflect.Field;
import java.util.List;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MethodLookupEngineConflictingInterfaceMethodTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeDescription typeDescription, firstType, secondType, thirdType;
    @Mock
    private MethodDescription first, second, third;

    private Field knownMethods;

    @Before
    public void setUp() throws Exception {
        when(first.getDeclaringType()).thenReturn(firstType);
        when(second.getDeclaringType()).thenReturn(secondType);
        when(third.getDeclaringType()).thenReturn(thirdType);
        knownMethods = MethodLookupEngine.ConflictingInterfaceMethod.class.getDeclaredField("methodDescriptions");
        knownMethods.setAccessible(true);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testResolutionCompatibleMethods() throws Exception {
        when(firstType.isAssignableFrom(thirdType)).thenReturn(true);
        MethodDescription firstIteration = MethodLookupEngine.ConflictingInterfaceMethod.of(typeDescription, first, second);
        MethodDescription secondIteration = MethodLookupEngine.ConflictingInterfaceMethod.of(typeDescription, firstIteration, third);
        List<MethodDescription> methodDescriptions = (List<MethodDescription>) knownMethods.get(secondIteration);
        assertThat(methodDescriptions.size(), is(2));
        assertThat(methodDescriptions, hasItems(second, third));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testResolutionNonCompatibleMethods() throws Exception {
        MethodDescription firstIteration = MethodLookupEngine.ConflictingInterfaceMethod.of(typeDescription, first, second);
        MethodDescription secondIteration = MethodLookupEngine.ConflictingInterfaceMethod.of(typeDescription, firstIteration, third);
        List<MethodDescription> methodDescriptions = (List<MethodDescription>) knownMethods.get(secondIteration);
        assertThat(methodDescriptions.size(), is(3));
        assertThat(methodDescriptions, hasItems(first, second, third));
    }

    @Test
    public void testIsSpecializable() throws Exception {
        MethodDescription methodDescription = MethodLookupEngine.ConflictingInterfaceMethod.of(typeDescription, first, second);
        when(first.isAbstract()).thenReturn(true);
        when(firstType.isAssignableFrom(typeDescription)).thenReturn(true);
        when(secondType.isAssignableFrom(typeDescription)).thenReturn(true);
        assertThat(methodDescription.isSpecializableFor(typeDescription), is(true));
        verify(first).isAbstract();
        verify(second).isAbstract();
        verify(secondType).isAssignableFrom(typeDescription);
    }
}
