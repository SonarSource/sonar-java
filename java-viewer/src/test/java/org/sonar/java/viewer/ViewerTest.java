/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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
package org.sonar.java.viewer;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class ViewerTest {

  @Rule
  public final ExpectedException exception = ExpectedException.none();

  @Test
  public void code_without_method_trigger_an_exception() {
    exception.expect(NullPointerException.class);
    exception.expectMessage("Unable to find a method in first class.");

    Viewer.getValues("class A { }");
  }

  @Test
  public void code_with_method_provide_everything_but_error_messages() {
    String source = "class A {"
      + "  int foo(boolean b) {"
      + "    if (b) {"
      + "      return 42;"
      + "    }"
      + "    return 21;"
      + "  }"
      + "}";
    Map<String, String> values = Viewer.getValues(source);

    assertThat(values.get("cfg")).isNotEmpty();

    assertThat(values.get("dotAST")).isNotEmpty();
    assertThat(values.get("dotCFG")).isNotEmpty();
    assertThat(values.get("dotEG")).isNotEmpty();

    assertThat(values.get("errorMessage")).isEmpty();
    assertThat(values.get("errorStackTrace")).isEmpty();
  }

  @Test
  public void values_with_error() {
    String message = "my exception message";
    Map<String, String> values = Viewer.getErrorValues(new Exception(message));

    assertThat(values.get("cfg")).isNull();

    assertThat(values.get("dotAST")).isNull();
    assertThat(values.get("dotCFG")).isNull();
    assertThat(values.get("dotEG")).isNull();

    assertThat(values.get("errorMessage")).isEqualTo(message);
    assertThat(values.get("errorStackTrace")).startsWith("java.lang.Exception: " + message);
  }

}
