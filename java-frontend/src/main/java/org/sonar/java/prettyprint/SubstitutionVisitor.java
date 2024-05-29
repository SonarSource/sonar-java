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
package org.sonar.java.prettyprint;

import java.util.Map;
import javax.annotation.Nullable;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

public final class SubstitutionVisitor extends DeepCopyVisitor {

  private static final String SUBST_TARGET_PREFIX = "$";

  private final Map<String, Tree> substitutions;

  static Tree substitute(Tree tree, Map<String, Tree> substitutions){
    var visitor = new SubstitutionVisitor(substitutions);
    tree.accept(visitor);
    return visitor.popUniqueResult();
  }

  public SubstitutionVisitor(Map<String, Tree> substitutions) {
    for (Map.Entry<String, Tree> entry : substitutions.entrySet()) {
      if (!entry.getKey().startsWith(SUBST_TARGET_PREFIX)) {
        throw new IllegalArgumentException();
      }
    }
    this.substitutions = Map.copyOf(substitutions);
  }

  private @Nullable String nameIfSubstitutionTarget(MethodInvocationTree invocation) {
    var typeArgs = invocation.typeArguments();
    return (typeArgs == null || typeArgs.isEmpty())
      && invocation.arguments().isEmpty()
      && invocation.methodSelect() instanceof IdentifierTree idTree
      && idTree.name().startsWith(SUBST_TARGET_PREFIX)
      ? idTree.name()
      : null;
  }

  @Override
  public void visitMethodInvocation(MethodInvocationTree tree) {
    var name = nameIfSubstitutionTarget(tree);
    if (name == null) {
      super.visitMethodInvocation(tree);
    } else {
      pushResult(substitutions.get(name));
    }
  }

}
