package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.instrumentation.AbstractInstrumentationTargetTest;
import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.MethodList;
import net.bytebuddy.instrumentation.method.MethodLookupEngine;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackSize;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Arrays;

import static net.bytebuddy.instrumentation.method.matcher.MethodMatchers.isOverridable;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class RedefineInstrumentationTargetTest extends AbstractInstrumentationTargetTest {

    private static final String BAR = "bar", BAZ = "baz", FOOBAR = "foobar", QUX = "qux";

    @Mock
    private MethodDescription superMethod, superMethodConstructor, instrumentedTypeMethod;
    @Mock
    private TypeDescription superType;
    @Mock
    private TypeList parameterTypes;
    @Mock
    private MethodLookupEngine methodLookupEngine;
    @Mock
    private MethodLookupEngine.Finding superTypeFinding;
    @Mock
    private MethodList superMethodList;

    @Override
    @Before
    public void setUp() throws Exception {
        when(instrumentedType.getSupertype()).thenReturn(superType);
        when(superType.getDeclaredMethods()).thenReturn(new MethodList.Explicit(Arrays.asList(superMethodConstructor)));
        when(superType.getInternalName()).thenReturn(BAR);
        when(superMethod.getDeclaringType()).thenReturn(superType);
        when(superType.getStackSize()).thenReturn(StackSize.ZERO);
        when(superMethod.getReturnType()).thenReturn(returnType);
        when(superMethod.getInternalName()).thenReturn(BAZ);
        when(superMethod.getDescriptor()).thenReturn(FOOBAR);
        when(superMethod.getParameterTypes()).thenReturn(parameterTypes);
        when(superMethodConstructor.isConstructor()).thenReturn(true);
        when(superMethodConstructor.getParameterTypes()).thenReturn(parameterTypes);
        when(superMethodConstructor.getReturnType()).thenReturn(returnType);
        when(superMethodConstructor.isSpecializableFor(superType)).thenReturn(true);
        when(superMethodConstructor.getInternalName()).thenReturn(QUXBAZ);
        when(superMethodConstructor.getDescriptor()).thenReturn(BAZBAR);
        when(methodLookupEngine.process(superType)).thenReturn(superTypeFinding);
        when(superTypeFinding.getInvokableMethods()).thenReturn(superMethodList);
        MethodList superMethodListFiltered = new MethodList.Explicit(Arrays.asList(superMethod));
        when(superMethodList.filter(isOverridable())).thenReturn(superMethodListFiltered);
        when(instrumentedTypeMethod.getDeclaringType()).thenReturn(instrumentedType);
        super.setUp();
    }

    @Override
    protected Instrumentation.Target makeInstrumentationTarget() {
        return new RedefineInstrumentationTarget(finding, bridgeMethodResolverFactory, methodLookupEngine);
    }

    @Test
    public void testSuperMethodRedefinedOnInstrumentedType() throws Exception {
        when(instrumentedTypeMethod.getInternalName()).thenReturn(BAZ);
        when(instrumentedTypeMethod.getDescriptor()).thenReturn(FOOBAR);
        when(superMethod.isSpecializableFor(superType)).thenReturn(true);
        Instrumentation.SpecialMethodInvocation specialMethodInvocation = makeInstrumentationTarget()
                .invokeSuper(instrumentedTypeMethod, Instrumentation.Target.MethodLookup.Default.EXACT);
        assertThat(specialMethodInvocation.isValid(), is(true));
    }

    @Test
    public void testSuperMethodDefinedExclusivelyOnInstrumentedType() throws Exception {
        Instrumentation.SpecialMethodInvocation specialMethodInvocation = makeInstrumentationTarget()
                .invokeSuper(instrumentedTypeMethod, Instrumentation.Target.MethodLookup.Default.EXACT);
        assertThat(specialMethodInvocation.isValid(), is(false));
    }
}
