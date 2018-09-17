package net.bytebuddy.description.type;

import java.lang.reflect.Type;
import java.util.List;

public class TypeListGenericForLoadedTypesTest extends AbstractTypeListGenericTest<Type> {

    protected Type getFirst() throws Exception {
        return Holder.class.getGenericInterfaces()[0];
    }

    protected Type getSecond() throws Exception {
        return Holder.class.getGenericInterfaces()[1];
    }

    protected TypeList.Generic asList(List<Type> elements) {
        return new TypeList.Generic.ForLoadedTypes(elements);
    }

    protected TypeDescription.Generic asElement(Type element) {
        return TypeDefinition.Sort.describe(element);
    }
}
