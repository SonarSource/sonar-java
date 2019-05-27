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
package org.sonar.java.checks.security;

import java.util.Collections;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.transform.TransformerFactory;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.sonar.java.matcher.TypeCriteria.subtypeOf;

@Rule(key = "S4435")
public class SecureXmlTransformerCheck extends AbstractMethodDetection {

  private static final String TRANSFORMER_FACTORY_CLASS_NAME = TransformerFactory.class.getName();

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return Collections.singletonList(
      MethodMatcher.create()
        .typeDefinition(subtypeOf(TRANSFORMER_FACTORY_CLASS_NAME))
        .name("newInstance")
        .withAnyParameters());
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree methodInvocation) {
    Tree enclosingMethod = ExpressionUtils.getEnclosingMethod(methodInvocation);
    if (enclosingMethod == null) {
      return;
    }
    MethodBodyVisitor visitor = new MethodBodyVisitor();
    enclosingMethod.accept(visitor);
    if (!visitor.foundCallsToSecuringMethods()) {
      reportIssue(methodInvocation.methodSelect(), "Secure this \"Transformer\" by either disabling external DTDs or enabling secure processing.");
    }
  }

  private static class MethodBodyVisitor extends BaseTreeVisitor {

    private static final MethodMatcher SET_FEATURE =
      MethodMatcher.create()
        .typeDefinition(subtypeOf(TRANSFORMER_FACTORY_CLASS_NAME))
        .name("setFeature")
        .parameters("java.lang.String", "boolean");

    private static final MethodMatcher SET_ATTRIBUTE =
      MethodMatcher.create()
        .typeDefinition(subtypeOf(TRANSFORMER_FACTORY_CLASS_NAME))
        .name("setAttribute")
        .parameters("java.lang.String", "java.lang.Object");

    private boolean hasSecureProcessingFeature = false;
    private boolean hasSecuredExternalDtd = false;
    private boolean hasSecuredExternalStylesheet = false;

    private boolean foundCallsToSecuringMethods() {
      return hasSecureProcessingFeature || (hasSecuredExternalDtd && hasSecuredExternalStylesheet);
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree methodInvocation) {
      Arguments arguments = methodInvocation.arguments();

      if (SET_FEATURE.matches(methodInvocation)
        && XMLConstants.FEATURE_SECURE_PROCESSING.equals(ExpressionsHelper.getConstantValueAsString(arguments.get(0)).value())
        && LiteralUtils.isTrue(arguments.get(1))) {

        hasSecureProcessingFeature = true;
      }

      if (SET_ATTRIBUTE.matches(methodInvocation)) {
        String attributeName = ExpressionsHelper.getConstantValueAsString(arguments.get(0)).value();
        String attributeValue = ExpressionsHelper.getConstantValueAsString(arguments.get(1)).value();
        if ("".equals(attributeValue)) {
          if (XMLConstants.ACCESS_EXTERNAL_DTD.equals(attributeName)) {
            hasSecuredExternalDtd = true;
          } else if (XMLConstants.ACCESS_EXTERNAL_STYLESHEET.equals(attributeName)) {
            hasSecuredExternalStylesheet = true;
          }
        }
      }

      super.visitMethodInvocation(methodInvocation);
    }

  }
}
