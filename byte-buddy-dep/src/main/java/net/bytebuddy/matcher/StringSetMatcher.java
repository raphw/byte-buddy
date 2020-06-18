/*
 * Copyright 2014 - 2020 Rafael Winterhalter
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
package net.bytebuddy.matcher;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;

import java.util.HashSet;
import java.util.Set;

/**
 * An element matcher which checks if a string is in a set of strings.
 */
@HashCodeAndEqualsPlugin.Enhance
public class StringSetMatcher extends ElementMatcher.Junction.AbstractBase<String> {

  private final Set<String> set;

  public StringSetMatcher(String... values) {
    this.set = new HashSet<String>(values.length * 4 / 3);
    // do it manually to avoid allocations or rehashing
    for (String value : values) {
      set.add(value);
    }
  }

  @Override
  public boolean matches(String target) {
    return set.contains(target);
  }

  @Override
  public String toString() {
    return "in(" + set + ")";
  }
}
