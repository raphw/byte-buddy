package net.bytebuddy.description.type;

import java.util.List;

public class TypeListGenericExplicitTest extends AbstractTypeListGenericTest<TypeDescription.Generic> {

    protected TypeDescription.Generic getFirst() throws Exception {
        return TypeDefinition.Sort.describe(Holder.class.getGenericInterfaces()[0]);
    }

    protected TypeDescription.Generic getSecond() throws Exception {
        return TypeDefinition.Sort.describe(Holder.class.getGenericInterfaces()[1]);
    }

    protected TypeList.Generic asList(List<TypeDescription.Generic> elements) {
        return new TypeList.Generic.Explicit(elements);
    }

    protected TypeDescription.Generic asElement(TypeDescription.Generic element) {
        return element;
    }
}
