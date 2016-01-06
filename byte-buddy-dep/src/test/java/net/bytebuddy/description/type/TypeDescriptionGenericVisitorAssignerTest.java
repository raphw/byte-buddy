package net.bytebuddy.description.type;

import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Test;

import java.io.Serializable;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class TypeDescriptionGenericVisitorAssignerTest {

    private TypeDescription.Generic collectionWildcard, collectionRaw;

    private TypeDescription.Generic collectionTypeVariableT, collectionTypeVariableS, collectionTypeVariableU;

    private TypeDescription.Generic collectionUpperBoundTypeVariableT, collectionUpperBoundTypeVariableS, collectionUpperBoundTypeVariableU;

    private TypeDescription.Generic collectionLowerBoundTypeVariableT, collectionLowerBoundTypeVariableS, collectionLowerBoundTypeVariableU;

    private TypeDescription.Generic listRaw, listWildcard;

    private TypeDescription.Generic abstractListRaw, arrayListRaw, arrayListWildcard;

    private TypeDescription.Generic callableWildcard;

    private TypeDescription.Generic arrayListTypeVariableT, arrayListTypeVariableS, arrayListTypeVariableU, arrayListTypeVariableV;

    private TypeDescription.Generic collectionRawArray, listRawArray, listWildcardArray, arrayListRawArray;

    private TypeDescription.Generic stringArray, objectArray, objectNestedArray;

    private TypeDescription.Generic unboundWildcard;

    private TypeDescription.Generic typeVariableT, typeVariableS, typeVariableU, typeVariableV;

    private TypeDescription.Generic tArray, sArray, uArray;

    private TypeDescription.Generic tNestedArray;

    @Before
    public void setUp() throws Exception {
        FieldList<?> fields = new TypeDescription.ForLoadedType(GenericTypes.class).getDeclaredFields();
        collectionRaw = fields.filter(named("collectionRaw")).getOnly().getType();
        collectionWildcard = fields.filter(named("collectionWildcard")).getOnly().getType();
        collectionTypeVariableT = fields.filter(named("collectionTypeVariableT")).getOnly().getType();
        collectionTypeVariableS = fields.filter(named("collectionTypeVariableS")).getOnly().getType();
        collectionTypeVariableU = fields.filter(named("collectionTypeVariableU")).getOnly().getType();
        collectionUpperBoundTypeVariableT = fields.filter(named("collectionUpperBoundTypeVariableT")).getOnly().getType();
        collectionUpperBoundTypeVariableS = fields.filter(named("collectionUpperBoundTypeVariableS")).getOnly().getType();
        collectionUpperBoundTypeVariableU = fields.filter(named("collectionUpperBoundTypeVariableU")).getOnly().getType();
        collectionLowerBoundTypeVariableT = fields.filter(named("collectionLowerBoundTypeVariableT")).getOnly().getType();
        collectionLowerBoundTypeVariableS = fields.filter(named("collectionLowerBoundTypeVariableS")).getOnly().getType();
        collectionLowerBoundTypeVariableU = fields.filter(named("collectionLowerBoundTypeVariableU")).getOnly().getType();
        listRaw = fields.filter(named("listRaw")).getOnly().getType();
        listWildcard = fields.filter(named("listWildcard")).getOnly().getType();
        arrayListTypeVariableT = fields.filter(named("arrayListTypeVariableT")).getOnly().getType();
        arrayListTypeVariableS = fields.filter(named("arrayListTypeVariableS")).getOnly().getType();
        arrayListTypeVariableU = fields.filter(named("arrayListTypeVariableU")).getOnly().getType();
        arrayListTypeVariableV = fields.filter(named("arrayListTypeVariableV")).getOnly().getType();
        abstractListRaw = fields.filter(named("abstractListRaw")).getOnly().getType();
        callableWildcard = fields.filter(named("callableWildcard")).getOnly().getType();
        arrayListRaw = fields.filter(named("arrayListRaw")).getOnly().getType();
        arrayListWildcard = fields.filter(named("arrayListWildcard")).getOnly().getType();
        collectionRawArray = fields.filter(named("collectionRawArray")).getOnly().getType();
        listRawArray = fields.filter(named("listRawArray")).getOnly().getType();
        listWildcardArray = fields.filter(named("listWildcardArray")).getOnly().getType();
        arrayListRawArray = fields.filter(named("arrayListRawArray")).getOnly().getType();
        stringArray = new TypeDescription.Generic.OfNonGenericType.ForLoadedType(String[].class);
        objectArray = new TypeDescription.Generic.OfNonGenericType.ForLoadedType(Object[].class);
        objectNestedArray = new TypeDescription.Generic.OfNonGenericType.ForLoadedType(Object[][].class);
        unboundWildcard = listWildcard.getParameters().getOnly();
        typeVariableT = arrayListTypeVariableT.getParameters().getOnly();
        typeVariableS = arrayListTypeVariableS.getParameters().getOnly();
        typeVariableU = arrayListTypeVariableU.getParameters().getOnly();
        typeVariableV = arrayListTypeVariableV.getParameters().getOnly();
        tArray = fields.filter(named("tArray")).getOnly().getType();
        sArray = fields.filter(named("sArray")).getOnly().getType();
        uArray = fields.filter(named("uArray")).getOnly().getType();
        tNestedArray = fields.filter(named("tNestedArray")).getOnly().getType();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAssignFromWildcardThrowsException() throws Exception {
        unboundWildcard.accept(TypeDescription.Generic.Visitor.Assigner.INSTANCE);
    }

    @Test
    public void testAssignNonGenericTypeFromAssignableNonGenericType() throws Exception {
        assertThat(TypeDescription.Generic.OBJECT.accept(TypeDescription.Generic.Visitor.Assigner.INSTANCE)
                .isAssignableFrom(TypeDescription.STRING.asGenericType()), is(true));
    }

    @Test
    public void testAssignNonGenericTypeFromNonAssignableNonGenericType() throws Exception {
        assertThat(TypeDescription.STRING.asGenericType().accept(TypeDescription.Generic.Visitor.Assigner.INSTANCE)
                .isAssignableFrom(TypeDescription.Generic.OBJECT), is(false));
    }

    @Test
    public void testAssignObjectTypeFromAssignableGenericType() throws Exception {
        assertThat(TypeDescription.Generic.OBJECT.accept(TypeDescription.Generic.Visitor.Assigner.INSTANCE)
                .isAssignableFrom(listWildcard), is(true));
    }

    @Test
    public void testAssignNonGenericTypeFromNonAssignableGenericType() throws Exception {
        assertThat(TypeDescription.STRING.asGenericType().accept(TypeDescription.Generic.Visitor.Assigner.INSTANCE)
                .isAssignableFrom(listWildcard), is(false));
    }

    @Test
    public void testAssignNonGenericSuperInterfaceTypeFromAssignableGenericInterfaceType() throws Exception {
        assertThat(collectionRaw.accept(TypeDescription.Generic.Visitor.Assigner.INSTANCE)
                .isAssignableFrom(listWildcard), is(true));
    }

    @Test
    public void testAssignNonGenericSuperInterfaceTypeFromAssignableGenericType() throws Exception {
        assertThat(collectionRaw.accept(TypeDescription.Generic.Visitor.Assigner.INSTANCE)
                .isAssignableFrom(arrayListWildcard), is(true));
    }

    @Test
    public void testAssignRawInterfaceTypeFromEqualGenericInterfaceType() throws Exception {
        assertThat(listRaw.accept(TypeDescription.Generic.Visitor.Assigner.INSTANCE)
                .isAssignableFrom(listWildcard), is(true));
    }

    @Test
    public void testAssignRawTypeFromEqualGenericType() throws Exception {
        assertThat(arrayListRaw.accept(TypeDescription.Generic.Visitor.Assigner.INSTANCE)
                .isAssignableFrom(arrayListWildcard), is(true));
    }

    @Test
    public void testAssignNonGenericSuperTypeFromAssignableGenericType() throws Exception {
        assertThat(abstractListRaw.accept(TypeDescription.Generic.Visitor.Assigner.INSTANCE)
                .isAssignableFrom(arrayListWildcard), is(true));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAssignNonGenericTypeFromWildcardThrowsException() throws Exception {
        TypeDescription.Generic.OBJECT.accept(TypeDescription.Generic.Visitor.Assigner.INSTANCE)
                .isAssignableFrom(unboundWildcard);
    }

    @Test
    public void testAssignNonGenericTypeFromAssignableTypeVariable() throws Exception {
        assertThat(TypeDescription.Generic.OBJECT.accept(TypeDescription.Generic.Visitor.Assigner.INSTANCE)
                .isAssignableFrom(typeVariableT), is(true));
    }

    @Test
    public void testAssignNonGenericTypeFromNonAssignableTypeVariable() throws Exception {
        assertThat(TypeDescription.STRING.asGenericType().accept(TypeDescription.Generic.Visitor.Assigner.INSTANCE)
                .isAssignableFrom(typeVariableT), is(false));
    }

    @Test
    public void testAssignNonGenericSuperArrayTypeFromAssignableGenericArrayType() throws Exception {
        assertThat(collectionRawArray.accept(TypeDescription.Generic.Visitor.Assigner.INSTANCE)
                .isAssignableFrom(listWildcardArray), is(true));
    }

    @Test
    public void testAssignRawArrayTypeFromEqualGenericArrayType() throws Exception {
        assertThat(listRawArray.accept(TypeDescription.Generic.Visitor.Assigner.INSTANCE)
                .isAssignableFrom(listWildcardArray), is(true));
    }

    @Test
    public void testAssignNonGenericArrayFromNonAssignableGenericArrayType() throws Exception {
        assertThat(stringArray.accept(TypeDescription.Generic.Visitor.Assigner.INSTANCE)
                .isAssignableFrom(listWildcardArray), is(false));
    }

    @Test
    public void testAssignNonGenericArrayFromAssignableGenericArrayType() throws Exception {
        assertThat(objectArray.accept(TypeDescription.Generic.Visitor.Assigner.INSTANCE)
                .isAssignableFrom(listWildcardArray), is(true));
    }

    @Test
    public void testAssignNonGenericArrayFromGenericArrayTypeOfIncompatibleArrity() throws Exception {
        assertThat(objectNestedArray.accept(TypeDescription.Generic.Visitor.Assigner.INSTANCE)
                .isAssignableFrom(listWildcardArray), is(false));
    }

    @Test
    public void testAssignObjectTypeFromGenericArrayType() throws Exception {
        assertThat(TypeDescription.Generic.OBJECT.accept(TypeDescription.Generic.Visitor.Assigner.INSTANCE)
                .isAssignableFrom(listWildcardArray), is(true));
    }

    @Test
    public void testAssignCloneableTypeFromGenericArrayType() throws Exception {
        assertThat(new TypeDescription.Generic.OfNonGenericType.ForLoadedType(Cloneable.class).accept(TypeDescription.Generic.Visitor.Assigner.INSTANCE)
                .isAssignableFrom(listWildcardArray), is(true));
    }

    @Test
    public void testAssignSerializableTypeFromGenericArrayType() throws Exception {
        assertThat(new TypeDescription.Generic.OfNonGenericType.ForLoadedType(Serializable.class).accept(TypeDescription.Generic.Visitor.Assigner.INSTANCE)
                .isAssignableFrom(listWildcardArray), is(true));
    }

    @Test
    public void testAssignTypeVariableFromNonGenericType() throws Exception {
        assertThat(typeVariableT.accept(TypeDescription.Generic.Visitor.Assigner.INSTANCE)
                .isAssignableFrom(TypeDescription.Generic.OBJECT), is(false));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAssignTypeVariableFromWildcardTypeThrowsException() throws Exception {
        typeVariableT.accept(TypeDescription.Generic.Visitor.Assigner.INSTANCE)
                .isAssignableFrom(unboundWildcard);
    }

    @Test
    public void testAssignTypeVariableFromGenericArrayType() throws Exception {
        assertThat(typeVariableT.accept(TypeDescription.Generic.Visitor.Assigner.INSTANCE)
                .isAssignableFrom(listWildcardArray), is(false));
    }

    @Test
    public void testAssignTypeVariableFromParameterizedType() throws Exception {
        assertThat(typeVariableT.accept(TypeDescription.Generic.Visitor.Assigner.INSTANCE)
                .isAssignableFrom(listWildcard), is(false));
    }

    @Test
    public void testAssignTypeVariableFromEqualTypeVariable() throws Exception {
        assertThat(typeVariableT.accept(TypeDescription.Generic.Visitor.Assigner.INSTANCE)
                .isAssignableFrom(typeVariableT), is(true));
    }

    @Test
    public void testAssignTypeVariableFromNonAssignableWildcard() throws Exception {
        assertThat(typeVariableT.accept(TypeDescription.Generic.Visitor.Assigner.INSTANCE)
                .isAssignableFrom(typeVariableS), is(false));
    }

    @Test
    public void testAssignTypeVariableFromAssignableWildcard() throws Exception {
        assertThat(typeVariableT.accept(TypeDescription.Generic.Visitor.Assigner.INSTANCE)
                .isAssignableFrom(typeVariableU), is(true));
    }

    @Test
    public void testAssignGenericArrayFromAssignableGenericArray() throws Exception {
        assertThat(tArray.accept(TypeDescription.Generic.Visitor.Assigner.INSTANCE)
                .isAssignableFrom(uArray), is(true));
    }

    @Test
    public void testAssignGenericArrayFromNonAssignableGenericArray() throws Exception {
        assertThat(tArray.accept(TypeDescription.Generic.Visitor.Assigner.INSTANCE)
                .isAssignableFrom(sArray), is(false));
    }

    @Test
    public void testAssignGenericArrayFromNonAssignableNonGenericNonArrayType() throws Exception {
        assertThat(tArray.accept(TypeDescription.Generic.Visitor.Assigner.INSTANCE)
                .isAssignableFrom(TypeDescription.Generic.OBJECT), is(false));
    }

    @Test
    public void testAssignGenericArrayFromNonAssignableNonGenericArrayType() throws Exception {
        assertThat(tArray.accept(TypeDescription.Generic.Visitor.Assigner.INSTANCE)
                .isAssignableFrom(objectArray), is(false));
    }

    @Test
    public void testAssignGenericArrayFromAssignableNonGenericArrayType() throws Exception {
        assertThat(listWildcardArray.accept(TypeDescription.Generic.Visitor.Assigner.INSTANCE)
                .isAssignableFrom(arrayListRawArray), is(true));
    }

    @Test
    public void testAssignGenericArrayFromNonAssignableTypeVariable() throws Exception {
        assertThat(tArray.accept(TypeDescription.Generic.Visitor.Assigner.INSTANCE)
                .isAssignableFrom(typeVariableT), is(false));
    }

    @Test
    public void testAssignGenericArrayFromNonAssignableParameterizedType() throws Exception {
        assertThat(tArray.accept(TypeDescription.Generic.Visitor.Assigner.INSTANCE)
                .isAssignableFrom(arrayListWildcard), is(false));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAssignGenericArrayFromWildcardThrowsException() throws Exception {
        tArray.accept(TypeDescription.Generic.Visitor.Assigner.INSTANCE)
                .isAssignableFrom(unboundWildcard);
    }

    @Test
    public void testAssignParameterizedWildcardTypeFromEqualType() throws Exception {
        assertThat(collectionWildcard.accept(TypeDescription.Generic.Visitor.Assigner.INSTANCE)
                .isAssignableFrom(collectionWildcard), is(true));
    }

    @Test
    public void testAssignParameterizedWildcardTypeFromEqualRawType() throws Exception {
        assertThat(collectionWildcard.accept(TypeDescription.Generic.Visitor.Assigner.INSTANCE)
                .isAssignableFrom(collectionRaw), is(true));
    }

    @Test
    public void testAssignParameterizedWildcardTypeFromAssignableParameterizedWildcardType() throws Exception {
        assertThat(collectionWildcard.accept(TypeDescription.Generic.Visitor.Assigner.INSTANCE)
                .isAssignableFrom(arrayListWildcard), is(true));
    }

    @Test
    public void testAssignParameterizedWildcardTypeFromAssignableParameterizedNonWildcardTypeType() throws Exception {
        assertThat(collectionWildcard.accept(TypeDescription.Generic.Visitor.Assigner.INSTANCE)
                .isAssignableFrom(arrayListTypeVariableT), is(true));
    }

    @Test
    public void testAssignParameterizedWildcardTypeFromAssignableTypeVariableType() throws Exception {
        assertThat(collectionWildcard.accept(TypeDescription.Generic.Visitor.Assigner.INSTANCE)
                .isAssignableFrom(typeVariableV), is(true));
    }

    @Test
    public void testAssignParameterizedWildcardTypeFromNonAssignableRawType() throws Exception {
        assertThat(collectionWildcard.accept(TypeDescription.Generic.Visitor.Assigner.INSTANCE)
                .isAssignableFrom(TypeDescription.STRING.asGenericType()), is(false));
    }

    @Test
    public void testAssignParameterizedWildcardTypeFromNonAssignableParameterizedType() throws Exception {
        assertThat(collectionWildcard.accept(TypeDescription.Generic.Visitor.Assigner.INSTANCE)
                .isAssignableFrom(callableWildcard), is(false));
    }

    @Test
    public void testAssignParameterizedWildcardTypeFromNonAssignableGenericArrayType() throws Exception {
        assertThat(collectionWildcard.accept(TypeDescription.Generic.Visitor.Assigner.INSTANCE)
                .isAssignableFrom(tArray), is(false));
    }

    @Test
    public void testAssignParameterizedWildcardTypeFromNonAssignableTypeVariableType() throws Exception {
        assertThat(collectionWildcard.accept(TypeDescription.Generic.Visitor.Assigner.INSTANCE)
                .isAssignableFrom(typeVariableT), is(false));
    }

    @Test
    public void testAssignParameterizedTypeVariableTypeFromEqualParameterizedTypeVariableTypeType() throws Exception {
        assertThat(collectionTypeVariableT.accept(TypeDescription.Generic.Visitor.Assigner.INSTANCE)
                .isAssignableFrom(collectionTypeVariableT), is(true));
    }

    @Test
    public void testAssignParameterizedTypeVariableTypeFromAssignableParameterizedTypeVariableTypeType() throws Exception {
        assertThat(collectionTypeVariableT.accept(TypeDescription.Generic.Visitor.Assigner.INSTANCE)
                .isAssignableFrom(arrayListTypeVariableT), is(true));
    }

    @Test
    public void testAssignParameterizedTypeVariableTypeFromNonAssignableParameterizedTypeVariableTypeType() throws Exception {
        assertThat(collectionTypeVariableT.accept(TypeDescription.Generic.Visitor.Assigner.INSTANCE)
                .isAssignableFrom(arrayListTypeVariableS), is(false));
    }

    // TODO: assignParameterizedTypeFromXXX

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypeDescription.Generic.Visitor.Assigner.class).apply();
        ObjectPropertyAssertion.of(TypeDescription.Generic.Visitor.Assigner.Dispatcher.ForGenericArray.class).apply();
        ObjectPropertyAssertion.of(TypeDescription.Generic.Visitor.Assigner.Dispatcher.ForNonGenericType.class).apply();
        ObjectPropertyAssertion.of(TypeDescription.Generic.Visitor.Assigner.Dispatcher.ForTypeVariable.class).apply();
        ObjectPropertyAssertion.of(TypeDescription.Generic.Visitor.Assigner.Dispatcher.ForParameterizedType.class).apply();
        ObjectPropertyAssertion.of(TypeDescription.Generic.Visitor.Assigner.Dispatcher.ForParameterizedType.ParameterAssigner.class).apply();
        ObjectPropertyAssertion.of(TypeDescription.Generic.Visitor.Assigner.Dispatcher.ForParameterizedType.ParameterAssigner.InvariantBinding.class).apply();
        ObjectPropertyAssertion.of(TypeDescription.Generic.Visitor.Assigner.Dispatcher.ForParameterizedType.ParameterAssigner.ContravariantBinding.class).apply();
    }

    @SuppressWarnings({"unused", "unchecked"})
    private static class GenericTypes<T, S, U extends T, V extends List<?>> {

        private Collection collectionRaw;

        private Collection<?> collectionWildcard;

        private Collection<T> collectionTypeVariableT;

        private Collection<S> collectionTypeVariableS;

        private Collection<U> collectionTypeVariableU;

        private Collection<? extends T> collectionUpperBoundTypeVariableT;

        private Collection<? extends S> collectionUpperBoundTypeVariableS;

        private Collection<? extends U> collectionUpperBoundTypeVariableU;

        private Collection<? super T> collectionLowerBoundTypeVariableT;

        private Collection<? super S> collectionLowerBoundTypeVariableS;

        private Collection<? super U> collectionLowerBoundTypeVariableU;

        private Collection[] collectionRawArray;

        private List listRaw;

        private List<?> listWildcard;

        private List[] listRawArray;

        private List<?>[] listWildcardArray;

        private AbstractList abstractListRaw;

        private ArrayList arrayListRaw;

        private ArrayList<?> arrayListWildcard;

        private ArrayList[] arrayListRawArray;

        private ArrayList<T> arrayListTypeVariableT;

        private ArrayList<S> arrayListTypeVariableS;

        private ArrayList<U> arrayListTypeVariableU;

        private ArrayList<V> arrayListTypeVariableV;

        private Callable<?> callableWildcard;

        private T[] tArray;

        private T[][] tNestedArray;

        private S[] sArray;

        private U[] uArray;
    }
}
