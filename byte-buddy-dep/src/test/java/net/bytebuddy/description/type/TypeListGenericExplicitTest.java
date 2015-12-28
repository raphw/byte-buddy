package net.bytebuddy.description.type;

import java.util.List;

public class TypeListGenericExplicitTest extends AbstractTypeListGenericTest<TypeDescription.Generic> {

    @Override
    protected TypeDescription.Generic getFirst() throws Exception {
        return TypeDefinition.Sort.describe(Holder.class.getGenericInterfaces()[0]);
    }

    @Override
    protected TypeDescription.Generic getSecond() throws Exception {
        return TypeDefinition.Sort.describe(Holder.class.getGenericInterfaces()[1]);
    }

    @Override
    protected TypeList.Generic asList(List<TypeDescription.Generic> elements) {
        return new TypeList.Generic.Explicit(elements);
    }

    @Override
    protected TypeDescription.Generic asElement(TypeDescription.Generic element) {
        return element;
    }
}
