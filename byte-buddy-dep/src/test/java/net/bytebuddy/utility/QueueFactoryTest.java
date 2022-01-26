package net.bytebuddy.utility;

import org.junit.Test;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Queue;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class QueueFactoryTest {

    @Test
    public void testMakeQueueWithoutElements() {
        Queue<Object> queue = QueueFactory.make();
        assertThat(queue.size(), is(0));
        assertThat(queue, instanceOf(ArrayDeque.class));
    }

    @Test
    public void testMakeQueueWithElements() {
        Object element = new Object();
        Queue<Object> queue = QueueFactory.make(Collections.singleton(element));
        assertThat(queue.size(), is(1));
        assertThat(queue.iterator().next(), is(element));
        assertThat(queue, instanceOf(ArrayDeque.class));
    }
}
