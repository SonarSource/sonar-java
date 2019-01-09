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
package org.sonar.java.ast.parser.grammar.statements;

import org.junit.Test;
import org.sonar.java.ast.parser.JavaLexer;
import org.sonar.sslr.grammar.LexerlessGrammarBuilder;

import static org.sonar.sslr.tests.Assertions.assertThat;

public class TryStatementTest {

  @Test
  public void ok() {
    LexerlessGrammarBuilder b = JavaLexer.createGrammarBuilder();

    assertThat(b, JavaLexer.STATEMENT)
      .matches("try {} catch (Exception e) {} catch (Exception e) {} finally {}")
      .matches("try {} catch (Exception e) {} finally {}")
      .matches("try {} catch (Exception e) {}")
      .matches("try {} finally {}");
  }

  @Test
  public void realLife() {
    // Java 7: multi-catch
    assertThat(JavaLexer.STATEMENT)
      .matches("try {} catch (ClassNotFoundException | IllegalAccessException ex) {}");
    // Java 7: try-with-resources
    assertThat(JavaLexer.STATEMENT)
      .matches("try (Resource resource = new Resource()) {}")
      .matches("try (final Resource resource = new Resource()) {}")
      .matches("try (@Nonnull Resource resource[] = new Resource()) {}")
      .matches("try (Resource resource = new Resource()) {} catch (Exception e) {} finally {}");
  }

  @Test
  public void java9_resources_without_initializer() {
    assertThat(JavaLexer.STATEMENT)
      .matches("try (resource) {}")
      .matches("try (new A().field) {}")
      .matches("try (super.resource;) {}")
      .matches("try (super.resource; Resource resource = new Resource()) {}")
      .matches("try (super.resource; Resource resource = new Resource();) {}");
  }

  @Test
  public void java10_resources_declared_with_var() {
    assertThat(JavaLexer.STATEMENT)
      .matches("try (var resource = new Resource()) {}");
  }
}
