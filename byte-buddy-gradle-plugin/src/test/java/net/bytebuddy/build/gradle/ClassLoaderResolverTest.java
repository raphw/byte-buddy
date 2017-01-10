package net.bytebuddy.build.gradle;

import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.io.File;
import java.net.URI;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class ClassLoaderResolverTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private File file;

    @Before
    public void setUp() throws Exception {
        when(file.toURI()).thenReturn(new URI("file://foo"));
    }

    @Test
    public void testResolution() throws Exception {
        ClassLoaderResolver classLoaderResolver = new ClassLoaderResolver();
        assertThat(classLoaderResolver.resolve(Collections.singleton(file)), sameInstance(classLoaderResolver.resolve(Collections.singleton(file))));
    }

    @Test
    public void testClose() throws Exception {
        ClassLoaderResolver classLoaderResolver = new ClassLoaderResolver();
        classLoaderResolver.resolve(Collections.singleton(file));
        classLoaderResolver.close();
    }
}
