package net.bytebuddy.description.type;

import net.bytebuddy.matcher.FilterableList;

import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.List;

public interface RecordComponentList extends FilterableList<RecordComponentDescription, RecordComponentList> {

    class ForLoadedRecordComponents extends AbstractBase<RecordComponentDescription, RecordComponentList> implements RecordComponentList {

        private final List<?> recordComponents;

        protected ForLoadedRecordComponents(Object[] recordComponent) {
            this(Arrays.asList(recordComponent));
        }

        protected ForLoadedRecordComponents(List<?> recordComponents) {
            this.recordComponents = recordComponents;
        }

        @Override
        protected RecordComponentList wrap(List<RecordComponentDescription> values) {
            return new ForLoadedRecordComponents(values);
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

    class Empty extends AbstractBase.Empty<RecordComponentDescription, RecordComponentList> implements RecordComponentList {
        /* empty */
    }
}
