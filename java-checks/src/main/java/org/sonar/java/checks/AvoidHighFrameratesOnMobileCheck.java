/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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

import java.util.List;
import java.util.Map;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S6898")
public class AvoidHighFrameratesOnMobileCheck extends IssuableSubscriptionVisitor {

  private static final int DEFAULT_THRESHOLD = 60;
  private static final Map<MethodMatchers, Integer> FRAME_RATE_SETTERS = Map.of(
    MethodMatchers.create().ofTypes("android.view.Surface").names("setFrameRate").withAnyParameters().build(), 0,
    MethodMatchers.create().ofTypes("android.view.SurfaceControl").names("setFrameRate").withAnyParameters().build(), 1);

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    var mit = (MethodInvocationTree) tree;
    FRAME_RATE_SETTERS.entrySet().stream().filter(e -> e.getKey().matches(mit)).findFirst().ifPresent(e -> {
      var frameRateArg = mit.arguments().get(e.getValue());
      var frameRateArgVal = ExpressionUtils.resolveAsConstant(frameRateArg);

      if (frameRateArgVal instanceof Number && ((Number) frameRateArgVal).intValue() > DEFAULT_THRESHOLD) {
        reportIssue(frameRateArg, "Avoid setting high frame rates higher than " + DEFAULT_THRESHOLD + " on mobile devices.");
      }
    });
  }
}
