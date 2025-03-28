/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
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

import com.sonar.sslr.api.RecognitionException;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.sonar.check.Rule;
import org.sonar.java.ExceptionHandler;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonarsource.analyzer.commons.annotations.DeprecatedRuleKey;

@DeprecatedRuleKey(ruleKey = "ParsingError", repositoryKey = "squid")
@Rule(key = "S2260")
public class ParsingErrorCheck implements ExceptionHandler, JavaFileScanner {

  private JavaFileScannerContext context;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
  }

  @Override
  public void processException(Exception e) {
    StringWriter exception = new StringWriter();
    e.printStackTrace(new PrintWriter(exception));
    context.addIssueOnFile(this, exception.toString());
  }

  @Override
  public void processRecognitionException(RecognitionException e) {
    context.addIssue(e.getLine(), this, "Parse error");
  }

}
