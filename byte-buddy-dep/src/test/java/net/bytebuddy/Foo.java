package net.bytebuddy;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class Foo {

    @Test
    public void testList() throws Exception {
        Map<String, String> map = new HashMap<String, String>(2);
        map.put("foo", "qux");
        map.put("bar", "baz");
    }
}
