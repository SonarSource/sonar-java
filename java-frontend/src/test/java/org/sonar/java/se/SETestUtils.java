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
package org.sonar.java.se;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.sonar.sslr.api.typed.ActionParser;

import org.apache.commons.io.FileUtils;
import org.mockito.Mockito;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.java.se.SymbolicExecutionVisitor;
import org.sonar.java.se.checks.SECheck;
import org.sonar.java.se.xproc.MethodBehavior;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol.MethodSymbol;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SETestUtils {

  private static final File TEST_JARS = new File("target/test-jars");
  private static final List<File> CLASS_PATH = new ArrayList<>(FileUtils.listFiles(TEST_JARS, new String[] {"jar", "zip"}, true));
  static {
    CLASS_PATH.add(new File("target/test-classes"));
  }

  public static SymbolicExecutionVisitor createSymbolicExecutionVisitor(String fileName, SECheck... checks) {
    ActionParser<Tree> p = JavaParser.createParser(Charsets.UTF_8);
    CompilationUnitTree cut = (CompilationUnitTree) p.parse(new File(fileName));
    SemanticModel semanticModel = SemanticModel.createFor(cut, new ArrayList<>(CLASS_PATH));
    SymbolicExecutionVisitor sev = new SymbolicExecutionVisitor(Lists.newArrayList(checks));
    JavaFileScannerContext context = mock(JavaFileScannerContext.class);
    when(context.getTree()).thenReturn(cut);
    when(context.getSemanticModel()).thenReturn(semanticModel);
    sev.scanFile(context);
    return sev;
  }

  public static Map.Entry<MethodSymbol, MethodBehavior> getSymbolWithMethodBehavior(SymbolicExecutionVisitor sev, String methodName) {
    Optional<Map.Entry<MethodSymbol, MethodBehavior>> mb = sev.behaviorCache.behaviors.entrySet().stream()
      .filter(e -> methodName.equals(e.getKey().name()))
      .findFirst();
    assertThat(mb.isPresent()).isTrue();
    return mb.get();
  }

  public static MethodBehavior getMethodBehavior(SymbolicExecutionVisitor sev, String methodName) {
    return getSymbolWithMethodBehavior(sev, methodName).getValue();
  }

  public static MethodBehavior mockMethodBehavior(int arity, boolean varArgs) {
    MethodBehavior mockMethodBehavior = Mockito.mock(MethodBehavior.class);
    Mockito.when(mockMethodBehavior.isMethodVarArgs()).thenReturn(varArgs);
    Mockito.when(mockMethodBehavior.methodArity()).thenReturn(arity);
    return mockMethodBehavior;
  }

  public static MethodBehavior mockMethodBehavior() {
    return mockMethodBehavior(0, false);
  }
}
