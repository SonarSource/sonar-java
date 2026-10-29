/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.model;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FileASTRequestor;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.ProgressMonitor;

final class EcjFrontendBackend implements FrontendBackend {

  private final Supplier<ASTParser> parserFactory;

  EcjFrontendBackend(Supplier<ASTParser> parserFactory) {
    this.parserFactory = parserFactory;
  }

  @Override
  public FrontendSession open(FrontendConfiguration configuration) {
    return new Session(configuration, parserFactory.get());
  }

  static ASTParser newParser(FrontendConfiguration configuration) {
    ASTParser parser = ASTParser.newParser(AST.getJLSLatest());
    Map<String, String> options = new HashMap<>(JavaCore.getOptions());
    JavaCore.setComplianceOptions(configuration.sourceVersion(), options);
    options.put(JavaCore.COMPILER_PB_MAX_PER_UNIT, "42000");
    if (configuration.ignoreUnnamedModuleForSplitPackage()) {
      options.put(JavaCore.COMPILER_IGNORE_UNNAMED_MODULE_FOR_SPLIT_PACKAGE, "enabled");
    }
    if (configuration.previewEnabled()) {
      options.put(JavaCore.COMPILER_PB_ENABLE_PREVIEW_FEATURES, "enabled");
    }
    EcjDiagnosticProvider.compilerOptions().forEach(option -> options.put(option, "warning"));
    parser.setCompilerOptions(options);
    boolean includeRunningVMBootclasspath = configuration.classpath().stream()
      .noneMatch(file -> Set.of("rt.jar", "jrt-fs.jar", "android.jar").contains(file.getName()));
    parser.setEnvironment(configuration.classpath().stream().map(File::getAbsolutePath).toArray(String[]::new),
      new String[] {}, new String[] {}, includeRunningVMBootclasspath);
    parser.setResolveBindings(true);
    parser.setBindingsRecovery(true);
    return parser;
  }

  private static final class Session extends FrontendSession {

    private final FrontendConfiguration configuration;
    private final ASTParser parser;

    private Session(FrontendConfiguration configuration, ASTParser parser) {
      this.configuration = configuration;
      this.parser = parser;
    }

    @Override
    JavaTree.CompilationUnitTreeImpl parse(String unitName, String source) {
      return JParser.parse(parser, configuration.sourceVersion(), unitName, source);
    }

    @Override
    void parseBatch(List<? extends InputFile> inputs, ProgressMonitor monitor, BiConsumer<InputFile, ParsedUnit> action) {
      Map<File, InputFile> byPath = new HashMap<>();
      inputs.forEach(input -> byPath.put(new File(input.absolutePath()), input));
      parser.createASTs(inputs.stream().map(InputFile::absolutePath).toArray(String[]::new),
        inputs.stream().map(input -> input.charset().name()).toArray(String[]::new), new String[0], new FileASTRequestor() {
          @Override
          public void acceptAST(String sourceFilePath, CompilationUnit ast) {
            InputFile input = byPath.get(new File(sourceFilePath));
            action.accept(input, () -> JParser.convert(configuration.sourceVersion(), input.filename(), input.contents(), ast));
          }
        }, new EcjProgressMonitor(monitor));
    }
  }
}
