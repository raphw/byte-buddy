package net.bytebuddy.description.type;

import java.util.List;

public class TypeListForLoadedTest extends AbstractTypeListTest<Class<?>> {

    protected Class<?> getFirst() throws Exception {
        return Foo.class;
    }

    protected Class<?> getSecond() throws Exception {
        return Bar.class;
    }

    protected TypeList asList(List<Class<?>> elements) {
        return new TypeList.ForLoadedTypes(elements);
    }

    protected TypeDescription asElement(Class<?> element) {
        return TypeDescription.ForLoadedType.of(element);
    }
}
