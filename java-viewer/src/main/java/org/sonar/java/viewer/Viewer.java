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

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.eclipse.jetty.util.MultiMap;
import org.eclipse.jetty.util.UrlEncoded;
import org.sonar.java.ast.ASTViewer;
import org.sonar.java.cfg.CFG;
import org.sonar.java.cfg.CFGDebug;
import org.sonar.java.cfg.CFGViewer;
import org.sonar.java.se.EGViewer;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static spark.Spark.exception;
import static spark.Spark.get;
import static spark.Spark.port;
import static spark.Spark.post;
import static spark.Spark.staticFiles;

import spark.ModelAndView;
import spark.Request;
import spark.template.velocity.VelocityTemplateEngine;

public class Viewer {

  private static final Logger LOGGER = Logger.getLogger(Viewer.class);
  private static final String DEFAULT_SOURCE_CODE_LOCATION = "/public/example/example.java";
  private static final String DEFAULT_SOURCE_CODE = defaultFileContent();

  public static void main(String[] args) {
    BasicConfigurator.configure();

    exception(Exception.class, (e, req, res) -> LOGGER.error("Unexpected exception.", e)); // print all exceptions

    staticFiles.location("/public");
    port(9999);

    get("/", (req, res) -> renderIndex(defaultFileContent()));
    post("/", (req, res) -> generate(req));
  }

  private static String generate(Request request) {
    MultiMap<String> params = new MultiMap<>();
    UrlEncoded.decodeUtf8To(request.body(), params);
    String javaCode = params.getOrDefault("javaCode", Collections.singletonList(DEFAULT_SOURCE_CODE)).get(0);
    try {
      return renderIndex(javaCode);
    } catch (Exception e) {
      return renderError(javaCode, e);
    }
  }

  private static String renderIndex(String javaCode) {
    HashMap<String, String> values = new HashMap<>();

    CFG cfg = CFGViewer.buildCFG(javaCode);

    values.put("cfg", CFGDebug.toString(cfg));

    values.put("dotAST", ASTViewer.toDot(javaCode));
    values.put("dotCFG", CFGViewer.toDot(cfg));
    values.put("dotEG", EGViewer.toDot(javaCode, cfg.blocks().get(0).id()));

    values.put("errorMessage", "");
    values.put("errorStackTrace", "");

    return renderWithValues(javaCode, values);
  }

  private static String renderError(String javaCode, Exception e) {
    HashMap<String, String> values = new HashMap<>();

    StringWriter sw = new StringWriter();
    e.printStackTrace(new PrintWriter(sw));
    String stackTrace = sw.toString();

    values.put("errorMessage", e.getMessage());
    values.put("errorStackTrace", stackTrace.replace(System.getProperty("line.separator"), "<br/>\n"));

    return renderWithValues(javaCode, values);
  }

  private static String renderWithValues(String javaCode, Map<String, String> values) {
    values.put("javaCode", javaCode);
    return new VelocityTemplateEngine().render(new ModelAndView(values, "velocity/index.vm"));
  }

  private static String defaultFileContent() {
    String result;
    try {
      Path path = Paths.get(Viewer.class.getResource(DEFAULT_SOURCE_CODE_LOCATION).toURI());
      result = new String(Files.readAllBytes(path));
    } catch (URISyntaxException | IOException e) {
      LOGGER.error("Unable to read default file.", e);
      result = "// Unable to read default file:\\n\\n";
    }
    return result;
  }
}
