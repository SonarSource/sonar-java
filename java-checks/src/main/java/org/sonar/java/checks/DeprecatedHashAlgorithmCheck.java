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
package org.sonar.java.checks;

import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.JavaPropertiesHelper;
import org.sonar.java.checks.helpers.MethodsHelper;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.TypeCriteria;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Rule(key = "S2070")
public class DeprecatedHashAlgorithmCheck extends AbstractMethodDetection {

  private static final String JAVA_LANG_STRING = "java.lang.String";
  private static final String MD5 = "MD5";
  private static final String SHA1 = "SHA1";

  private static final Map<String, String> ALGORITHM_BY_METHOD_NAME = new HashMap<>();
  static {
    ALGORITHM_BY_METHOD_NAME.put("getMd5Digest", MD5);
    ALGORITHM_BY_METHOD_NAME.put("getShaDigest", SHA1);
    ALGORITHM_BY_METHOD_NAME.put("getSha1Digest", SHA1);
    ALGORITHM_BY_METHOD_NAME.put("md5", MD5);
    ALGORITHM_BY_METHOD_NAME.put("md5Hex", MD5);
    ALGORITHM_BY_METHOD_NAME.put("sha1", SHA1);
    ALGORITHM_BY_METHOD_NAME.put("sha1Hex", SHA1);
    ALGORITHM_BY_METHOD_NAME.put("sha", SHA1);
    ALGORITHM_BY_METHOD_NAME.put("shaHex", SHA1);
  }

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    List<MethodMatcher> builder = new ArrayList<>();
    builder.add(MethodMatcher.create()
      .typeDefinition("java.security.MessageDigest")
      .name("getInstance")
      .addParameter(JAVA_LANG_STRING));
    builder.add(MethodMatcher.create()
      .typeDefinition("java.security.MessageDigest")
      .name("getInstance")
      .addParameter(JAVA_LANG_STRING)
      .addParameter(TypeCriteria.anyType()));
    builder.add(MethodMatcher.create()
      .typeDefinition("org.apache.commons.codec.digest.DigestUtils")
      .name("getDigest")
      .addParameter(JAVA_LANG_STRING));
    for (String methodName : ALGORITHM_BY_METHOD_NAME.keySet()) {
      builder.add(MethodMatcher.create()
        .typeDefinition("org.apache.commons.codec.digest.DigestUtils")
        .name(methodName)
        .withAnyParameters());
    }
    for (String methodName : new String[] {"md5", "sha1"}) {
      builder.add(MethodMatcher.create()
        .typeDefinition("com.google.common.hash.Hashing")
        .name(methodName)
        .withoutParameter());
    }
    return Collections.unmodifiableList(builder);
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    String methodName = MethodsHelper.methodName(mit).name();
    String algorithm = ALGORITHM_BY_METHOD_NAME.get(methodName);
    if (algorithm == null) {
      algorithm = algorithm(mit.arguments().get(0));
    }
    boolean isMd5 = MD5.equalsIgnoreCase(algorithm);
    boolean isSha1 = SHA1.equalsIgnoreCase(algorithm);
    if (isMd5 || isSha1) {
      String msgAlgo = isSha1 ? "SHA-1" : algorithm;
      reportIssue(MethodsHelper.methodName(mit), "Use a stronger hashing algorithm than " + msgAlgo + ".");
    }
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
