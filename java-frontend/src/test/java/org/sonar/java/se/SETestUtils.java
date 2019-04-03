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
package org.sonar.java.se;

import com.sonar.sslr.api.typed.ActionParser;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.io.FileUtils;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.TestUtils;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.bytecode.cfg.BytecodeCFG;
import org.sonar.java.bytecode.cfg.BytecodeCFGMethodVisitor;
import org.sonar.java.bytecode.loader.SquidClassLoader;
import org.sonar.java.bytecode.se.MethodLookup;
import org.sonar.java.model.DefaultJavaFileScannerContext;
import org.sonar.java.model.JavaVersionImpl;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.java.se.checks.SECheck;
import org.sonar.java.se.xproc.BehaviorCache;
import org.sonar.java.se.xproc.MethodBehavior;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.assertj.core.api.Assertions.assertThat;

public class SETestUtils {

  private static final File TEST_JARS = new File("target/test-jars");
  private static final List<File> CLASS_PATH = new ArrayList<>(FileUtils.listFiles(TEST_JARS, new String[] {"jar", "zip"}, true));
  static {
    CLASS_PATH.add(new File("target/test-classes"));
  }

  private static final ActionParser<Tree> PARSER = JavaParser.createParser();
  public static final SquidClassLoader CLASSLOADER = new SquidClassLoader(CLASS_PATH);

  public static SymbolicExecutionVisitor createSymbolicExecutionVisitor(String fileName, SECheck... checks) {
    return createSymbolicExecutionVisitorAndSemantic(fileName, checks).a;
  }

  public static Pair<SymbolicExecutionVisitor, SemanticModel> createSymbolicExecutionVisitorAndSemantic(String fileName, SECheck... checks) {
    return createSymbolicExecutionVisitorAndSemantic(fileName, true, checks);
  }

  public static Pair<SymbolicExecutionVisitor, SemanticModel> createSymbolicExecutionVisitorAndSemantic(String fileName, boolean crossFileEnabled, SECheck... checks) {
    InputFile inputFile = TestUtils.inputFile(fileName);
    CompilationUnitTree cut;
    try {
      cut = (CompilationUnitTree) PARSER.parse(inputFile.contents());
    } catch (IOException e) {
      throw new IllegalStateException(String.format("Unable to load file for test: '%s'", inputFile));
    }
    SemanticModel semanticModel = SemanticModel.createFor(cut, CLASSLOADER);
    SymbolicExecutionVisitor sev = new SymbolicExecutionVisitor(Arrays.asList(checks), new BehaviorCache(CLASSLOADER, crossFileEnabled));
    sev.scanFile(new DefaultJavaFileScannerContext(cut, inputFile, semanticModel, null, new JavaVersionImpl(8), true));
    return new Pair<>(sev, semanticModel);
  }

  public static SemanticModel getSemanticModel(String filename) {
    File file = new File(filename);
    CompilationUnitTree cut = (CompilationUnitTree) PARSER.parse(file);
    return SemanticModel.createFor(cut, CLASSLOADER);
  }

  public static MethodBehavior getMethodBehavior(SymbolicExecutionVisitor sev, String methodName) {
    Optional<MethodBehavior> mb = sev.behaviorCache.behaviors.entrySet().stream()
      .filter(e -> e.getKey().contains("#" + methodName))
      .map(Map.Entry::getValue)
      .findFirst();
    assertThat(mb.isPresent()).isTrue();
    return mb.get();
  }

  public static MethodBehavior mockMethodBehavior(int arity, boolean varArgs) {
    return new MethodBehaviorStub(arity, varArgs);
  }

  public static MethodBehavior mockMethodBehavior() {
    return mockMethodBehavior(0, false);
  }

  private static class MethodBehaviorStub extends MethodBehavior {

    private final int arity;

    public MethodBehaviorStub(int arity, boolean varArgs) {
      super("()", varArgs);
      this.arity = arity;
    }

    @Override
    public int methodArity() {
      return arity;
    }
  }

  public static BytecodeCFG bytecodeCFG(String signature, SquidClassLoader classLoader) {
    BytecodeCFGMethodVisitor cfgMethodVisitor = new BytecodeCFGMethodVisitor() {
      @Override
      public boolean shouldVisitMethod(int methodFlags, String methodSignature) {
        return true;
      }
    };

    MethodLookup.lookup(signature, classLoader, cfgMethodVisitor);
    return cfgMethodVisitor.getCfg();
  }
}
