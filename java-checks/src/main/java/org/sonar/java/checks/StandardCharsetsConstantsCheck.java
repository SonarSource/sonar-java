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
package org.sonar.java.checks;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.java.JavaVersionAwareVisitor;
import org.sonar.java.checks.helpers.ConstantUtils;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S4719")
public class StandardCharsetsConstantsCheck extends AbstractMethodDetection implements JavaVersionAwareVisitor {

  private static final String INT = "int";
  private static final String BOOLEAN = "boolean";
  private static final String BYTE_ARRAY = "byte[]";

  private static final String TO_STRING = "toString";
  private static final String WRITE = "write";

  private static final String JAVA_IO_FILE = "java.io.File";
  private static final String JAVA_IO_INPUTSTREAM = "java.io.InputStream";
  private static final String JAVA_IO_OUTPUTSTREAM = "java.io.OutputStream";
  private static final String JAVA_IO_OUTPUTSTREAMWRITER = "java.io.OutputStreamWriter";
  private static final String JAVA_IO_INPUTSTREAMREADER = "java.io.InputStreamReader";
  private static final String JAVA_IO_WRITER = "java.io.Writer";
  private static final String JAVA_IO_READER = "java.io.Reader";
  private static final String JAVA_NIO_CHARSET = "java.nio.charset.Charset";
  private static final String JAVA_NET_URI = "java.net.URI";
  private static final String JAVA_NET_URL = "java.net.URL";
  private static final String JAVA_LANG_STRING = "java.lang.String";
  private static final String JAVA_LANG_STRINGBUFFER = "java.lang.StringBuffer";
  private static final String JAVA_LANG_CHARSEQUENCE = "java.lang.CharSequence";
  private static final String JAVA_UTIL_COLLECTION = "java.util.Collection";

  private static final String COMMONS_CODEC_CHARSETS = "org.apache.commons.codec.Charsets";
  private static final String COMMONS_CODEC_HEX = "org.apache.commons.codec.binary.Hex";
  private static final String COMMONS_CODEC_QUOTEDPRINTABLECODEC = "org.apache.commons.codec.net.QuotedPrintableCodec";
  private static final String COMMONS_IO = "org.apache.commons.io";
  private static final String COMMONS_IO_CHARSETS = COMMONS_IO + ".Charsets";
  private static final String COMMONS_IO_FILEUTILS = COMMONS_IO + ".FileUtils";
  private static final String COMMONS_IO_IOUTILS = COMMONS_IO + ".IOUtils";
  private static final String COMMONS_IO_CHARSEQUENCEINPUTSTREAM = COMMONS_IO + ".input.CharSequenceInputStream";
  private static final String COMMONS_IO_READERINPUTSTREAM = COMMONS_IO + ".input.ReaderInputStream";
  private static final String COMMONS_IO_REVERSEDLINESFILEREADER = COMMONS_IO + ".input.ReversedLinesFileReader";
  private static final String COMMONS_IO_LOCKABLEFILEWRITER = COMMONS_IO + ".output.LockableFileWriter";
  private static final String COMMONS_IO_WRITEROUTPUTSTREAM = COMMONS_IO + ".output.WriterOutputStream";

  private static final List<Charset> STANDARD_CHARSETS = Arrays.asList(
          StandardCharsets.ISO_8859_1,
          StandardCharsets.US_ASCII,
          StandardCharsets.UTF_16,
          StandardCharsets.UTF_16BE,
          StandardCharsets.UTF_16LE,
          StandardCharsets.UTF_8);

  private static final Map<String, String> ALIAS_TO_CONSTANT = createAliasToConstantNameMap();

  private static Map<String, String> createAliasToConstantNameMap() {
    ImmutableMap.Builder<String, String> constantNames = ImmutableMap.builder();
    for (Charset charset : STANDARD_CHARSETS) {
      String constantName = charset.name().replaceAll("-", "_");
      constantNames.put(charset.name(), constantName);

      for (String alias : charset.aliases()) {
        constantNames.put(alias, constantName);
      }
    }

    return constantNames.build();
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.METHOD_INVOCATION, Tree.Kind.NEW_CLASS, Tree.Kind.IDENTIFIER);
  }

  @Override
  public void visitNode(Tree tree) {
    super.visitNode(tree);
    if (hasSemantic() && tree.is(Tree.Kind.IDENTIFIER)) {
      onMemberSelectExpressionFound((IdentifierTree) tree);
    }
  }

  private void onMemberSelectExpressionFound(IdentifierTree identifierTree) {
    Symbol symbol = identifierTree.symbol();
    if (symbol.isVariableSymbol() && symbol.owner().type().is("com.google.common.base.Charsets")) {
      String identifier = identifierTree.name();
      String aliasedIdentigier = identifier.replace("_", "-");
      if (STANDARD_CHARSETS.stream().anyMatch(c -> c.name().equals(aliasedIdentigier))) {
        reportIssue(identifierTree, "Replace \"com.google.common.base.Charsets." + identifier + "\" with \"StandardCharsets." + identifier + "\".");
      }
    }
  }

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return Arrays.asList(
      method(JAVA_NIO_CHARSET, "forName").parameters(JAVA_LANG_STRING),
      method(JAVA_LANG_STRING, "getBytes").parameters(JAVA_LANG_STRING),
      method(JAVA_LANG_STRING, "getBytes").parameters(JAVA_NIO_CHARSET),
      method(COMMONS_CODEC_CHARSETS, "toCharset").parameters(JAVA_LANG_STRING),
      method(COMMONS_IO_CHARSETS, "toCharset").parameters(JAVA_LANG_STRING),
      method(COMMONS_IO_FILEUTILS, "readFileToString").parameters(JAVA_IO_FILE, JAVA_LANG_STRING),
      method(COMMONS_IO_FILEUTILS, "readLines").parameters(JAVA_IO_FILE, JAVA_LANG_STRING),
      method(COMMONS_IO_FILEUTILS, WRITE).parameters(JAVA_IO_FILE, JAVA_LANG_CHARSEQUENCE, JAVA_LANG_STRING),
      method(COMMONS_IO_FILEUTILS, WRITE).parameters(JAVA_IO_FILE, JAVA_LANG_CHARSEQUENCE, JAVA_LANG_STRING, BOOLEAN),
      method(COMMONS_IO_FILEUTILS, "writeStringToFile").parameters(JAVA_IO_FILE, JAVA_LANG_STRING, JAVA_LANG_STRING),
      method(COMMONS_IO_FILEUTILS, "writeStringToFile").parameters(JAVA_IO_FILE, JAVA_LANG_STRING, JAVA_LANG_STRING, BOOLEAN),
      method(COMMONS_IO_IOUTILS, "copy").parameters(JAVA_IO_INPUTSTREAM, JAVA_IO_WRITER, JAVA_LANG_STRING),
      method(COMMONS_IO_IOUTILS, "copy").parameters(JAVA_IO_READER, JAVA_IO_OUTPUTSTREAM, JAVA_LANG_STRING),
      method(COMMONS_IO_IOUTILS, "lineIterator").parameters(JAVA_IO_INPUTSTREAM, JAVA_LANG_STRING),
      method(COMMONS_IO_IOUTILS, "readLines").parameters(JAVA_IO_INPUTSTREAM, JAVA_LANG_STRING),
      method(COMMONS_IO_IOUTILS, "toByteArray").parameters(JAVA_IO_READER, JAVA_LANG_STRING),
      method(COMMONS_IO_IOUTILS, "toCharArray").parameters(JAVA_IO_INPUTSTREAM, JAVA_LANG_STRING),
      method(COMMONS_IO_IOUTILS, "toInputStream").parameters(JAVA_LANG_CHARSEQUENCE, JAVA_LANG_STRING),
      method(COMMONS_IO_IOUTILS, "toInputStream").parameters(JAVA_LANG_STRING, JAVA_LANG_STRING),
      method(COMMONS_IO_IOUTILS, TO_STRING).parameters(BYTE_ARRAY, JAVA_LANG_STRING),
      method(COMMONS_IO_IOUTILS, TO_STRING).parameters(JAVA_IO_INPUTSTREAM, JAVA_LANG_STRING),
      method(COMMONS_IO_IOUTILS, TO_STRING).parameters(JAVA_NET_URI, JAVA_LANG_STRING),
      method(COMMONS_IO_IOUTILS, TO_STRING).parameters(JAVA_NET_URL, JAVA_LANG_STRING),
      method(COMMONS_IO_IOUTILS, WRITE).parameters(BYTE_ARRAY, JAVA_IO_WRITER, JAVA_LANG_STRING),
      method(COMMONS_IO_IOUTILS, WRITE).parameters("char[]", JAVA_IO_OUTPUTSTREAM, JAVA_LANG_STRING),
      method(COMMONS_IO_IOUTILS, WRITE).parameters(JAVA_LANG_CHARSEQUENCE, JAVA_IO_OUTPUTSTREAM, JAVA_LANG_STRING),
      method(COMMONS_IO_IOUTILS, WRITE).parameters(JAVA_LANG_STRING, JAVA_IO_OUTPUTSTREAM, JAVA_LANG_STRING),
      method(COMMONS_IO_IOUTILS, WRITE).parameters(JAVA_LANG_STRINGBUFFER, JAVA_IO_OUTPUTSTREAM, JAVA_LANG_STRING),
      method(COMMONS_IO_IOUTILS, "writeLines").parameters(JAVA_UTIL_COLLECTION, JAVA_LANG_STRING, JAVA_IO_OUTPUTSTREAM, JAVA_LANG_STRING),
      constructor(JAVA_LANG_STRING).parameters(BYTE_ARRAY, JAVA_LANG_STRING),
      constructor(JAVA_LANG_STRING).parameters(BYTE_ARRAY, INT, INT, JAVA_LANG_STRING),
      constructor(JAVA_IO_INPUTSTREAMREADER).parameters(JAVA_IO_INPUTSTREAM, JAVA_LANG_STRING),
      constructor(JAVA_IO_OUTPUTSTREAMWRITER).parameters(JAVA_IO_OUTPUTSTREAM, JAVA_LANG_STRING),
      constructor(COMMONS_IO_CHARSEQUENCEINPUTSTREAM).parameters(JAVA_LANG_CHARSEQUENCE, JAVA_LANG_STRING),
      constructor(COMMONS_IO_CHARSEQUENCEINPUTSTREAM).parameters(JAVA_LANG_CHARSEQUENCE, JAVA_LANG_STRING, INT),
      constructor(COMMONS_IO_READERINPUTSTREAM).parameters(JAVA_IO_READER, JAVA_LANG_STRING),
      constructor(COMMONS_IO_READERINPUTSTREAM).parameters(JAVA_IO_READER, JAVA_LANG_STRING, INT),
      constructor(COMMONS_IO_REVERSEDLINESFILEREADER).parameters(JAVA_IO_FILE, INT, JAVA_LANG_STRING),
      constructor(COMMONS_IO_LOCKABLEFILEWRITER).parameters(JAVA_IO_FILE, JAVA_LANG_STRING),
      constructor(COMMONS_IO_LOCKABLEFILEWRITER).parameters(JAVA_IO_FILE, JAVA_LANG_STRING, BOOLEAN, JAVA_LANG_STRING),
      constructor(COMMONS_IO_WRITEROUTPUTSTREAM).parameters(JAVA_IO_WRITER, JAVA_LANG_STRING),
      constructor(COMMONS_IO_WRITEROUTPUTSTREAM).parameters(JAVA_IO_WRITER, JAVA_LANG_STRING, INT, BOOLEAN),
      constructor(COMMONS_CODEC_HEX).parameters(JAVA_LANG_STRING),
      constructor(COMMONS_CODEC_QUOTEDPRINTABLECODEC).parameters(JAVA_LANG_STRING));
  }

  private static MethodMatcher method(String type, String name) {
    return MethodMatcher.create().typeDefinition(type).name(name);
  }

  private static MethodMatcher constructor(String type) {
    return MethodMatcher.create().typeDefinition(type).name("<init>");
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    checkCall(mit, mit.symbol(), mit.arguments());
  }

  @Override
  protected void onConstructorFound(NewClassTree newClassTree) {
    checkCall(newClassTree, newClassTree.constructorSymbol(), newClassTree.arguments());
  }

  private void checkCall(ExpressionTree callExpression, Symbol symbol, Arguments arguments) {
    getCharsetNameArgument(symbol, arguments)
      .ifPresent(charsetNameArgument -> getConstantName(charsetNameArgument)
        .ifPresent(constantName -> {
          String methodRef = getMethodRef(symbol);
          switch (methodRef) {
            case "Charset.forName":
            case "Charsets.toCharset":
              reportIssue(callExpression, String.format("Replace %s() call with StandardCharsets.%s", methodRef, constantName));
              break;
            case "IOUtils.toString":
              if (arguments.size() == 2 && arguments.get(0).symbolType().is(BYTE_ARRAY)) {
                reportIssue(callExpression, "Replace IOUtils.toString() call with new String(..., StandardCharsets." + constantName + ");");
              } else {
                reportDefaultIssue(charsetNameArgument, constantName);
              }
              break;
            default:
              reportDefaultIssue(charsetNameArgument, constantName);
              break;
          }
        }));
  }

  private void reportDefaultIssue(ExpressionTree charsetNameArgument, String constantName) {
    reportIssue(charsetNameArgument, "Replace charset name argument with StandardCharsets." + constantName);
  }

  private static Optional<ExpressionTree> getCharsetNameArgument(Symbol symbol, Arguments arguments) {
    List<ExpressionTree> stringArguments = arguments.stream().filter(argument -> argument.symbolType().is(JAVA_LANG_STRING)).collect(Collectors.toList());
    if (stringArguments.isEmpty()) {
      return Optional.empty();
    }
    if (stringArguments.size() == 1) {
      return Optional.of(stringArguments.get(0));
    }
    switch (getMethodRef(symbol)) {
      case "FileUtils.writeStringToFile":
      case "IOUtils.toInputStream":
      case "IOUtils.write":
      case "IOUtils.writeLines":
        return Optional.of(Iterables.getLast(stringArguments));
      case "LockableFileWriter.<init>":
        return Optional.of(stringArguments.get(0));
      default:
        return Optional.empty();
    }
  }

  private static String getMethodRef(Symbol symbol) {
    return symbol.enclosingClass().name() + "." + symbol.name();
  }

  private static Optional<String> getConstantName(ExpressionTree argument) {
    String constantValue = ConstantUtils.resolveAsStringConstant(argument);
    return Optional.ofNullable(ALIAS_TO_CONSTANT.get(constantValue));
  }

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava7Compatible();
  }
}
