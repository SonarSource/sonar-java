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
package org.sonar.java.checks;

import com.google.common.annotations.VisibleForTesting;
import org.sonar.api.utils.WildcardPattern;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.RspecKey;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Rule(key = "MaximumInheritanceDepth")
@RspecKey("S110")
public class DITCheck extends BaseTreeVisitor implements JavaFileScanner {

  public static final int DEFAULT_MAX = 5;

  private JavaFileScannerContext context;

  @RuleProperty(
      key = "max",
      description = "Maximum depth of the inheritance tree. (Number)",
      defaultValue = "" + DEFAULT_MAX)
  private Integer max = DEFAULT_MAX;

  @RuleProperty(
    key = "filteredClasses",
    description = "Classes to be filtered out of the count of inheritance. Ex : java.fwk.AbstractFwkClass, java.fwkPackage.*",
    defaultValue = "")
  private String filteredClasses = "";

  private List<WildcardPattern> filteredPatterns;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    if (context.getSemanticModel() != null) {
      scan(context.getTree());
    }
  }

  @Override
  public void visitClass(ClassTree tree) {
    if (!isBodyOfEnumConstantTree(tree)) {
      Type superClass = tree.symbol().superClass();
      int dit = 0;
      while (superClass != null) {
        String fullyQualifiedName = superClass.fullyQualifiedName();
        if (getPatterns().stream().anyMatch(wp -> wp.match(fullyQualifiedName))) {
          break;
        }
        dit++;
        superClass = superClass.symbol().superClass();
      }
      if (dit > max) {
        Tree reportTree = tree.simpleName();
        if (tree.parent().is(Tree.Kind.NEW_CLASS)) {
          reportTree = ((NewClassTree) tree.parent()).newKeyword();
        }
        context.reportIssue(this, reportTree, "This class has " + dit + " parents which is greater than " + max + " authorized.",
          new ArrayList<>(), dit - max);
      }
    }
    super.visitClass(tree);
  }

  private static boolean isBodyOfEnumConstantTree(ClassTree tree) {
    Tree parent = tree.parent();
    return parent.is(Tree.Kind.NEW_CLASS) && parent.parent().is(Tree.Kind.ENUM_CONSTANT);
  }

  @VisibleForTesting
  void setMax(int max) {
    this.max = max;
  }

  private List<WildcardPattern> getPatterns() {
    if (filteredPatterns == null) {
      filteredPatterns = Arrays.asList(PatternUtils.createPatterns(filteredClasses));
    }
    return filteredPatterns;
  }

  public void setFilteredClasses(String filteredClasses) {
    this.filteredClasses = filteredClasses;
  }
}
