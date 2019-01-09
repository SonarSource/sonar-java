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
package org.sonar.java.checks;

import com.sonar.sslr.api.RecognitionException;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.sonar.check.Rule;
import org.sonar.java.ExceptionHandler;
import org.sonar.java.RspecKey;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;

@Rule(key = "ParsingError")
@RspecKey("S2260")
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
