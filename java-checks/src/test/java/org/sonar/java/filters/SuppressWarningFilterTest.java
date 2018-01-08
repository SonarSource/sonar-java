/*
 * SonarQube Java
 * Copyright (C) 2012-2018 SonarSource SA
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
package org.sonar.java.filters;

import org.junit.Test;

import com.sonar.sslr.api.Trivia;

import org.sonar.check.Rule;
import org.sonar.java.checks.SuppressWarningsCheck;
import org.sonar.java.checks.TodoTagPresenceCheck;
import org.sonar.java.checks.naming.BadConstantNameCheck;
import org.sonar.java.checks.unused.UnusedPrivateFieldCheck;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Collections;
import java.util.List;

public class SuppressWarningFilterTest {

  @Test
  public void verify() {
    FilterVerifier.verify("src/test/files/filters/SuppressWarningFilter.java", new SuppressWarningFilter(),
      // activated rules
      new UnusedPrivateFieldCheck(),
      new BadConstantNameCheck(),
      new SuppressWarningsCheck(),
      new TodoTagPresenceCheck()
    );
  }

}
