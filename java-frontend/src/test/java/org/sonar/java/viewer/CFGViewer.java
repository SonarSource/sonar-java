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

import com.google.common.collect.Lists;
import com.sonar.sslr.api.typed.ActionParser;
import javafx.scene.web.WebEngine;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.cfg.CFG;
import org.sonar.java.cfg.CFGDebug;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.nio.charset.StandardCharsets;

public class CFGViewer {

  private static final ActionParser<Tree> PARSER = JavaParser.createParser(StandardCharsets.UTF_8);
  private final Viewer viewer;

  CFGViewer(Viewer viewer) {
    this.viewer = viewer;
  }

  public void analyse(String source){
    CFG cfg = buildCFG(source);
    viewer.textArea.setText(CFGDebug.toString(cfg));
    String dot = CFGDebug.toDot(cfg);
    WebEngine webEngine = viewer.webView.getEngine();
    webEngine.executeScript("loadCFG('" + dot + "')");
  }

  static CFG buildCFG(String source) {
    CompilationUnitTree cut = (CompilationUnitTree) PARSER.parse(source);
    SemanticModel.createFor(cut, Lists.newArrayList());
    MethodTree firstMethod = ((MethodTree) ((ClassTree) cut.types().get(0)).members().get(0));
    return CFG.build(firstMethod);
  }

}
