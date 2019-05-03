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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.JavaTree;
import org.sonar.java.resolve.JavaSymbol;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S3398")
public class CallOuterPrivateMethodCheck extends IssuableSubscriptionVisitor {

  private MethodInvocationVisitor methodInvocationVisitor;

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.CLASS, Tree.Kind.INTERFACE);
  }

  @Override
  public void setContext(JavaFileScannerContext context) {
    methodInvocationVisitor = new MethodInvocationVisitor();
    super.setContext(context);
  }

  @Override
  public void leaveFile(JavaFileScannerContext context) {
    methodInvocationVisitor.checkUsages();
    methodInvocationVisitor = null;
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    Symbol.TypeSymbol classSymbol = classTree.symbol();
    if (isInnerClass(classSymbol)) {
      methodInvocationVisitor.setClassSymbol(classSymbol);
      classTree.accept(methodInvocationVisitor);
    }
  }

  private static boolean isInnerClass(Symbol symbol) {
    return symbol.owner().isTypeSymbol();
  }

  private class MethodInvocationVisitor extends BaseTreeVisitor {
    private final Map<Symbol.TypeSymbol, Multiset<Symbol>> usagesByInnerClass = new HashMap<>();
    private final Multimap<String, MethodInvocationTree> unknownInvocations = HashMultimap.create();
    private Symbol.TypeSymbol classSymbol;
    private Multiset<Symbol> usages;

    public void setClassSymbol(Symbol.TypeSymbol classSymbol) {
      this.classSymbol = classSymbol;
      usages = HashMultiset.create();
      usagesByInnerClass.put(classSymbol, usages);
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      Symbol symbol = tree.symbol();
      if(symbol.isUnknown()) {
        unknownInvocations.put(ExpressionUtils.methodName(tree).name(), tree);
      } else if (isPrivateMethodOfOuterClass(symbol)) {
        usages.add(symbol);
      }
      super.visitMethodInvocation(tree);
    }

    private boolean isPrivateMethodOfOuterClass(Symbol symbol) {
      return symbol.isPrivate() && symbol.owner().equals(classSymbol.owner()) && !"<init>".equals(symbol.name());
    }

    void checkUsages() {
      for (Map.Entry<Symbol.TypeSymbol, Multiset<Symbol>> usageByInnerClassEntry : usagesByInnerClass.entrySet()) {
        Multiset<Symbol> innerClassUsages = usageByInnerClassEntry.getValue();
        for (Symbol methodUsed : innerClassUsages.elementSet()) {
          boolean matchArity = unknownInvocations.get(methodUsed.name())
            .stream()
            .anyMatch(mit -> hasSameArity((Symbol.MethodSymbol) methodUsed, mit));
          // if an unknown method has same name and same arity, do not report, likely a FP.
          if (!matchArity && methodUsed.usages().size() == innerClassUsages.count(methodUsed)) {
            reportIssueOnMethod((MethodTree) methodUsed.declaration(), usageByInnerClassEntry.getKey());
          }
        }
      }
    }

    private boolean hasSameArity(Symbol.MethodSymbol methodUsed, MethodInvocationTree mit) {
      int formalArity = methodUsed.parameterTypes().size();
      int invokedArity = mit.arguments().size();
      return formalArity == invokedArity ||
        (((JavaSymbol.MethodJavaSymbol) methodUsed).isVarArgs() && invokedArity >= formalArity - 1);
    }

    private void reportIssueOnMethod(@Nullable MethodTree declaration, Symbol.TypeSymbol classSymbol) {
      if (declaration != null) {
        String message = "Move this method into ";
        if (classSymbol.name().isEmpty()) {
          message += "the anonymous class declared at line " + ((JavaTree) classSymbol.declaration()).getLine()+".";
        } else {
          message += "\"" + classSymbol.name() + "\".";
        }
        reportIssue(declaration.simpleName(), message);
      }
    }

  }
}
