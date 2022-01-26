/*
 * Copyright 2014 - Present Rafael Winterhalter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.bytebuddy.utility;

import net.bytebuddy.build.AccessControllerPlugin;
import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.utility.dispatcher.JavaDispatcher;
import net.bytebuddy.utility.nullability.MaybeNull;

import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;

/**
 * A factory for creating a {@link Queue}. For Java 5, a {@link LinkedList} is created whereas a
 * {@code java.util.ArrayDeque} is used for any future JVM version.
 */
@HashCodeAndEqualsPlugin.Enhance
public class QueueFactory {

    /**
     * The singleton instance.
     */
    private static final QueueFactory INSTANCE = new QueueFactory();

    /**
     * The dispatcher to use.
     */
    private final Dispatcher dispatcher;

    /**
     * Creates a new queue factory.
     */
    private QueueFactory() {
        dispatcher = doPrivileged(JavaDispatcher.of(Dispatcher.class));
    }

    /**
     * Creates a new queue.
     *
     * @param <T> The type of the queue elements.
     * @return An appropriate queue.
     */
    public static <T> Queue<T> make() {
        Queue<T> queue = INSTANCE.dispatcher.arrayDeque();
        return queue == null
                ? new LinkedList<T>()
                : queue;
    }

    /**
     * Creates a new queue.
     *
     * @param <T>      The type of the queue elements.
     * @param elements The elements to provide to the queue constructor.
     * @return An appropriate queue.
     */
    public static <T> Queue<T> make(Collection<? extends T> elements) {
        Queue<T> queue = INSTANCE.dispatcher.arrayDeque(elements);
        return queue == null
                ? new LinkedList<T>(elements)
                : queue;
    }

    /**
     * A proxy for {@code java.security.AccessController#doPrivileged} that is activated if available.
     *
     * @param action The action to execute from a privileged context.
     * @param <T>    The type of the action's resolved value.
     * @return The action's resolved value.
     */
    @AccessControllerPlugin.Enhance
    private static <T> T doPrivileged(PrivilegedAction<T> action) {
        return action.run();
    }

    /**
     * A dispatcher for creating an {@code java.util.ArrayDeque}.
     */
    @JavaDispatcher.Defaults
    @JavaDispatcher.Proxied("java.util.ArrayDeque")
    protected interface Dispatcher {

        /**
         * Creates a new array deque.
         *
         * @param <T> The type of the action's resolved value.
         * @return An array deque or {@code null} if this class is not supplied by the current VM.
         */
        @MaybeNull
        @JavaDispatcher.IsConstructor
        <T> Queue<T> arrayDeque();

        /**
         * Creates a new array deque.
         *
         * @param <T>      The type of the action's resolved value.
         * @param elements The elements to provide to the queue constructor.
         * @return An array deque or {@code null} if this class is not supplied by the current VM.
         */
        @MaybeNull
        @JavaDispatcher.IsConstructor
        <T> Queue<T> arrayDeque(Collection<? extends T> elements);
    }
}
