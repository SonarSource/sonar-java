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
package org.sonar.java.checks.helpers;

import java.util.List;
import org.sonar.plugins.java.api.semantic.MethodMatchers;

public class CredentialMethod {
  public final String cls;
  public final String name;
  public final List<String> args;
  public final List<Integer> indices;

  private MethodMatchers methodMatcher;

  public CredentialMethod(String cls, String name, List<String> args, List<Integer> indices) {
    this.cls = cls;
    this.name = name;
    this.args = args;
    this.indices = indices;
  }

  public boolean isConstructor() {
    int sep = Math.max(cls.lastIndexOf('.'), cls.lastIndexOf('$'));
    if (sep == -1) {
      return cls.equals(name);
    }
    return cls.substring(sep + 1).equals(name);
  }

  public MethodMatchers methodMatcher() {
    if (methodMatcher != null) {
      return methodMatcher;
    }
    MethodMatchers.NameBuilder nameBuilder = MethodMatchers.create()
      .ofTypes(this.cls);

    MethodMatchers.ParametersBuilder parametersBuilder = isConstructor() ?
      nameBuilder.constructor() : nameBuilder.names(this.name);

    this.methodMatcher = parametersBuilder
      .addParametersMatcher(args.toArray(new String[0]))
      .build();
    return methodMatcher;
  }
}
