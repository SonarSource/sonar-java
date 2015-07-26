/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableMap;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.checks.methods.MethodMatcher;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;
import java.util.Map;

@Rule(
  key = "S2070",
  name = "SHA-1 and Message-Digest hash algorithms should not be used",
  tags = {"cwe", "owasp-a6", "sans-top25-porous", "security"},
  priority = Priority.CRITICAL)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.SECURITY_FEATURES)
@SqaleConstantRemediation("30min")
public class DeprecatedHashAlgorithmCheck extends AbstractMethodDetection {

  private static final String MD5 = "MD5";
  private static final String SHA1 = "SHA-1";

  private static final Map<String, String> ALGORITHM_BY_METHOD_NAME = ImmutableMap.<String, String>builder()
    .put("getMd5Digest", MD5)
    .put("getShaDigest", SHA1)
    .put("getSha1Digest", SHA1)
    .put("md5", MD5)
    .put("md5Hex", MD5)
    .put("sha1", SHA1)
    .put("sha1Hex", SHA1)
    .put("sha", SHA1)
    .put("shaHex", SHA1)
    .build();

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    Builder<MethodMatcher> builder = ImmutableList.<MethodMatcher>builder()
      .add(MethodMatcher.create()
        .typeDefinition("java.security.MessageDigest")
        .name("getInstance")
        .addParameter("java.lang.String"))
      .add(MethodMatcher.create()
        .typeDefinition("org.apache.commons.codec.digest.DigestUtils")
        .name("getDigest")
        .addParameter("java.lang.String"));
    for (String methodName : ALGORITHM_BY_METHOD_NAME.keySet()) {
      builder.add(MethodMatcher.create()
        .typeDefinition("org.apache.commons.codec.digest.DigestUtils")
        .name(methodName)
        .withNoParameterConstraint());
    }
    for (String methodName : ImmutableList.of("md5", "sha1")) {
      builder.add(MethodMatcher.create()
        .typeDefinition("com.google.common.hash.Hashing")
        .name(methodName));
    }
    return builder.build();
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    String methodName = methodName(mit);
    String algorithm = ALGORITHM_BY_METHOD_NAME.get(methodName);
    if (algorithm == null) {
      List<ExpressionTree> arguments = mit.arguments();
      algorithm = algorithm(arguments.get(0));
    }
    if (MD5.equals(algorithm) || SHA1.equals(algorithm)) {
      addIssue(mit, "Use a stronger encryption algorithm than " + algorithm + ".");
    }
  }

  private static String methodName(MethodInvocationTree mit) {
    String name = null;
    ExpressionTree methodSelect = mit.methodSelect();
    if (methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree memberSelectExpressionTree = (MemberSelectExpressionTree) methodSelect;
      name = memberSelectExpressionTree.identifier().name();
    } else if (methodSelect.is(Tree.Kind.IDENTIFIER)) {
      IdentifierTree identifier = (IdentifierTree) methodSelect;
      name = identifier.name();
    }
    return name;
  }

  private static String algorithm(ExpressionTree invocationArgument) {
    if (invocationArgument.is(Tree.Kind.STRING_LITERAL)) {
      return LiteralUtils.trimQuotes(((LiteralTree) invocationArgument).value());
    }
    return null;
  }

}
