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

import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public abstract class AbstractBadFieldNameChecker extends IssuableSubscriptionVisitor {

  protected static final String DEFAULT_FORMAT_KEY = "format";

  protected static final String DEFAULT_FORMAT_DESCRIPTION = "Regular expression used to check the field names against.";

  protected static final String DEFAULT_FORMAT_VALUE = "^[a-z][a-zA-Z0-9]*$";

  private Pattern pattern = null;

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.CLASS, Tree.Kind.ENUM);
  }

  @Override
  public void setContext(JavaFileScannerContext context) {
    if (pattern == null) {
      pattern = Pattern.compile(getFormat(), Pattern.DOTALL);
    }
    super.setContext(context);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    for (Tree member : classTree.members()) {
      if (member.is(Tree.Kind.VARIABLE)) {
        VariableTree field = (VariableTree) member;
        if (isFieldModifierConcernedByRule(field.modifiers()) && !pattern.matcher(field.simpleName().name()).matches()) {
          reportIssue(field.simpleName(), String.format("Rename this field \"%s\" to match the regular expression '%s'.", field.simpleName().name(), getFormat()));
        }
      }
    }
  }

  protected abstract String getFormat();

  protected abstract boolean isFieldModifierConcernedByRule(ModifiersTree modifier);

}
