/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.java.cfg;

import com.google.common.collect.Lists;
import com.sonar.sslr.api.typed.ActionParser;

import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.StyleSpans;
import org.fxmisc.richtext.StyleSpansBuilder;
import org.sonar.java.ast.api.JavaKeyword;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.util.Duration;

public class CFGViewer extends Application {

  private static final ActionParser<Tree> PARSER = JavaParser.createParser(StandardCharsets.UTF_8);
  private static final String DEFAULT_SOURCE_CODE = "/cfgviewer/default.java";

  private final VBox verticalLayout = new VBox();
  private final CodeArea codeArea = new CodeArea();
  private final TextArea cfgText = new TextArea();
  private final WebView webView = new WebView();

  private static final String KEYWORD_PATTERN = "\\b(" + String.join("|", javaKeywords()) + ")\\b";
  private static final String PAREN_PATTERN = "\\(|\\)";
  private static final String BRACE_PATTERN = "\\{|\\}";
  private static final String BRACKET_PATTERN = "\\[|\\]";
  private static final String SEMICOLON_PATTERN = "\\;";
  private static final String STRING_PATTERN = "\"([^\"\\\\]|\\\\.)*\"";
  private static final String COMMENT_PATTERN = "//[^\n]*" + "|" + "/\\*(.|\\R)*?\\*/";

  private static final String[] RECOGNIZED_SYNTAX = {"KEYWORD", "PAREN", "BRACE", "BRACKET", "SEMICOLON", "STRING", "COMMENT"};
  private static final Pattern PATTERN = Pattern.compile(
    "(?<KEYWORD>" + KEYWORD_PATTERN + ")"
      + "|(?<PAREN>" + PAREN_PATTERN + ")"
      + "|(?<BRACE>" + BRACE_PATTERN + ")"
      + "|(?<BRACKET>" + BRACKET_PATTERN + ")"
      + "|(?<SEMICOLON>" + SEMICOLON_PATTERN + ")"
      + "|(?<STRING>" + STRING_PATTERN + ")"
      + "|(?<COMMENT>" + COMMENT_PATTERN + ")");

  private String lastAnalysed = "";

  private static String defaultFileContent() {
    String result;
    try {
      Path path = Paths.get(CFGViewer.class.getResource(DEFAULT_SOURCE_CODE).toURI());
      result = new String(Files.readAllBytes(path));
    } catch (URISyntaxException | IOException e) {
      e.printStackTrace();
      result = "// Unable to read default file:\\n\\n";
    }
    return result;
  }

  private static String[] javaKeywords() {
    return Arrays.stream(JavaKeyword.values())
      .map(JavaKeyword::getValue)
      .toArray(size -> new String[size]);
  }

  public static void main(String[] args) {
    launch(args);
  }

  @Override
  public void start(Stage primaryStage) throws Exception {
    setupLayout();

    primaryStage.setTitle("SonarQube Java Analyzer - CFG Viewer");
    codeArea.insertText(0, defaultFileContent());

    SplitPane splitPane = new SplitPane();
    splitPane.getItems().addAll(verticalLayout, webView);
    webView.getEngine().load(CFGViewer.class.getResource("/cfgviewer/cfg.html").toExternalForm());
    primaryStage.setScene(new Scene(splitPane, 1200, 800));
    primaryStage.show();

    Timeline timeline = new Timeline(new KeyFrame(
      Duration.millis(500),
      ae -> checkForUpdate()));
    timeline.setCycleCount(Animation.INDEFINITE);
    timeline.play();
  }

  private void checkForUpdate() {
    String text = codeArea.getText();
    if (!text.equals(lastAnalysed)) {
      lastAnalysed = text;
      analyse(text);
    }
  }

  private void analyse(String source) {
    CFG cfg = buildCFG(source);
    addCFGText(cfg);
    String dot = CFGDebug.toDot(cfg);
    WebEngine webEngine = webView.getEngine();
    webEngine.executeScript("loadCfg('" + dot + "')");
  }

  private void addCFGText(CFG cfg) {
    cfgText.setText(CFGDebug.toString(cfg));
  }

  private static CFG buildCFG(String source) {
    CompilationUnitTree cut = (CompilationUnitTree) PARSER.parse(source);
    SemanticModel.createFor(cut, Lists.newArrayList());
    MethodTree firstMethod = ((MethodTree) ((ClassTree) cut.types().get(0)).members().get(0));
    return CFG.build(firstMethod);
  }

  private void setupLayout() {
    codeArea.setStyle("-fx-min-height: 400px;");
    codeArea.getStylesheets().add(CFGViewer.class.getResource("/cfgviewer/java-keywords.css").toExternalForm());
    codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
    codeArea.richChanges()
      .filter(ch -> !ch.getInserted().equals(ch.getRemoved()))
      .subscribe(change -> {
        codeArea.setStyleSpans(0, computeHighlighting(codeArea.getText()));
      });

    cfgText.setStyle("-fx-min-height: 390px; -fx-font-family: monospace;");

    verticalLayout.getChildren().addAll(codeArea, cfgText);
  }

  private static StyleSpans<Collection<String>> computeHighlighting(String text) {
    Matcher matcher = PATTERN.matcher(text);
    int lastKwEnd = 0;
    StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
    while (matcher.find()) {
      String styleClass = null;
      for (String syntax : RECOGNIZED_SYNTAX) {
        if (matcher.group(syntax) != null) {
          styleClass = syntax.toLowerCase();
          break;
        }
      }
      spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
      spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
      lastKwEnd = matcher.end();
    }
    spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
    return spansBuilder.create();
  }

}
