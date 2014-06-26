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

import com.google.common.collect.Lists;
import org.sonar.api.rule.RuleKey;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.java.resolve.Symbol;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.Collection;

@Rule(
    key = FieldNameMatchingTypeNameCheck.RULE_KEY,
    priority = Priority.MAJOR,
    tags = {"brain-overload"})
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class FieldNameMatchingTypeNameCheck extends BaseTreeVisitor implements JavaFileScanner {

  public static final String RULE_KEY = "S1700";
  private final RuleKey ruleKey = RuleKey.of(CheckList.REPOSITORY_KEY, RULE_KEY);

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
    if (tree.simpleName() != null) {
      Symbol.TypeSymbol classSymbol = ((ClassTreeImpl) tree).getSymbol();
      Collection<Symbol> members = classSymbol.members().scopeSymbols();
      for (Symbol sym : members) {
        if (sym.isKind(Symbol.VAR) && !staticFieldSameType(classSymbol, sym)) {
          //Exclude static fields of the same type.
          fields.add(semanticModel.getTree(sym));
        }
      }
      currentClassName = tree.simpleName().name();
    }
    super.visitClass(tree);
    currentClassName = "";
    fields.clear();
  }

  private boolean staticFieldSameType(Symbol.TypeSymbol classSymbol, Symbol sym) {
    return sym.getType() != null && sym.getType().equals(classSymbol.getType()) && sym.isStatic();
  }

  @Override
  public void visitVariable(VariableTree tree) {
    String name = tree.simpleName().name();
    if (fields.contains(tree) && currentClassName.equalsIgnoreCase(name)) {
      context.addIssue(tree, ruleKey, "Rename field \"" + name + "\"");
    }
    super.visitVariable(tree);
  }
}
