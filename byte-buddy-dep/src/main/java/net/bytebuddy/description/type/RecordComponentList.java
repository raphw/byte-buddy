package net.bytebuddy.description.type;

import net.bytebuddy.matcher.FilterableList;

import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.List;

public interface RecordComponentList extends FilterableList<RecordComponentDescription, RecordComponentList> {

    abstract class AbstractBase extends FilterableList.AbstractBase<RecordComponentDescription, RecordComponentList> implements RecordComponentList {

        @Override
        protected RecordComponentList wrap(List<RecordComponentDescription> values) {
            return new Explicit(values);
        }
    }

    class ForLoadedRecordComponents extends AbstractBase implements RecordComponentList {

        private final List<?> recordComponents;

        protected ForLoadedRecordComponents(Object[] recordComponent) {
            this(Arrays.asList(recordComponent));
        }

        protected ForLoadedRecordComponents(List<?> recordComponents) {
            this.recordComponents = recordComponents;
        }

        @Override
        public RecordComponentDescription get(int index) {
            return new RecordComponentDescription.ForLoadedRecordComponent((AnnotatedElement) recordComponents.get(index));
        }

        @Override
        public int size() {
            return recordComponents.size();
        }
    }

    class Explicit extends AbstractBase {

        private final List<RecordComponentDescription> recordComponents;

        public Explicit(RecordComponentDescription... recordComponent) {
            this(Arrays.asList(recordComponent));
        }

        public Explicit(List<RecordComponentDescription> recordComponents) {
            this.recordComponents = recordComponents;
        }

        @Override
        public RecordComponentDescription get(int index) {
            return recordComponents.get(index);
        }

        @Override
        public int size() {
            return recordComponents.size();
        }
    }

    class Empty extends FilterableList.Empty<RecordComponentDescription, RecordComponentList> implements RecordComponentList {
        /* empty */
    }
}
