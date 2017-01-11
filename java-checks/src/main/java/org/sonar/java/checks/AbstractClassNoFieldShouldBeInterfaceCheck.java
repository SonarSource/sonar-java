/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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

import com.google.common.collect.ImmutableList;
import org.sonar.check.Rule;
import org.sonar.java.JavaVersionAwareVisitor;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.List;

@Rule(key = "S1610")
public class AbstractClassNoFieldShouldBeInterfaceCheck extends IssuableSubscriptionVisitor implements JavaVersionAwareVisitor {

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava8Compatible();
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    if (classTree.superClass() == null && classIsAbstract(classTree) && classHasNoFieldAndProtectedMethod(classTree)) {
      IdentifierTree simpleName = classTree.simpleName();
      reportIssue(
        simpleName,
        "Convert the abstract class \"" + simpleName.name() + "\" into an interface." + context.getJavaVersion().java8CompatibilityMessage());
    }
  }

  private static boolean classIsAbstract(ClassTree tree) {
    return ModifiersUtils.hasModifier(tree.modifiers(), Modifier.ABSTRACT);
  }

  private static boolean classHasNoFieldAndProtectedMethod(ClassTree tree) {
    for (Tree member : tree.members()) {
      if (member.is(Tree.Kind.VARIABLE) || (member.is(Tree.Kind.METHOD) && isProtectedOrOverriding((MethodTreeImpl) member))) {
        return false;
      }
    }
    return true;
  }

  private static boolean isProtectedOrOverriding(MethodTreeImpl member) {
    return ModifiersUtils.hasModifier(member.modifiers(), Modifier.PROTECTED) || !Boolean.FALSE.equals(member.isOverriding());
  }
}
