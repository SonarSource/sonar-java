/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import org.apache.commons.lang.BooleanUtils;
import org.sonar.check.Rule;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.List;
import java.util.Optional;

@Rule(key = "S1845")
public class MembersDifferOnlyByCapitalizationCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.CLASS, Tree.Kind.INTERFACE, Tree.Kind.ENUM);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    ClassTree classTree = (ClassTree) tree;
    List<Symbol> allMembers = retrieveMembers(classTree.symbol());
    Multimap<String, Symbol> membersByName = sortByName(allMembers);
    for (Tree member : classTree.members()) {
      if (member.is(Tree.Kind.METHOD)) {
        MethodTree methodTree = (MethodTree) member;
        checkForIssue(methodTree.symbol(), methodTree.simpleName(), membersByName);
      } else if (member.is(Tree.Kind.VARIABLE)) {
        VariableTree variableTree = (VariableTree) member;
        checkForIssue(variableTree.symbol(), variableTree.simpleName(), membersByName);
      }
    }
  }

  private void checkForIssue(Symbol symbol, IdentifierTree reportTree, Multimap<String, Symbol> membersByName) {
    String name = symbol.name();
    for (String knownMemberName : membersByName.keySet()) {
      if (name.equalsIgnoreCase(knownMemberName)) {
        Optional<Symbol> conflictingMember = membersByName.get(knownMemberName).stream()
          .filter(knownMemberSymbol -> !symbol.equals(knownMemberSymbol) && isValidIssueLocation(symbol, knownMemberSymbol) && isInvalidMember(symbol, knownMemberSymbol))
          .findFirst();
        if (conflictingMember.isPresent()) {
          Symbol conflictingSymbol = conflictingMember.get();
          reportIssue(reportTree,
            "Rename "
              + getSymbolTypeName(symbol) + " \"" + name + "\" "
              + "to prevent any misunderstanding/clash with "
              + getSymbolTypeName(conflictingSymbol) + " \"" + knownMemberName + "\""
              + getDefinitionPlace(symbol, conflictingSymbol) + ".");
        }
      }
    }
  }

  private static boolean isOverriding(Symbol symbol) {
    if (symbol.isMethodSymbol()) {
      MethodTreeImpl methodDeclaration = (MethodTreeImpl) symbol.declaration();
      return methodDeclaration != null && BooleanUtils.isTrue(methodDeclaration.isOverriding());
    }
    return false;
  }

  private static boolean isInvalidMember(Symbol currentMember, Symbol knownMember) {
    if (!isOverriding(currentMember)) {
      return differentTypes(currentMember, knownMember) ? sameVisibilityNotPrivate(currentMember, knownMember) : !sameName(currentMember, knownMember);
    }
    return false;
  }

  private static boolean isValidIssueLocation(Symbol currentMember, Symbol knownMember) {
    return !sameOwner(currentMember, knownMember) || isOverriding(knownMember) || getDeclarationLine(currentMember) > getDeclarationLine(knownMember);
  }

  private static boolean sameVisibilityNotPrivate(Symbol s1, Symbol s2) {
    return bothPublic(s1, s2) || bothProtected(s1, s2) || bothPackageVisibility(s1, s2);
  }

  private static boolean bothPackageVisibility(Symbol s1, Symbol s2) {
    return s1.isPackageVisibility() && s2.isPackageVisibility();
  }

  private static boolean bothProtected(Symbol s1, Symbol s2) {
    return s1.isProtected() && s2.isProtected();
  }

  private static boolean bothPublic(Symbol s1, Symbol s2) {
    return s1.isPublic() && s2.isPublic();
  }

  private static boolean sameOwner(Symbol currentMember, Symbol knownMember) {
    return currentMember.owner().equals(knownMember.owner());
  }

  private static boolean sameName(Symbol currentMember, Symbol knownMember) {
    return currentMember.name().equals(knownMember.name());
  }

  private static boolean differentTypes(Symbol s1, Symbol s2) {
    return variableAndMethod(s1, s2) || variableAndMethod(s2, s1);
  }

  private static boolean variableAndMethod(Symbol s1, Symbol s2) {
    return s1.isVariableSymbol() && s2.isMethodSymbol();
  }

  private static String getDefinitionPlace(Symbol symbol, Symbol knownMemberSymbol) {
    if (sameOwner(symbol, knownMemberSymbol)) {
      int declarationLine = getDeclarationLine(knownMemberSymbol);
      if (declarationLine == -1) {
        return "";
      }
      return " defined on line " + declarationLine;
    }
    return " defined in " + (knownMemberSymbol.owner().isInterface() ? "interface" : "superclass") + " \"" + knownMemberSymbol.owner().type().fullyQualifiedName() + "\"";
  }

  private static int getDeclarationLine(Symbol symbol) {
    if (symbol.declaration() == null) {
      return -1;
    }
    if (symbol.isVariableSymbol()) {
      return ((Symbol.VariableSymbol) symbol).declaration().simpleName().identifierToken().line();
    }
    return ((Symbol.MethodSymbol) symbol).declaration().simpleName().identifierToken().line();
  }

  private static String getSymbolTypeName(Symbol symbol) {
    return symbol.isMethodSymbol() ? "method" : "field";
  }

  private static Multimap<String, Symbol> sortByName(List<Symbol> members) {
    Multimap<String, Symbol> membersByName = LinkedListMultimap.create();
    for (Symbol member : members) {
      membersByName.put(member.name(), member);
    }
    return membersByName;
  }

  private static List<Symbol> retrieveMembers(Symbol.TypeSymbol classSymbol) {
    List<Symbol> results = Lists.newLinkedList();
    results.addAll(extractMembers(classSymbol, false));

    for (Type parentInterface : classSymbol.interfaces()) {
      results.addAll(extractMembers(parentInterface.symbol(), true));
    }
    Type superClass = classSymbol.superClass();
    if (superClass != null) {
      results.addAll(extractMembers(superClass.symbol(), true));
    }

    return results;
  }

  private static List<Symbol> extractMembers(Symbol.TypeSymbol classSymbol, boolean ignorePrivate) {
    List<Symbol> results = Lists.newLinkedList();
    for (Symbol symbol : classSymbol.memberSymbols()) {
      if ((isVariableToExtract(symbol) || isMethodToExtract(symbol)) && !(symbol.isPrivate() && ignorePrivate)) {
        results.add(symbol);
      }
    }
    return results;
  }

  private static boolean isVariableToExtract(Symbol symbol) {
    String name = symbol.name();
    return !symbol.isEnum() && symbol.isVariableSymbol() && !"this".equals(name) && !"super".equals(name);
  }

  private static boolean isMethodToExtract(Symbol symbol) {
    return symbol.isMethodSymbol() && !"<init>".equals(symbol.name());
  }
}
