package net.bytebuddy.asm;

import net.bytebuddy.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class ClassVisitorWrapperChainHashCodeEqualsTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private ClassVisitorWrapper classVisitorWrapper;

    @Test
    public void testHashCodeEquals() throws Exception {
        assertThat(new ClassVisitorWrapper.Chain().hashCode(), is(new ClassVisitorWrapper.Chain().hashCode()));
        assertThat(new ClassVisitorWrapper.Chain(), equalTo(new ClassVisitorWrapper.Chain()));
        assertThat(new ClassVisitorWrapper.Chain().hashCode(), not(is(new ClassVisitorWrapper.Chain().append(classVisitorWrapper).hashCode())));
        assertThat(new ClassVisitorWrapper.Chain(), not(equalTo(new ClassVisitorWrapper.Chain().append(classVisitorWrapper))));
    }
}
