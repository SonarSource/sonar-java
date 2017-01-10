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
package org.sonar.java.signature;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Lists;
import org.sonar.java.ast.api.JavaKeyword;
import org.sonar.plugins.java.api.tree.ArrayTypeTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ParameterizedTypeTree;
import org.sonar.plugins.java.api.tree.PrimitiveTypeTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.List;

public final class MethodSignatureScanner {

  private final String bytecodeMethodSignature;
  private final MethodTree methodTree;

  private static final BiMap<String, JvmJavaType> JAVA_TYPE_MAPPING = HashBiMap.create();

  static {
    JAVA_TYPE_MAPPING.put(JavaKeyword.BYTE.getValue(), JvmJavaType.B);
    JAVA_TYPE_MAPPING.put(JavaKeyword.CHAR.getValue(), JvmJavaType.C);
    JAVA_TYPE_MAPPING.put(JavaKeyword.SHORT.getValue(), JvmJavaType.S);
    JAVA_TYPE_MAPPING.put(JavaKeyword.INT.getValue(), JvmJavaType.I);
    JAVA_TYPE_MAPPING.put(JavaKeyword.LONG.getValue(), JvmJavaType.J);
    JAVA_TYPE_MAPPING.put(JavaKeyword.BOOLEAN.getValue(), JvmJavaType.Z);
    JAVA_TYPE_MAPPING.put(JavaKeyword.FLOAT.getValue(), JvmJavaType.F);
    JAVA_TYPE_MAPPING.put(JavaKeyword.DOUBLE.getValue(), JvmJavaType.D);
    JAVA_TYPE_MAPPING.put(JavaKeyword.VOID.getValue(), JvmJavaType.V);
  }

  public MethodSignatureScanner(MethodTree methodTree) {
    this.methodTree = methodTree;
    this.bytecodeMethodSignature = null;
  }

  private MethodSignatureScanner(String bytecodeMethodSignature) {
    this.methodTree = null;
    this.bytecodeMethodSignature = bytecodeMethodSignature;
  }

  public static MethodSignature scan(MethodTree methodTree) {
    MethodSignatureScanner scanner = new MethodSignatureScanner(methodTree);
    return scanner.scanTree();
  }

  public static String getReadableType(JvmJavaType jvmJavaType) {
    return JAVA_TYPE_MAPPING.inverse().get(jvmJavaType);
  }

  private MethodSignature scanTree() {
    String name = methodTree.simpleName().name();
    Parameter returnTypeParam;
    if (methodTree.is(Tree.Kind.CONSTRUCTOR)) {
      name = "<init>";
      returnTypeParam = new Parameter(JvmJavaType.V, false);
    } else {
      returnTypeParam = getParameter(methodTree.returnType());
    }

    List<Parameter> argumentTypes = Lists.newArrayList();
    for (VariableTree variableTree : methodTree.parameters()) {
      argumentTypes.add(getParameter(variableTree.type()));
    }

    return new MethodSignature(name, returnTypeParam, argumentTypes);
  }

  private static Parameter getParameter(Tree typeTree) {
    Parameter parameter;
    boolean isArray = false;
    Tree type = typeTree;
    if (type.is(Tree.Kind.ARRAY_TYPE)) {
      isArray = true;
      while (type.is(Tree.Kind.ARRAY_TYPE)) {
        type = ((ArrayTypeTree) type).type();
      }
    }
    JvmJavaType jvmType = jvmJavaTypeOf(type);
    if (jvmType.equals(JvmJavaType.L)) {
      parameter = new Parameter(getTypeName(type), isArray);
    } else {
      parameter = new Parameter(jvmType, isArray);
    }
    return parameter;
  }

  private static String getTypeName(Tree typeTree) {
    if (typeTree.is(Tree.Kind.IDENTIFIER)) {
      return ((IdentifierTree) typeTree).name();
    } else if (typeTree.is(Tree.Kind.MEMBER_SELECT)) {
      return ((MemberSelectExpressionTree) typeTree).identifier().name();
    } else if (typeTree.is(Tree.Kind.PARAMETERIZED_TYPE)) {
      return getTypeName(((ParameterizedTypeTree) typeTree).type());
    }
    return "";
  }

  private static JvmJavaType jvmJavaTypeOf(Tree type) {
    if (type.is(Tree.Kind.PRIMITIVE_TYPE)) {
      return JAVA_TYPE_MAPPING.get(((PrimitiveTypeTree) type).keyword().text());
    }
    return JvmJavaType.L;
  }

  public static MethodSignature scan(String bytecodeMethodSignature) {
    Preconditions.checkNotNull(bytecodeMethodSignature);
    MethodSignatureScanner scanner = new MethodSignatureScanner(bytecodeMethodSignature);
    return scanner.scan();
  }

  private MethodSignature scan() {
    int leftBracketIndex = bytecodeMethodSignature.indexOf('(');
    int rightBracketIndex = bytecodeMethodSignature.indexOf(')');
    String methodName = bytecodeMethodSignature.substring(0, leftBracketIndex);
    Parameter returnType = ParameterSignatureScanner.scan(bytecodeMethodSignature.substring(rightBracketIndex + 1));
    List<Parameter> argumentTypes = ParameterSignatureScanner.scanArguments(bytecodeMethodSignature.substring(leftBracketIndex + 1,
      rightBracketIndex));
    return new MethodSignature(methodName, returnType, argumentTypes);
  }

}
