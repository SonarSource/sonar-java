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
package org.sonar.java.checks.verifier.internal;

public class QuickFixExpectation {
  String prefix;
  String suffix;
  String before;
  String after;

  public QuickFixExpectation() {
    prefix = "";
    suffix = "";
  }

  public QuickFixExpectation(String prefix, String suffix) {
    this.prefix = prefix;
    this.suffix = suffix;
  }

  public QuickFixExpectation setBefore(String before) {
    this.before = before;
    return this;
  }

  public QuickFixExpectation setAfter(String after) {
    this.after = after;
    return this;
  }

  public String getBefore() {
    return prefix + before + suffix;
  }

  public String getAfter() {
    return prefix + after + suffix;
  }
}
