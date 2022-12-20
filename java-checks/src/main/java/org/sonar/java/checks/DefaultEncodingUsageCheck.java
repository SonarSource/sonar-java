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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.java.JavaVersionAwareVisitor;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeCastTree;

@Rule(key = "S1943")
public class DefaultEncodingUsageCheck extends AbstractMethodDetection implements JavaVersionAwareVisitor {

  private static final String INT = "int";
  private static final String BOOLEAN = "boolean";
  private static final String BYTE_ARRAY = "byte[]";
  private static final String TO_STRING = "toString";
  private static final String WRITE = "write";
  private static final String JAVA_IO_FILE = "java.io.File";
  private static final String JAVA_IO_FILEDESCRIPTOR = "java.io.FileDescriptor";
  private static final String JAVA_IO_READER = "java.io.Reader";
  private static final String JAVA_IO_WRITER = "java.io.Writer";
  private static final String JAVA_IO_FILEWRITER = "java.io.FileWriter";
  private static final String JAVA_IO_FILEREADER = "java.io.FileReader";
  private static final String JAVA_IO_PRINTWRITER = "java.io.PrintWriter";
  private static final String JAVA_IO_PRINTSTREAM = "java.io.PrintStream";
  private static final String JAVA_IO_INPUTSTREAM = "java.io.InputStream";
  private static final String JAVA_IO_OUTPUTSTREAM = "java.io.OutputStream";
  private static final String JAVA_IO_BYTEARRAYOUTPUTSTREAM = "java.io.ByteArrayOutputStream";
  private static final String JAVA_IO_OUTPUTSTREAMWRITER = "java.io.OutputStreamWriter";
  private static final String JAVA_IO_INPUTSTREAMREADER = "java.io.InputStreamReader";
  private static final String JAVA_NIO_FILE_PATH = "java.nio.file.Path";
  private static final String JAVA_NIO_CHARSET = "java.nio.charset.Charset";
  private static final String JAVA_LANG_CHARSEQUENCE = "java.lang.CharSequence";
  private static final String JAVA_LANG_STRING = "java.lang.String";
  private static final String JAVA_UTIL_SCANNER = "java.util.Scanner";
  private static final String JAVA_UTIL_FORMATTER = "java.util.Formatter";

  private static final String COMMONS_IOUTILS = "org.apache.commons.io.IOUtils";
  private static final String COMMONS_FILEUTILS = "org.apache.commons.io.FileUtils";

  private static final List<MethodMatchers> COMMONS_IO = new ArrayList<>();
  private static final List<MethodMatchers> COMMONS_IO_WITH_CHARSET = new ArrayList<>();

  static {
    withAndWithoutCharset(COMMONS_IOUTILS, "copy", JAVA_IO_INPUTSTREAM, JAVA_IO_WRITER);
    withAndWithoutCharset(COMMONS_IOUTILS, "copy", JAVA_IO_READER, JAVA_IO_OUTPUTSTREAM);
    withAndWithoutCharset(COMMONS_IOUTILS, "readLines", JAVA_IO_INPUTSTREAM);
    withAndWithoutCharset(COMMONS_IOUTILS, "toByteArray", JAVA_IO_READER);
    withAndWithoutCharset(COMMONS_IOUTILS, "toCharArray", JAVA_IO_INPUTSTREAM);
    withAndWithoutCharset(COMMONS_IOUTILS, "toInputStream", JAVA_LANG_CHARSEQUENCE);
    withAndWithoutCharset(COMMONS_IOUTILS, "toInputStream", JAVA_LANG_STRING);
    withAndWithoutCharset(COMMONS_IOUTILS, TO_STRING, BYTE_ARRAY);
    withAndWithoutCharset(COMMONS_IOUTILS, TO_STRING, "java.net.URI");
    withAndWithoutCharset(COMMONS_IOUTILS, TO_STRING, "java.net.URL");
    withAndWithoutCharset(COMMONS_IOUTILS, WRITE, "char[]", JAVA_IO_OUTPUTSTREAM);
    withAndWithoutCharset(COMMONS_IOUTILS, "writeLines", "java.util.Collection", JAVA_LANG_STRING, JAVA_IO_OUTPUTSTREAM);

    withAndWithoutCharset(COMMONS_FILEUTILS, "readFileToString", JAVA_IO_FILE);
    withAndWithoutCharset(COMMONS_FILEUTILS, "readLines", JAVA_IO_FILE);
    withAndWithoutCharset(COMMONS_FILEUTILS, WRITE, JAVA_IO_FILE, JAVA_LANG_CHARSEQUENCE);
    withAndWithoutCharset(COMMONS_FILEUTILS, WRITE, JAVA_IO_FILE, JAVA_LANG_CHARSEQUENCE, BOOLEAN);
    withAndWithoutCharset(COMMONS_FILEUTILS, "writeStringToFile", JAVA_IO_FILE, JAVA_LANG_STRING);


    // subtypeOf is used to cover also signatures with String and StringBuffer
    MethodMatchers.ParametersBuilder parametersBuilder = MethodMatchers.create().ofTypes(COMMONS_IOUTILS).names(WRITE);

    COMMONS_IO.add(parametersBuilder.addParametersMatcher(params ->
      params.size() == 2 && params.get(0).isSubtypeOf(JAVA_LANG_CHARSEQUENCE) && params.get(1).is(JAVA_IO_OUTPUTSTREAM)
    ).build());

    COMMONS_IO_WITH_CHARSET.add(parametersBuilder.addParametersMatcher(params ->
      params.size() == 3 && params.get(0).isSubtypeOf(JAVA_LANG_CHARSEQUENCE) && params.get(1).is(JAVA_IO_OUTPUTSTREAM)
        && (params.get(2).is(JAVA_NIO_CHARSET) || params.get(2).is(JAVA_LANG_STRING))
    ).build());
  }

  private static void withAndWithoutCharset(String type, String methodName, String... parameters) {
    MethodMatchers.ParametersBuilder nameBuilder = MethodMatchers.create().ofTypes(type).names(methodName);
    COMMONS_IO.add(nameBuilder.addParametersMatcher(parameters).build());
    int originalSize = parameters.length;
    String[] copy = Arrays.copyOf(parameters, originalSize + 1);
    copy[originalSize] = JAVA_LANG_STRING;
    COMMONS_IO_WITH_CHARSET.add(nameBuilder.addParametersMatcher(copy).build());
    copy[originalSize] = JAVA_NIO_CHARSET;
    COMMONS_IO_WITH_CHARSET.add(nameBuilder.addParametersMatcher(copy).build());
  }

  private static final MethodMatchers COMMONS_IO_CHARSET_MATCHERS = MethodMatchers.or(COMMONS_IO_WITH_CHARSET);

  private static final MethodMatchers FILEUTILS_WRITE_WITH_CHARSET =
    MethodMatchers.create().ofTypes(COMMONS_FILEUTILS).names(WRITE)
      .addParametersMatcher(JAVA_IO_FILE, JAVA_LANG_CHARSEQUENCE, JAVA_LANG_STRING, BOOLEAN)
      .addParametersMatcher(JAVA_IO_FILE, JAVA_LANG_CHARSEQUENCE, JAVA_NIO_CHARSET, BOOLEAN)
      .build();

  private static final MethodMatchers FILEUTILS_WRITE_WITH_CHARSET_MATCHERS =
    MethodMatchers.or(FILEUTILS_WRITE_WITH_CHARSET);

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isSet() && version.asInt() < 18;
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.METHOD_INVOCATION, Tree.Kind.NEW_CLASS);
  }

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    ArrayList<MethodMatchers> matchers = new ArrayList<>(Arrays.asList(
      MethodMatchers.create().ofTypes(JAVA_LANG_STRING).names("getBytes")
        .addWithoutParametersMatcher()
        .addParametersMatcher(INT, INT, BYTE_ARRAY, INT)
        .build(),
      MethodMatchers.create().ofTypes(JAVA_LANG_STRING).constructor()
        .addParametersMatcher(BYTE_ARRAY)
        .addParametersMatcher(BYTE_ARRAY, INT, INT)
        .build(),
      MethodMatchers.create().ofTypes(JAVA_IO_BYTEARRAYOUTPUTSTREAM).names(TO_STRING)
        .addWithoutParametersMatcher()
        .build(),
      MethodMatchers.create().ofTypes(JAVA_IO_FILEREADER).constructor()
        .addParametersMatcher(JAVA_IO_FILE)
        .addParametersMatcher(JAVA_IO_FILEDESCRIPTOR)
        .addParametersMatcher(JAVA_LANG_STRING)
        .build(),
      MethodMatchers.create().ofTypes(JAVA_IO_FILEWRITER).constructor()
        .addParametersMatcher(JAVA_IO_FILE)
        .addParametersMatcher(JAVA_IO_FILEDESCRIPTOR)
        .addParametersMatcher(JAVA_IO_FILE, BOOLEAN)
        .addParametersMatcher(JAVA_LANG_STRING)
        .addParametersMatcher(JAVA_LANG_STRING, BOOLEAN)
        .build(),
      MethodMatchers.create().ofTypes(JAVA_IO_INPUTSTREAMREADER).constructor()
        .addParametersMatcher(JAVA_IO_INPUTSTREAM)
        .build(),
      MethodMatchers.create().ofTypes(JAVA_IO_OUTPUTSTREAMWRITER).constructor()
        .addParametersMatcher(JAVA_IO_OUTPUTSTREAM)
        .build(),
      MethodMatchers.create().ofTypes(JAVA_IO_PRINTSTREAM).constructor()
        .addParametersMatcher(JAVA_IO_FILE)
        .addParametersMatcher(JAVA_IO_OUTPUTSTREAM)
        .addParametersMatcher(JAVA_IO_OUTPUTSTREAM, BOOLEAN)
        .addParametersMatcher(JAVA_LANG_STRING)
        .build(),
      MethodMatchers.create().ofTypes(JAVA_IO_PRINTWRITER).constructor()
        .addParametersMatcher(JAVA_IO_FILE)
        .addParametersMatcher(JAVA_IO_OUTPUTSTREAM)
        .addParametersMatcher(JAVA_IO_OUTPUTSTREAM, BOOLEAN)
        .addParametersMatcher(JAVA_LANG_STRING)
        .build(),
      MethodMatchers.create().ofTypes(JAVA_UTIL_FORMATTER).constructor()
        .addParametersMatcher(JAVA_LANG_STRING)
        .addParametersMatcher(JAVA_IO_FILE)
        .addParametersMatcher(JAVA_IO_OUTPUTSTREAM)
        .build(),
      MethodMatchers.create().ofTypes(JAVA_UTIL_SCANNER).constructor()
        .addParametersMatcher(JAVA_IO_FILE)
        .addParametersMatcher(JAVA_NIO_FILE_PATH)
        .addParametersMatcher(JAVA_IO_INPUTSTREAM)
        .build()
    ));
    matchers.addAll(COMMONS_IO);
    matchers.addAll(COMMONS_IO_WITH_CHARSET);
    matchers.add(FILEUTILS_WRITE_WITH_CHARSET);
    return MethodMatchers.or(matchers);
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    if (COMMONS_IO_CHARSET_MATCHERS.matches(mit)) {
      Arguments arguments = mit.arguments();
      ExpressionTree lastArgument = arguments.get(arguments.size() - 1);
      testNullLiteralPassedForEncoding(lastArgument);
    } else if (FILEUTILS_WRITE_WITH_CHARSET_MATCHERS.matches(mit)) {
      testNullLiteralPassedForEncoding(mit.arguments().get(2));
    } else {
      reportIssue(ExpressionUtils.methodName(mit), "Remove this use of \"" + mit.symbol().name() + "\".");
    }
  }

  private void testNullLiteralPassedForEncoding(ExpressionTree argument) {
    if (isNullLiteral(argument)) {
      reportIssue(argument, "Replace this \"null\" with actual charset.");
    }
  }

  private static boolean isNullLiteral(ExpressionTree lastArgument) {
    ExpressionTree arg = ExpressionUtils.skipParentheses(lastArgument);
    return arg.is(Tree.Kind.NULL_LITERAL)
      || (arg.is(Tree.Kind.TYPE_CAST) && isNullLiteral(((TypeCastTree) arg).expression()));
  }

  @Override
  protected void onConstructorFound(NewClassTree newClassTree) {
    Symbol.MethodSymbol symbol = newClassTree.constructorSymbol();
    if (!symbol.isUnknown()) {
      String signature = symbol.owner().name() + "(" + symbol.parameterTypes().stream().map(Type::toString).collect(Collectors.joining(",")) + ")";
      reportIssue(newClassTree.identifier(), "Remove this use of constructor \"" + signature + "\".");
    }
  }

}
