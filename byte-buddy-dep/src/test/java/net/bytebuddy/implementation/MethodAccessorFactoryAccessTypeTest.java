package net.bytebuddy.implementation;

import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MethodAccessorFactoryAccessTypeTest {

    @Test
    public void testVisibility() throws Exception {
        assertThat(MethodAccessorFactory.AccessType.DEFAULT.getVisibility(), is(Visibility.PACKAGE_PRIVATE));
        assertThat(MethodAccessorFactory.AccessType.PUBLIC.getVisibility(), is(Visibility.PUBLIC));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(MethodAccessorFactory.AccessType.class).apply();
    }
}
