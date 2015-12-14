package net.bytebuddy.utility;

import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.lang.reflect.AccessibleObject;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class AccessActionTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private AccessibleObject accessibleObject;

    @Test
    public void testAccessAction() throws Exception {
        assertThat(AccessAction.of(accessibleObject).run(), is(accessibleObject));
        verify(accessibleObject).setAccessible(true);
        verifyNoMoreInteractions(accessibleObject);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(AccessAction.class).apply();
    }
}
