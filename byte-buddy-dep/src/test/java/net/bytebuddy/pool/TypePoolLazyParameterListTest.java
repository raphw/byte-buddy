package net.bytebuddy.pool;

import net.bytebuddy.description.method.AbstractParameterListTest;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.method.ParameterList;
import org.junit.After;
import org.junit.Before;

import java.util.LinkedList;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.is;

public class TypePoolLazyParameterListTest extends AbstractParameterListTest<ParameterDescription.InDefinedShape, ParameterDescription> {

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
    protected ParameterDescription getFirst() throws Exception {
        return new MethodDescription.ForLoadedMethod(Foo.class.getDeclaredMethod("foo", Void.class)).getParameters().getOnly();
    }

    @Override
    protected ParameterDescription getSecond() throws Exception {
        return new MethodDescription.ForLoadedMethod(Foo.class.getDeclaredMethod("bar", Void.class)).getParameters().getOnly();
    }

    @Override
    protected ParameterList<ParameterDescription.InDefinedShape> asList(List<ParameterDescription> elements) {
        List<ParameterDescription.InDefinedShape> parameterDescriptions = new LinkedList<ParameterDescription.InDefinedShape>();
        for (ParameterDescription element : elements) {
            parameterDescriptions.add(typePool.describe(Foo.class.getName()).resolve()
                    .getDeclaredMethods()
                    .filter(is(element.getDeclaringMethod()))
                    .getOnly()
                    .getParameters()
                    .getOnly());
        }
        return new ParameterList.Explicit<ParameterDescription.InDefinedShape>(parameterDescriptions);
    }

    @Override
    protected ParameterDescription.InDefinedShape asElement(ParameterDescription element) {
        return element.asDefined();
    }
}
