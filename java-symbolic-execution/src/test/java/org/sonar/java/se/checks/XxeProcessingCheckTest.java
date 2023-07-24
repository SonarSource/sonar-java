/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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
package org.sonar.java.se.checks;

import org.junit.jupiter.api.Test;
import org.sonar.java.se.SECheckVerifier;
import org.sonar.java.se.utils.SETestUtils;
import org.sonar.java.checks.verifier.TestUtils;

class XxeProcessingCheckTest {

  @Test
  void Xml_input_factory() {
    SECheckVerifier.newVerifier()
      .onFile(TestUtils.mainCodeSourcesPath("symbolicexecution/checks/S2755_XxeProcessingCheck_XmlInputFactory.java"))
      .withCheck(new XxeProcessingCheck())
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyIssues();
  }

  @Test
  void document_builder_factory() {
    SECheckVerifier.newVerifier()
      .onFile(TestUtils.mainCodeSourcesPath("symbolicexecution/checks/S2755_XxeProcessingCheck_DocumentBuilderFactory.java"))
      .withCheck(new XxeProcessingCheck())
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyIssues();
  }

  @Test
  void sax_parser() {
    SECheckVerifier.newVerifier()
      .onFile(TestUtils.mainCodeSourcesPath("symbolicexecution/checks/S2755_XxeProcessingCheck_SaxParser.java"))
      .withCheck(new XxeProcessingCheck())
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyIssues();
  }

  @Test
  void schema_factory() {
    SECheckVerifier.newVerifier()
      .onFile(TestUtils.mainCodeSourcesPath("symbolicexecution/checks/S2755_XxeProcessingCheck_SchemaFactory_Validator.java"))
      .withCheck(new XxeProcessingCheck())
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyIssues();
  }

  @Test
  void transformer_factory() {
    SECheckVerifier.newVerifier()
      .onFile(TestUtils.mainCodeSourcesPath("symbolicexecution/checks/S2755_XxeProcessingCheck_TransformerFactory.java"))
      .withCheck(new XxeProcessingCheck())
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyIssues();
  }

  @Test
  void xml_reader() {
    SECheckVerifier.newVerifier()
      .onFile(TestUtils.mainCodeSourcesPath("symbolicexecution/checks/S2755_XxeProcessingCheck_XmlReader.java"))
      .withCheck(new XxeProcessingCheck())
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyIssues();
  }

  @Test
  void sax_builder() {
    SECheckVerifier.newVerifier()
      .onFile(TestUtils.mainCodeSourcesPath("symbolicexecution/checks/S2755_XxeProcessingCheck_SaxBuilder.java"))
      .withCheck(new XxeProcessingCheck())
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyIssues();
  }

  @Test
  void sax_reader() {
    SECheckVerifier.newVerifier()
      .onFile(TestUtils.mainCodeSourcesPath("symbolicexecution/checks/S2755_XxeProcessingCheck_SaxReader.java"))
      .withCheck(new XxeProcessingCheck())
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyIssues();
  }

  @Test
  void document_builder_factory_java_11() {
    SECheckVerifier.newVerifier()
      .onFile(TestUtils.mainCodeSourcesPath("symbolicexecution/checks/S2755_XxeProcessingCheck_DocumentBuilderFactory_version_11.java"))
      .withCheck(new XxeProcessingCheck())
      .withClassPath(SETestUtils.CLASS_PATH)
      .withJavaVersion(11)
      .verifyIssues();
  }

  @Test
  void document_builder_factory_java_13() {
    SECheckVerifier.newVerifier()
      .onFile(TestUtils.mainCodeSourcesPath("symbolicexecution/checks/S2755_XxeProcessingCheck_DocumentBuilderFactory_version_13.java"))
      .withCheck(new XxeProcessingCheck())
      .withClassPath(SETestUtils.CLASS_PATH)
      .withJavaVersion(13)
      .verifyIssues();
  }

  @Test
  void non_compiling_code() {
    SECheckVerifier.newVerifier()
      .onFile(TestUtils.nonCompilingTestSourcesPath("symbolicexecution/checks/S2755_XxeProcessingCheck_DocumentBuilderFactory.java"))
      .withCheck(new XxeProcessingCheck())
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyNoIssues();
  }

}
