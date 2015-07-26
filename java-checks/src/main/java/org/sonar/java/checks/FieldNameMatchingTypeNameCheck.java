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

import com.google.common.collect.Lists;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.Collection;

@Rule(
  key = "S1700",
  name = "A field should not duplicate the name of its containing class",
  tags = {"brain-overload"},
  priority = Priority.MAJOR)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.UNDERSTANDABILITY)
@SqaleConstantRemediation("10min")
public class FieldNameMatchingTypeNameCheck extends BaseTreeVisitor implements JavaFileScanner {

  private JavaFileScannerContext context;
  private SemanticModel semanticModel;
  private String currentClassName;
  private Collection<Tree> fields;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    this.semanticModel = (SemanticModel) context.getSemanticModel();
    currentClassName = "";
    fields = Lists.newArrayList();
    if (semanticModel != null) {
      scan(context.getTree());
    }
  }

  @Override
  public void visitClass(ClassTree tree) {
    IdentifierTree simpleName = tree.simpleName();
    if (simpleName != null) {
      Symbol.TypeSymbol classSymbol = tree.symbol();
      Collection<Symbol> members = classSymbol.memberSymbols();
      for (Symbol sym : members) {
        if (sym.isVariableSymbol() && !staticFieldSameType(classSymbol, sym)) {
          //Exclude static fields of the same type.
          fields.add(((Symbol.VariableSymbol) sym).declaration());
        }
      }
      currentClassName = simpleName.name();
    }
    super.visitClass(tree);
    currentClassName = "";
    fields.clear();
  }

  private boolean staticFieldSameType(Symbol classSymbol, Symbol sym) {
    return sym.type().equals(classSymbol.type()) && sym.isStatic();
  }

  @Override
  public void visitVariable(VariableTree tree) {
    String name = tree.simpleName().name();
    if (fields.contains(tree) && currentClassName.equalsIgnoreCase(name)) {
      context.addIssue(tree, this, "Rename field \"" + name + "\"");
    }
    super.visitVariable(tree);
  }
}
