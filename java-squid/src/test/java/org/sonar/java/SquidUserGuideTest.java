/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
package org.sonar.java;

import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import org.apache.commons.io.FileUtils;
import org.fest.assertions.Delta;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Resource;
import org.sonar.java.bytecode.visitor.ResourceMapping;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.JavaResourceLocator;
import org.sonar.squidbridge.api.CodeVisitor;
import org.sonar.squidbridge.api.SourceCode;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class SquidUserGuideTest {

  private static JavaSquid squid;
  private static SensorContext context;

  private void initAndScan(boolean separateAccessorsFromMethods) {
    File prjDir = new File("target/test-projects/commons-collections-3.2.1");
    File srcDir = new File(prjDir, "src");
    File binDir = new File(prjDir, "bin");

    JavaConfiguration conf = new JavaConfiguration(Charsets.UTF_8);
    conf.setSeparateAccessorsFromMethods(separateAccessorsFromMethods);
    context = mock(SensorContext.class);
    DefaultFileSystem fs = new DefaultFileSystem(srcDir);
    Collection<File> files = FileUtils.listFiles(srcDir, new String[]{"java"}, true);
    for (File file : files) {
      fs.add(new DefaultInputFile(file.getPath()));
    }
    Measurer measurer = new Measurer(fs, context, separateAccessorsFromMethods);
    JavaResourceLocator javaResourceLocator = new JavaResourceLocator() {
      public Map<String, String> sourceFileCache = Maps.newHashMap();

      @Override
      public Resource findResourceByClassName(String className) {
        return null;
      }

      @Override
      public String findSourceFileKeyByClassName(String className) {
        String name = className.replace('.', '/');
        return sourceFileCache.get(name);
      }

      @Override
      public Collection<String> classKeys() {
        return sourceFileCache.keySet();
      }

      @Override
      public Collection<File> classFilesToAnalyze() {
        return Collections.emptyList();
      }

      @Override
      public Collection<File> classpath() {
        return null;
      }

      @Override
      public Integer getMethodStartLine(String fullyQualifiedMethodName) {
        return null;
      }

      @Override
      public ResourceMapping getResourceMapping() {
        return null;
      }

      @Override
      public void scanFile(JavaFileScannerContext context) {
        JavaFilesCache javaFilesCache = new JavaFilesCache();
        javaFilesCache.scanFile(context);
        for (String key : javaFilesCache.resourcesCache.keySet()){
          sourceFileCache.put(key, context.getFileKey());
        }
      }
    };
    squid = new JavaSquid(conf, null, measurer, javaResourceLocator, new CodeVisitor[0]);
    squid.scan(files, Collections.<File>emptyList(), Collections.singleton(binDir));
  }

  private Map<String, Double> getMetrics() {
    ArgumentCaptor<Measure> captor = ArgumentCaptor.forClass(Measure.class);
    ArgumentCaptor<InputFile> files = ArgumentCaptor.forClass(InputFile.class);
    verify(context, atLeastOnce()).saveMeasure(files.capture(), captor.capture());
    Map<String, Double> metrics = new HashMap<>();
    for (Measure measure : captor.getAllValues()) {
      if(measure.getValue() != null ){
        if(metrics.get(measure.getMetricKey())==null) {
          metrics.put(measure.getMetricKey(), measure.getValue());
        } else {
          metrics.put(measure.getMetricKey(), metrics.get(measure.getMetricKey()) + measure.getValue());
        }
      }
    }
    return metrics;
  }

  private void verifySameResults(Map<String, Double> metrics) {
    assertThat(metrics.get("classes").intValue()).isEqualTo(412);
    assertThat(metrics.get("lines").intValue()).isEqualTo(64125);
    assertThat(metrics.get("ncloc").intValue()).isEqualTo(26323);
    assertThat(metrics.get("statements").intValue()).isEqualTo(12047);
    assertThat(metrics.get("comment_lines").intValue()).isEqualTo(17908);
    double density = 1.0;
    if (metrics.get("public_api").intValue() != 0) {
      density = (metrics.get("public_api") - metrics.get("public_undocumented_api")) / metrics.get("public_api");
    }
    assertThat(density).isEqualTo(0.64, Delta.delta(0.01));
  }

  @Test
  public void measures_on_project_accessors_separated_from_methods() throws Exception {
    initAndScan(true);
    Map<String, Double> metrics = getMetrics();

    verifySameResults(metrics);

    // 69: SONARJAVA-861 separatedAccessorsFromMethods property of the measurer is set to true. Getters and setters ignored.
    assertThat(metrics.get("functions").intValue()).isEqualTo(3762 - 69);
    assertThat(metrics.get("public_api").intValue()).isEqualTo(3221 - 69);
    assertThat(metrics.get("complexity").intValue()).isEqualTo(8462 - 80 /* SONAR-3793 */- 2 /* SONAR-3794 */+ 13 /* SONARJAVA-861 */);
  }

  @Test
  public void measures_on_project_accessors_handled_as_methods() throws Exception {
    initAndScan(false);
    Map<String, Double> metrics = getMetrics();

    verifySameResults(metrics);

    assertThat(metrics.get("functions").intValue()).isEqualTo(3762);
    assertThat(metrics.get("public_api").intValue()).isEqualTo(3221);
    assertThat(metrics.get("complexity").intValue()).isEqualTo(8462);
  }

  @Test
  public void getDependenciesBetweenPackages() {
    initAndScan(true);
    SourceCode collectionsPackage = squid.search("org/apache/commons/collections");
    SourceCode bufferPackage = squid.search("org/apache/commons/collections/buffer");
    SourceCode bidimapPackage = squid.search("org/apache/commons/collections/bidimap");
    // assertThat(squid.getDependency(bidimapPackage, collectionsPackage).getUsage()).isEqualTo(SourceCodeEdgeUsage.USES);
    // assertThat(squid.getDependency(collectionsPackage, bufferPackage).getUsage()).isEqualTo(SourceCodeEdgeUsage.USES);
    // assertThat(squid.getDependency(collectionsPackage, bufferPackage).getRootEdges().size()).isEqualTo(7);
  }

}
