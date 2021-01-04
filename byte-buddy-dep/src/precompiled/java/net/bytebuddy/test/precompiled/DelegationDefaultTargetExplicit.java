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
package net.bytebuddy.test.precompiled;

import net.bytebuddy.implementation.bind.annotation.Default;

import java.io.Serializable;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class DelegationDefaultTargetExplicit {

    private static final String FOO = "foo", BAR = "bar";

    public static String intercept(@Default(proxyType = DelegationDefaultInterface.class) Object proxy) throws Exception {
        assertThat(proxy, not(instanceOf(Serializable.class)));
        return DelegationDefaultInterface.class.getDeclaredMethod(FOO).invoke(proxy) + BAR;
    }
}
