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

import com.sonar.sslr.api.AstNode;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.tree.*;

import java.util.HashSet;
import java.util.Set;

public class ImmutableContract {
  private static final Set<String> immutableTypes = new HashSet<String>(19);

  static {
    immutableTypes.add("java.lang.Enum");
    immutableTypes.add("java.lang.Boolean");
    immutableTypes.add("java.lang.Character");
    immutableTypes.add("java.lang.Short");
    immutableTypes.add("java.lang.Integer");
    immutableTypes.add("java.lang.Long");
    immutableTypes.add("java.lang.Float");
    immutableTypes.add("java.lang.Double");
    immutableTypes.add("java.lang.Byte");
    immutableTypes.add("java.lang.String");
    immutableTypes.add("java.awt.Font");
    immutableTypes.add("java.awt.Color");
    immutableTypes.add("java.math.BigDecimal");
    immutableTypes.add("java.math.BigInteger");
    immutableTypes.add("java.math.MathContext");
    immutableTypes.add("java.nio.channels.FileLock");
    immutableTypes.add("java.nio.charset.Charset");
    immutableTypes.add("java.io.File");
    immutableTypes.add("java.net.URI");
    immutableTypes.add("java.util.regex.Pattern");
  }

  public static boolean isImmutable(VariableTree variableTree) {
    if(variableTree.type() instanceof PrimitiveTypeTree) {
      return true;
    }

    String className = TypeUtils.getFullQualifiedTypeName(variableTree.type(), false);
    if(!className.contains(".")) {
      className = "java.lang." + className;
    }
    return immutableTypes.contains(className);
  }

  private static String getTypeName(Tree typeTree) {
    AstNode blockAstNode = ((JavaTree) typeTree).getAstNode();
    if (typeTree != null) {
      if (typeTree.is(Tree.Kind.IDENTIFIER)) {
        return ((IdentifierTree) typeTree).name();
      } else if (typeTree.is(Tree.Kind.MEMBER_SELECT)) {
        return ((MemberSelectExpressionTree) typeTree).identifier().name();
      } else if (typeTree.is(Tree.Kind.PARAMETERIZED_TYPE)) {
        return getTypeName(((ParameterizedTypeTree) typeTree).type());
      }
    }
    return "";
  }
}
