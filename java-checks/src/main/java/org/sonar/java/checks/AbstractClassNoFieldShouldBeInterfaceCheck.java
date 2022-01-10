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
package org.sonar.java.checks;

import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.JavaVersionAwareVisitor;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S1610")
public class AbstractClassNoFieldShouldBeInterfaceCheck extends IssuableSubscriptionVisitor implements JavaVersionAwareVisitor {

  private static final String IMMUTABLE_ANNOTATION = "org.immutables.value.Value$Immutable";

  private int javaVersionAsInt;

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava8Compatible();
  }

  @Override
  public void setContext(JavaFileScannerContext context) {
    javaVersionAsInt = context.getJavaVersion().asInt();
    super.setContext(context);
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    if (classTree.superClass() == null
      && classIsAbstract(classTree)
      && classHasNoFieldAndProtectedMethod(classTree)
      && classHasNoImmutableAnnotation(classTree)
      && supportPrivateMethod(classTree)) {
      IdentifierTree simpleName = classTree.simpleName();
      reportIssue(
        simpleName,
        "Convert the abstract class \"" + simpleName.name() + "\" into an interface." + context.getJavaVersion().java8CompatibilityMessage());
    }
  }

  private static boolean classIsAbstract(ClassTree tree) {
    return ModifiersUtils.hasModifier(tree.modifiers(), Modifier.ABSTRACT);
  }

  /**
   * Java 9 introduce private method in interfaces.
   * Before Java 9, an abstract class with private methods can not be turned into an interface.
   */
  private boolean supportPrivateMethod(ClassTree tree) {
    return !hasPrivateMethod(tree) || javaVersionAsInt >= 9;
  }

  private static boolean hasPrivateMethod(ClassTree tree) {
    return tree.members().stream()
      .filter(member -> member.is(Tree.Kind.METHOD))
      .anyMatch(member -> ModifiersUtils.hasModifier(((MethodTree) member).modifiers(), Modifier.PRIVATE));
  }

  private static boolean classHasNoFieldAndProtectedMethod(ClassTree tree) {
    return tree.members().stream()
      .noneMatch(member -> member.is(Tree.Kind.VARIABLE) || (member.is(Tree.Kind.METHOD) && isProtectedOrOverriding((MethodTree) member)));
  }

  private static boolean isProtectedOrOverriding(MethodTree member) {
    return ModifiersUtils.hasModifier(member.modifiers(), Modifier.PROTECTED) || !Boolean.FALSE.equals(member.isOverriding());
  }

  private static boolean classHasNoImmutableAnnotation(ClassTree tree) {
    List<SymbolMetadata.AnnotationInstance> annotations = tree.symbol().metadata().annotations();
    if (annotations.isEmpty()) {
      return true;
    }
    for (SymbolMetadata.AnnotationInstance annotation : annotations) {
      // If semantic is incomplete, we may not be able to resolve the full name on the class annotation.
      // To reduce FPs, we test for a match on the end of the annotation names
      Symbol symbol = annotation.symbol();
      if (symbol.isUnknown() || symbol.type().is(IMMUTABLE_ANNOTATION)) {
        return false;
      }
    }
    return true;
  }
}
