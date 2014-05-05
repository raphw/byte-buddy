package net.bytebuddy;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.modifier.TypeManifestation;
import net.bytebuddy.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.io.Serializable;

import static net.bytebuddy.instrumentation.method.matcher.MethodMatchers.any;
import static net.bytebuddy.instrumentation.method.matcher.MethodMatchers.none;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class ByteBuddyHashCodeEqualsTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private Instrumentation instrumentation;

    @Test
    public void testHashCodeEquals() throws Exception {
        assertThat(new ByteBuddy().hashCode(), is(new ByteBuddy().hashCode()));
        assertThat(new ByteBuddy(), equalTo(new ByteBuddy()));
        assertThat(new ByteBuddy().withModifiers(TypeManifestation.FINAL).hashCode(), is(new ByteBuddy().withModifiers(TypeManifestation.FINAL).hashCode()));
        assertThat(new ByteBuddy().withModifiers(TypeManifestation.FINAL), equalTo(new ByteBuddy().withModifiers(TypeManifestation.FINAL)));
        assertThat(new ByteBuddy().hashCode(), not(is(new ByteBuddy().withImplementing(Serializable.class).hashCode())));
        assertThat(new ByteBuddy(), not(equalTo((ByteBuddy) new ByteBuddy().withImplementing(Serializable.class))));
        assertThat(new ByteBuddy().withImplementing(Serializable.class).hashCode(), not(is(new ByteBuddy().hashCode())));
        assertThat(new ByteBuddy().withImplementing(Serializable.class), not(equalTo(new ByteBuddy())));
    }

    @Test
    public void testMatchedMethodInterceptionTargetHashCodeEquals() throws Exception {
        assertThat(new ByteBuddy().method(any()).hashCode(), is(new ByteBuddy().method(any()).hashCode()));
        assertThat(new ByteBuddy().method(any()), equalTo(new ByteBuddy().method(any())));
        assertThat(new ByteBuddy().method(any()).hashCode(), not(is(new ByteBuddy().method(none()).hashCode())));
        assertThat(new ByteBuddy().method(any()), not(equalTo(new ByteBuddy().method(none()))));
    }

    @Test
    public void testMethodInvocationTargetHashCodeEquals() throws Exception {
        assertThat(new ByteBuddy().method(any()).withoutCode().hashCode(), is(new ByteBuddy().method(any()).withoutCode().hashCode()));
        assertThat(new ByteBuddy().method(any()).withoutCode(), equalTo(new ByteBuddy().method(any()).withoutCode()));
        assertThat(new ByteBuddy().method(any()).withoutCode().hashCode(), not(is(new ByteBuddy().method(none()).intercept(instrumentation).hashCode())));
        assertThat(new ByteBuddy().method(any()).withoutCode(), not(equalTo(new ByteBuddy().method(none()).intercept(instrumentation))));
    }
}
