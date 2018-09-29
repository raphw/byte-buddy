package net.bytebuddy.pool;

import net.bytebuddy.description.type.TypeDescription;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

public class TypePoolDefaultCacheTest {

    @Test
    public void testCache() throws Exception {
        TypePool typePool = TypePool.Default.ofSystemLoader();
        TypeDescription typeDescription = typePool.describe(Void.class.getName()).resolve();
        assertThat(typePool.describe(Void.class.getName()).resolve(), sameInstance(typeDescription));
        typePool.clear();
        assertThat(typePool.describe(Void.class.getName()).resolve(), not(sameInstance(typeDescription)));
    }
}
