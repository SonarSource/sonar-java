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
package org.sonar.java.checks;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.java.JavaVersionAwareVisitor;
import org.sonarsource.analyzer.commons.collections.ListUtils;
import org.sonarsource.analyzer.commons.collections.MapBuilder;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
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
  private static final String JAVA_IO_BYTEARRAYOUTPUTSTREAM = "java.io.ByteArrayOutputStream";
  private static final String JAVA_IO_WRITER = "java.io.Writer";
  private static final String JAVA_IO_READER = "java.io.Reader";
  private static final String JAVA_NIO_CHARSET = "java.nio.charset.Charset";
  private static final String JAVA_NIO_FILE_PATH = "java.nio.file.Path";
  private static final String JAVA_NIO_CHANNELS_READABLEBYTECHANNEL = "java.nio.channels.ReadableByteChannel";
  private static final String JAVA_NET_URI = "java.net.URI";
  private static final String JAVA_NET_URL = "java.net.URL";
  private static final String JAVA_LANG_STRING = "java.lang.String";
  private static final String JAVA_LANG_STRINGBUFFER = "java.lang.StringBuffer";
  private static final String JAVA_LANG_CHARSEQUENCE = "java.lang.CharSequence";
  private static final String JAVA_UTIL_COLLECTION = "java.util.Collection";
  private static final String JAVA_UTIL_SCANNER = "java.util.Scanner";

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
  private static final int JAVA_10 = 10;

  private static final MethodMatchers JAVA10_METHOD_MATCHERS = MethodMatchers.or(
    MethodMatchers.create().ofTypes(JAVA_IO_BYTEARRAYOUTPUTSTREAM).names(TO_STRING)
      .addParametersMatcher(JAVA_LANG_STRING)
      .build(),
    MethodMatchers.create().ofTypes(JAVA_UTIL_SCANNER).constructor()
      .addParametersMatcher(JAVA_IO_INPUTSTREAM, JAVA_LANG_STRING)
      .addParametersMatcher(JAVA_IO_FILE, JAVA_LANG_STRING)
      .addParametersMatcher(JAVA_NIO_FILE_PATH, JAVA_LANG_STRING)
      .addParametersMatcher(JAVA_NIO_CHANNELS_READABLEBYTECHANNEL, JAVA_LANG_STRING)
      .build());

  private static final MethodMatchers JAVA8_METHOD_MATCHERS = MethodMatchers.or(
    MethodMatchers.create().ofTypes(JAVA_NIO_CHARSET).names("forName")
      .addParametersMatcher(JAVA_LANG_STRING)
      .build(),
    MethodMatchers.create().ofTypes(JAVA_LANG_STRING).names("getBytes")
      .addParametersMatcher(JAVA_LANG_STRING)
      .addParametersMatcher(JAVA_NIO_CHARSET)
      .build(),
    MethodMatchers.create().ofTypes(COMMONS_CODEC_CHARSETS, COMMONS_IO_CHARSETS).names("toCharset")
      .addParametersMatcher(JAVA_LANG_STRING)
      .build(),
    MethodMatchers.create().ofTypes(COMMONS_IO_FILEUTILS).names("readFileToString", "readLines")
      .addParametersMatcher(JAVA_IO_FILE, JAVA_LANG_STRING)
      .build(),
    MethodMatchers.create().ofTypes(COMMONS_IO_FILEUTILS).names(WRITE)
      .addParametersMatcher(JAVA_IO_FILE, JAVA_LANG_CHARSEQUENCE, JAVA_LANG_STRING)
      .addParametersMatcher(JAVA_IO_FILE, JAVA_LANG_CHARSEQUENCE, JAVA_LANG_STRING, BOOLEAN)
      .build(),
    MethodMatchers.create().ofTypes(COMMONS_IO_FILEUTILS).names("writeStringToFile")
      .addParametersMatcher(JAVA_IO_FILE, JAVA_LANG_STRING, JAVA_LANG_STRING)
      .addParametersMatcher(JAVA_IO_FILE, JAVA_LANG_STRING, JAVA_LANG_STRING, BOOLEAN)
      .build(),
    MethodMatchers.create().ofTypes(COMMONS_IO_IOUTILS).names("copy")
      .addParametersMatcher(JAVA_IO_INPUTSTREAM, JAVA_IO_WRITER, JAVA_LANG_STRING)
      .addParametersMatcher(JAVA_IO_READER, JAVA_IO_OUTPUTSTREAM, JAVA_LANG_STRING)
      .build(),
    MethodMatchers.create().ofTypes(COMMONS_IO_IOUTILS).names("lineIterator")
      .addParametersMatcher(JAVA_IO_INPUTSTREAM, JAVA_LANG_STRING)
      .build(),
    MethodMatchers.create().ofTypes(COMMONS_IO_IOUTILS).names("readLines")
      .addParametersMatcher(JAVA_IO_INPUTSTREAM, JAVA_LANG_STRING)
      .build(),
    MethodMatchers.create().ofTypes(COMMONS_IO_IOUTILS).names("toByteArray")
      .addParametersMatcher(JAVA_IO_READER, JAVA_LANG_STRING)
      .build(),
    MethodMatchers.create().ofTypes(COMMONS_IO_IOUTILS).names("toCharArray")
      .addParametersMatcher(JAVA_IO_INPUTSTREAM, JAVA_LANG_STRING)
      .build(),
    MethodMatchers.create().ofTypes(COMMONS_IO_IOUTILS).names("toInputStream")
      .addParametersMatcher(JAVA_LANG_CHARSEQUENCE, JAVA_LANG_STRING)
      .addParametersMatcher(JAVA_LANG_STRING, JAVA_LANG_STRING)
      .build(),
    MethodMatchers.create().ofTypes(COMMONS_IO_IOUTILS).names(TO_STRING)
      .addParametersMatcher(BYTE_ARRAY, JAVA_LANG_STRING)
      .addParametersMatcher(JAVA_IO_INPUTSTREAM, JAVA_LANG_STRING)
      .addParametersMatcher(JAVA_NET_URI, JAVA_LANG_STRING)
      .addParametersMatcher(JAVA_NET_URL, JAVA_LANG_STRING)
      .build(),
    MethodMatchers.create().ofTypes(COMMONS_IO_IOUTILS).names(WRITE)
      .addParametersMatcher(BYTE_ARRAY, JAVA_IO_WRITER, JAVA_LANG_STRING)
      .addParametersMatcher("char[]", JAVA_IO_OUTPUTSTREAM, JAVA_LANG_STRING)
      .addParametersMatcher(JAVA_LANG_CHARSEQUENCE, JAVA_IO_OUTPUTSTREAM, JAVA_LANG_STRING)
      .addParametersMatcher(JAVA_LANG_STRING, JAVA_IO_OUTPUTSTREAM, JAVA_LANG_STRING)
      .addParametersMatcher(JAVA_LANG_STRINGBUFFER, JAVA_IO_OUTPUTSTREAM, JAVA_LANG_STRING)
      .build(),
    MethodMatchers.create().ofTypes(COMMONS_IO_IOUTILS).names("writeLines")
      .addParametersMatcher(JAVA_UTIL_COLLECTION, JAVA_LANG_STRING, JAVA_IO_OUTPUTSTREAM, JAVA_LANG_STRING)
      .build(),
    MethodMatchers.create().ofTypes(JAVA_LANG_STRING).constructor()
      .addParametersMatcher(BYTE_ARRAY, JAVA_LANG_STRING)
      .addParametersMatcher(BYTE_ARRAY, INT, INT, JAVA_LANG_STRING)
      .build(),
    MethodMatchers.create().ofTypes(JAVA_IO_INPUTSTREAMREADER).constructor()
      .addParametersMatcher(JAVA_IO_INPUTSTREAM, JAVA_LANG_STRING)
      .build(),
    MethodMatchers.create().ofTypes(JAVA_IO_OUTPUTSTREAMWRITER).constructor()
      .addParametersMatcher(JAVA_IO_OUTPUTSTREAM, JAVA_LANG_STRING)
      .build(),
    MethodMatchers.create().ofTypes(COMMONS_IO_CHARSEQUENCEINPUTSTREAM).constructor()
      .addParametersMatcher(JAVA_LANG_CHARSEQUENCE, JAVA_LANG_STRING)
      .addParametersMatcher(JAVA_LANG_CHARSEQUENCE, JAVA_LANG_STRING, INT)
      .build(),
    MethodMatchers.create().ofTypes(COMMONS_IO_READERINPUTSTREAM).constructor()
      .addParametersMatcher(JAVA_IO_READER, JAVA_LANG_STRING)
      .addParametersMatcher(JAVA_IO_READER, JAVA_LANG_STRING, INT)
      .build(),
    MethodMatchers.create().ofTypes(COMMONS_IO_REVERSEDLINESFILEREADER).constructor()
      .addParametersMatcher(JAVA_IO_FILE, INT, JAVA_LANG_STRING)
      .build(),
    MethodMatchers.create().ofTypes(COMMONS_IO_LOCKABLEFILEWRITER).constructor()
      .addParametersMatcher(JAVA_IO_FILE, JAVA_LANG_STRING)
      .addParametersMatcher(JAVA_IO_FILE, JAVA_LANG_STRING, BOOLEAN, JAVA_LANG_STRING)
      .build(),
    MethodMatchers.create().ofTypes(COMMONS_IO_WRITEROUTPUTSTREAM).constructor()
      .addParametersMatcher(JAVA_IO_WRITER, JAVA_LANG_STRING)
      .addParametersMatcher(JAVA_IO_WRITER, JAVA_LANG_STRING, INT, BOOLEAN)
      .build(),
    MethodMatchers.create().ofTypes(COMMONS_CODEC_HEX).constructor()
      .addParametersMatcher(JAVA_LANG_STRING)
      .build(),
    MethodMatchers.create().ofTypes(COMMONS_CODEC_QUOTEDPRINTABLECODEC).constructor()
      .addParametersMatcher(JAVA_LANG_STRING)
      .build());

  private static Map<String, String> createAliasToConstantNameMap() {
    MapBuilder<String, String> constantNames = MapBuilder.newMap();
    for (Charset charset : STANDARD_CHARSETS) {
      String constantName = charset.name().replace("-", "_");
      constantNames.put(charset.name(), constantName);

      for (String alias : charset.aliases()) {
        constantNames.put(alias.toUpperCase(Locale.ROOT), constantName);
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
    if (tree.is(Tree.Kind.IDENTIFIER)) {
      onMemberSelectExpressionFound((IdentifierTree) tree);
    }
  }

  private void onMemberSelectExpressionFound(IdentifierTree identifierTree) {
    Symbol symbol = identifierTree.symbol();
    if (symbol.isVariableSymbol() && symbol.owner().type().is("com.google.common.base.Charsets")) {
      String identifier = identifierTree.name();
      String aliasedIdentifier = identifier.replace("_", "-");
      if (STANDARD_CHARSETS.stream().anyMatch(c -> c.name().equals(aliasedIdentifier))) {
        reportIssue(identifierTree, "Replace \"com.google.common.base.Charsets." + identifier + "\" with \"StandardCharsets." + identifier + "\".");
      }
    }
  }

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    if (context.getJavaVersion().asInt() >= JAVA_10) {
      return MethodMatchers.or(JAVA8_METHOD_MATCHERS, JAVA10_METHOD_MATCHERS);
    } else {
      return JAVA8_METHOD_MATCHERS;
    }
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
        return Optional.of(ListUtils.getLast(stringArguments));
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
    return argument.asConstant(String.class)
      .map(String::toUpperCase)
      .map(ALIAS_TO_CONSTANT::get);
  }

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava7Compatible();
  }
}
