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
package org.sonar.java.checks.helpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.java.annotations.Beta;
import org.sonar.java.annotations.VisibleForTesting;
import org.sonarsource.analyzer.commons.collections.ListUtils;
import org.sonar.java.model.DefaultJavaFileScannerContext;
import org.sonar.java.model.JavaTree;
import org.sonar.java.reporting.InternalJavaIssueBuilder;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.location.Position;
import org.sonar.plugins.java.api.tree.CaseGroupTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ImportTree;
import org.sonar.plugins.java.api.tree.PackageDeclarationTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;


/**
 * For internal use only. Can not be used outside SonarJava analyzer.
 */
@Beta
public class QuickFixHelper {

  private QuickFixHelper() {
    // Utility class
  }

  public static InternalJavaIssueBuilder newIssue(JavaFileScannerContext context) {
    return (InternalJavaIssueBuilder) internalContext(context).newIssue();
  }

  public static DefaultJavaFileScannerContext internalContext(JavaFileScannerContext context) {
    return (DefaultJavaFileScannerContext) context;
  }

  public static SyntaxToken nextToken(Tree tree) {
    Tree parent = tree.parent();
    if (parent == null) {
      return tree.lastToken();
    }
    List<Tree> children = ((JavaTree) parent).getChildren();
    if (tree.equals(ListUtils.getLast(children))) {
      // last tree, check next from parent
      return nextToken(parent);
    }
    SyntaxToken nextToken = tree.lastToken();
    for (int i = children.indexOf(tree) + 1; i < children.size(); i++) {
      SyntaxToken token = children.get(i).firstToken();
      if (token != null) {
        nextToken = token;
        break;
      }
    }
    return nextToken;
  }

  public static SyntaxToken previousToken(Tree tree) {
    Tree parent = tree.parent();
    if (parent == null) {
      return tree.firstToken();
    }
    List<Tree> children = ((JavaTree) parent).getChildren();
    if (tree.equals(children.get(0))) {
      // first tree, check last from parent
      return previousToken(parent);
    }
    for (int i = children.indexOf(tree) - 1; i >= 0; i--) {
      SyntaxToken token = children.get(i).lastToken();
      if (token != null) {
        return token;
      }
    }
    return previousToken(parent);
  }

  /**
   * Returns the preceding variable in a mutli-variable declaration.
   *
   * @param current The target variable
   * @return An Optional with the preceding variable, Optional.empty otherwise.
   */
  public static Optional<VariableTree> previousVariable(VariableTree current) {
    return previousVariable(current, getSiblings(current));
  }

  private static Optional<VariableTree> previousVariable(VariableTree current, List<? extends Tree> trees) {
    int currentIndex = trees.indexOf(current);
    // If the variable is the first element that follows the opening token, there is no predecessor to return
    if (currentIndex <= 0) {
      return Optional.empty();
    }
    // If there is a predecessor, we check that it is a variable and that it is part of the same declaration
    Tree preceding = trees.get(currentIndex - 1);
    if (preceding.is(Tree.Kind.VARIABLE) && preceding.firstToken().equals(current.firstToken())) {
      return Optional.of((VariableTree) preceding);
    }
    return Optional.empty();
  }

  /**
   * Returns the following variable in a mutli-variable declaration.
   *
   * @param variable The target variable
   * @return An Optional with the following variable, Optional.empty otherwise.
   */
  public static Optional<VariableTree> nextVariable(VariableTree variable) {
    return nextVariable(variable, getSiblings(variable));
  }

  private static Optional<VariableTree> nextVariable(VariableTree current, List<? extends Tree> trees) {
    int currentIndex = trees.indexOf(current);
    // If the variable cannot be found in the parent (a bug) or if the variable is the last one in the block before the closing brace, there is no follower to return.
    if (currentIndex == -1 || trees.size() <= (currentIndex + 1)) {
      return Optional.empty();
    }
    // If there is a following variable, we check that it is a variable and that it is part of the same declaration
    Tree following = trees.get(currentIndex + 1);
    if (following.is(Tree.Kind.VARIABLE) && following.firstToken().equals(current.firstToken())) {
      return Optional.of((VariableTree) following);
    }
    return Optional.empty();
  }

  private static List<? extends Tree> getSiblings(VariableTree current) {
    Tree parent = current.parent();
    switch (parent.kind()) {
      case LIST:
        // parent.parent() is Kind.TRY_STATEMENT or Kind.FOR_STATEMENT
        return (List<? extends Tree>) parent;
      case BLOCK:
      case INITIALIZER:
      case STATIC_INITIALIZER:
        return ((JavaTree) parent).getChildren();
      case CASE_GROUP:
        return ((CaseGroupTree) parent).body();
      case METHOD:
      case CONSTRUCTOR:
      case CATCH:
      case LAMBDA_EXPRESSION:
      case FOR_EACH_STATEMENT:
      case PATTERN_INSTANCE_OF:
        return Collections.emptyList();
      case CLASS:
      case ENUM:
      case INTERFACE:
      case ANNOTATION_TYPE:
        return ((ClassTree) parent).members();
      case RECORD:
        ClassTree classLike = (ClassTree) parent;
        return classLike.recordComponents().contains(current) ? Collections.emptyList() : classLike.members();
      default:
        throw new IllegalArgumentException("The variable's parent kind " + parent.kind() + " is not handled by this method!");
    }
  }

  public static String contentForTree(Tree tree, JavaFileScannerContext context) {
    SyntaxToken firstToken = tree.firstToken();
    if (firstToken == null) {
      return "";
    }
    return contentForRange(firstToken, tree.lastToken(), context);
  }

  public static String contentForRange(SyntaxToken firstToken, SyntaxToken lastToken,
    JavaFileScannerContext context) {

    Position firstPosition = Position.startOf(firstToken);
    Position lastPosition = Position.endOf(lastToken);

    int startLine = firstPosition.line();
    int endLine = lastPosition.line();

    int beginIndex = firstPosition.columnOffset();
    int endIndex = lastPosition.columnOffset();

    if (startLine == endLine) {
      // one-liners
      return context.getFileLines().get(startLine - 1).substring(beginIndex, endIndex);
    }

    // rely on file content KEEPING line separators
    List<String> lines = context.getFileLines().subList(startLine - 1, endLine);

    // rebuild content of tree as String
    StringBuilder sb = new StringBuilder();
    sb.append(lines.get(0)
        .substring(beginIndex))
      .append("\n");
    for (int i = 1; i < lines.size() - 1; i++) {
      sb.append(lines.get(i))
        .append("\n");
    }
    sb.append(ListUtils.getLast(lines), 0, endIndex);

    return sb.toString();
  }

  /**
   * Check if a given type "requiredType" is available in the current "context". Imports are cached to not have to recompute order all the time.
   * A new import is required if the given type:
   * <ul>
   *   <li>is not in the same package of the current compilation unit</li>
   *   <li>is not yet imported</li>
   * </ul>
   * If the type is not yet known in the context, the supplier will provide an edit to be inserted with a quick-fix,
   * which will add the type as import in the existing list of imports, at the best place it can.
   *
   * If the type is already available in the current context, then no changes are required. There is no need of an extra import.
   *
   * @param context The context of analysis
   */
  public static ImportSupplier newImportSupplier(JavaFileScannerContext context) {
    return new ImportSupplier(context.getTree());
  }

  public static class ImportSupplier {

    @Nullable private final PackageDeclarationTree packageDeclaration;
    @Nullable private final String packageName;
    @Nullable private final Tree firstType;
    private final List<ImportWithName> sortedNonStaticImports;
    private final Set<String> importedTypes;
    private final Set<String> starImportPackages;

    private final Map<String, Optional<JavaTextEdit>> cachedResults = new HashMap<>();

    private ImportSupplier(CompilationUnitTree cut) {
      this.packageDeclaration = cut.packageDeclaration();
      this.packageName = packageDeclaration == null ? null : ExpressionsHelper.concatenate(packageDeclaration.packageName());

      List<Tree> types = cut.types();
      this.firstType = types.isEmpty() ? null : types.get(0);

      this.sortedNonStaticImports = new ArrayList<>();
      this.importedTypes = new HashSet<>();
      this.starImportPackages = new HashSet<>();

      cut.imports()
        .stream()
        .filter(importClauseTree -> importClauseTree.is(Tree.Kind.IMPORT))
        .map(ImportTree.class::cast)
        .filter(importTree -> !importTree.isStatic())
        .map(ImportWithName::new)
        .sorted(ImportWithName.COMPARATOR)
        .forEach(importWithName -> {
          sortedNonStaticImports.add(importWithName);
          String importName = importWithName.importName();
          if (importName.endsWith(".*")) {
            starImportPackages.add(typeToPackageName(importName));
          } else {
            importedTypes.add(importName);
          }
        });
    }

    /**
     * Only entry point to the supplier. Check if importing the required type is needed.
     *
     * @param requiredType The fully qualified name of a the type required to compile the associated quick-fix
     * @return An empty Optional if there is no need of an extra import.
     *         Otherwise, the edit inserting a new import at the best possible place corresponding to the required type.
     */
    public Optional<JavaTextEdit> newImportEdit(String requiredType) {
      return cachedResults.computeIfAbsent(requiredType, this::locateNewImportEdit);
    }

    // We use "\n" systematically, the IDE will decide which one to use,
    // therefore suppressing java:S3457 (Printf-style format strings should be used correctly)
    @SuppressWarnings("java:S3457")
    private Optional<JavaTextEdit> locateNewImportEdit(String requiredType) {
      if (!requiresImportOf(requiredType)) {
        return Optional.empty();
      }

      if (sortedNonStaticImports.isEmpty()) {
        if (packageDeclaration != null) {
          // could be a normal compilation unit or a package-info file: 2 lines after the package declaration
          return Optional.of(JavaTextEdit.insertAfterTree(packageDeclaration, String.format("\n\nimport %s;", requiredType)));
        }
        // default package
        if (firstType != null) {
          // two lines before the first type
          return Optional.of(JavaTextEdit.insertBeforeTree(firstType, String.format("import %s;\n\n", requiredType)));
        }
        // no package declaration and no type? Should be impossible or an empty file
        return Optional.empty();
      }

      ImportTree lastCheckedImport = null;
      for (ImportWithName importWithName : sortedNonStaticImports) {
        String importedType = importWithName.importName();
        if (requiredType.compareTo(importedType) <= 0) {
          break;
        }
        lastCheckedImport = importWithName.tree();
      }

      if (lastCheckedImport != null) {
        // in between the similar ones, in a logical order, alphabetically
        return Optional.of(JavaTextEdit.insertAfterTree(lastCheckedImport, String.format("\nimport %s;", requiredType)));
      }
      // before the first one in alphabetical order
      return Optional.of(JavaTextEdit.insertBeforeTree(sortedNonStaticImports.get(0).tree(), String.format("import %s;\n", requiredType)));
    }

    @VisibleForTesting
    boolean requiresImportOf(String requiredType) {
      if (!requiredType.contains(".") || importedTypes.contains(requiredType)) {
        // already explicitly imported
        return false;
      }

      String requiredPackage = typeToPackageName(requiredType);
      if (starImportPackages.contains(requiredPackage)) {
        // included in a star-import
        return false;
      }

      // not part of the same package
      return !requiredPackage.equals(packageName);
    }

    private static String typeToPackageName(String requiredType) {
      return requiredType.substring(0, requiredType.lastIndexOf("."));
    }

    private static class ImportWithName {

      private static final Comparator<ImportWithName> COMPARATOR = (i1, i2) -> i1.importName.compareTo(i2.importName);

      private final String importName;
      private final ImportTree tree;

      private ImportWithName(ImportTree tree) {
        this.tree = tree;
        this.importName = ExpressionsHelper.concatenate((ExpressionTree) tree.qualifiedIdentifier());
      }

      public ImportTree tree() {
        return tree;
      }

      public String importName() {
        return importName;
      }
    }

  }
}
