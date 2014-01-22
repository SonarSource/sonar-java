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

import org.sonar.api.rule.RuleKey;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.regex.Pattern;

@Rule(
  key = BadConstantName_S00115_Check.RULE_KEY,
  priority = Priority.MAJOR)
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class BadConstantName_S00115_Check extends BaseTreeVisitor implements JavaFileScanner {

  public static final String RULE_KEY = "S00115";
  private final RuleKey ruleKey = RuleKey.of(CheckList.REPOSITORY_KEY, RULE_KEY);

  private static final String DEFAULT_FORMAT = "^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$";
  private static final boolean DEFAULT_IMMUTABLE = false;

  @RuleProperty(
    key = "format",
    defaultValue = "" + DEFAULT_FORMAT)
  public String format = DEFAULT_FORMAT;

  @RuleProperty(
    key = "immutable",
    defaultValue = "" + DEFAULT_IMMUTABLE)
  public boolean immutable = DEFAULT_IMMUTABLE;

  private Pattern pattern = null;
  private JavaFileScannerContext context;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    if (pattern == null) {
      pattern = Pattern.compile(format, Pattern.DOTALL);
    }
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitClass(ClassTree tree) {
    for (Tree member : tree.members()) {
      if (member.is(Tree.Kind.VARIABLE)) {
        VariableTree variableTree = (VariableTree) member;
        if (tree.is(Tree.Kind.INTERFACE) || tree.is(Tree.Kind.ANNOTATION_TYPE)) {
          checkName(variableTree);
        } else if (isStaticFinal(variableTree)) {
          checkName(variableTree);
        }
      } else if (member.is(Tree.Kind.ENUM_CONSTANT)) {
        checkName((VariableTree) member);
      }
    }
    super.visitClass(tree);
  }

  private void checkName(VariableTree variableTree) {
    if (!SerializableContract.SERIAL_VERSION_UID_FIELD.equals(variableTree.simpleName())) {
      if (!pattern.matcher(variableTree.simpleName()).matches()) {
        if (!immutable || ImmutableContract.isImmutable(variableTree)) {
          context.addIssue(variableTree, ruleKey, "Rename this constant name to match the regular expression '" + format + "'.");
        }
      }
    }
  }


  private boolean isStaticFinal(VariableTree variableTree) {
    boolean isStatic = false;
    boolean isFinal = false;
    for (Modifier modifier : variableTree.modifiers().modifiers()) {
      if (modifier == Modifier.STATIC) {
        isStatic = true;
      }
      if (modifier == Modifier.FINAL) {
        isFinal = true;
      }
    }
    return isStatic && isFinal;
  }

}
