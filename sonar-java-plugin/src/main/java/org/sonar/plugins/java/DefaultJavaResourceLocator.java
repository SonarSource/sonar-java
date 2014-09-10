/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.java;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.java.JavaClasspath;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.JavaResourceLocator;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.io.File;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;

public class DefaultJavaResourceLocator extends BaseTreeVisitor implements JavaResourceLocator, JavaFileScanner {

  private static final Logger LOG = LoggerFactory.getLogger(JavaResourceLocator.class);

  private final Project project;
  private final JavaClasspath javaClasspath;
  @VisibleForTesting
  Map<String, Resource> resourcesCache;
  private Resource currentResource;
  private Deque<String> currentClassKey = new LinkedList<String>();
  private Deque<Tree> parent = new LinkedList<Tree>();
  private Deque<Integer> anonymousInnerClassCounter = new LinkedList<Integer>();
  private String currentPackage;

  public DefaultJavaResourceLocator(Project project, JavaClasspath javaClasspath) {
    this.project = project;
    this.javaClasspath = javaClasspath;
    resourcesCache = Maps.newHashMap();
  }

  @Override
  public Resource findResourceByClassName(String className) {
    String name = className.replace('.', '/');
    Resource resource = resourcesCache.get(name);
    if (resource == null) {
      LOG.debug("Class not found in resource cache : {}", className);
    }
    return resource;
  }

  @Override
  public Collection<File> classFilesToAnalyze() {
    ImmutableList.Builder<File> result = ImmutableList.builder();
    for (String key : resourcesCache.keySet()) {
      String filePath = key + ".class";
      for (File binaryDir : javaClasspath.getBinaryDirs()) {
        File classFile = new File(binaryDir, filePath);
        if (classFile.isFile()) {
          result.add(classFile);
          break;
        }
      }
    }
    return result.build();
  }

  @Override
  public void scanFile(JavaFileScannerContext context) {
    JavaTree.CompilationUnitTreeImpl tree = (JavaTree.CompilationUnitTreeImpl) context.getTree();
    currentPackage = tree.packageNameAsString().replace('.', '/');
    currentResource = org.sonar.api.resources.File.fromIOFile(context.getFile(), project);
    Preconditions.checkNotNull(currentResource, "resource not found : "+context.getFile().getName());
    currentClassKey.clear();
    parent.clear();
    anonymousInnerClassCounter.clear();
    scan(tree);
  }

  @Override
  public void visitClass(ClassTree tree) {
    String className = "";
    if(tree.simpleName()!=null){
      className = tree.simpleName().name();
    }
    String key = getClassKey(className);
    currentClassKey.push(key);
    parent.push(tree);
    anonymousInnerClassCounter.push(0);
    resourcesCache.put(key, currentResource);
    super.visitClass(tree);
    currentClassKey.pop();
    parent.pop();
    anonymousInnerClassCounter.pop();
  }

  private String getClassKey(String className) {
    String key = className;
    if(StringUtils.isNotEmpty(currentPackage)) {
      key = currentPackage + "/" + className;
    }
    if("".equals(className) || (parent.peek()!=null && parent.peek().is(Tree.Kind.METHOD))) {
      //inner class declared within method
      int count = anonymousInnerClassCounter.pop()+1;
      key = currentClassKey.peek()+"$"+count+className;
      anonymousInnerClassCounter.push(count);
    } else if (currentClassKey.peek() != null) {
      key = currentClassKey.peek()+"$"+className;
    }
    LOG.error(key);
    return key;
  }

  @Override
  public void visitMethod(MethodTree tree) {
    parent.push(tree);
    super.visitMethod(tree);
    parent.pop();
  }
}
