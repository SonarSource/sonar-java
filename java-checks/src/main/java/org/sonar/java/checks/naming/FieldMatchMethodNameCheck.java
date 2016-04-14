/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.java.checks.naming;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Rule(key = "S1701")
public class FieldMatchMethodNameCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.CLASS, Tree.Kind.INTERFACE, Tree.Kind.ENUM, Tree.Kind.ANNOTATION_TYPE);
  }

  @Override
  public void visitNode(Tree tree) {
    Symbol.TypeSymbol classSymbol = ((ClassTree) tree).symbol();
    if (classSymbol != null) {
      Map<String, Symbol> indexSymbol = Maps.newHashMap();
      Multiset<String> fields = HashMultiset.create();
      Map<String, String> fieldsOriginal = Maps.newHashMap();
      Set<String> methodNames = Sets.newHashSet();
      Collection<Symbol> symbols = classSymbol.memberSymbols();
      for (Symbol sym : symbols) {
        String symName = sym.name().toLowerCase(Locale.US);
        if (sym.isVariableSymbol()) {
          indexSymbol.put(symName, sym);
          fields.add(symName);
          fieldsOriginal.put(symName, sym.name());
        }
        if (sym.isMethodSymbol()) {
          methodNames.add(symName);
        }
      }
      fields.addAll(methodNames);
      for (Multiset.Entry<String> entry : fields.entrySet()) {
        if (entry.getCount() > 1) {
          Tree field = indexSymbol.get(entry.getElement()).declaration();
          if (field != null) {
            reportIssue(((VariableTree) field).simpleName(), "Rename the \"" + fieldsOriginal.get(entry.getElement()) + "\" member.");
          }
        }
      }
    }
  }
}
