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
package org.sonar.java.se.checks;

import java.util.Arrays;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.se.CheckerContext;
import org.sonar.java.se.FlowComputation;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.checks.XxeProcessingCheck.XmlSetXIncludeAware;
import org.sonar.java.se.checks.XxeProperty.FeatureXInclude;
import org.sonar.java.se.constraint.ConstraintManager;
import org.sonar.java.se.constraint.ConstraintsByDomain;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S6373")
public class AllowXMLInclusionCheck extends SECheck {

  @Override
  public ProgramState checkPreStatement(CheckerContext context, Tree syntaxNode) {
    PreStatementVisitor visitor = new PreStatementVisitor(context);
    syntaxNode.accept(visitor);
    return visitor.programState;
  }

  private class PreStatementVisitor extends CheckerTreeNodeVisitor {

    private final CheckerContext context;

    private PreStatementVisitor(CheckerContext context) {
      super(context.getState());
      this.context = context;
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree mit) {
      if (XxeProcessingCheck.PARSING_METHODS.matches(mit)) {
        SymbolicValue peek = programState.peekValue(mit.arguments().size());

        if (peek instanceof XxeProcessingCheck.XxeSymbolicValue) {
          XxeProcessingCheck.XxeSymbolicValue xxeSymbolicValue = (XxeProcessingCheck.XxeSymbolicValue) peek;
          reportIfNotSecured(context, xxeSymbolicValue, programState.getConstraints(xxeSymbolicValue));
        }
      }
    }
  }

  @Override
  public void checkEndOfExecutionPath(CheckerContext context, ConstraintManager constraintManager) {
    ProgramState endState = context.getState();
    if (endState.exitingOnRuntimeException()) {
      return;
    }

    // We want to report only when the unsecured factory is returned, if it is the case, it will be on the top of the stack.
    SymbolicValue peek = endState.peekValue();
    if (peek instanceof XxeProcessingCheck.XxeSymbolicValue) {
      XxeProcessingCheck.XxeSymbolicValue xxeSV = (XxeProcessingCheck.XxeSymbolicValue) peek;
      reportIfNotSecured(context, xxeSV, endState.getConstraints(xxeSV));
    }
  }

  private void reportIfNotSecured(CheckerContext context, XxeProcessingCheck.XxeSymbolicValue xxeSV, @Nullable ConstraintsByDomain constraintsByDomain) {
    if (!xxeSV.isField && isUnSecuredByProperty(constraintsByDomain)) {
      context.reportIssue(getIssueLocation(context, xxeSV),
        this,
        "Disable the inclusion of files in XML processing.");
    }
  }

  private static Tree getIssueLocation(CheckerContext context, XxeProcessingCheck.XxeSymbolicValue xxeSV) {
    return FlowComputation.flowWithoutExceptions(context.getNode(), xxeSV, c -> c == FeatureXInclude.ENABLE || c == XmlSetXIncludeAware.ENABLE,
      Arrays.asList(FeatureXInclude.class, XmlSetXIncludeAware.class), FlowComputation.FIRST_FLOW)
      .stream()
      .findFirst()
      .flatMap(f -> f.elements().stream().findFirst())
      .map(e -> e.syntaxNode)
      // Last step should never occurs, we add it for defensive programming
      .orElse(xxeSV.init);
  }

  private static boolean isUnSecuredByProperty(@Nullable ConstraintsByDomain constraintsByDomain) {
    if (constraintsByDomain == null) {
      // Not vulnerable unless some properties are explicitly set.
      return false;
    }
    return (constraintsByDomain.hasConstraint(FeatureXInclude.ENABLE)
      || constraintsByDomain.hasConstraint(XmlSetXIncludeAware.ENABLE))
      && !constraintsByDomain.hasConstraint(XxeProcessingCheck.XxeEntityResolver.CUSTOM_ENTITY_RESOLVER);
  }


  /*
     TODO: we should ensure that we add a XxeProperty.FeatureXInclude constraint for the following cases:

     1) Setter with one argument
          .ofSubTypes("javax.xml.parsers.DocumentBuilderFactory", "javax.xml.parsers.SAXParserFactory")
          .names("setXIncludeAware")
          .addParametersMatcher("boolean")

     2) Set feature with two arguments, the first one matching "http://apache.org/xml/features/xinclude"
          .ofSubTypes("javax.xml.stream.XMLInputFactory")
          .names("setProperty")
          .addParametersMatcher("java.lang.String", "java.lang.Object")

          .ofSubTypes(
            "javax.xml.transform.TransformerFactory",
            "javax.xml.validation.SchemaFactory",
            "org.dom4j.io.SAXReader",
            "org.jdom2.input.SAXBuilder")
          .names("setFeature")
          .addParametersMatcher("java.lang.String", "boolean")

     TODO: we should report when the last argument of "setXIncludeAware|setProperty|setFeature" is `true`
           and there's no XxeEntityResolver.CUSTOM_ENTITY_RESOLVER constraints
           with the message "Disable the inclusion of files in XML processing."

     TODO: update the rspec to add an exception about EntityResolver
   */

}
