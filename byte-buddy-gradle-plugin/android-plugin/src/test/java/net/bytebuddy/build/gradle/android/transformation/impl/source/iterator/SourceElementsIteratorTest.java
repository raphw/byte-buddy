package net.bytebuddy.build.gradle.android.transformation.impl.source.iterator;

import net.bytebuddy.build.Plugin;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static net.bytebuddy.build.gradle.android.utils.Many.listOf;
import static net.bytebuddy.build.gradle.android.utils.Many.map;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;

public class SourceElementsIteratorTest {

    @Test
    public void iterateThroughAllElements() {
        SourceElementsIterator iterator = createSourceElementsIterator(
                createIteratorOfNames("item1_1", "item1_2", "item1_3"),
                createIteratorOfNames("item2_1", "item2_2")
        );

        List<String> result = getAllNames(iterator);

        assertEquals(
                listOf("item1_1", "item1_2", "item1_3", "item2_1", "item2_2"),
                result
        );
    }

    @Test
    public void skipEmptyIteratorsAndContinueWithFollowingOnes() {
        SourceElementsIterator iterator = createSourceElementsIterator(
                createIteratorOfNames("item1_1", "item1_2", "item1_3"),
                createIteratorOfNames(),
                createIteratorOfNames(
                        "item3_1", "item3_2"
                )
        );

        List<String> result = getAllNames(iterator);

        assertEquals(
                listOf("item1_1", "item1_2", "item1_3", "item3_1", "item3_2"),
                result
        );
    }

    private List<String> getAllNames(SourceElementsIterator iterator) {
        ArrayList<String> names = new ArrayList<>();
        while (iterator.hasNext()) {
            names.add(iterator.next().getName());
        }

        return names;
    }

    private SourceElementsIterator createSourceElementsIterator(Iterator<Plugin.Engine.Source.Element>... iterators) {
        return new SourceElementsIterator(listOf(iterators));
    }

    private Iterator<Plugin.Engine.Source.Element> createIteratorOfNames(String... names) {
        List<Plugin.Engine.Source.Element> elements = map(listOf(names), this::createSourceElementMock);
        return createIteratorOfSourceElements(elements);
    }

    private Iterator<Plugin.Engine.Source.Element> createIteratorOfSourceElements(List<Plugin.Engine.Source.Element> items) {
        return items.iterator();
    }

    private Plugin.Engine.Source.Element createSourceElementMock(String name) {
        Plugin.Engine.Source.Element mock = Mockito.mock(Plugin.Engine.Source.Element.class);
        doReturn(name).when(mock).getName();

        return mock;
    }
}