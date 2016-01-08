package net.bytebuddy.description.type;

import java.lang.reflect.Type;
import java.util.List;

public class TypeListGenericForLoadedTypesTest extends AbstractTypeListGenericTest<Type> {

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
        return new TypeList.Generic.ForLoadedTypes(elements);
    }

    @Override
    protected TypeDescription.Generic asElement(Type element) {
        return TypeDefinition.Sort.describe(element);
    }
}
