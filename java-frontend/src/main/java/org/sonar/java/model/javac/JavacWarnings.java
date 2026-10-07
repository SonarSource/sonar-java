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
package org.sonar.java.model.javac;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreeScanner;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.PackageElement;
import javax.lang.model.util.Elements;
import org.sonar.java.model.JProblem;
import org.sonar.java.model.JWarning;

public final class JavacWarnings {

  private JavacWarnings() {
  }

  public static List<JWarning> unusedImports(CompilationUnitTree unit, SourcePositions positions, String source, Elements elements) {
    if (unit.getModule() != null) {
      return List.of();
    }
    SourceTokens tokens = new SourceTokens(source);
    Set<String> seen = new HashSet<>();
    List<JWarning> warnings = new ArrayList<>();
    String packageName = unit.getPackageName() == null ? "" : unit.getPackageName().toString();
    List<SourceTokens.Token> sourceTokens = tokens.tokens();
    List<int[]> importRanges = unit.getImports().stream()
      .map(declaration -> new int[] {(int) positions.getStartPosition(unit, declaration), (int) positions.getEndPosition(unit, declaration)})
      .toList();
    for (ImportTree declaration : unit.getImports()) {
      String name = declaration.getQualifiedIdentifier().toString();
      String simpleName = name.substring(name.lastIndexOf('.') + 1);
      int start = (int) positions.getStartPosition(unit, declaration);
      int end = (int) positions.getEndPosition(unit, declaration);
      boolean duplicate = !seen.add(name);
      boolean moduleImport = source.substring(start, end).startsWith("import module ");
      if (moduleImport && !duplicate) {
        continue;
      }
      if ("*".equals(simpleName)) {
        String importedPackage = name.substring(0, name.length() - 2);
        PackageElement element = elements.getPackageElement(importedPackage);
        boolean knownPackageUnused = element != null && element.getEnclosedElements().stream()
          .filter(child -> child.getKind().isClass() || child.getKind().isInterface())
          .noneMatch(child -> usedOutsideImports(sourceTokens, importRanges, child.getSimpleName().toString(), List.of()));
        if (importedPackage.equals(packageName) || knownPackageUnused) {
          warnings.add(new JWarning("The import " + importedPackage + " is never used", JProblem.Type.UNUSED_IMPORT,
            tokens.line(start), tokens.column(start), tokens.line(end), tokens.column(end)));
        }
        continue;
      }
      List<int[]> localOwnerRanges = declaration.isStatic()
        ? localOwnerRanges(unit, positions, tokens, packageName, name.substring(0, name.lastIndexOf('.')))
        : List.of();
      boolean used = usedOutsideImports(sourceTokens, importRanges, simpleName, localOwnerRanges);
      boolean samePackage = name.startsWith(packageName + ".") && name.lastIndexOf('.') == packageName.length();
      if (!used || duplicate || samePackage) {
        warnings.add(new JWarning("The import " + name + " is never used", JProblem.Type.UNUSED_IMPORT,
          tokens.line(start), tokens.column(start), tokens.line(end), tokens.column(end)));
      }
    }
    return warnings;
  }

  private static boolean usedOutsideImports(List<SourceTokens.Token> sourceTokens, List<int[]> importRanges,
    String simpleName, List<int[]> excludedRanges) {
    for (int i = 0; i < sourceTokens.size(); i++) {
      SourceTokens.Token token = sourceTokens.get(i);
      if (!token.text().equals(simpleName) || i > 0 && sourceTokens.get(i - 1).text().equals(".")) {
        continue;
      }
      if (outsideRanges(token.start(), importRanges) && outsideRanges(token.start(), excludedRanges)) {
        return true;
      }
    }
    return false;
  }

  private static boolean outsideRanges(int offset, List<int[]> ranges) {
    return ranges.stream().noneMatch(range -> offset >= range[0] && offset < range[1]);
  }

  private static List<int[]> localOwnerRanges(CompilationUnitTree unit, SourcePositions positions, SourceTokens tokens,
    String packageName, String ownerName) {
    List<int[]> ranges = new ArrayList<>();
    new TreeScanner<Void, String>() {
      @Override
      public Void visitClass(ClassTree declaration, String enclosingName) {
        String name = enclosingName + "." + declaration.getSimpleName();
        if (name.equals(ownerName)) {
          int start = (int) positions.getStartPosition(unit, declaration);
          int end = (int) positions.getEndPosition(unit, declaration);
          ranges.add(new int[] {tokens.firstToken(start, end, declaration.getSimpleName().toString()).start(), end});
        }
        return super.visitClass(declaration, name);
      }
    }.scan(unit, packageName);
    return ranges;
  }
}
