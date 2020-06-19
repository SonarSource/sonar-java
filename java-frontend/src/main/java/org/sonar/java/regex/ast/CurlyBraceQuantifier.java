/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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
package org.sonar.java.regex.ast;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class CurlyBraceQuantifier extends Quantifier {

  private final RegexToken minimumRepetitionsToken;

  private final int minimumRepetitions;

  private final RegexToken commaToken;

  private final RegexToken maximumRepetitionsToken;

  private final Integer maximumRepetitions;

  public CurlyBraceQuantifier(
    RegexSource source,
    IndexRange range,
    Modifier modifier,
    RegexToken minimumRepetitionsToken,
    @Nullable RegexToken commaToken,
    @Nullable RegexToken maximumRepetitionsToken
  ) {
    super(source, range, modifier);
    this.minimumRepetitionsToken = minimumRepetitionsToken;
    this.minimumRepetitions = Integer.parseInt(minimumRepetitionsToken.getText());
    this.commaToken = commaToken;
    this.maximumRepetitionsToken = maximumRepetitionsToken;
    if (maximumRepetitionsToken == null) {
      this.maximumRepetitions = null;
    } else {
      this.maximumRepetitions = Integer.parseInt(maximumRepetitionsToken.getText());
    }
  }

  @Override
  public int getMinimumRepetitions() {
    return minimumRepetitions;
  }

  @CheckForNull
  @Override
  public Integer getMaximumRepetitions() {
    if (commaToken == null) {
      return minimumRepetitions;
    } else {
      return maximumRepetitions;
    }
  }

  public RegexToken getMinimumRepetitionsToken() {
    return minimumRepetitionsToken;
  }

  public RegexToken getCommaToken() {
    return commaToken;
  }

  public RegexToken getMaximumRepetitionsToken() {
    return maximumRepetitionsToken;
  }

  @Override
  public boolean isFixed() {
    return commaToken == null;
  }

}
