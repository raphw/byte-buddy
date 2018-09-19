package net.bytebuddy.description.method;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.AccessibleObject;

public class ParameterDescriptionForLoadedParameterDispatcherTest {

    private static final int FOO = 42;

    private AccessibleObject accessibleObject;

    @Before
    public void setUp() throws Exception {
        accessibleObject = Foo.class.getDeclaredConstructor();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testLegacyVmGetName() throws Exception {
        ParameterDescription.ForLoadedParameter.Dispatcher.ForLegacyVm.INSTANCE.getName(accessibleObject, FOO);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testLegacyVmGetModifiers() throws Exception {
        ParameterDescription.ForLoadedParameter.Dispatcher.ForLegacyVm.INSTANCE.getModifiers(accessibleObject, FOO);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testLegacyVmIsNamePresent() throws Exception {
        ParameterDescription.ForLoadedParameter.Dispatcher.ForLegacyVm.INSTANCE.isNamePresent(accessibleObject, FOO);
    }

    private static class Foo {
        /* empty */
    }
}
