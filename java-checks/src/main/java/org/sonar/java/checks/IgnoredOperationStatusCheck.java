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
package org.sonar.java.checks;

import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S899")
public class IgnoredOperationStatusCheck extends AbstractMethodDetection {

  private static final String FILE = "java.io.File";
  private static final String CONDITION = "java.util.concurrent.locks.Condition";
  private static final String BLOCKING_QUEUE = "java.util.concurrent.BlockingQueue";

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.or(
      MethodMatchers.create().ofSubTypes("java.util.concurrent.locks.Lock").names("tryLock").addWithoutParametersMatcher().build(),
      MethodMatchers.create().ofTypes(FILE)
        .name(name -> name.equals("delete") || name.equals("exists") || name.equals("createNewFile") ||
          name.startsWith("can") || name.startsWith("is"))
        .addWithoutParametersMatcher()
        .build(),

      MethodMatchers.create().ofTypes(FILE).name(name -> name.startsWith("set"))
        .withAnyParameters().build(),

      MethodMatchers.create().ofTypes(FILE).names("renameTo").addParametersMatcher(FILE).build(),

      MethodMatchers.create().ofSubTypes("java.util.Iterator").names("hasNext").addWithoutParametersMatcher().build(),
      MethodMatchers.create().ofSubTypes("java.util.Enumeration").names("hasMoreElements").addWithoutParametersMatcher().build(),

      MethodMatchers.create().ofSubTypes(CONDITION).names("await").addParametersMatcher("long", "java.util.concurrent.TimeUnit").build(),
      MethodMatchers.create().ofSubTypes(CONDITION).names("awaitUntil").addParametersMatcher("java.util.Date").build(),
      MethodMatchers.create().ofSubTypes(CONDITION).names("awaitNanos").addParametersMatcher("long").build(),

      MethodMatchers.create().ofTypes("java.util.concurrent.CountDownLatch").names("await").addParametersMatcher("long", "java.util.concurrent.TimeUnit").build(),
      MethodMatchers.create().ofTypes("java.util.concurrent.Semaphore").names("tryAcquire").withAnyParameters().build(),

      MethodMatchers.create().ofSubTypes(BLOCKING_QUEUE).names("offer").withAnyParameters().build(),
      MethodMatchers.create().ofSubTypes(BLOCKING_QUEUE).names("remove").withAnyParameters().build());
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    Tree parent = mit.parent();
    if (parent.is(Tree.Kind.EXPRESSION_STATEMENT)
      || (parent.is(Tree.Kind.VARIABLE) && ((VariableTree) parent).symbol().usages().isEmpty())) {
      reportIssue(parent, "Do something with the \"" + mit.symbolType().name() + "\" value returned by \"" + mit.symbol().name() + "\".");
    }
  }

}
