/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import javax.crypto.Cipher;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.checks.helpers.HardcodedStringExpressionChecker;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.expression.MemberSelectExpressionTreeImpl;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

@Rule(key = "S6432")
public class CounterModeIVShouldNotBeReusedCheck extends IssuableSubscriptionVisitor {

  private static final String PRIMARY_LOCATION_ISSUE_MESSAGE = "Use a dynamically-generated initialization vector (IV) to avoid IV-key pair reuse.";
  private static final String SECONDARY_LOCATION_ISSUE_MESSAGE = "The static value is defined here.";

  private static final MethodMatchers JCA_CHIPER_INIT_METHODS = MethodMatchers.create()
    .ofTypes("javax.crypto.Cipher")
    .names("init")
    .addParametersMatcher("int", MethodMatchers.ANY, "java.security.spec.AlgorithmParameterSpec")
    .build();

  private static final MethodMatchers BC_CHIPER_INIT_METHODS = MethodMatchers.create()
    .ofTypes("org.bouncycastle.crypto.modes.GCMBlockCipher", "org.bouncycastle.crypto.modes.CCMBlockCipher")
    .names("init")
    .addParametersMatcher("boolean", "org.bouncycastle.crypto.CipherParameters")
    .build();

  private static final MethodMatchers GCM_CONSTRUCTOR = MethodMatchers.create()
    .ofTypes("javax.crypto.spec.GCMParameterSpec")
    .constructor()
    .addParametersMatcher(parameters -> !parameters.isEmpty())
    .build();

  private static final MethodMatchers AEAD_CONSTRUCTOR = MethodMatchers.create()
    .ofTypes("org.bouncycastle.crypto.params.AEADParameters")
    .constructor()
    .addParametersMatcher(parameters -> !parameters.isEmpty())
    .build();

  @Override
  public List<Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodInvocationTree method = (MethodInvocationTree) tree;
    var secondaryLocations = new ArrayList<JavaFileScannerContext.Location>();

    if (isJCAOperationModeEncrypt(method)) {
      checkForHardcodedIVInitialization(method, 2, secondaryLocations);
    } else if (isBCCipherForEncryption(method)) {
      checkForHardcodedIVInitialization(method, 1, secondaryLocations);
    }

  }

  private void checkForHardcodedIVInitialization(MethodInvocationTree method, int constructorParamIndex, List<JavaFileScannerContext.Location> secondaryLocations) {
    if (checkForJCAHardcodedIVInitialization(method.arguments().get(constructorParamIndex), secondaryLocations)) {
      MemberSelectExpressionTree methodSelect = (MemberSelectExpressionTreeImpl) method.methodSelect();
      reportIssue(methodSelect.identifier(), PRIMARY_LOCATION_ISSUE_MESSAGE, secondaryLocations, null);
    }
  }

  private static boolean isJCAOperationModeEncrypt(MethodInvocationTree method) {
    if (JCA_CHIPER_INIT_METHODS.matches(method)) {
      Optional<Integer> value = method.arguments().get(0).asConstant(Integer.class);
      return value.isPresent() && value.get() == Cipher.ENCRYPT_MODE;
    }
    return false;
  }

  private static boolean isBCCipherForEncryption(MethodInvocationTree method) {
    if (BC_CHIPER_INIT_METHODS.matches(method)) {
      Optional<Boolean> value = method.arguments().get(0).asConstant(Boolean.class);
      return value.isPresent() && value.get();
    }
    return false;
  }

  // argument here is going to be a GCMParameterSpec
  private static boolean checkForJCAHardcodedIVInitialization(ExpressionTree expression, List<JavaFileScannerContext.Location> secondaryLocations) {
    ExpressionTree argument = ExpressionUtils.skipParentheses(expression);
    switch (argument.kind()) {
      case IDENTIFIER:
        List<ExpressionTree> assignments = ExpressionsHelper.getIdentifierAssignments((IdentifierTree) argument);
        secondaryLocations.add(new JavaFileScannerContext.Location(SECONDARY_LOCATION_ISSUE_MESSAGE, argument));
        return assignments.stream()
          .allMatch(assignment -> checkForJCAHardcodedIVInitialization(assignment, secondaryLocations));
      case NEW_CLASS:
        NewClassTree constructor = (NewClassTree) argument;
        if (GCM_CONSTRUCTOR.matches(constructor)) {
          ExpressionTree arg = constructor.arguments().get(1);
          secondaryLocations.add(new JavaFileScannerContext.Location(SECONDARY_LOCATION_ISSUE_MESSAGE, arg));
          return HardcodedStringExpressionChecker.isExpressionDerivedFromPlainText(arg, secondaryLocations, new HashSet<>());
        } else if (AEAD_CONSTRUCTOR.matches(constructor)) {
          ExpressionTree arg = constructor.arguments().get(2);
          secondaryLocations.add(new JavaFileScannerContext.Location(SECONDARY_LOCATION_ISSUE_MESSAGE, arg));
          return HardcodedStringExpressionChecker.isExpressionDerivedFromPlainText(arg, secondaryLocations, new HashSet<>());
        }
        return false;
      default:
        return false;
    }

  }

}
