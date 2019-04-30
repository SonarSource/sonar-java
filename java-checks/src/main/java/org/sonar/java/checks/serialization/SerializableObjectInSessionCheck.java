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
package org.sonar.java.checks.serialization;

import com.google.common.collect.ImmutableList;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.ecj.TypeUtils;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.TypeCriteria;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;

import java.util.List;

@Rule(key = "S2441")
public class SerializableObjectInSessionCheck extends AbstractMethodDetection {


  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return ImmutableList.of(MethodMatcher.create().typeDefinition("javax.servlet.http.HttpSession")
      .name("setAttribute").addParameter("java.lang.String").addParameter(TypeCriteria.anyType()));
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    ExpressionTree argument = mit.arguments().get(1);
    Type type = argument.symbolType();
    if (!isSerializable(type)) {
      String andParameters = TypeUtils.isParameterized(type) ? " and its parameters" : "";
      reportIssue(argument, "Make \"" + type + "\"" + andParameters + " serializable or don't store it in the session.");
    }
  }

  private static boolean isSerializable(Type type) {
    if (type.isPrimitive()) {
      return true;
    }
    if (isSerializableArray(type)) {
      return true;
    }
    if (TypeUtils.isParameterized(type)) {
      return isSerializableParametrized(type);
    }
    return type.isSubtypeOf("java.io.Serializable");
  }

  private static boolean isSerializableArray(Type type) {
    return type.isArray() && isSerializable(((Type.ArrayType) type).elementType());
  }

  private static boolean isSerializableParametrized(Type type) {
    // note: this is assuming that custom implementors of Collection
    // have the good sense to make it serializable just like all implementations in the JDK
    //
    // note: type.substitution(t) should never be null
    return (type.isSubtypeOf("java.io.Serializable") || type.isSubtypeOf("java.util.Collection"))
      && TypeUtils.typeArguments(type).stream().allMatch(SerializableObjectInSessionCheck::isSerializable);
  }
}
