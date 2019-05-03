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

import com.google.common.collect.ImmutableList;

import org.sonar.check.Rule;
import org.sonar.java.resolve.JavaType;
import org.sonar.java.resolve.ParametrizedTypeJavaType;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Rule(key = "S3553")
public class OptionalAsParameterCheck extends IssuableSubscriptionVisitor {

  private static final String JAVA_UTIL_OPTIONAL = "java.util.Optional";
  private static final String GUAVA_OPTIONAL = "com.google.common.base.Optional";
  private static final List<String> PRIMITIVE_OPTIONALS = ImmutableList.<String>builder()
    .add("java.util.OptionalDouble")
    .add("java.util.OptionalInt")
    .add("java.util.OptionalLong")
    .build();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.METHOD, Tree.Kind.CONSTRUCTOR);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    for (VariableTree parameter : ((MethodTree) tree).parameters()) {
      TypeTree typeTree = parameter.type();
      Optional<String> msg = expectedTypeInsteadOfOptional(typeTree.symbolType());
      if (msg.isPresent()) {
        reportIssue(typeTree, msg.get());
      }
    }
  }

  private static Optional<String> expectedTypeInsteadOfOptional(Type type) {
    if (type.is(JAVA_UTIL_OPTIONAL) || type.is(GUAVA_OPTIONAL)) {
      String msg;
      if (((JavaType) type).isParameterized()) {
        ParametrizedTypeJavaType ptjt = (ParametrizedTypeJavaType) type;
        String parameterTypeName = ptjt.substitution(ptjt.typeParameters().get(0)).erasure().name();
        msg = formatMsg(parameterTypeName);
      } else {
        msg = "Specify a type instead.";
      }
      return Optional.of(msg);
    }
    return PRIMITIVE_OPTIONALS.stream()
      .filter(type::is)
      .findFirst()
      .map(optional -> formatMsg(optional.substring(JAVA_UTIL_OPTIONAL.length()).toLowerCase()));
  }

  private static String formatMsg(String typeName) {
    return "Specify a \"" + typeName + "\" parameter instead.";
  }

}
