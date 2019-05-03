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

import org.sonar.check.Rule;
import org.sonar.java.model.SyntacticEquivalence;
import org.sonar.java.resolve.ClassJavaType;
import org.sonar.java.resolve.JavaSymbol;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol.TypeSymbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.ListTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.ParameterizedTypeTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Rule(key = "S1939")
public class UselessExtendsCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.CLASS, Tree.Kind.INTERFACE, Tree.Kind.ENUM);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    ClassTree classTree = (ClassTree) tree;
    checkExtendsObject(classTree);

    ListTree<TypeTree> superInterfaces = classTree.superInterfaces();
    if (superInterfaces.isEmpty()) {
      return;
    }

    Set<ClassJavaType> superTypes = ((JavaSymbol.TypeJavaSymbol) classTree.symbol()).superTypes();
    List<Type> superInterfacesTypes = getTypes(superInterfaces);
    Set<String> reportedNames = new HashSet<>();
    for (TypeTree superInterface : superInterfaces) {
      String superInterfaceName = extractInterfaceName(superInterface);
      if (isDuplicate(superInterfaces, superInterface) && !reportedNames.add(superInterfaceName)) {
        // add an issue on a duplicated interface the second time it is encountered
        reportIssue(superInterface, "\"" + superInterfaceName + "\" is listed multiple times.");
      }
      if (!superInterface.symbolType().isUnknown()) {
        checkRedundancy(superInterface, superInterfacesTypes, superTypes);
      }
    }
  }

  private void checkExtendsObject(ClassTree classTree) {
    TypeTree superClassTree = classTree.superClass();
    if (superClassTree != null && superClassTree.symbolType().is("java.lang.Object")) {
      reportIssue(superClassTree, "\"Object\" should not be explicitly extended.");
    }
  }

  private static boolean isDuplicate(ListTree<TypeTree> superInterfaces, TypeTree currentInterfaceTree) {
    for (TypeTree superInterfaceTree : superInterfaces) {
      if (!currentInterfaceTree.equals(superInterfaceTree) && SyntacticEquivalence.areEquivalent(currentInterfaceTree, superInterfaceTree)) {
        return true;
      }
    }
    return false;
  }

  private static List<Type> getTypes(ListTree<TypeTree> superInterfaces) {
    List<Type> types = new ArrayList<>(superInterfaces.size());
    for (TypeTree superInterface : superInterfaces) {
      types.add(superInterface.symbolType());
    }
    return types;
  }

  private void checkRedundancy(TypeTree currentInterface, List<Type> superInterfacesTypes, Set<ClassJavaType> superTypes) {
    Type interfaceType = currentInterface.symbolType();
    for (ClassJavaType superType : superTypes) {
      TypeSymbol superTypeSymbol = superType.symbol();
      if (superTypeSymbol.interfaces().contains(interfaceType)) {
        String typeOfParentMsg = "implemented by a super class";
        if (superTypeSymbol.isInterface() && superInterfacesTypes.contains(superType)) {
          typeOfParentMsg = "already extended by \"" + superTypeSymbol.name() + "\"";
        }
        reportIssue(currentInterface, "\"" + interfaceType.name() + "\" is " + typeOfParentMsg + "; there is no need to implement it here.");
        break;
      }
    }
  }

  private static String extractInterfaceName(Tree interfaceTree) {
    if (interfaceTree.is(Tree.Kind.IDENTIFIER)) {
      return ((IdentifierTree) interfaceTree).name();
    } else if (interfaceTree.is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree mset = (MemberSelectExpressionTree) interfaceTree;
      return extractInterfaceName(mset.expression()) + "." + mset.identifier().name();
    } else {
      return extractInterfaceName(((ParameterizedTypeTree) interfaceTree).type());
    }
  }
}
