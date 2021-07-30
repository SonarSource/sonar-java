/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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
package org.sonar.java.checks.helpers;

import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonarsource.sonarlint.core.analyzer.issue.DefaultQuickFix;
import org.sonarsource.sonarlint.core.client.api.common.QuickFix;
import org.sonarsource.sonarlint.core.client.api.util.QuickFixUtils;
import org.sonarsource.sonarlint.core.container.analysis.filesystem.DefaultTextPointer;
import org.sonarsource.sonarlint.core.container.analysis.filesystem.DefaultTextRange;
import org.sonarsource.sonarlint.plugin.api.issue.NewFileEdit;

public class QuickFixHelper {

  private QuickFixHelper() {
    // Utility class
  }

  public static String quickFixApplicator(JavaQuickFix javaQuickFix, String before) {
    return QuickFixUtils.applyOnSingleSource(quickFixFromJavaQuickFix(javaQuickFix), before);
  }

  private static QuickFix quickFixFromJavaQuickFix(JavaQuickFix javaQuickFix) {
    DefaultQuickFix f = new DefaultQuickFix();
    NewFileEdit fe = f.newEdit();
    javaQuickFix.getTextEdits()
      .forEach(e -> {
          AnalyzerMessage.TextSpan textSpan = e.getTextSpan();
          fe.addTextEdit(fe.newTextEdit()
            .at(new DefaultTextRange(new DefaultTextPointer(textSpan.startLine, textSpan.startCharacter), new DefaultTextPointer(textSpan.endLine, textSpan.endCharacter)))
            .withNewText(e.getReplacement()));
        }
      );
    f.addEdit(fe);
    return f;
  }

}
