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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.sonar.sslr.api.typed.ActionParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.java.ast.ASTDotGraph;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.bytecode.loader.SquidClassLoader;
import org.sonar.java.cfg.CFG;
import org.sonar.java.cfg.CFGDotGraph;
import org.sonar.java.cfg.CFGPrinter;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.java.se.EGDotGraph;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import spark.ModelAndView;
import spark.Request;
import spark.template.velocity.VelocityTemplateEngine;

import javax.annotation.CheckForNull;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static spark.Spark.awaitInitialization;
import static spark.Spark.exception;
import static spark.Spark.get;
import static spark.Spark.port;
import static spark.Spark.post;
import static spark.Spark.staticFiles;

public class Viewer {

  private static final Logger LOGGER = LoggerFactory.getLogger(Viewer.class);
  private static final String DEFAULT_SOURCE_CODE_LOCATION = "/public/example/example.java";
  private static final String DEFAULT_SOURCE_CODE = defaultFileContent();
  private static final int DEFAULT_PORT = 9999;

  private static final ActionParser<Tree> PARSER = JavaParser.createParser();

  public static void main(String[] args) {
    startWebServer(DEFAULT_PORT, DEFAULT_SOURCE_CODE);
  }

  @VisibleForTesting
  static void startWebServer(int port, String defaultSourceCode) {
    // print all exceptions
    exception(Exception.class, (e, req, res) -> LOGGER.error("Unexpected exception.", e));

    staticFiles.location("/public");
    port(port);

    get("/", (req, res) -> generate(defaultSourceCode));
    post("/", (req, res) -> generate(req, defaultSourceCode));

    awaitInitialization();
    LOGGER.info("Viewer at http://localhost:{}", port);
  }

  private static String generate(Request request, String defaultSourceCode) {
    String javaCode = request.queryParams("javaCode");
    if (javaCode == null) {
      javaCode = defaultSourceCode;
    }
    return generate(javaCode);
  }

  private static String generate(String javaCode) {
    Map<String, String> values;
    try {
      values = getValues(javaCode);
    } catch (Exception e) {
      values = getErrorValues(e);
    }
    return renderWithValues(javaCode, values);
  }

  @VisibleForTesting
  static Map<String, String> getValues(String javaCode) {
    Map<String, String> values = new HashMap<>();

    CompilationUnitTree cut = (CompilationUnitTree) PARSER.parse(javaCode);
    SemanticModel semanticModel = SemanticModel.createFor(cut, new SquidClassLoader(Collections.emptyList()));
    MethodTree firstMethod = getFirstMethod(cut);

    Preconditions.checkNotNull(firstMethod, "Unable to find a method in first class.");

    CFG cfg = CFG.build(firstMethod);

    values.put("cfg", CFGPrinter.toString(cfg));

    values.put("dotAST", new ASTDotGraph(cut).toDot());
    values.put("dotCFG", new CFGDotGraph(cfg).toDot());
    values.put("dotEG", new EGDotGraph(cut, firstMethod, semanticModel, cfg.blocks().get(0).id()).toDot());

    // explicitly force empty message and stack trace
    values.put("errorMessage", "");
    values.put("errorStackTrace", "");

    return values;
  }

  @CheckForNull
  private static MethodTree getFirstMethod(CompilationUnitTree cut) {
    ClassTree classTree = (ClassTree) cut.types().get(0);
    return (MethodTree) classTree.members().stream()
      .filter(m -> m.is(Tree.Kind.METHOD))
      .findFirst()
      .orElse(null);
  }

  @VisibleForTesting
  static Map<String, String> getErrorValues(Exception e) {
    Map<String, String> values = new HashMap<>();

    StringWriter sw = new StringWriter();
    e.printStackTrace(new PrintWriter(sw));
    String stackTrace = sw.toString();

    String message = e.getMessage();
    values.put("errorMessage", message == null ? "Unexpected error" : message);
    values.put("errorStackTrace", stackTrace.replace(System.getProperty("line.separator"), "<br/>\n"));

    return values;
  }

  private static String renderWithValues(String javaCode, Map<String, String> values) {
    values.put("javaCode", javaCode);
    return new VelocityTemplateEngine().render(new ModelAndView(values, "velocity/index.vm"));
  }

  private static String defaultFileContent() {
    String result;
    try {
      Path path = Paths.get(Viewer.class.getResource(DEFAULT_SOURCE_CODE_LOCATION).toURI());
      result = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    } catch (URISyntaxException | IOException e) {
      LOGGER.error("Unable to read default file.", e);
      result = "// Unable to read default file:\\n\\n";
    }
    return result;
  }
}
