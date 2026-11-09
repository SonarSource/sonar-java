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
package org.sonar.java.psi;

import com.intellij.core.CoreJavaFileManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiJavaModule;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.impl.file.PsiPackageImpl;
import com.intellij.psi.search.GlobalSearchScope;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

final class ExplicitJavaFileManager extends CoreJavaFileManager {

  private final PsiManager manager;
  private final Map<String, List<PsiClass>> classes = new HashMap<>();
  private final JavaPsiModuleGraph moduleGraph;
  private final Set<String> packages = new HashSet<>();
  private final Map<String, PsiJavaFile> packageInfos = new HashMap<>();

  ExplicitJavaFileManager(PsiManager manager, int javaVersion, boolean ignoreUnnamedModuleForSplitPackage) {
    super(manager);
    this.manager = manager;
    moduleGraph = new JavaPsiModuleGraph(javaVersion, ignoreUnnamedModuleForSplitPackage);
  }

  void index(List<PsiJavaFile> files) {
    for (var file : files) {
      var name = file.getPackageName();
      if (file.getName().equals("package-info.java")) {
        packageInfos.putIfAbsent(name, file);
      }
      moduleGraph.addSourcePackage(name);
      while (true) {
        packages.add(name);
        if (name.isEmpty()) {
          break;
        }
        int separator = name.lastIndexOf('.');
        name = separator < 0 ? "" : name.substring(0, separator);
      }
      for (var declaration : file.getClasses()) {
        index(declaration);
      }
    }
  }

  void indexClasspathModule(VirtualFile root) {
    ReadAction.run(() -> {
      PsiJavaModule module = null;
      if (root.getFileSystem() instanceof JrtVirtualFileSystem) {
        var descriptor = root.findChild("module-info.class");
        if (descriptor != null && manager.findFile(descriptor) instanceof PsiJavaFile file) {
          module = file.getModuleDeclaration();
        }
      }
      moduleGraph.addRoot(root, module);
    });
  }

  JavaPsiModuleGraph moduleGraph() {
    return moduleGraph;
  }

  @Override
  public Collection<PsiJavaModule> findModules(String name, GlobalSearchScope scope) {
    return moduleGraph.findModules(name, scope);
  }

  private void index(PsiClass declaration) {
    var name = declaration.getQualifiedName();
    if (name != null) {
      classes.computeIfAbsent(name, ignored -> new ArrayList<>()).add(declaration);
    }
    for (var member : declaration.getInnerClasses()) {
      index(member);
    }
  }

  @Override
  public PsiClass findClass(String name, GlobalSearchScope scope) {
    var source = sources(name, scope).findFirst();
    return source.orElseGet(() -> super.findClass(name, scope));
  }

  @Override
  public PsiClass[] findClasses(String name, GlobalSearchScope scope) {
    return Stream.concat(sources(name, scope), Stream.of(super.findClasses(name, scope))).toArray(PsiClass[]::new);
  }

  @Override
  public PsiPackage findPackage(String name) {
    var info = packageInfos.get(name);
    if (info != null) {
      return new PsiPackageImpl(manager, name) {
        @Override
        public PsiModifierList getAnnotationList() {
          var statement = info.getPackageStatement();
          return statement == null ? null : statement.getAnnotationList();
        }
      };
    }
    return packages.contains(name) ? new PsiPackageImpl(manager, name) : super.findPackage(name);
  }

  private Stream<PsiClass> sources(String name, GlobalSearchScope scope) {
    return classes.getOrDefault(name, List.of()).stream().filter(declaration -> scope.contains(declaration.getContainingFile().getVirtualFile()));
  }
}
