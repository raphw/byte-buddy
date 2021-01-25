package net.bytebuddy.description.type;

public class ForTypeVariableToken {
    public static void main(String[] args) {
        TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(GenericArrayTypeTest.class).getSymbol();
    }
}
