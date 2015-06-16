/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.checks;

import java.util.regex.Pattern;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

public abstract class AbstractBadFieldNameChecker extends BaseTreeVisitor implements JavaFileScanner {

  protected static final String DEFAULT_FORMAT_KEY = "format";

  protected static final String DEFAULT_FORMAT_DESCRIPTION = "Regular expression used to check the field names against.";

  protected static final String DEFAULT_FORMAT_VALUE = "^[a-z][a-zA-Z0-9]*$";

  private Pattern pattern = null;
  private JavaFileScannerContext context;

  abstract String getFormat();

  abstract boolean isFieldModifierConcernedByRule(ModifiersTree modifier);

  @Override
  public void scanFile(JavaFileScannerContext context) {
    if (pattern == null) {
      pattern = Pattern.compile(getFormat(), Pattern.DOTALL);
    }
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitClass(ClassTree tree) {
    for (Tree member : tree.members()) {
      if ((tree.is(Tree.Kind.CLASS) || tree.is(Tree.Kind.ENUM)) && member.is(Tree.Kind.VARIABLE)) {
        VariableTree field = (VariableTree) member;
        if (isFieldModifierConcernedByRule(field.modifiers()) && !pattern.matcher(field.simpleName().name()).matches()) {
          context.addIssue(field, this, String.format("Rename this field \"%s\" to match the regular expression '%s'.", field.symbol().name(), getFormat()));
        }
      }
      scan(member);
    }
  }

}
