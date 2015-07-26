/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

@Rule(
  key = "MaximumInheritanceDepth",
  name = "Inheritance tree of classes should not be too deep",
  tags = {"design"},
  priority = Priority.MAJOR)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.UNDERSTANDABILITY)
@SqaleConstantRemediation("4h")
public class DITCheck extends BaseTreeVisitor implements JavaFileScanner {

  public static final int DEFAULT_MAX = 5;

  private JavaFileScannerContext context;

  @RuleProperty(
      key = "max",
      description = "Maximum depth of the inheritance tree. (Number)",
      defaultValue = "" + DEFAULT_MAX)
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
    Type superClass = tree.symbol().superClass();
    int dit = 0;
    while(superClass != null ){
      dit++;
      superClass = superClass.symbol().superClass();
    }
    if(dit > max) {
      context.addIssue(tree, this, "This class has "+dit+" parents which is greater than "+max+" authorized.");
    }
    super.visitClass(tree);
  }

  @VisibleForTesting
  void setMax(int max) {
    this.max = max;
  }

}
