/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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
package org.sonar.java.checks.design;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.checks.helpers.ClassPatternsUtils;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.expression.IdentifierTreeImpl;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ImportTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S6539")
public class ClassImportCouplingCheck extends AbstractCouplingChecker {

  private static final int COUPLING_THRESHOLD = 20;
  @RuleProperty(
    key = "couplingThreshold",
    description = "Maximum number of classes a single class is allowed to depend upon. This value is an experimental value.",
    defaultValue = "" + COUPLING_THRESHOLD)
  public int couplingThreshold = COUPLING_THRESHOLD;
  private String packageName;
  private Set<String> imports;
  private Set<String> filteredImports;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    super.scanFile(context);
    imports = null;
    filteredImports = null;
  }

  @Override
  public void visitClass(ClassTree tree) {
    // if class is utility or private inner class -> don't report
    if (ClassPatternsUtils.isUtilityClass(tree) || ClassPatternsUtils.isPrivateInnerClass(tree)) {
      return;
    }

    if (tree.is(Tree.Kind.CLASS) && tree.simpleName() != null) {
      nesting.push(types);
      types = new HashSet<>();
    }
    CompilationUnitTree compilationUnitTree = (CompilationUnitTree) ExpressionUtils.getParentOfType(tree, Tree.Kind.COMPILATION_UNIT);
    packageName = JavaTree.PackageDeclarationTreeImpl.packageNameAsString(compilationUnitTree.packageDeclaration());

    if (imports == null) {
      imports = compilationUnitTree.imports().stream()
        .filter(i -> !i.is(Tree.Kind.EMPTY_STATEMENT))
        .map(ImportTree.class::cast)
        .map(ImportTree::qualifiedIdentifier)
        .map(i -> ExpressionsHelper.concatenate(((ExpressionTree) i)))
        .collect(Collectors.toSet());

      String fileProjectName = context.getProject().key();
      filteredImports = imports.stream()
        .filter(i -> i.startsWith(fileProjectName))
        .collect(Collectors.toSet());
    }

    checkTypes(tree.superClass(), types);
    checkTypes(tree.superInterfaces());
    super.visitClass(tree);

    if (tree.is(Tree.Kind.CLASS) && tree.simpleName() != null) {
      filteredImports.addAll(types);
      int size = filteredImports.size();
      if (size > couplingThreshold) {
        context.reportIssue(this, tree.simpleName(), "Split this class into smaller and more specialized ones to reduce its dependencies on other classes from " +
          size + " to the maximum authorized " + couplingThreshold + " or less.");
      }
      types = nesting.pop();
    }
  }

  @Override
  public void checkTypes(@Nullable Tree type, Set<String> types) {
    if (type == null || types == null) {
      return;
    }
    if (type.is(Tree.Kind.IDENTIFIER)) {
      IdentifierTreeImpl identifierTree = (IdentifierTreeImpl) type;
      String fullyQualifiedName = identifierTree.symbolType().fullyQualifiedName();
      if (fullyQualifiedName.contains(packageName)) {
        types.add(fullyQualifiedName);
      }
    } else if (type.is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree memberSelect = (MemberSelectExpressionTree) type;
      String name = ExpressionsHelper.concatenate(memberSelect);
      if (name.contains(packageName)) {
        types.add(name);
      }
    }
  }

}
