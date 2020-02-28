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
package org.sonar.java.se.checks;

import org.junit.Test;
import org.sonar.java.se.JavaCheckVerifier;

public class XxeProcessingCheckTest {

  @Test
  public void Xml_input_factory() {
    JavaCheckVerifier.verify("src/test/files/se/XxeProcessingCheck_XmlInputFactory.java", new XxeProcessingCheck());
  }

  @Test
  public void document_builder_factory() {
    JavaCheckVerifier.verify("src/test/files/se/XxeProcessingCheck_DocumentBuilderFactory.java", new XxeProcessingCheck());
  }

  @Test
  public void sax_parser() {
    JavaCheckVerifier.verify("src/test/files/se/XxeProcessingCheck_SaxParser.java", new XxeProcessingCheck());
  }

  @Test
  public void schema_factory() {
    JavaCheckVerifier.verify("src/test/files/se/XxeProcessingCheck_SchemaFactory_Validator.java", new XxeProcessingCheck());
  }

  @Test
  public void transformer_factory() {
    JavaCheckVerifier.verify("src/test/files/se/XxeProcessingCheck_TransformerFactory.java", new XxeProcessingCheck());
  }

  @Test
  public void xml_reader() {
    JavaCheckVerifier.verify("src/test/files/se/XxeProcessingCheck_XmlReader.java", new XxeProcessingCheck());
  }

  @Test
  public void sax_builder() {
    JavaCheckVerifier.verify("src/test/files/se/XxeProcessingCheck_SaxBuilder.java", new XxeProcessingCheck());
  }

  @Test
  public void sax_reader() {
    JavaCheckVerifier.verify("src/test/files/se/XxeProcessingCheck_SaxReader.java", new XxeProcessingCheck());
  }

}
