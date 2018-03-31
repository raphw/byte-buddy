package net.bytebuddy.implementation.bytecode;

import org.junit.Test;

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
        assertThat(mergedLeft.getOperandStackSize(), is(BIGGER));
        assertThat(mergedLeft.getLocalVariableSize(), is(BIGGER));
        assertThat(mergedRight.getOperandStackSize(), is(BIGGER));
        assertThat(mergedRight.getLocalVariableSize(), is(BIGGER));
    }
}
