package net.bytebuddy.instrumentation.type;

public class TypeDescriptionForLoadedTypeTest extends AbstractTypeDescriptionTest {

    @Override
    protected TypeDescription describe(Class<?> type) {
        return new TypeDescription.ForLoadedType(type);
    }
}
