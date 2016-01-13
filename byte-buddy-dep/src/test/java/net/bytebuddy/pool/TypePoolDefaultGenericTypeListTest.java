package net.bytebuddy.pool;

import net.bytebuddy.description.type.AbstractTypeListGenericTest;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import org.junit.After;
import org.junit.Before;

import java.lang.reflect.Type;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.anyOf;

public class TypePoolDefaultGenericTypeListTest extends AbstractTypeListGenericTest<Type> {

    private TypePool typePool;

    @Before
    public void setUp() throws Exception {
        typePool = TypePool.Default.ofClassPath();
    }

    @After
    public void tearDown() throws Exception {
        typePool.clear();
    }

    @Override
    protected Type getFirst() throws Exception {
        return Holder.class.getGenericInterfaces()[0];
    }

    @Override
    protected Type getSecond() throws Exception {
        return Holder.class.getGenericInterfaces()[1];
    }

    @Override
    protected TypeList.Generic asList(List<Type> elements) {
        return typePool.describe(Holder.class.getName()).resolve().getInterfaces().filter(anyOf(elements.toArray(new Type[elements.size()])));
    }

    @Override
    protected TypeDescription.Generic asElement(Type element) {
        return TypeDefinition.Sort.describe(element);
    }
}
