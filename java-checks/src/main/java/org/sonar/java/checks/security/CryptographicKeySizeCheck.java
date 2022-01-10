/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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
package org.sonar.java.checks.security;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonarsource.analyzer.commons.collections.MapBuilder;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;

import static org.sonar.java.model.ExpressionUtils.getAssignedSymbol;
import static org.sonar.java.model.ExpressionUtils.isInvocationOnVariable;

@Rule(key = "S4426")
public class CryptographicKeySizeCheck extends AbstractMethodDetection {

  private static final String KEY_PAIR_GENERATOR = "java.security.KeyPairGenerator";
  private static final String KEY_GENERATOR = "javax.crypto.KeyGenerator";
  private static final String EC_GEN_PARAMETER_SPEC = "java.security.spec.ECGenParameterSpec";
  private static final String GET_INSTANCE_METHOD = "getInstance";
  private static final String STRING = "java.lang.String";

  private static final int EC_MIN_KEY = 224;
  private static final Pattern EC_KEY_PATTERN = Pattern.compile("^(secp|prime|sect|c2tnb)(\\d+)");

  private static final Map<String, Integer> ALGORITHM_KEY_SIZE_MAP = MapBuilder.<String, Integer>newMap()
    .put("RSA", 2048)
    .put("DH", 2048)
    .put("DIFFIEHELLMAN", 2048)
    .put("DSA", 2048)
    .put("AES", 128)
    .build();

  private static final MethodMatchers KEY_GEN = MethodMatchers.or(
    MethodMatchers.create()
      .ofTypes(KEY_GENERATOR)
      .names("init")
      .addParametersMatcher("int")
      .build(),
    MethodMatchers.create()
      .ofTypes(KEY_PAIR_GENERATOR)
      .names("initialize")
      .addParametersMatcher("int")
      .addParametersMatcher("int", "java.security.SecureRandom")
      .build()) ;

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.or(
      MethodMatchers.create()
        .ofTypes(KEY_GENERATOR, KEY_PAIR_GENERATOR)
        .names(GET_INSTANCE_METHOD)
        .addParametersMatcher(STRING)
        .build(),
      MethodMatchers.create()
        .ofTypes(EC_GEN_PARAMETER_SPEC)
        .constructor()
        .addParametersMatcher(STRING)
        .build());
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    MethodTree methodTree = ExpressionUtils.getEnclosingMethod(mit);
    String getInstanceArg = ExpressionsHelper.getConstantValueAsString(mit.arguments().get(0)).value();
    if (methodTree != null && getInstanceArg != null) {
      Optional<Symbol> assignedSymbol = getAssignedSymbol(mit);
      MethodVisitor methodVisitor = new MethodVisitor(getInstanceArg, assignedSymbol.orElse(null));
      methodTree.accept(methodVisitor);
    }
  }

  @Override
  protected void onConstructorFound(NewClassTree newClassTree) {
    String firstArgument = ExpressionsHelper.getConstantValueAsString(newClassTree.arguments().get(0)).value();
    if (firstArgument != null) {
      Matcher matcher = EC_KEY_PATTERN.matcher(firstArgument);
      if (matcher.find() && Integer.valueOf(matcher.group(2)) < EC_MIN_KEY) {
        reportIssue(newClassTree, "Use a key length of at least " + EC_MIN_KEY + " bits for EC cipher algorithm.");
      }
    }
  }

  private class MethodVisitor extends BaseTreeVisitor {

    private final String algorithm;
    private final Integer minKeySize;
    private final Symbol variable;

    public MethodVisitor(String getInstanceArg, @Nullable Symbol variable) {
      this.algorithm = getInstanceArg;
      this.minKeySize = ALGORITHM_KEY_SIZE_MAP.get(this.algorithm.toUpperCase(Locale.ENGLISH));
      this.variable = variable;
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree mit) {
      if (minKeySize != null && KEY_GEN.matches(mit)) {
        Integer keySize = LiteralUtils.intLiteralValue(mit.arguments().get(0));
        if (keySize != null && keySize < minKeySize && isInvocationOnVariable(mit, variable, false)) {
          reportIssue(mit, "Use a key length of at least " + minKeySize + " bits for " + algorithm + " cipher algorithm.");
        }
      }
    }
  }
}
