package net.bytebuddy.agent.builder;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class ResettableClassFileTransformerResetTest {

    @Test
    public void testSimpleActive() throws Exception {
        assertThat(ResettableClassFileTransformer.Reset.Simple.ACTIVE.isApplied(), is(true));
        assertThat(ResettableClassFileTransformer.Reset.Simple.ACTIVE.getErrors().isEmpty(), is(true));
    }

    @Test
    public void testSimpleInactive() throws Exception {
        assertThat(ResettableClassFileTransformer.Reset.Simple.INACTIVE.isApplied(), is(false));
        assertThat(ResettableClassFileTransformer.Reset.Simple.INACTIVE.getErrors().isEmpty(), is(true));
    }

    @Test
    public void testWithErrors() throws Exception {
        Throwable throwable = new Throwable();
        assertThat(new ResettableClassFileTransformer.Reset.Simple.WithErrors(Collections.<Class<?>, Throwable>singletonMap(Object.class,
                throwable)).isApplied(), is(true));
        assertThat(new ResettableClassFileTransformer.Reset.Simple.WithErrors(Collections.<Class<?>, Throwable>singletonMap(Object.class,
                throwable)).getErrors().get(Object.class), sameInstance(throwable));
    }

    @Test
    public void testFactory() throws Exception {
        assertThat(ResettableClassFileTransformer.Reset.Simple.WithErrors.ofPotentiallyErroneous(Collections.<Class<?>, Throwable>singletonMap(Object.class,
                new Throwable())), instanceOf(ResettableClassFileTransformer.Reset.WithErrors.class));
        assertThat(ResettableClassFileTransformer.Reset.Simple.WithErrors.ofPotentiallyErroneous(Collections.<Class<?>, Throwable>emptyMap()),
                is((ResettableClassFileTransformer.Reset) ResettableClassFileTransformer.Reset.Simple.ACTIVE));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(ResettableClassFileTransformer.Reset.Simple.class).apply();
        ObjectPropertyAssertion.of(ResettableClassFileTransformer.Reset.WithErrors.class).apply();
    }
}
