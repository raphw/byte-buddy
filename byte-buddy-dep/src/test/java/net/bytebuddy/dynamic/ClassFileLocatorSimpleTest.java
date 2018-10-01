package net.bytebuddy.dynamic;

import net.bytebuddy.description.type.TypeDescription;
import org.junit.Test;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ClassFileLocatorSimpleTest {

    private static final String FOO = "foo", BAR = "bar";

    private static final byte[] QUX = new byte[]{1, 2, 3};

    @Test
    public void testSuccessfulLocation() throws Exception {
        ClassFileLocator.Resolution resolution = ClassFileLocator.Simple.of(FOO, QUX).locate(FOO);
        assertThat(resolution.isResolved(), is(true));
        assertThat(resolution.resolve(), is(QUX));
    }

    @Test
    public void testInSuccessfulLocation() throws Exception {
        ClassFileLocator.Resolution resolution = ClassFileLocator.Simple.of(FOO, QUX).locate(BAR);
        assertThat(resolution.isResolved(), is(false));
    }

    @Test
    public void testClose() throws Exception {
        ClassFileLocator.Simple.of(FOO, QUX).close();
    }

    @Test
    public void testDynamicType() throws Exception {
        DynamicType dynamicType = mock(DynamicType.class);
        TypeDescription typeDescription = mock(TypeDescription.class);
        when(typeDescription.getName()).thenReturn(FOO);
        when(dynamicType.getAllTypes()).thenReturn(Collections.singletonMap(typeDescription, QUX));
        ClassFileLocator classFileLocator = ClassFileLocator.Simple.of(dynamicType);
        assertThat(classFileLocator.locate(FOO).isResolved(), is(true));
        assertThat(classFileLocator.locate(FOO).resolve(), is(QUX));
        assertThat(classFileLocator.locate(BAR).isResolved(), is(false));
    }

    @Test
    public void testOfResources() throws Exception{
        ClassFileLocator.Resolution resolution = ClassFileLocator.Simple
                .ofResources(Collections.singletonMap(FOO + "/" + BAR + ".class", QUX))
                .locate(FOO+ "." + BAR);
        assertThat(resolution.isResolved(), is(true));
        assertThat(resolution.resolve(), is(QUX));
    }

    @Test
    public void testOfResourcesNoClassFile() throws Exception{
        ClassFileLocator.Resolution resolution = ClassFileLocator.Simple
                .ofResources(Collections.singletonMap(FOO + "/" + BAR, QUX))
                .locate(FOO+ "." + BAR);
        assertThat(resolution.isResolved(), is(false));
    }
}
