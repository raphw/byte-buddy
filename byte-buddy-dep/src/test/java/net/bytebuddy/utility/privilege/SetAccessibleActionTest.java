package net.bytebuddy.utility.privilege;

import org.junit.Test;

import java.lang.reflect.AccessibleObject;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class SetAccessibleActionTest {

    private static final String BAR = "bar";

    @Test
    public void testAccessAction() throws Exception {
        AccessibleObjectSpy accessibleObjectSpy = new AccessibleObjectSpy(Foo.class.getDeclaredField(BAR));
        assertThat(new SetAccessibleAction<AccessibleObjectSpy>(accessibleObjectSpy).run(), is(accessibleObjectSpy));
        assertThat(accessibleObjectSpy.accessible, is(true));
    }

    @SuppressWarnings("unused")
    private static class Foo {

        Object bar, qux;
    }

    private static class AccessibleObjectSpy extends AccessibleObject {

        private final AccessibleObject accessibleObject;

        public boolean accessible;

        public AccessibleObjectSpy(AccessibleObject accessibleObject) {
            this.accessibleObject = accessibleObject;
        }

        @Override
        public void setAccessible(boolean flag) {
            accessible = flag;
            accessibleObject.setAccessible(flag);
        }
    }
}
