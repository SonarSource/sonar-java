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

import com.google.common.annotations.VisibleForTesting;
import org.sonar.api.rule.RuleKey;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.java.resolve.Symbol;
import org.sonar.java.resolve.Type;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;

@Rule(key = DITCheck.RULE_KEY, priority = Priority.MAJOR)
public class DITCheck extends BaseTreeVisitor implements JavaFileScanner {

  public static final String RULE_KEY = "MaximumInheritanceDepth";
  private final RuleKey ruleKey = RuleKey.of(CheckList.REPOSITORY_KEY, RULE_KEY);

  public static final int DEFAULT_MAX = 5;

  private JavaFileScannerContext context;

  @RuleProperty(defaultValue = "" + DEFAULT_MAX)
  private Integer max = DEFAULT_MAX;


  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    if (context.getSemanticModel() != null) {
      scan(context.getTree());
    }
  }

  @Override
  public void visitClass(ClassTree tree) {
    Symbol.TypeSymbol typeSymbol = ((ClassTreeImpl) tree).getSymbol();
    int dit = 0;
    while(typeSymbol.getSuperclass() != null ){
      dit++;
      typeSymbol = ((Type.ClassType) typeSymbol.getSuperclass()).getSymbol();
    }
    if(dit > max) {
      context.addIssue(tree, ruleKey, "This class has "+dit+" parents which is greater than "+max+" authorized.");
    }
    super.visitClass(tree);
  }

  @VisibleForTesting
  void setMax(int max) {
    this.max = max;
  }

  @Override
  public String toString() {
    return RULE_KEY + " rule";
  }

}
