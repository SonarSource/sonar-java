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
import org.junit.Test;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.checks.CheckMessagesVerifier;

import java.io.File;

public class SQLInjectionCheckTest {

  private final SQLInjectionCheck check = new SQLInjectionCheck();

  @Test
  public void test() {
    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/SQLInjection.java"), new VisitorsBridge(check, ImmutableList.of(new File("target/test-classes"))));
    CheckMessagesVerifier.verify(file.getCheckMessages())
        .next().atLine(14).withMessage("\"param\" is provided externally to the method and not sanitized before use.")
        .next().atLine(16)
        .next().atLine(30)
        .next().atLine(31)
        .next().atLine(32)
        .next().atLine(37)
        .next().atLine(38).withMessage("\"param2\" is provided externally to the method and not sanitized before use.")
        .next().atLine(39)
        .next().atLine(66).withMessage("Use Hibernate's parameter binding instead of concatenation.")
        .next().atLine(67).withMessage("Use Hibernate's parameter binding instead of concatenation.")
        .noMore();
  }

}
