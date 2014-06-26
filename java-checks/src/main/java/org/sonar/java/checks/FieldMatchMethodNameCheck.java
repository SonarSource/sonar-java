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

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import org.sonar.api.rule.RuleKey;
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

import java.util.Collection;
import java.util.Map;
import java.util.Set;

@Rule(
    key = FieldMatchMethodNameCheck.RULE_KEY,
    priority = Priority.MAJOR,
    tags = {"brain-overload"})
public class FieldMatchMethodNameCheck extends BaseTreeVisitor implements JavaFileScanner {

  public static final String RULE_KEY = "S1701";
  private final RuleKey ruleKey = RuleKey.of(CheckList.REPOSITORY_KEY, RULE_KEY);

  private JavaFileScannerContext context;
  private SemanticModel semanticModel;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    this.semanticModel = (SemanticModel) context.getSemanticModel();
    if (semanticModel != null) {
      scan(context.getTree());
    }
  }

  @Override
  public void visitClass(ClassTree tree) {
    Symbol.TypeSymbol classSymbol = ((ClassTreeImpl) tree).getSymbol();
    if (classSymbol != null) {
      Map<String, Symbol> indexSymbol = Maps.newHashMap();
      Multiset<String> fields = HashMultiset.create();
      Map<String, String> fieldsOriginal = Maps.newHashMap();
      Set<String> methodNames = Sets.newHashSet();
      Collection<Symbol> symbols = classSymbol.members().scopeSymbols();
      for (Symbol sym : symbols) {
        String symName = sym.getName().toLowerCase();
        if (sym.isKind(Symbol.VAR)) {
          indexSymbol.put(symName, sym);
          fields.add(symName);
          fieldsOriginal.put(symName, sym.getName());
        }
        if (sym.isKind(Symbol.MTH)) {
          methodNames.add(symName);
        }
      }
      fields.addAll(methodNames);
      for (Multiset.Entry<String> entry : fields.entrySet()) {
        if (entry.getCount() > 1) {
          Tree field = semanticModel.getTree(indexSymbol.get(entry.getElement()));
          if(field != null) {
            context.addIssue(field, ruleKey, "Rename the \"" + fieldsOriginal.get(entry.getElement()) + "\" member.");
          }
        }
      }
    }
    super.visitClass(tree);
  }
}
