/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
 * mailto:info AT sonarsource DOT com
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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sonar.plugins.java.api.semantic.MethodMatchers;

public class MethodMatcherFactory {

  private static final Pattern CLASS_PATTERN = Pattern.compile("^(\\w+[\\.\\w+]*(?:\\[\\])?)(\\()?");
  private static final Pattern METHOD_PATTERN = Pattern.compile("^([a-zA-Z_0-9\\$]+[\\.[a-zA-Z_0-9\\$]+]*(?:\\[\\])?)#(\\w+)(\\()?");
  private static final Pattern ARGUMENT_PATTERN = Pattern.compile("\\G(\\w+[\\.\\w+]*(?:\\[\\])?)([,\\)])");

  private MethodMatcherFactory() {
    // no instances, only static, factory methods
  }

  public static MethodMatchers constructorMatcher(String descriptor) {
    Matcher matcher = CLASS_PATTERN.matcher(descriptor);
    if (!matcher.find()) {
      throw new IllegalArgumentException("Illegal constructor specification: " + descriptor);
    }
    MethodMatchers.ParametersBuilder constructorMatcher = MethodMatchers.create().ofTypes(matcher.group(1)).constructor();
    return collectArguments(descriptor, matcher, 2, constructorMatcher);
  }

  public static MethodMatchers methodMatchers(String descriptor) {
    Matcher matcher = METHOD_PATTERN.matcher(descriptor);
    if (!matcher.find()) {
      throw new IllegalArgumentException("Illegal method specification: " + descriptor);
    }
    MethodMatchers.ParametersBuilder methodMatcher = MethodMatchers.create().ofTypes(matcher.group(1)).names(matcher.group(2));
    return collectArguments(descriptor, matcher, 3, methodMatcher);
  }

  public static MethodMatchers collectArguments(String descriptor, Matcher initialMatcher, int groupOffset, MethodMatchers.ParametersBuilder methodMatcher) {
    if ("(".equals(initialMatcher.group(groupOffset))) {
      String remainder = descriptor.substring(initialMatcher.group().length());
      if (!")".equals(remainder)) {
        Matcher matcher = ARGUMENT_PATTERN.matcher(remainder);
        int matchedLength = 0;
        List<String> argumentTypes = new ArrayList<>();
        while (matcher.find()) {
          argumentTypes.add(matcher.group(1));
          matchedLength = matcher.end();
        }
        if (matchedLength < remainder.length()) {
          throw new IllegalArgumentException("Illegal method or constructor arguments specification: " + descriptor);
        }
        return methodMatcher.addParametersMatcher(argumentTypes.toArray(new String[0])).build();
      } else {
        return methodMatcher.addWithoutParametersMatcher().build();
      }
    } else {
      if (initialMatcher.end() < descriptor.length()) {
        throw new IllegalArgumentException("Illegal method or constructor arguments specification: " + descriptor);
      }
      return methodMatcher.withAnyParameters().build();
    }
  }
}
