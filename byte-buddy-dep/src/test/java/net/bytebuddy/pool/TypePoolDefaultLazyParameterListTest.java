package net.bytebuddy.pool;

import net.bytebuddy.description.method.AbstractParameterListTest;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.method.ParameterList;
import org.junit.After;
import org.junit.Before;

import java.util.ArrayList;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.is;

public class TypePoolDefaultLazyParameterListTest extends AbstractParameterListTest<ParameterDescription.InDefinedShape, ParameterDescription> {

    private TypePool typePool;

    @Before
    public void setUp() throws Exception {
        typePool = TypePool.Default.ofSystemLoader();
    }

    @After
    public void tearDown() throws Exception {
        typePool.clear();
    }

    protected ParameterDescription getFirst() throws Exception {
        return new MethodDescription.ForLoadedMethod(Foo.class.getDeclaredMethod("foo", Void.class)).getParameters().getOnly();
    }

    protected ParameterDescription getSecond() throws Exception {
        return new MethodDescription.ForLoadedMethod(Foo.class.getDeclaredMethod("bar", Void.class)).getParameters().getOnly();
    }

    protected ParameterList<ParameterDescription.InDefinedShape> asList(List<ParameterDescription> elements) {
        List<ParameterDescription.InDefinedShape> parameterDescriptions = new ArrayList<ParameterDescription.InDefinedShape>(elements.size());
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

    protected ParameterDescription.InDefinedShape asElement(ParameterDescription element) {
        return element.asDefined();
    }
}
