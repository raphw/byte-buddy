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

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class TypeDescriptionGenericVisitorAssignerTest {

    private TypeDescription.Generic collectionRaw, listRaw, listWildcard;

    private TypeDescription.Generic abstractListRaw, arrayListRaw, arrayListWildcard;

    private TypeDescription.Generic listTypeVariableT, listTypeVariableS, listTypeVariableU, tNestedArray;

    private TypeDescription.Generic collectionRawArray, listRawArray, listWildcardArray, arrayListRawArray;

    private TypeDescription.Generic stringArray, objectArray, objectNestedArray;

    private TypeDescription.Generic unboundWildcard;

    private TypeDescription.Generic typeVariableT, typeVariableS, typeVariableU;

    private TypeDescription.Generic tArray, sArray, uArray;

    @Before
    public void setUp() throws Exception {
        FieldList<?> fields = new TypeDescription.ForLoadedType(GenericTypes.class).getDeclaredFields();
        collectionRaw = fields.filter(named("collectionRaw")).getOnly().getType();
        listRaw = fields.filter(named("listRaw")).getOnly().getType();
        listWildcard = fields.filter(named("listWildcard")).getOnly().getType();
        listTypeVariableT = fields.filter(named("listTypeVariableT")).getOnly().getType();
        listTypeVariableS = fields.filter(named("listTypeVariableS")).getOnly().getType();
        listTypeVariableU = fields.filter(named("listTypeVariableU")).getOnly().getType();
        abstractListRaw = fields.filter(named("abstractListRaw")).getOnly().getType();
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
        typeVariableT = listTypeVariableT.getParameters().getOnly();
        typeVariableS = listTypeVariableS.getParameters().getOnly();
        typeVariableU = listTypeVariableU.getParameters().getOnly();
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
        tArray.accept(TypeDescription.Generic.Visitor.Assigner.INSTANCE).isAssignableFrom(unboundWildcard);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypeDescription.Generic.Visitor.Assigner.class).apply();
        ObjectPropertyAssertion.of(TypeDescription.Generic.Visitor.Assigner.Dispatcher.ForGenericArray.class).apply();
        ObjectPropertyAssertion.of(TypeDescription.Generic.Visitor.Assigner.Dispatcher.ForNonGenericType.class).apply();
        ObjectPropertyAssertion.of(TypeDescription.Generic.Visitor.Assigner.Dispatcher.ForTypeVariable.class).apply();
        ObjectPropertyAssertion.of(TypeDescription.Generic.Visitor.Assigner.Dispatcher.ForParameterizedType.class).apply();
        ObjectPropertyAssertion.of(TypeDescription.Generic.Visitor.Assigner.Dispatcher.ForParameterizedType.ParameterAssigner.class).apply();
        ObjectPropertyAssertion.of(TypeDescription.Generic.Visitor.Assigner.Dispatcher.ForParameterizedType.ParameterAssigner.InvariantBinding.class).apply();
        ObjectPropertyAssertion.of(TypeDescription.Generic.Visitor.Assigner.Dispatcher.ForParameterizedType.ParameterAssigner.CovariantBinding.class).apply();
        ObjectPropertyAssertion.of(TypeDescription.Generic.Visitor.Assigner.Dispatcher.ForParameterizedType.ParameterAssigner.ContravariantBinding.class).apply();
    }

    @SuppressWarnings({"unused", "unchecked"})
    private static class GenericTypes<T, S, U extends T> {

        private Collection collectionRaw;

        private List listRaw;

        private List<?> listWildcard;

        private Collection[] collectionRawArray;

        private List[] listRawArray;

        private List<?>[] listWildcardArray;

        private AbstractList abstractListRaw;

        private ArrayList arrayListRaw;

        private ArrayList<?> arrayListWildcard;

        private ArrayList[] arrayListRawArray;

        private ArrayList<T> listTypeVariableT;

        private ArrayList<S> listTypeVariableS;

        private ArrayList<U> listTypeVariableU;

        private T[] tArray;

        private T[][] tNestedArray;

        private S[] sArray;

        private U[] uArray;
    }
}