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
package org.sonar.plugins.java.bridges;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.SquidUtils;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.JavaPackage;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.java.JavaSquid;
import org.sonar.java.api.JavaClass;
import org.sonar.java.api.JavaMethod;
import org.sonar.java.ast.api.JavaMetric;
import org.sonar.squid.api.*;
import org.sonar.squid.indexer.QueryByMeasure;
import org.sonar.squid.indexer.QueryByType;
import org.sonar.squid.indexer.SquidIndex;
import org.sonar.squid.measures.Metric;

import java.util.Collection;
import java.util.HashMap;

public final class ResourceIndex extends HashMap<SourceCode, Resource> {

  private static final long serialVersionUID = -918346378374943773L;

  public ResourceIndex loadSquidResources(JavaSquid squid, SensorContext context, Project project) {
    loadSquidProject(squid.getIndex(), project);
    loadSquidPackages(squid.getIndex(), context);
    loadSquidFiles(squid.getIndex(), context);
    loadSquidClasses(squid.getIndex(), context);
    loadSquidMethods(squid.getIndex(), context);
    return this;
  }

  private void loadSquidProject(SquidIndex squid, Project project) {
    put(squid.search(new QueryByType(SourceProject.class)).iterator().next(), project);
  }

  private void loadSquidPackages(SquidIndex squid, SensorContext context) {
    Collection<SourceCode> packages = squid.search(new QueryByType(SourcePackage.class));
    for (SourceCode squidPackage : packages) {
      JavaPackage sonarPackage = SquidUtils.convertJavaPackageKeyFromSquidFormat(squidPackage.getKey());
      context.index(sonarPackage);
      // resource is reloaded to get the id:
      put(squidPackage, context.getResource(sonarPackage));
    }
  }

  private void loadSquidFiles(SquidIndex squid, SensorContext context) {
    Collection<SourceCode> files = squid.search(new QueryByType(SourceFile.class));
    for (SourceCode squidFile : files) {
      JavaFile sonarFile = SquidUtils.convertJavaFileKeyFromSquidFormat(squidFile.getKey());
      JavaPackage sonarPackage = (JavaPackage) get(squidFile.getParent(SourcePackage.class));
      context.index(sonarFile, sonarPackage);
      // resource is reloaded to get the id:
      put(squidFile, context.getResource(sonarFile));
    }
  }

  /**
   * @deprecated usage of {@link JavaMethod} should be removed for SQ 4.2 (SONARJAVA-438)
   */
  @Deprecated
  private void loadSquidClasses(SquidIndex squid, SensorContext context) {
    Collection<SourceCode> classes = squid.search(new QueryByType(SourceClass.class), new QueryByMeasure(JavaMetric.CLASSES, QueryByMeasure.Operator.GREATER_THAN_EQUALS, 1));
    for (SourceCode squidClass : classes) {
      JavaFile sonarFile = (JavaFile) get(squidClass.getParent(SourceFile.class));
      JavaClass sonarClass = new JavaClass.Builder()
          .setName(convertClassKey(squidClass.getKey()))
          .setFromLine(squidClass.getStartAtLine())
          .setToLine(squidClass.getEndAtLine())
          .create();
      context.index(sonarClass, sonarFile);
      put(squidClass, sonarClass);
    }
  }

  /**
   * @deprecated usage of {@link JavaMethod} should be removed for SQ 4.2 (SONARJAVA-438)
   */
  @Deprecated
  private void loadSquidMethods(SquidIndex squid, SensorContext context) {
    Collection<SourceCode> methods = squid.search(new QueryByType(SourceMethod.class));
    for (SourceCode squidMethod : methods) {
      SourceClass squidClass = squidMethod.getParent(SourceClass.class);
      JavaClass sonarClass = (JavaClass) get(squidClass);
      if (sonarClass != null) {
        JavaMethod sonarMethod = new JavaMethod.Builder()
            .setClass(sonarClass)
            .setSignature(squidMethod.getName())
            .setFromLine(squidMethod.getStartAtLine())
            .setToLine(squidMethod.getEndAtLine())
            .setAccessor(squidMethod.getInt(Metric.ACCESSORS) > 0)
            .create();

        context.index(sonarMethod, sonarClass);
        put(squidMethod, sonarMethod);
      }
    }
  }

  static String convertClassKey(String squidClassKey) {
    return StringUtils.replace(squidClassKey, "/", ".");
  }

}
