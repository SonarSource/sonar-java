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
import org.sonar.java.checks.helpers.JavaPropertiesHelper;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.checks.methods.MethodMatcher;
import org.sonar.java.checks.methods.TypeCriteria;
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

  private static final String JAVA_LANG_STRING = "java.lang.String";
  private static final String MD5 = "MD5";
  private static final String SHA1 = "SHA1";

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
        .addParameter(JAVA_LANG_STRING))
      .add(MethodMatcher.create()
        .typeDefinition("java.security.MessageDigest")
        .name("getInstance")
        .addParameter(JAVA_LANG_STRING)
        .addParameter(TypeCriteria.anyType()))
      .add(MethodMatcher.create()
        .typeDefinition("org.apache.commons.codec.digest.DigestUtils")
        .name("getDigest")
        .addParameter(JAVA_LANG_STRING));
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
      algorithm = algorithm(mit.arguments().get(0));
    }
    boolean isMd5 = MD5.equalsIgnoreCase(algorithm);
    boolean isSha1 = SHA1.equalsIgnoreCase(algorithm);
    if (isMd5 || isSha1) {
      String msgAlgo = isSha1 ? "SHA-1" : algorithm;
      addIssue(mit, "Use a stronger encryption algorithm than " + msgAlgo + ".");
    }
  }

  private static String methodName(MethodInvocationTree mit) {
    String name = null;
    ExpressionTree methodSelect = mit.methodSelect();
    if (methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
      name = ((MemberSelectExpressionTree) methodSelect).identifier().name();
    } else if (methodSelect.is(Tree.Kind.IDENTIFIER)) {
      name = ((IdentifierTree) methodSelect).name();
    }
    return name;
  }

  private static String algorithm(ExpressionTree invocationArgument) {
    ExpressionTree expectedAlgorithm = invocationArgument;
    ExpressionTree defaultPropertyValue = JavaPropertiesHelper.retrievedPropertyDefaultValue(invocationArgument);
    if (defaultPropertyValue != null) {
      expectedAlgorithm = defaultPropertyValue;
    }
    if (expectedAlgorithm.is(Tree.Kind.STRING_LITERAL)) {
      String algo = LiteralUtils.trimQuotes(((LiteralTree) expectedAlgorithm).value());
      return algo.replaceAll("-", "");
    }
    return null;
  }

}
