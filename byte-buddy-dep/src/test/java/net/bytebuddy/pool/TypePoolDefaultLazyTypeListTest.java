package net.bytebuddy.pool;

import net.bytebuddy.description.type.AbstractTypeListTest;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import org.junit.After;
import org.junit.Before;

import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.anyOf;

public class TypePoolDefaultLazyTypeListTest extends AbstractTypeListTest<Class<?>> {

    private TypePool typePool;

    @Before
    public void setUp() throws Exception {
        typePool = TypePool.Default.ofSystemLoader();
    }

    @After
    public void tearDown() throws Exception {
        typePool.clear();
    }

    protected Class<?> getFirst() throws Exception {
        return Foo.class;
    }

    protected Class<?> getSecond() throws Exception {
        return Bar.class;
    }

    protected TypeList asList(List<Class<?>> elements) {
        return typePool.describe(Holder.class.getName()).resolve().getInterfaces().asErasures().filter(anyOf(elements.toArray(new Class<?>[elements.size()])));
    }

    protected TypeDescription asElement(Class<?> element) {
        return TypeDescription.ForLoadedType.of(element);
    }
}
