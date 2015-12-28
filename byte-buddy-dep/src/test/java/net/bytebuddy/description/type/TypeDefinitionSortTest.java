package net.bytebuddy.description.type;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Parameterized.class)
public class TypeDefinitionSortTest {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {TypeDefinition.Sort.NON_GENERIC, true, false, false, false, false},
                {TypeDefinition.Sort.PARAMETERIZED, false, true, false, false, false},
                {TypeDefinition.Sort.VARIABLE, false, false, true, false, false},
                {TypeDefinition.Sort.VARIABLE_SYMBOLIC, false, false, true, false, false},
                {TypeDefinition.Sort.GENERIC_ARRAY, false, false, false, true, false},
                {TypeDefinition.Sort.WILDCARD, false, false, false, false, true}
        });
    }

    private final TypeDefinition.Sort sort;

    private final boolean nonGeneric, parameterized, typeVariable, genericArray, wildcard;

    public TypeDefinitionSortTest(TypeDefinition.Sort sort,
                                  boolean nonGeneric,
                                  boolean parameterized,
                                  boolean typeVariable,
                                  boolean genericArray,
                                  boolean wildcard) {
        this.sort = sort;
        this.nonGeneric = nonGeneric;
        this.parameterized = parameterized;
        this.typeVariable = typeVariable;
        this.genericArray = genericArray;
        this.wildcard = wildcard;
    }

    @Test
    public void testNonGeneric() throws Exception {
        assertThat(sort.isNonGeneric(), is(nonGeneric));
    }

    @Test
    public void testParameterized() throws Exception {
        assertThat(sort.isParameterized(), is(parameterized));
    }

    @Test
    public void testTypeVariable() throws Exception {
        assertThat(sort.isTypeVariable(), is(typeVariable));
    }

    @Test
    public void testGenericArray() throws Exception {
        assertThat(sort.isGenericArray(), is(genericArray));
    }

    @Test
    public void testWildcard() throws Exception {
        assertThat(sort.isWildcard(), is(wildcard));
    }
}
