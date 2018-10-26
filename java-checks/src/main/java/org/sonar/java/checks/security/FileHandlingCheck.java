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
package org.sonar.java.checks.security;

import java.util.Arrays;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;

@Rule(key = "S4797")
public class FileHandlingCheck extends AbstractMethodDetection {

  private static final String MESSAGE = "Make sure this file handling is safe here.";
  private static final String JAVA_IO_FILE = "java.io.File";
  private static final String INIT = "<init>";
  private static final String APACHE_FILEUTILS = "org.apache.commons.io.FileUtils";
  private static final String GUAVA_RESOURCES = "com.google.common.io.Resources";
  private static final String JAVA_NET_URL = "java.net.URL";
  private static final String JAVA_LANG_STRING = "java.lang.String";
  private static final String JAVA_LANG_STRING_ARRAY = "java.lang.String[]";
  private static final String NIO_CHARSET = "java.nio.charset.Charset";
  private static final String GUAVA_FILES = "com.google.common.io.Files";
  private static final String BOOLEAN = "boolean";

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return Arrays.asList(
      MethodMatcher.create().typeDefinition(JAVA_IO_FILE).name(INIT).parameters(JAVA_LANG_STRING),
      MethodMatcher.create().typeDefinition(JAVA_IO_FILE).name(INIT).parameters(JAVA_LANG_STRING, JAVA_LANG_STRING),
      MethodMatcher.create().typeDefinition(JAVA_IO_FILE).name(INIT).parameters("java.net.URI"),
      MethodMatcher.create().typeDefinition(JAVA_IO_FILE).name("createTempFile").parameters(JAVA_LANG_STRING, JAVA_LANG_STRING),
      MethodMatcher.create().typeDefinition("java.nio.file.Paths").name("get").parameters(JAVA_LANG_STRING, JAVA_LANG_STRING_ARRAY),
      MethodMatcher.create().typeDefinition("java.nio.file.Paths").name("get").parameters("java.net.URI"),
      MethodMatcher.create().typeDefinition("java.nio.file.FileSystem").name("getRootDirectories").withoutParameter(),
      MethodMatcher.create().typeDefinition("java.nio.file.FileSystem").name("getPath").parameters(JAVA_LANG_STRING, JAVA_LANG_STRING_ARRAY),
      MethodMatcher.create().typeDefinition("java.nio.file.Files").name("createTempDirectory").parameters(JAVA_LANG_STRING, "java.nio.file.attribute.FileAttribute[]"),
      MethodMatcher.create().typeDefinition("java.nio.file.Files").name("createTempFile")
        .parameters(JAVA_LANG_STRING, JAVA_LANG_STRING, "java.nio.file.attribute.FileAttribute[]"),

      MethodMatcher.create().typeDefinition("java.io.FileInputStream").name(INIT).parameters(JAVA_LANG_STRING),
      MethodMatcher.create().typeDefinition("java.io.FileOutputStream").name(INIT).parameters(JAVA_LANG_STRING),
      MethodMatcher.create().typeDefinition("java.io.FileOutputStream").name(INIT).parameters(JAVA_LANG_STRING, BOOLEAN),
      MethodMatcher.create().typeDefinition("java.io.FileReader").name(INIT).parameters(JAVA_LANG_STRING),
      MethodMatcher.create().typeDefinition("java.io.FileWriter").name(INIT).parameters(JAVA_LANG_STRING),
      MethodMatcher.create().typeDefinition("java.io.FileWriter").name(INIT).parameters(JAVA_LANG_STRING, BOOLEAN),
      MethodMatcher.create().typeDefinition("java.io.RandomAccessFile").name(INIT).parameters(JAVA_LANG_STRING, JAVA_LANG_STRING),

      MethodMatcher.create().typeDefinition(APACHE_FILEUTILS).name("getFile").parameters(JAVA_LANG_STRING_ARRAY),
      MethodMatcher.create().typeDefinition(APACHE_FILEUTILS).name("getTempDirectory").withoutParameter(),
      MethodMatcher.create().typeDefinition(APACHE_FILEUTILS).name("getUserDirectory").withoutParameter(),

      MethodMatcher.create().typeDefinition(GUAVA_FILES).name("createTempDir").withoutParameter(),
      MethodMatcher.create().typeDefinition(GUAVA_FILES).name("fileTreeTraverser").withoutParameter(),
      MethodMatcher.create().typeDefinition(GUAVA_FILES).name("fileTraverser").withoutParameter(),
      MethodMatcher.create().typeDefinition("com.google.common.io.MoreFiles").name("directoryTreeTraverser").withoutParameter(),
      MethodMatcher.create().typeDefinition("com.google.common.io.MoreFiles").name("fileTraverser").withoutParameter(),

      MethodMatcher.create().typeDefinition(GUAVA_RESOURCES).name("asByteSource").parameters(JAVA_NET_URL),
      MethodMatcher.create().typeDefinition(GUAVA_RESOURCES).name("asCharSource").parameters(JAVA_NET_URL, NIO_CHARSET),
      MethodMatcher.create().typeDefinition(GUAVA_RESOURCES).name("copy").parameters(JAVA_NET_URL, "java.io.OutputStream"),
      MethodMatcher.create().typeDefinition(GUAVA_RESOURCES).name("getResource").parameters("java.lang.Class", JAVA_LANG_STRING),
      MethodMatcher.create().typeDefinition(GUAVA_RESOURCES).name("getResource").parameters(JAVA_LANG_STRING),
      MethodMatcher.create().typeDefinition(GUAVA_RESOURCES).name("readLines").parameters(JAVA_NET_URL, NIO_CHARSET),
      MethodMatcher.create().typeDefinition(GUAVA_RESOURCES).name("readLines")
        .parameters(JAVA_NET_URL, NIO_CHARSET, "com.google.common.io.LineProcessor"),
      MethodMatcher.create().typeDefinition(GUAVA_RESOURCES).name("toByteArray").parameters(JAVA_NET_URL),
      MethodMatcher.create().typeDefinition(GUAVA_RESOURCES).name("toString").parameters(JAVA_NET_URL, NIO_CHARSET),

      MethodMatcher.create().typeDefinition("com.google.common.io.FileBackedOutputStream").name(INIT).parameters("int"),
      MethodMatcher.create().typeDefinition("com.google.common.io.FileBackedOutputStream").name(INIT).parameters("int", BOOLEAN)
      );
  }

  @Override
  protected void onConstructorFound(NewClassTree newClassTree) {
    reportIssue(newClassTree.identifier(), MESSAGE);
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    reportIssue(ExpressionUtils.methodName(mit), MESSAGE);
  }

}
