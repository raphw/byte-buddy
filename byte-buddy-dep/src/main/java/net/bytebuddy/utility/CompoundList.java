package net.bytebuddy.utility;

import java.util.AbstractList;
import java.util.Collections;
import java.util.List;

public class CompoundList<T> extends AbstractList<T> {

    private final List<? extends T> left;

    private final List<? extends T> right;

    protected CompoundList(List<? extends T> left, List<? extends T> right) {
        this.left = left;
        this.right = right;
    }

    public static <S> List<S> of(S left, List<? extends S> right) {
        return of(Collections.singletonList(left), right);
    }

    public static <S> List<S> of(List<? extends S> left, S right) {
        return of(left, Collections.singletonList(right));
    }

    public static <S> List<S> of(List<? extends S> left, List<? extends S> right) {
        return new CompoundList<S>(left, right);
    }

    @Override
    public T get(int index) {
        int leftSize = left.size();
        return leftSize - index > 0
                ? left.get(index)
                : right.get(index - leftSize);
    }

    @Override
    public int size() {
        return left.size() + right.size();
    }
}
