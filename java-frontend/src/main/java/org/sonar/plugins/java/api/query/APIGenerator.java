/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
package org.sonar.plugins.java.api.query;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.sonar.plugins.java.api.tree.Tree;

import static java.nio.charset.StandardCharsets.UTF_8;

public record APIGenerator(
  String srcPackageName,
  String dstPackageName,
  Path dstPackageDirectory,
  Deque<Class> classToGenerate,
  Map<Class, String> generatedQueries) {

  public static void main(String[] args) throws IOException, ClassNotFoundException {
    System.out.println("APIGenerator");
    Path projectBaseDir = java.nio.file.Path.of(args.length > 0 ? args[0] : ".");
    System.out.println("Project Base Dir: " + projectBaseDir);
    Path projectBuildDir = java.nio.file.Path.of(args.length > 1 ? args[1] : "target");
    System.out.println("Project Build Dir: " + projectBuildDir);

    String srcPackageName = "org.sonar.plugins.java.api.tree";
    Path srcPackageDirectory = projectBaseDir.resolve(
      ("src.main.java." + srcPackageName).replace('.', File.separatorChar));
    System.out.println("Src Package Directory: " + srcPackageDirectory);
    // /Users/alban/dev/github/SonarSource/sonar-java/java-frontend/src/main/java/org/sonar/plugins/java/api/tree
    String dstPackageName = "org.sonar.plugins.java.api.query";
    Path dstPackageDirectory = projectBuildDir.resolve(
      ("generated-sources.java." + dstPackageName).replace('.', File.separatorChar));
    System.out.println("Dst Package Directory: " + dstPackageDirectory);
    Files.createDirectories(dstPackageDirectory);

    var generator = new APIGenerator(
      srcPackageName,
      dstPackageName,
      dstPackageDirectory,
      new LinkedList<>(),
      new HashMap<>());
    try (Stream<Path> children = Files.list(srcPackageDirectory)) {
      Set<Class<?>> treeClasses = new LinkedHashSet<>();
      Map<Class<?>, Set<Class<?>>> derivedTreeClasses = new HashMap<>();
      for (Path path : children.toList()) {
        var filename = path.getFileName().toString();
        if (filename.endsWith(".java") && !filename.equals("package-info.java")) {
          var className = filename.substring(0, filename.length() - 5);
          var srcClass = java.lang.Class.forName(srcPackageName + "." + className);
          if (Tree.class.isAssignableFrom(srcClass)) {
            treeClasses.add(srcClass);
          }
        }
      }
      for (var treeClass : treeClasses) {
        derivedTreeClasses.put(treeClass, new LinkedHashSet<>());
      }
      for (var treeClass : treeClasses) {
        appendDerivedClasses(derivedTreeClasses, treeClass, treeClass.getSuperclass());
        appendDerivedClasses(derivedTreeClasses, treeClass, treeClass.getInterfaces());
      }
      generator.generateCommonTreeQuery();
      for (var treeClass : treeClasses) {
        generator.generate(treeClass, derivedTreeClasses);
      }
    }
  }

  private static void appendDerivedClasses(Map<Class<?>, Set<Class<?>>> derivedTreeClasses, Class<?> derivedClass, Class<?>... superClasses) {
    for (Class<?> superClass : superClasses) {
      if (superClass != null) {
        if (derivedTreeClasses.containsKey(superClass)) {
          derivedTreeClasses.get(superClass).add(derivedClass);
        }
        appendDerivedClasses(derivedTreeClasses, derivedClass, superClass.getSuperclass());
        appendDerivedClasses(derivedTreeClasses, derivedClass, superClass.getInterfaces());
      }
    }
  }

  public void generateCommonTreeQuery() throws IOException {
    System.out.println("Generating CommonTreeQuery class");
    Path dstClassPath = dstPackageDirectory.resolve("CommonTreeQuery.java");
    Files.writeString(dstClassPath, """
      package ${dstPackageName};
      
      import ${srcPackageName}.Tree;
      
      public class CommonTreeQuery<T extends Tree> extends Selector<T> {
      
        public CommonTreeQuery(Class<T> selectorType, Selector<?> parent) {
          super(selectorType, parent);
        }
      
        public Query subtreesIf(java.util.function.BiPredicate<Context, Tree> visitChildren) {
          Query query = new Query(this);
          SubTreeVisitor subTreeVisitor = new Selector.SubTreeVisitor(query, visitChildren);
          this.visitors.add(subTreeVisitor::visit);
          return query;
        }
      }
      """
      .replace("${srcPackageName}", srcPackageName)
      .replace("${dstPackageName}", dstPackageName), UTF_8);
  }

  public void generate(Class<?> srcClass, Map<Class<?>, Set<Class<?>>> derivedTreeClasses) throws IOException {
    if (Tree.class.isAssignableFrom(srcClass)) {
      String srcClassName = srcClass.getSimpleName();
      String dstClassName = queryClassName(srcClass);
      System.out.println("Generating " + dstClassName + " from " + srcClass.getName());
      Path dstClassPath = dstPackageDirectory.resolve(dstClassName + ".java");
      Files.writeString(dstClassPath, """
        package ${dstPackageName};

        import java.util.function.Function;
        import java.util.List;
        import ${srcPackageName}.*;

        public class ${dstClassName} extends CommonTreeQuery<${srcClassName}> {

          public ${dstClassName}() {
            this(null);
          }
        
          protected ${dstClassName}(Selector<? extends Tree> parent) {
            super(${srcClassName}.class, parent);
          }

        ${getters}${filters}
        }
        """
        .replace("${srcPackageName}", srcPackageName)
        .replace("${srcClassName}", srcClassName)
        .replace("${dstPackageName}", dstPackageName)
        .replace("${dstClassName}", dstClassName)
        .replace("${getters}", generateGetters(srcClass))
        .replace("${filters}", generateFilters(srcClass, derivedTreeClasses)), UTF_8);
    }
  }

  private CharSequence generateFilters(Class<?> srcClass, Map<Class<?>, Set<Class<?>>> derivedTreeClasses) {
    StringBuilder out = new StringBuilder();
    derivedTreeClasses.get(srcClass).forEach(derivedClass -> {
      var derivedClassName = derivedClass.getSimpleName();
      var derivedQueryName = queryClassName(derivedClass);
      out.append("""
          public ${derivedQueryName} filter${derivedClassName}() {
            return  addConversion(new ${derivedQueryName}(this), tree -> tree instanceof ${derivedClassName} t ? t : null);
          }

        """
        .replace("${derivedClassName}", derivedClassName)
        .replace("${derivedQueryName}", derivedQueryName));
    });
    return out;
  }

  private CharSequence generateGetters(Class<?> srcClass) {
    String srcClassName = srcClass.getSimpleName();
    StringBuilder out = new StringBuilder();
    for (var method : srcClass.getMethods()) {
      if (method.getParameterCount() == 0 ) {
        Type returnType = method.getGenericReturnType();
        Class<?> returnClass = method.getReturnType();
        if (List.class.isAssignableFrom(returnClass) &&
          returnType instanceof ParameterizedType type &&
          type.getActualTypeArguments().length == 1 &&
          type.getActualTypeArguments()[0] instanceof Class<?> elemClass &&
          Tree.class.isAssignableFrom(elemClass)) {
          generateListGetter(out, method, srcClassName, elemClass);
        } else if (Tree.class.isAssignableFrom(returnClass)) {
          generateGetter(out, method, srcClassName);
        }
      }
    }
    return out;
  }

  private void generateGetter(StringBuilder out, Method method, String srcClassName) {
    var returnTreeName = method.getReturnType().getSimpleName();
    var returnQueryName = queryClassName(method.getReturnType());
    out.append("""
        public ${returnQueryName} ${methodName}() {
          Function<${srcClassName}, ${returnTreeName}> conversion = tree -> tree.${methodName}();
          return add(new Conversion<>(new ${returnQueryName}(this), conversion));
        }

      """
      .replace("${srcClassName}", srcClassName)
      .replace("${methodName}", method.getName())
      .replace("${returnTreeName}", returnTreeName)
      .replace("${returnQueryName}", returnQueryName));
  }

  private void generateListGetter(StringBuilder out, Method method, String srcClassName, Class<?> listElementClass) {
    var elementTreeName = listElementClass.getSimpleName();
    var returnQueryName = queryClassName(listElementClass);
    out.append("""
        public ${returnQueryName} ${methodName}() {
          Function<${srcClassName}, List<${elementTreeName}>> conversion = tree -> tree.${methodName}();
          return add(new ListConversion<>(new ${returnQueryName}(this), conversion));
        }

      """
      .replace("${srcClassName}", srcClassName)
      .replace("${methodName}", method.getName())
      .replace("${elementTreeName}", elementTreeName)
      .replace("${returnQueryName}", returnQueryName));
  }

  private String queryClassName(Class<?> treeClassName) {
    return treeClassName.getSimpleName().replaceFirst("Tree$", "") + "Query";
  }
}
