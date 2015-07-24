package net.bytebuddy.description;

public interface TypeDefinable<T extends TypeDefinable<T, S>, S extends T> {

    S asDefined();
}
