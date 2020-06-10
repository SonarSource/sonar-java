/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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
package org.sonar.java.checks.regex;

import org.sonar.java.regex.RegexParseResult;
import org.sonar.java.regex.ast.RegexVisitor;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;

public abstract class AbstractRegexVisitorCheck extends AbstractRegexCheck {

  protected abstract RegexVisitor createVisitor();

  @Override
  public void checkRegex(RegexParseResult regexForLiterals, MethodInvocationTree mit) {
    if (!regexForLiterals.hasSyntaxErrors()) {
      RegexVisitor visitor = createVisitor();
      visitor.setActiveFlags(getFlags(mit));
      visitor.visit(regexForLiterals.getResult());
    }
  }
}
