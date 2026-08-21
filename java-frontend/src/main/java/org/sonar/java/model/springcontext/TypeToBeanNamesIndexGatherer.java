/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.model.springcontext;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.sonar.java.utils.SpringUtils;
import org.sonar.plugins.java.api.ModuleScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

/**
 * Populates {@link TypeToBeanNamesIndex} by mapping every type in a bean's hierarchy
 * (concrete class, superclasses, interfaces) to the bean's name.
 */
public class TypeToBeanNamesIndexGatherer extends SpringContextModelGatherer {

  private record BeanTypeEntry(String beanName, Set<String> typeHierarchy) {}

  private final List<BeanTypeEntry> collectedEntries = new ArrayList<>();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    if (classTree.simpleName() == null) {
      return;
    }

    var meta = classTree.symbol().metadata();
    if (SpringUtils.STEREOTYPE_ANNOTATIONS.stream().anyMatch(meta::isAnnotatedWith)) {
      String beanName = SpringUtils.resolveStereotypeBeanName(meta, classTree.simpleName().name());
      collectedEntries.add(new BeanTypeEntry(beanName, collectTypeHierarchy(classTree.symbol())));

      for (MethodTree method : SpringUtils.getBeanMethods(classTree)) {
        if (method.returnType() == null) {
          continue;
        }
        String methodBeanName = SpringUtils.resolveBeanMethodName(method);
        Symbol.TypeSymbol returnTypeSymbol = method.returnType().symbolType().symbol();
        collectedEntries.add(new BeanTypeEntry(methodBeanName, collectTypeHierarchy(returnTypeSymbol)));
      }
    }
  }

  @Override
  public void gatherSpringContextData(ModuleScannerContext context, SpringContextModel springContextModel) {
    TypeToBeanNamesIndex index = springContextModel.getTypeToBeanNamesIndex();
    for (BeanTypeEntry entry : collectedEntries) {
      for (String typeFqn : entry.typeHierarchy()) {
        index.addBeanForType(typeFqn, entry.beanName());
      }
    }
  }

  private static Set<String> collectTypeHierarchy(Symbol.TypeSymbol symbol) {
    Set<String> visited = new LinkedHashSet<>();
    walkTypeHierarchy(symbol, visited);
    return visited;
  }

  private static void walkTypeHierarchy(Symbol.TypeSymbol symbol, Set<String> visited) {
    String fqn = symbol.type().fullyQualifiedName();
    if ("java.lang.Object".equals(fqn) || symbol.type().isUnknown() || !visited.add(fqn)) {
      return;
    }
    Type superClass = symbol.superClass();
    if (superClass != null && !superClass.isUnknown()) {
      walkTypeHierarchy(superClass.symbol(), visited);
    }
    for (Type iface : symbol.interfaces()) {
      if (!iface.isUnknown()) {
        walkTypeHierarchy(iface.symbol(), visited);
      }
    }
  }
}
