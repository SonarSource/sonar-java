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
package org.sonar.java.checks;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.stream.Stream;
import org.sonar.check.Rule;
import org.sonar.java.JavaVersionAwareVisitor;
import org.sonar.java.checks.helpers.ConstantUtils;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.resolve.JavaSymbol;
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
  private static final String COMMONS_IO_CHARSETS = "org.apache.commons.io.Charsets";
  private static final String COMMONS_IO_FILEUTILS = "org.apache.commons.io.FileUtils";
  private static final String COMMONS_IO_IOUTILS = "org.apache.commons.io.IOUtils";
  private static final String COMMONS_IO_CHARSEQUENCEINPUTSTREAM = "org.apache.commons.io.input.CharSequenceInputStream";
  private static final String COMMONS_IO_READERINPUTSTREAM = "org.apache.commons.io.input.ReaderInputStream";
  private static final String COMMONS_IO_REVERSEDLINESFILEREADER = "org.apache.commons.io.input.ReversedLinesFileReader";
  private static final String COMMONS_IO_LOCKABLEFILEWRITER = "org.apache.commons.io.output.LockableFileWriter";
  private static final String COMMONS_IO_WRITEROUTPUTSTREAM = "org.apache.commons.io.output.WriterOutputStream";

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
    return ImmutableList.of(Tree.Kind.METHOD_INVOCATION, Tree.Kind.NEW_CLASS, Tree.Kind.IDENTIFIER);
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
      if (STANDARD_CHARSETS.stream().anyMatch(c -> c.name().equals(identifier.replace("_", "-")))) {
        reportIssue(identifierTree, "Replace \"com.google.common.base.Charsets." + identifier + "\" with \"StandardCharsets." + identifier + "\".");
      }
    }
  }

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return Arrays.asList(
      MethodMatcher.create().typeDefinition(JAVA_NIO_CHARSET).name("forName").parameters(JAVA_LANG_STRING),
      MethodMatcher.create().typeDefinition(JAVA_LANG_STRING).name("getBytes").parameters(JAVA_LANG_STRING),
      MethodMatcher.create().typeDefinition(JAVA_LANG_STRING).name("getBytes").parameters(JAVA_NIO_CHARSET),
      MethodMatcher.create().typeDefinition(COMMONS_CODEC_CHARSETS).name("toCharset").parameters(JAVA_LANG_STRING),
      MethodMatcher.create().typeDefinition(COMMONS_IO_CHARSETS).name("toCharset").parameters(JAVA_LANG_STRING),
      MethodMatcher.create().typeDefinition(COMMONS_IO_FILEUTILS).name("readFileToString").parameters(JAVA_IO_FILE, JAVA_LANG_STRING),
      MethodMatcher.create().typeDefinition(COMMONS_IO_FILEUTILS).name("readLines").parameters(JAVA_IO_FILE, JAVA_LANG_STRING),
      MethodMatcher.create().typeDefinition(COMMONS_IO_FILEUTILS).name("write").parameters(JAVA_IO_FILE, JAVA_LANG_CHARSEQUENCE, JAVA_LANG_STRING),
      MethodMatcher.create().typeDefinition(COMMONS_IO_FILEUTILS).name("write").parameters(JAVA_IO_FILE, JAVA_LANG_CHARSEQUENCE, JAVA_LANG_STRING, "boolean"),
      MethodMatcher.create().typeDefinition(COMMONS_IO_FILEUTILS).name("writeStringToFile").parameters(JAVA_IO_FILE, JAVA_LANG_STRING, JAVA_LANG_STRING),
      MethodMatcher.create().typeDefinition(COMMONS_IO_FILEUTILS).name("writeStringToFile").parameters(JAVA_IO_FILE, JAVA_LANG_STRING, JAVA_LANG_STRING, "boolean"),
      MethodMatcher.create().typeDefinition(COMMONS_IO_IOUTILS).name("copy").parameters(JAVA_IO_INPUTSTREAM, JAVA_IO_WRITER, JAVA_LANG_STRING),
      MethodMatcher.create().typeDefinition(COMMONS_IO_IOUTILS).name("copy").parameters(JAVA_IO_READER, JAVA_IO_OUTPUTSTREAM, JAVA_LANG_STRING),
      MethodMatcher.create().typeDefinition(COMMONS_IO_IOUTILS).name("lineIterator").parameters(JAVA_IO_INPUTSTREAM, JAVA_LANG_STRING),
      MethodMatcher.create().typeDefinition(COMMONS_IO_IOUTILS).name("readLines").parameters(JAVA_IO_INPUTSTREAM, JAVA_LANG_STRING),
      MethodMatcher.create().typeDefinition(COMMONS_IO_IOUTILS).name("toByteArray").parameters(JAVA_IO_READER, JAVA_LANG_STRING),
      MethodMatcher.create().typeDefinition(COMMONS_IO_IOUTILS).name("toCharArray").parameters(JAVA_IO_INPUTSTREAM, JAVA_LANG_STRING),
      MethodMatcher.create().typeDefinition(COMMONS_IO_IOUTILS).name("toInputStream").parameters(JAVA_LANG_CHARSEQUENCE, JAVA_LANG_STRING),
      MethodMatcher.create().typeDefinition(COMMONS_IO_IOUTILS).name("toInputStream").parameters(JAVA_LANG_STRING, JAVA_LANG_STRING),
      MethodMatcher.create().typeDefinition(COMMONS_IO_IOUTILS).name("toString").parameters("byte[]", JAVA_LANG_STRING),
      MethodMatcher.create().typeDefinition(COMMONS_IO_IOUTILS).name("toString").parameters(JAVA_IO_INPUTSTREAM, JAVA_LANG_STRING),
      MethodMatcher.create().typeDefinition(COMMONS_IO_IOUTILS).name("toString").parameters(JAVA_NET_URI, JAVA_LANG_STRING),
      MethodMatcher.create().typeDefinition(COMMONS_IO_IOUTILS).name("toString").parameters(JAVA_NET_URL, JAVA_LANG_STRING),
      MethodMatcher.create().typeDefinition(COMMONS_IO_IOUTILS).name("write").parameters("byte[]", JAVA_IO_WRITER, JAVA_LANG_STRING),
      MethodMatcher.create().typeDefinition(COMMONS_IO_IOUTILS).name("write").parameters("char[]", JAVA_IO_OUTPUTSTREAM, JAVA_LANG_STRING),
      MethodMatcher.create().typeDefinition(COMMONS_IO_IOUTILS).name("write").parameters(JAVA_LANG_CHARSEQUENCE, JAVA_IO_OUTPUTSTREAM, JAVA_LANG_STRING),
      MethodMatcher.create().typeDefinition(COMMONS_IO_IOUTILS).name("write").parameters(JAVA_LANG_STRING, JAVA_IO_OUTPUTSTREAM, JAVA_LANG_STRING),
      MethodMatcher.create().typeDefinition(COMMONS_IO_IOUTILS).name("write").parameters(JAVA_LANG_STRINGBUFFER, JAVA_IO_OUTPUTSTREAM, JAVA_LANG_STRING),
      MethodMatcher.create().typeDefinition(COMMONS_IO_IOUTILS).name("writeLines").parameters(JAVA_UTIL_COLLECTION, JAVA_LANG_STRING, JAVA_IO_OUTPUTSTREAM, JAVA_LANG_STRING),
      constructor(JAVA_LANG_STRING).parameters("byte[]", JAVA_LANG_STRING),
      constructor(JAVA_LANG_STRING).parameters("byte[]", "int", "int", JAVA_LANG_STRING),
      constructor(JAVA_IO_INPUTSTREAMREADER).parameters(JAVA_IO_INPUTSTREAM, JAVA_LANG_STRING),
      constructor(JAVA_IO_OUTPUTSTREAMWRITER).parameters(JAVA_IO_OUTPUTSTREAM, JAVA_LANG_STRING),
      constructor(COMMONS_IO_CHARSEQUENCEINPUTSTREAM).parameters(JAVA_LANG_CHARSEQUENCE, JAVA_LANG_STRING),
      constructor(COMMONS_IO_CHARSEQUENCEINPUTSTREAM).parameters(JAVA_LANG_CHARSEQUENCE, JAVA_LANG_STRING, "int"),
      constructor(COMMONS_IO_READERINPUTSTREAM).parameters(JAVA_IO_READER, JAVA_LANG_STRING),
      constructor(COMMONS_IO_READERINPUTSTREAM).parameters(JAVA_IO_READER, JAVA_LANG_STRING, "int"),
      constructor(COMMONS_IO_REVERSEDLINESFILEREADER).parameters(JAVA_IO_FILE, "int", JAVA_LANG_STRING),
      constructor(COMMONS_IO_LOCKABLEFILEWRITER).parameters(JAVA_IO_FILE, JAVA_LANG_STRING),
      constructor(COMMONS_IO_LOCKABLEFILEWRITER).parameters(JAVA_IO_FILE, JAVA_LANG_STRING, "boolean", JAVA_LANG_STRING),
      constructor(COMMONS_IO_WRITEROUTPUTSTREAM).parameters(JAVA_IO_WRITER, JAVA_LANG_STRING),
      constructor(COMMONS_IO_WRITEROUTPUTSTREAM).parameters(JAVA_IO_WRITER, JAVA_LANG_STRING, "int", "boolean"),
      constructor(COMMONS_CODEC_HEX).parameters(JAVA_LANG_STRING),
      constructor(COMMONS_CODEC_QUOTEDPRINTABLECODEC).parameters(JAVA_LANG_STRING));
  }

  private static MethodMatcher constructor(String type) {
    return MethodMatcher.create().typeDefinition(type).name("<init>");
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    try {
      checkCall(mit, mit.symbol(), mit.arguments());
    } catch (IllegalStateException e) {
      // TODO
      throw new RuntimeException("Could not check invocation at " + mit.firstToken().line(), e);
    }
  }

  @Override
  protected void onConstructorFound(NewClassTree newClassTree) {
    try {
      checkCall(newClassTree, newClassTree.constructorSymbol(), newClassTree.arguments());
    } catch (IllegalStateException e) {
      // TODO
      throw new RuntimeException("Could not check invocation at " + newClassTree.firstToken().line(), e);
    }
  }

  private void checkCall(ExpressionTree callExpression, Symbol symbol, Arguments arguments) {
    ExpressionTree charsetNameArgument = getCharsetNameArgument(symbol, arguments);

    // TODO
    //System.out.println(symbol.name() + " " + charsetNameArgument.firstToken().line() + ":" + charsetNameArgument.firstToken().column());

    String constantName = getConstantName(charsetNameArgument);
    if (constantName != null) {
      String methodRef = getMethodRef(symbol);
      switch (methodRef) {
        case "Charset.forName":
          reportIssue(callExpression, "Replace Charset.forName() call with StandardCharsets." + constantName);
          break;
        case "Charsets.toCharset":
          reportIssue(callExpression, "Replace Charsets.toCharset() call with StandardCharsets." + constantName);
          break;
        case "IOUtils.toString":
          if (arguments.size() == 2 && arguments.get(0).symbolType().is("byte[]")) {
            reportIssue(callExpression, "Replace IOUtils.toString() call with new String(..., StandardCharsets." + constantName + ");");
          } else {
            reportDefaultIssue(charsetNameArgument, constantName);
          }
          break;
        default:
          reportDefaultIssue(charsetNameArgument, constantName);
          break;
      }
    }
  }

  private void reportDefaultIssue(ExpressionTree charsetNameArgument, String constantName) {
    reportIssue(charsetNameArgument, "Replace charset name argument with StandardCharsets." + constantName);
  }

  private ExpressionTree getCharsetNameArgument(Symbol symbol, Arguments arguments) {
    BinaryOperator<ExpressionTree> reducer;

    String symbolRef = getMethodRef(symbol);
    switch (symbolRef) {
      case "FileUtils.writeStringToFile":
      case "IOUtils.toInputStream":
      case "IOUtils.write":
      case "IOUtils.writeLines":
        reducer = (previous, current) -> current;
        break;
      case "LockableFileWriter.<init>":
        reducer = (previous, current) -> previous;
        break;
      default:
        reducer = (previous, current) -> {
          throw new IllegalStateException("Could not identify which string argument of " + symbolRef + " is the charset name");
        };
    }

    Stream<ExpressionTree> stringArgumentStream = arguments.stream().filter(argument -> argument.symbolType().is(JAVA_LANG_STRING));
    return stringArgumentStream.reduce(reducer).orElseGet(() -> {
      // No String argument, so this must be an overload that has a Charset argument
      return arguments.get(arguments.size() - 1);
    });
  }

  private String getMethodRef(Symbol symbol) {
    return symbol.enclosingClass().name() + "." + symbol.name();
  }

  private String getFullyQualifiedMethodRef(Symbol symbol) {
    return ((JavaSymbol.TypeJavaSymbol)symbol.owner()).getFullyQualifiedName() + "." + symbol.name();
  }

  private static String getConstantName(ExpressionTree argument) {
    String constantValue = ConstantUtils.resolveAsStringConstant(argument);
    return ALIAS_TO_CONSTANT.get(constantValue);
  }

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava7Compatible();
  }
}
