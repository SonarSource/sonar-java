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

import com.google.common.collect.ImmutableList;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.resolve.JavaSymbol;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Rule(
  key = "S1939",
  name = "Object should not be extended",
  tags = {"clumsy"},
  priority = Priority.MINOR)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.UNDERSTANDABILITY)
@SqaleConstantRemediation("1min")
public class UselessExtendsCheck extends SubscriptionBaseVisitor implements JavaFileScanner {

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    TypeTree superClass = classTree.superClass();
    if (superClass != null && superClass.symbolType().is("java.lang.Object")) {
      super.addIssue(superClass, "\"Object\" should not be explicitly extended.");
    }
    Set<Symbol.TypeSymbol> interfaces = new HashSet<Symbol.TypeSymbol>();
    for (TypeTree superInterface : classTree.superInterfaces()) {
      Symbol.TypeSymbol interfaceSymbol = superInterface.symbolType().symbol();
      String interfaceName = ((JavaSymbol.TypeJavaSymbol) interfaceSymbol).getFullyQualifiedName();
      if (interfaces.contains(interfaceSymbol)) {
        super.addIssue(superInterface, String.format("\"%s\" is listed multiple times.", interfaceName));
      } else {
        checkExtending(classTree, interfaceSymbol, interfaceName);
      }
      interfaces.add(interfaceSymbol);
    }
  }

  private void checkExtending(ClassTree classTree, Symbol.TypeSymbol currentInterfaceSymbol, String currentInterfaceName) {
    for (TypeTree superInterface : classTree.superInterfaces()) {
      if (((JavaSymbol.TypeJavaSymbol) currentInterfaceSymbol).superTypes().contains(superInterface.symbolType())) {
        String interfaceName = ((JavaSymbol.TypeJavaSymbol) superInterface.symbolType().symbol()).getFullyQualifiedName();
        super.addIssue(superInterface, String.format("\"%s\" is an \"%s\" so \"%s\" can be removed from the extension list.",
          currentInterfaceName, interfaceName, interfaceName));
      }
    }
  }

}
