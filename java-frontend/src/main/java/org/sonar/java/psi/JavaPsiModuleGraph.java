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

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiJavaModule;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.search.GlobalSearchScope;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Session-owned module visibility for SonarJava's explicit sources and ordered classpath. */
final class JavaPsiModuleGraph {

  private final boolean modulesEnabled;
  private final boolean ignoreUnnamedModuleForSplitPackage;
  private final Map<String, List<PsiJavaModule>> modules = new HashMap<>();
  private final Map<VirtualFile, PsiJavaModule> moduleRoots = new HashMap<>();
  private final List<VirtualFile> unnamedRoots = new ArrayList<>();
  private final Set<String> sourcePackages = new HashSet<>();
  private final Map<String, Set<PsiJavaModule>> exporters = new HashMap<>();
  private final Map<PsiJavaModule, Set<PsiJavaModule>> transitive = new HashMap<>();

  JavaPsiModuleGraph(int javaVersion, boolean ignoreUnnamedModuleForSplitPackage) {
    modulesEnabled = javaVersion >= 9;
    this.ignoreUnnamedModuleForSplitPackage = ignoreUnnamedModuleForSplitPackage;
  }

  void addRoot(VirtualFile root, PsiJavaModule module) {
    if (module == null) {
      unnamedRoots.add(root);
      return;
    }
    moduleRoots.put(root, module);
    modules.computeIfAbsent(module.getName(), ignored -> new ArrayList<>()).add(module);
    for (var export : module.getExports()) {
      var name = export.getPackageName();
      if (name != null) {
        exporters.computeIfAbsent(name, ignored -> new LinkedHashSet<>()).add(module);
      }
    }
    transitive.clear();
  }

  void addSourcePackage(String name) {
    sourcePackages.add(name);
  }

  Collection<PsiJavaModule> findModules(String name, GlobalSearchScope scope) {
    return modules.getOrDefault(name, List.of()).stream()
      .filter(module -> scope.contains(module.getContainingFile().getVirtualFile())).toList();
  }

  private PsiJavaModule findModule(String name) {
    var candidates = modules.getOrDefault(name, List.of());
    return candidates.size() == 1 ? candidates.getFirst() : null;
  }

  PsiJavaModule descriptor(PsiElement element) {
    if (element == null) {
      return null;
    }
    if (element instanceof PsiJavaModule module) {
      return module;
    }
    var file = element instanceof PsiFile psiFile ? psiFile : element.getContainingFile();
    var entry = file == null ? null : file.getVirtualFile();
    while (entry != null) {
      var module = moduleRoots.get(entry);
      if (module != null) {
        return module;
      }
      entry = entry.getParent();
    }
    return null;
  }

  /** Module imports follow only requires-transitive edges, including static-transitive edges at analysis time. */
  Set<PsiJavaModule> transitiveDependencies(PsiJavaModule source) {
    return transitive.computeIfAbsent(source, module -> {
      var visited = new LinkedHashSet<PsiJavaModule>();
      var pending = new ArrayDeque<PsiJavaModule>();
      pending.add(module);
      while (!pending.isEmpty()) {
        ProgressManager.checkCanceled();
        for (var requires : pending.removeFirst().getRequires()) {
          if (requires.hasModifierProperty(PsiModifier.TRANSITIVE)) {
            var dependency = findModule(requires.getModuleName());
            if (dependency != null && dependency != module && visited.add(dependency)) {
              pending.add(dependency);
            }
          }
        }
      }
      return Set.copyOf(visited);
    });
  }

  /** ECJ treats a type reached twice within one module import as ambiguous, even when both paths reach the same declaration. */
  boolean ambiguousModuleImport(PsiClass target, PsiElement place) {
    if (!(place instanceof PsiJavaCodeReferenceElement reference) || reference.getQualifier() != null ||
      !java.util.Objects.equals(reference.getReferenceName(), target.getName()) || target.getContainingClass() != null ||
      !(place.getContainingFile() instanceof PsiJavaFile file) || !(target.getContainingFile() instanceof PsiJavaFile targetFile)) {
      return false;
    }
    var packageName = targetFile.getPackageName();
    var imports = file.getImportList();
    var targetModule = descriptor(target);
    if (imports == null || targetModule == null || "java.lang".equals(packageName) || packageName.equals(file.getPackageName()) ||
      imports.findSingleClassImportStatement(target.getQualifiedName()) != null || imports.findOnDemandImportStatement(packageName) != null) {
      return false;
    }
    for (var statement : imports.getImportModuleStatements()) {
      var module = findModule(statement.getReferenceName());
      if (module != null && importPaths(module, targetModule, new HashSet<>()) > 1) {
        return true;
      }
    }
    return false;
  }

  private int importPaths(PsiJavaModule module, PsiJavaModule target, Set<PsiJavaModule> path) {
    ProgressManager.checkCanceled();
    if (!path.add(module)) {
      return 0;
    }
    int count = module == target ? 1 : 0;
    try {
      for (var requires : module.getRequires()) {
        if (requires.hasModifierProperty(PsiModifier.TRANSITIVE)) {
          var dependency = findModule(requires.getModuleName());
          if (dependency != null) {
            count += importPaths(dependency, target, path);
            if (count > 1) {
              return 2;
            }
          }
        }
      }
      return count;
    } finally {
      path.remove(module);
    }
  }

  private boolean reads(PsiJavaModule source, PsiJavaModule target) {
    if (source == null || source == target || PsiJavaModule.JAVA_BASE.equals(target.getName())) {
      return true;
    }
    for (var requires : source.getRequires()) {
      var dependency = findModule(requires.getModuleName());
      if (dependency == target || dependency != null && transitiveDependencies(dependency).contains(target)) {
        return true;
      }
    }
    return false;
  }

  boolean accessible(PsiJavaModule target, PsiElement place) {
    return !modulesEnabled || reads(descriptor(place), target);
  }

  private boolean exports(PsiJavaModule module, String name, PsiJavaModule reader) {
    if (module == reader) {
      return true;
    }
    for (var export : module.getExports()) {
      if (name.equals(export.getPackageName()) &&
        (export.getModuleNames().isEmpty() || reader != null && export.getModuleNames().contains(reader.getName()))) {
        return true;
      }
    }
    return false;
  }

  private boolean hasUnnamedPackage(String name) {
    if (sourcePackages.contains(name)) {
      return true;
    }
    var path = name.replace('.', '/');
    return unnamedRoots.stream().anyMatch(root -> root.findFileByRelativePath(path) != null);
  }

  boolean accessible(String packageName, PsiFile targetFile, PsiElement place) {
    if (!modulesEnabled) {
      return true;
    }
    var reader = descriptor(place);
    var target = descriptor(targetFile);
    if (reader != null && reader == target) {
      return true;
    }
    var visible = exporters.getOrDefault(packageName, Set.of()).stream()
      .filter(module -> reads(reader, module) && exports(module, packageName, reader)).toList();
    if (visible.size() > 1 || !visible.isEmpty() && !ignoreUnnamedModuleForSplitPackage && hasUnnamedPackage(packageName)) {
      return false;
    }
    if (targetFile == null) {
      return !visible.isEmpty() || reader == null && hasUnnamedPackage(packageName);
    }
    return target == null ? reader == null : reads(reader, target) && exports(target, packageName, reader);
  }
}
