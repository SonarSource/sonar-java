/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks;

import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonarsource.analyzer.commons.collections.SetUtils;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;

@Rule(key = "S4925")
public class JdbcDriverExplicitLoadingCheck extends AbstractMethodDetection implements JavaVersionAwareVisitor {

  private static final Set<String> JDBC_4_DRIVERS = SetUtils.immutableSetOf(
    "com.mysql.jdbc.Driver",
    "oracle.jdbc.driver.OracleDriver",
    "com.ibm.db2.jdbc.app.DB2Driver",
    "com.ibm.db2.jdbc.net.DB2Driver",
    "com.sybase.jdbc.SybDriver",
    "com.sybase.jdbc2.jdbc.SybDriver",
    "com.teradata.jdbc.TeraDriver",
    "com.microsoft.sqlserver.jdbc.SQLServerDriver",
    "org.postgresql.Driver",
    "sun.jdbc.odbc.JdbcOdbcDriver",
    "org.hsqldb.jdbc.JDBCDriver",
    "org.h2.Driver",
    "org.firebirdsql.jdbc.FBDriver",
    "net.sourceforge.jtds.jdbc.Driver",
    "com.ibm.db2.jcc.DB2Driver"
  );

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.create()
      .ofTypes("java.lang.Class")
      .names("forName")
      .addParametersMatcher("java.lang.String")
      .build();
  }

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava6Compatible();
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    mit.arguments().get(0).asConstant(String.class).ifPresent(driverClassName -> {
      if (JDBC_4_DRIVERS.contains(driverClassName)) {
        reportIssue(ExpressionUtils.methodName(mit), "Remove this \"Class.forName()\", it is useless." + context.getJavaVersion().java6CompatibilityMessage());
      }
    });
  }
}
