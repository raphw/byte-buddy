package net.bytebuddy.agent.builder;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class AgentBuilderCircularityLockTest {

    @Test
    public void testCircularityLockDefault() throws Exception {
        AgentBuilder.CircularityLock.Default circularityLock = new AgentBuilder.CircularityLock.Default();
        assertThat(circularityLock.acquire(), is(true));
        assertThat(circularityLock.acquire(), is(false));
        circularityLock.release();
        assertThat(circularityLock.acquire(), is(true));
        assertThat(circularityLock.acquire(), is(false));
        circularityLock.release();
        assertThat(circularityLock.get(), nullValue(Boolean.class));
    }

    @Test
    public void testCircularityLockInactive() throws Exception {
        AgentBuilder.CircularityLock circularityLock = AgentBuilder.CircularityLock.Inactive.INSTANCE;
        assertThat(circularityLock.acquire(), is(true));
        assertThat(circularityLock.acquire(), is(true));
        circularityLock.release();
    }

    @Test
    public void testGlobalLock() throws Exception {
        AgentBuilder.CircularityLock circularityLock = new AgentBuilder.CircularityLock.Global();
        assertThat(circularityLock.acquire(), is(true));
        assertThat(circularityLock.acquire(), is(true));
        circularityLock.release();
    }

    @Test
    public void testGlobalLockWithTimeout() throws Exception {
        AgentBuilder.CircularityLock circularityLock = new AgentBuilder.CircularityLock.Global(10, TimeUnit.MILLISECONDS);
        assertThat(circularityLock.acquire(), is(true));
        assertThat(circularityLock.acquire(), is(true));
        circularityLock.release();
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(AgentBuilder.CircularityLock.Inactive.class).apply();
    }
}
