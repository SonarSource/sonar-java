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
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S9388")
public class S9388Check extends IssuableSubscriptionVisitor {

  private static final String DATA_PROVIDER_ANNOTATION = "org.testng.annotations.DataProvider";
  private static final String JAVA_LANG_OBJECT = "java.lang.Object";
  private static final String MESSAGE = "Change this return type to \"Object[][]\", \"Iterator<Object[]>\", \"Iterator<Object>\", or \"Object[]\".";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree methodTree = (MethodTree) tree;
    if (!methodTree.symbol().metadata().isAnnotatedWith(DATA_PROVIDER_ANNOTATION)) {
      return;
    }
    Type returnType = methodTree.returnType().symbolType();
    if (returnType.isUnknown()) {
      return;
    }
    if (!isValidDataProviderReturnType(returnType)) {
      reportIssue(methodTree.returnType(), MESSAGE);
    }
  }

  private static boolean isValidDataProviderReturnType(Type returnType) {
    return isObjectArray2D(returnType)
      || isObjectArray1D(returnType)
      || isValidIterator(returnType);
  }

  private static boolean isObjectArray2D(Type type) {
    if (!type.isArray()) {
      return false;
    }
    Type elementType = ((Type.ArrayType) type).elementType();
    return elementType.isArray() && ((Type.ArrayType) elementType).elementType().is(JAVA_LANG_OBJECT);
  }

  private static boolean isObjectArray1D(Type type) {
    if (!type.isArray()) {
      return false;
    }
    Type elementType = ((Type.ArrayType) type).elementType();
    return !elementType.isArray() && elementType.is(JAVA_LANG_OBJECT);
  }

  private static boolean isValidIterator(Type type) {
    if (!type.isSubtypeOf("java.util.Iterator")) {
      return false;
    }
    if (!type.isParameterized()) {
      return true;
    }
    List<Type> typeArgs = type.typeArguments();
    if (typeArgs.size() != 1) {
      return false;
    }
    Type typeArg = typeArgs.get(0);
    return typeArg.is(JAVA_LANG_OBJECT)
      || (typeArg.isArray() && ((Type.ArrayType) typeArg).elementType().is(JAVA_LANG_OBJECT));
  }

}
