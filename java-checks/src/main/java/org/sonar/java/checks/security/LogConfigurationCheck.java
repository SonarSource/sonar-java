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

import java.util.Arrays;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;

@Rule(key = "S4792")
public class LogConfigurationCheck extends AbstractMethodDetection {

  private static final String LOG4J_CONFIGURATOR = "org.apache.logging.log4j.core.config.Configurator";
  private static final String LOG4J_CONFIGURATION_SOURCE = "org.apache.logging.log4j.core.config.ConfigurationSource";
  private static final String MESSAGE = "Make sure that this logger's configuration is safe.";
  private static final String SET_LEVEL = "setLevel";
  private static final String ADD_APPENDER = "addAppender";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.METHOD_INVOCATION, Tree.Kind.NEW_CLASS, Tree.Kind.METHOD_REFERENCE, Tree.Kind.CLASS);
  }

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.or(
      MethodMatchers.create()
        .ofTypes("org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory")
        .names("newConfigurationBuilder")
        .addWithoutParametersMatcher()
        .build(),
      MethodMatchers.create()
        .ofTypes(LOG4J_CONFIGURATOR)
        .names("setAllLevels", SET_LEVEL, "setRootLevel")
        .withAnyParameters()
        .build(),
      MethodMatchers.create()
        .ofTypes("org.apache.logging.log4j.core.config.Configuration")
        .names(ADD_APPENDER)
        .withAnyParameters()
        .build(),
      MethodMatchers.create()
        .ofTypes("org.apache.logging.log4j.core.config.LoggerConfig")
        .names(ADD_APPENDER, SET_LEVEL)
        .withAnyParameters()
        .build(),
      MethodMatchers.create()
        .ofTypes("org.apache.logging.log4j.core.LoggerContext")
        .names("setConfigLocation")
        .withAnyParameters()
        .build(),
      MethodMatchers.create().ofTypes(LOG4J_CONFIGURATION_SOURCE).names("<init>", "fromResource", "fromUri").withAnyParameters().build(),
      MethodMatchers.create().ofTypes("java.util.logging.LogManager").names("readConfiguration").withAnyParameters().build(),
      MethodMatchers.create().ofTypes("java.util.logging.Logger").names(SET_LEVEL, "addHandler").withAnyParameters().build(),
      MethodMatchers.create().ofTypes("ch.qos.logback.classic.Logger").names(ADD_APPENDER, SET_LEVEL).withAnyParameters().build(),
      MethodMatchers.create().ofTypes("ch.qos.logback.classic.joran.JoranConfigurator").constructor().withAnyParameters().build(),
      MethodMatchers.create().ofTypes("java.lang.System").names("setProperty").addParametersMatcher("java.lang.String", "java.lang.String").build());
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.CLASS)) {
      checkConfigurationFactoryExtension(((ClassTree) tree));
    } else {
      super.visitNode(tree);
    }
  }

  private void checkConfigurationFactoryExtension(ClassTree tree) {
    TypeTree superClass = tree.superClass();
    if (superClass != null && superClass.symbolType().is("org.apache.logging.log4j.core.config.ConfigurationFactory")) {
      reportIssue(superClass, MESSAGE);
    }
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    if ("setProperty".equals(mit.methodSymbol().name())) {
      String stringConstant = ExpressionsHelper.getConstantValueAsString(mit.arguments().get(0)).value();
      if ("logback.configurationFile".equals(stringConstant)) {
        reportIssue(mit, MESSAGE);
      }
    } else {
      reportIssue(mit, MESSAGE);
    }
  }

  @Override
  protected void onConstructorFound(NewClassTree newClassTree) {
    reportIssue(newClassTree, MESSAGE);
  }
}
