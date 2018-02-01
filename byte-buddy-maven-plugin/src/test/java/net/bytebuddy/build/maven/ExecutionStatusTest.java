/*
 * Copyright 2017 Kapralov Sergey.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.bytebuddy.build.maven;

import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ExecutionStatusTest {

    @Test
    public void emptyStatusCombinationIsSuccessful() {
        assertThat(new ExecutionStatus.Combined(Collections.<ExecutionStatus>emptySet()).failed(), is(false));
    }

    @Test
    public void statusIsSuccessfulWhenCombinedFromSuccessfulItems() {
        Set<ExecutionStatus> set = new HashSet<ExecutionStatus>();
        set.add(new ExecutionStatus.Successful());
        set.add(new ExecutionStatus.Successful());
        set.add(new ExecutionStatus.Successful());
        assertThat(new ExecutionStatus.Combined(set).failed(), is(false));
    }

    @Test
    public void statusIsFailedWhenAtLeastOneItemIsFailure() {
        Set<ExecutionStatus> set = new HashSet<ExecutionStatus>();
        set.add(new ExecutionStatus.Successful());
        set.add(new ExecutionStatus.Failed());
        set.add(new ExecutionStatus.Successful());
        assertThat(new ExecutionStatus.Combined(set).failed(), is(true));
    }
}
