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
package org.sonar.java.checks;

import com.google.common.collect.ImmutableList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.java.cfg.CFG;
import org.sonar.java.cfg.LiveVariables;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Symbol.TypeSymbol;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.sonar.java.se.ProgramState.isField;

@Rule(key = "S1450")
public class PrivateFieldUsedLocallyCheck extends IssuableSubscriptionVisitor {

  private static final String MESSAGE = "Remove the \"%s\" field and declare it as a local variable in the relevant methods.";

  // set of fields which are live by entering at least one method
  private Set<Symbol> fieldsLiveInMethodEntry = new HashSet<>();

  private Set<Symbol> fieldsReadOnAnotherInstance = new HashSet<>();


  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    TypeSymbol classSymbol = ((ClassTree) tree).symbol();
    initFieldsMetaData(classSymbol);

    classSymbol.memberSymbols().stream()
      .filter(PrivateFieldUsedLocallyCheck::isPrivateField)
      .filter(memberSymbol -> !memberSymbol.usages().isEmpty())
      .filter(memberSymbol -> !isUsedOutsideMethods(memberSymbol, classSymbol))
      .forEach(this::checkPrivateField);

    fieldsReadOnAnotherInstance.clear();
    fieldsLiveInMethodEntry.clear();
  }

  private static boolean isPrivateField(Symbol memberSymbol) {
    return memberSymbol.isPrivate() && memberSymbol.isVariableSymbol();
  }

  private static boolean isUsedOutsideMethods(Symbol privateFieldSymbol, TypeSymbol classSymbol) {
    for (IdentifierTree usageIdentifier : privateFieldSymbol.usages()) {
      Tree containingClassOrMethod = containingClassOrMethod(usageIdentifier);

      if (containingClassOrMethod.is(Kind.CLASS)
        || (containingClassOrMethod.is(Kind.METHOD) && !((MethodTree) containingClassOrMethod).symbol().owner().equals(classSymbol))) {
        return true;
      }
    }

    return false;
  }

  private void checkPrivateField(Symbol privateFieldSymbol) {
    if (!fieldsReadOnAnotherInstance.contains(privateFieldSymbol) && !fieldsLiveInMethodEntry.contains(privateFieldSymbol)) {
      IdentifierTree declarationIdentifier = ((VariableTree) privateFieldSymbol.declaration()).simpleName();
      String message = String.format(MESSAGE, privateFieldSymbol.name());
      reportIssue(declarationIdentifier, message);
    }
  }

  private static Tree containingClassOrMethod(IdentifierTree usageIdentifier) {
    Tree parent = usageIdentifier;
    do {
      parent = parent.parent();
    } while (!parent.is(Kind.METHOD, Kind.CLASS));

    return parent;
  }

  private void initFieldsMetaData(TypeSymbol classSymbol) {
    fieldsReadOnAnotherInstance = FieldsReadOnAnotherInstanceVisitor.getFrom(classSymbol);

    for (Symbol memberSymbol : classSymbol.memberSymbols()) {

      if (memberSymbol.isMethodSymbol() && memberSymbol.declaration() != null) {
        MethodTree methodTree = (MethodTree) memberSymbol.declaration();

        if (methodTree.block() != null) {
          CFG cfg = CFG.build(methodTree);
          LiveVariables liveVariables = LiveVariables.analyzeWithFields(cfg);
          fieldsLiveInMethodEntry.addAll(liveVariables.getIn(cfg.entry()));
        }
      }
    }
  }

  private static class FieldsReadOnAnotherInstanceVisitor extends BaseTreeVisitor {

    private Set<Symbol> fieldsReadOnAnotherInstance = new HashSet<>();

    static Set<Symbol> getFrom(TypeSymbol classSymbol) {
      FieldsReadOnAnotherInstanceVisitor fieldsReadOnAnotherInstanceVisitor = new FieldsReadOnAnotherInstanceVisitor();
      fieldsReadOnAnotherInstanceVisitor.scan(classSymbol.declaration());
      return fieldsReadOnAnotherInstanceVisitor.fieldsReadOnAnotherInstance;
    }

    @Override
    public void visitMemberSelectExpression(MemberSelectExpressionTree tree) {
      Symbol symbol = tree.identifier().symbol();
      if (isField(symbol) && !symbol.isStatic()) {

        if (tree.expression().is(Kind.IDENTIFIER)) {
          String objectName = ((IdentifierTree) tree.expression()).name();

          if (!"this".equals(objectName)) {
            fieldsReadOnAnotherInstance.add(symbol);
          }

        } else {
          fieldsReadOnAnotherInstance.add(symbol);
        }
      }

      super.visitMemberSelectExpression(tree);
    }
  }

}
