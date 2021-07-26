/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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
package org.sonar.java.testing;

import java.io.File;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.java.SonarComponents;
import org.sonar.java.annotations.VisibleForTesting;
import org.sonar.java.model.Sema;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;

public class VisitorsBridgeForTests extends VisitorsBridge {

  private JavaFileScannerContextForTests testContext;
  private boolean enableSemantic = true;


  @VisibleForTesting
  public VisitorsBridgeForTests(JavaFileScanner visitor, SonarComponents sonarComponents) {
    this(Collections.singletonList(visitor), Collections.emptyList(), sonarComponents);
  }

  public VisitorsBridgeForTests(Iterable<? extends JavaCheck> visitors, @Nullable SonarComponents sonarComponents) {
    super(visitors, Collections.emptyList(), sonarComponents);
    enableSemantic = false;
  }

  public VisitorsBridgeForTests(Iterable<? extends JavaCheck> visitors, List<File> projectClasspath, @Nullable SonarComponents sonarComponents) {
    super(visitors, projectClasspath, sonarComponents);
  }

  @Override
  protected JavaFileScannerContext createScannerContext(CompilationUnitTree tree, Sema semanticModel, SonarComponents sonarComponents, boolean failedParsing) {
    Sema model = enableSemantic ? semanticModel : null;
    testContext = new JavaFileScannerContextForTests(tree, currentFile, model, sonarComponents, javaVersion, failedParsing);
    return testContext;
  }

  public JavaFileScannerContextForTests lastCreatedTestContext() {
    return testContext;
  }
}
