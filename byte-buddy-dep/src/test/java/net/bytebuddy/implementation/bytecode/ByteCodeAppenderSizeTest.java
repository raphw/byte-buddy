package net.bytebuddy.implementation.bytecode;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ByteCodeAppenderSizeTest {

    private static final int LOWER = 3, BIGGER = 5;

    @Test
    public void testMerge() throws Exception {
        ByteCodeAppender.Size left = new ByteCodeAppender.Size(LOWER, BIGGER);
        ByteCodeAppender.Size right = new ByteCodeAppender.Size(BIGGER, LOWER);
        ByteCodeAppender.Size mergedLeft = left.merge(right);
        ByteCodeAppender.Size mergedRight = right.merge(left);
        assertThat(mergedLeft, equalTo(mergedRight));
        assertThat(mergedLeft.getOperandStackSize(), is(BIGGER));
        assertThat(mergedLeft.getLocalVariableSize(), is(BIGGER));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(ByteCodeAppender.Size.class).apply();
    }
}
