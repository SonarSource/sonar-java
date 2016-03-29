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

import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.resolve.JavaSymbol;
import org.sonar.java.resolve.JavaType;
import org.sonar.java.tag.Tag;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol.TypeSymbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ListTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Rule(
  key = "S2640",
  name = "Interfaces should not be redundantly implemented",
  priority = Priority.MAJOR,
  tags = {Tag.CLUMSY})
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.UNDERSTANDABILITY)
@SqaleConstantRemediation("5min")
public class RedundantInterfaceCheck extends IssuableSubscriptionVisitor {

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
    ListTree<TypeTree> superInterfaces = classTree.superInterfaces();
    if (superInterfaces.isEmpty()) {
      return;
    }

    Set<JavaType.ClassJavaType> superTypes = ((JavaSymbol.TypeJavaSymbol) classTree.symbol()).superTypes();
    List<Type> superInterfacesTypes = getTypes(superInterfaces);
    for (TypeTree superInterface : superInterfaces) {
      if (!superInterface.symbolType().isUnknown()) {
        checkRedundancy(superInterface, superInterfacesTypes, superTypes);
      }
    }
  }

  private static List<Type> getTypes(ListTree<TypeTree> superInterfaces) {
    List<Type> types = new ArrayList<>(superInterfaces.size());
    for (TypeTree superInterface : superInterfaces) {
      types.add(superInterface.symbolType());
    }
    return types;
  }

  private void checkRedundancy(TypeTree currentInterface, List<Type> superInterfacesTypes, Set<JavaType.ClassJavaType> superTypes) {
    Type interfaceType = currentInterface.symbolType();
    for (JavaType.ClassJavaType superType : superTypes) {
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

}
