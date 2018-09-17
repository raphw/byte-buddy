package net.bytebuddy.test.scope;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public class GenericType<U> {

    public class Inner<T extends String, S extends T> extends GenericType<GenericType<U>.Inner<T, S>> implements Callable<Map<? super String, ? extends String>> {

        <V extends T, W extends Exception> List<T[]> foo(V value) throws W {
            return null;
        }

        public Map<? super String, ? extends String> call() throws Exception {
            return null;
        }
    }

}
