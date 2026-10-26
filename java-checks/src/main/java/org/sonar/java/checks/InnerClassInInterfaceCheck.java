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
package org.sonar.java.checks;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ParameterizedTypeTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;

@Rule(key = "S9398")
public class InnerClassInInterfaceCheck extends IssuableSubscriptionVisitor {

  private static final String BUILDER_SUFFIX = "Builder";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    if (ModifiersUtils.hasModifier(classTree.modifiers(), Modifier.ABSTRACT)) {
      return;
    }
    Tree parent = tree.parent();
    if (parent == null || !parent.is(Tree.Kind.INTERFACE)) {
      return;
    }
    ClassTree interfaceTree = (ClassTree) parent;
    if (implementsInterface(classTree, interfaceTree) || !isHelperType(classTree, interfaceTree)) {
      reportIssue(classTree.simpleName(), "Move this class out of this interface.");
    }
  }

  private static boolean implementsInterface(ClassTree classTree, ClassTree interfaceTree) {
    Type classType = classTree.symbol().type();
    Type interfaceType = interfaceTree.symbol().type();
    if (!classType.isUnknown() && !interfaceType.isUnknown()) {
      return classType.isSubtypeOf(interfaceType);
    }
    String interfaceName = interfaceTree.simpleName().name();
    return Stream.concat(Stream.ofNullable(classTree.superClass()), classTree.superInterfaces().stream())
      .anyMatch(superType -> interfaceName.equals(mainTypeName(superType)));
  }

  @Nullable
  private static String mainTypeName(TypeTree typeTree) {
    if (typeTree instanceof ParameterizedTypeTree parameterizedType) {
      return mainTypeName(parameterizedType.type());
    }
    if (typeTree instanceof MemberSelectExpressionTree memberSelect) {
      return memberSelect.identifier().name();
    }
    if (typeTree instanceof IdentifierTree identifier) {
      return identifier.name();
    }
    return null;
  }

  /**
   * A nested class is considered a helper type of its enclosing interface when it is a builder,
   * or when it appears in the signature (return type, parameter types or thrown exceptions)
   * of a method declared by the interface.
   */
  private static boolean isHelperType(ClassTree classTree, ClassTree interfaceTree) {
    String className = classTree.simpleName().name();
    if (className.endsWith(BUILDER_SUFFIX)) {
      return true;
    }
    TypeNamesCollector collector = new TypeNamesCollector();
    interfaceTree.members().stream()
      .filter(MethodTree.class::isInstance)
      .map(MethodTree.class::cast)
      .forEach(collector::scanSignature);
    return collector.names.contains(className);
  }

  private static class TypeNamesCollector extends BaseTreeVisitor {

    private final Set<String> names = new HashSet<>();

    void scanSignature(MethodTree methodTree) {
      scan(methodTree.returnType());
      methodTree.parameters().forEach(parameter -> scan(parameter.type()));
      scan(methodTree.throwsClauses());
    }

    @Override
    public void visitIdentifier(IdentifierTree tree) {
      names.add(tree.name());
    }
  }

}
