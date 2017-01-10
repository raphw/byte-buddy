package net.bytebuddy.description.type;

import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.util.Iterator;
import java.util.NoSuchElementException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class TypeDefinitionSuperClassIteratorTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeDescription typeDescription;

    @Mock
    private TypeDescription.Generic superClass;

    @Before
    public void setUp() throws Exception {
        when(typeDescription.getSuperClass()).thenReturn(superClass);
    }

    @Test
    public void testHasNext() throws Exception {
        Iterator<TypeDefinition> iterator = new TypeDefinition.SuperClassIterator(typeDescription);
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is((TypeDefinition) typeDescription));
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is((TypeDefinition) superClass));
        assertThat(iterator.hasNext(), is(false));
    }

    @Test(expected = NoSuchElementException.class)
    public void testHasNotNext() throws Exception {
        Iterator<TypeDefinition> iterator = new TypeDefinition.SuperClassIterator(typeDescription);
        assertThat(iterator.next(), is((TypeDefinition) typeDescription));
        assertThat(iterator.next(), is((TypeDefinition) superClass));
        iterator.next();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testNoRemoval() throws Exception {
        new TypeDefinition.SuperClassIterator(typeDescription).remove();
    }
}
