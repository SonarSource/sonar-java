/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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

import org.sonar.check.Rule;

@Rule(key = "S6373")
public class AllowXMLInclusionCheck extends SECheck {

  /*
     TODO: we should ensure that we add a XxeProperty.FeatureXInclude constraint for the following cases:

     1) Setter with one argument
          .ofSubTypes("javax.xml.parsers.DocumentBuilderFactory", "javax.xml.parsers.SAXParserFactory")
          .names("setXIncludeAware")
          .addParametersMatcher("boolean")

     2) Set feature with two arguments, the first one matching "http://apache.org/xml/features/xinclude"
          .ofSubTypes("javax.xml.stream.XMLInputFactory")
          .names("setProperty")
          .addParametersMatcher("java.lang.String", "java.lang.Object")

          .ofSubTypes(
            "javax.xml.transform.TransformerFactory",
            "javax.xml.validation.SchemaFactory",
            "org.dom4j.io.SAXReader",
            "org.jdom2.input.SAXBuilder")
          .names("setFeature")
          .addParametersMatcher("java.lang.String", "boolean")

     TODO: we should report when the last argument of "setXIncludeAware|setProperty|setFeature" is `true`
           and there's no XxeEntityResolver.CUSTOM_ENTITY_RESOLVER constraints
           with the message "Disable the inclusion of files in XML processing."

     TODO: update the rspec to add an exception about EntityResolver
   */

}
