package net.bytebuddy.build.maven;

import java.util.Set;

/**
 * Execution status of some operation.
 * @author Kapralov Sergey
 */
public interface ExecutionStatus {
    /**
     * Is status considered as failure?
     * @return true if status represents successful completion, false otherwise
     */
    boolean failed();

    /**
     * Success.
     * @author Kapralov Sergey
     */
    class Successful implements ExecutionStatus {
        @Override
        public final boolean failed() {
            return false;
        }
    }

    /**
     * Failure.
     * @author Kapralov Sergey
     */
    class Failed implements ExecutionStatus {
        @Override
        public final boolean failed() {
            return true;
        }
    }

    /**
     * Overall status of some composite operation. Successful only if all the
     * dependant items are successful.
     * @author Kapralov Sergey
     */
    class Combined implements ExecutionStatus {
        /**
         * A set of dependant statuses.
         */
        private final Set<ExecutionStatus> statuses;

        /**
         * Ctor.
         * @param statuses set of statuses to combine
         */
        public Combined(Set<ExecutionStatus> statuses) {
            this.statuses = statuses;
        }

        @Override
        public final boolean failed() {
            boolean result = false;
            for (ExecutionStatus sts : statuses) {
                result = result || sts.failed();
            }
            return result;
        }
    }
}
