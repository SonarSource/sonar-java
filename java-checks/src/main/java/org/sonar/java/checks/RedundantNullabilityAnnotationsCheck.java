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

import java.util.Arrays;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.sonar.plugins.java.api.semantic.SymbolMetadata.NullabilityLevel.PACKAGE;

@Rule(key = "S6665")
public class RedundantNullabilityAnnotationsCheck  extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.INTERFACE, Tree.Kind.CLASS, Tree.Kind.RECORD);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    // have I (the class), or my package or module, a non-null annotation?
    if (classTree.symbol().metadata().nullabilityData().isNonNull(PACKAGE, false, false)) {
      // if so, highlight members that also have a non-null annotation
      checkClass(classTree);
    }
  }

  private void checkClass(ClassTree tree) {
    tree.members().forEach(member -> {
      if (member.is(Tree.Kind.METHOD)) {
        checkMethod((MethodTree) member);
      } else if (member.is(Tree.Kind.CLASS)) {
        // TODO check member class DIRECT annotations (not inherited)
        // then check inside the member class
        checkClass((ClassTree) member);
      }
    });
  }

  private void checkMethod(MethodTree tree) {
    // TODO check member DIRECT annotations (not inherited) for returns, parameters
  }

}
