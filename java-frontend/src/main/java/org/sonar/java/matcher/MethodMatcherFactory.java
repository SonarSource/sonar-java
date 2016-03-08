/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.matcher;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MethodMatcherFactory {

  private static final Pattern CLASS_PATTERN = Pattern.compile("^(\\w+[\\.\\w+]*(?:\\[\\])?)([\\(])?");
  private static final Pattern METHOD_PATTERN = Pattern.compile("^(\\w+[\\.\\w+]*(?:\\[\\])?)#(\\w+)([\\(])?");
  private static final Pattern ARGUMENT_PATTERN = Pattern.compile("(\\w+[\\.\\w+]*(?:\\[\\])?)([,\\)])?");

  private MethodMatcherFactory() {
    // no instances, only static, factory methods
  }

  public static MethodMatcher constructorMatcher(String descriptor) {
    Matcher matcher = CLASS_PATTERN.matcher(descriptor);
    if (!matcher.find()) {
      throw new IllegalArgumentException("Illegal constructor specification: " + descriptor);
    }
    MethodMatcher constructorMatcher = MethodMatcher.create().typeDefinition(matcher.group(1)).name("<init>");
    collectArguments(descriptor, matcher, 2, constructorMatcher);
    return constructorMatcher;
  }

  public static MethodMatcher methodMatcher(String descriptor) {
    Matcher matcher = METHOD_PATTERN.matcher(descriptor);
    if (!matcher.find()) {
      throw new IllegalArgumentException("Illegal constructor specification: " + descriptor);
    }
    MethodMatcher constructorMatcher = MethodMatcher.create().typeDefinition(matcher.group(1)).name(matcher.group(2));
    collectArguments(descriptor, matcher, 3, constructorMatcher);
    return constructorMatcher;
  }

  public static void collectArguments(String descriptor, Matcher initialMatcher, final int groupOffset, MethodMatcher constructorMatcher) {
    if ("(".equals(initialMatcher.group(groupOffset))) {
      String remainder = descriptor.substring(initialMatcher.group().length());
      if (!")".equals(remainder)) {
        Matcher matcher = ARGUMENT_PATTERN.matcher(remainder);
        while (matcher.find()) {
          constructorMatcher.addParameter(matcher.group(1));
        }
        if (!matcher.hitEnd()) {
          throw new IllegalArgumentException("Illegal method or constructor arguments specification: " + descriptor);
        }
      }
    } else {
      constructorMatcher.withNoParameterConstraint();
    }
  }
}
