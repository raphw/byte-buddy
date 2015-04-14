package net.bytebuddy.pool;

import net.bytebuddy.description.type.TypeDescription;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class TypePoolGenericTypeTest {

    private TypePool typePool;

    @Before
    public void setUp() throws Exception {
        typePool = TypePool.Default.ofClassPath();
    }

    @After
    public void tearDown() throws Exception {
        typePool.clear();
    }

    @Test
    public void testGenericTypesAreRetained() throws Exception {
        TypeDescription typeDescription = typePool.describe(Foo.class.getName()).resolve();
        assertThat(typeDescription.getGenericSignature(), is("<T:Ljava/lang/Object;>Ljava/lang/Object;"));
        assertThat(typeDescription.getDeclaredFields().getOnly().getGenericSignature(), is("Ljava/util/List<Ljava/lang/String;>;"));
        assertThat(typeDescription.getDeclaredMethods().filter(isMethod()).getOnly().getGenericSignature(),
                is("<S:Ljava/lang/Object;>(Ljava/util/List<TS;>;)TS;"));
    }

    private static class Foo<T> {

        List<String> field;

        <S> S method(List<S> arg) {
            return null;
        }
    }
}
