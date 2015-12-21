package net.bytebuddy.description.type;

import java.util.List;

public class TypeListForLoadedTest extends AbstractTypeListTest<Class<?>> {

    @Override
    protected Class<?> getFirst() throws Exception {
        return Foo.class;
    }

    @Override
    protected Class<?> getSecond() throws Exception {
        return Bar.class;
    }

    @Override
    protected TypeList asList(List<Class<?>> elements) {
        return new TypeList.ForLoadedTypes(elements);
    }

    @Override
    protected TypeDescription asElement(Class<?> element) {
        return new TypeDescription.ForLoadedType(element);
    }
}
