package com.blogspot.mydailyjava.bytebuddy.method.bytecode.bind.annotation;

import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assignment;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IdentifiedBindingTest {

    private Assignment assignment;

    @Before
    public void setUp() throws Exception {
        assignment = mock(Assignment.class);
        when(assignment.isValid()).thenReturn(true);
    }

    @Test
    public void testIllegal() throws Exception {
        AnnotationDrivenBinder.ArgumentBinder.IdentifiedBinding<?> identifiedBinding =
                AnnotationDrivenBinder.ArgumentBinder.IdentifiedBinding.makeIllegal();
        assertThat(identifiedBinding.getIdentificationToken(), notNullValue());
        assertThat(identifiedBinding.isValid(), is(false));
        assertThat(identifiedBinding.getAssignment().isValid(), is(false));
    }

    @Test
    public void testAnonymous() throws Exception {
        AnnotationDrivenBinder.ArgumentBinder.IdentifiedBinding<?> identifiedBinding =
                AnnotationDrivenBinder.ArgumentBinder.IdentifiedBinding.makeAnonymous(assignment);
        assertThat(identifiedBinding.getAssignment(), is(assignment));
        assertThat(identifiedBinding.getIdentificationToken(), notNullValue());
        assertThat(identifiedBinding.isValid(), is(true));
    }

    @Test
    public void testIdentified() throws Exception {
        Object token = new Object();
        AnnotationDrivenBinder.ArgumentBinder.IdentifiedBinding<?> identifiedBinding =
                AnnotationDrivenBinder.ArgumentBinder.IdentifiedBinding.makeIdentified(assignment, token);
        assertThat(identifiedBinding.getAssignment(), is(assignment));
        assertThat(identifiedBinding.getIdentificationToken(), is(token));
        assertThat(identifiedBinding.isValid(), is(true));
    }
}
