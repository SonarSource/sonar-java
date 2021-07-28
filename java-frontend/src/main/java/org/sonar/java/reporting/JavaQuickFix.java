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
package org.sonar.java.reporting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JavaQuickFix {
  private final String description;
  private final List<JavaTextEdit> textEdits;

  private JavaQuickFix(String description, List<JavaTextEdit> textEdits) {
    this.description = description;
    this.textEdits = textEdits;
  }

  public String getDescription() {
    return description;
  }

  public List<JavaTextEdit> getTextEdits() {
    return textEdits;
  }

  public static Builder newQuickFix(String description) {
    return new Builder(description);
  }

  public static class Builder {
    private final String description;
    private final List<JavaTextEdit> textEdits = new ArrayList<>();

    Builder(String description) {
      this.description = description;
    }

    public Builder addTextEdit(JavaTextEdit... textEdit) {
      textEdits.addAll(Arrays.asList(textEdit));
      return this;
    }

    public JavaQuickFix build() {
      return new JavaQuickFix(description, textEdits);
    }
  }

}
