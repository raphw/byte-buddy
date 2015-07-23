package net.bytebuddy.description;

public interface TypeDefinable<T extends TypeDefinable<T, ? extends S>, S extends T> {

    S asDefined();
}
