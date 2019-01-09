/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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
package org.sonar.java.se.checks.debug;

import com.google.common.base.Preconditions;
import java.util.Deque;
import java.util.LinkedList;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.DebugCheck;
import org.sonar.java.cfg.CFG;
import org.sonar.java.se.CheckerContext;
import org.sonar.java.se.CheckerDispatcher;
import org.sonar.java.se.checks.SECheck;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;

@Rule(
  key = "DEBUG-SE-Interrupted",
  name = "DEBUG(SE): Interrupted exploration",
  description = "Raise an issue on each methods which triggered SE, but did not completed.",
  priority = Priority.INFO,
  tags = "debug")
public class DebugInterruptedExecutionCheck extends SECheck implements DebugCheck {

  private Deque<IdentifierTree> methodNames = new LinkedList<>();

  @Override
  public void init(MethodTree methodTree, CFG cfg) {
    methodNames.push(methodTree.simpleName());
  }

  @Override
  public void interruptedExecution(CheckerContext context) {
    Exception cause = ((CheckerDispatcher) context).interruptionCause();
    Preconditions.checkNotNull(cause, "cause should always be present when exploration is interrupted");
    reportIssue(methodNames.pop(), "SE Interrupted: " + cause.getMessage());
  }

  @Override
  public void checkEndOfExecution(CheckerContext context) {
    methodNames.pop();
  }

}
