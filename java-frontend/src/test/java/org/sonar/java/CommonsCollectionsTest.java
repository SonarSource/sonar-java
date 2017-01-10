/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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
package org.sonar.java;

import com.google.common.collect.Maps;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.JavaResourceLocator;
import org.sonar.squidbridge.api.CodeVisitor;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class CommonsCollectionsTest {

  private static JavaSquid squid;
  private static SensorContextTester context;

  private void initAndScan() {
    File prjDir = new File("target/test-projects/commons-collections-3.2.1");
    File srcDir = new File(prjDir, "src");

    JavaConfiguration conf = new JavaConfiguration(StandardCharsets.UTF_8);
    context = SensorContextTester.create(srcDir);
    DefaultFileSystem fs = context.fileSystem();
    Collection<File> files = FileUtils.listFiles(srcDir, new String[]{"java"}, true);
    for (File file : files) {
      fs.add(new DefaultInputFile("", file.getPath()));
    }
    Measurer measurer = new Measurer(fs, context, mock(NoSonarFilter.class));
    JavaResourceLocator javaResourceLocator = new JavaResourceLocator() {
      public Map<String, String> sourceFileCache = Maps.newHashMap();

      @Override
      public InputFile findResourceByClassName(String className) {
        return null;
      }

      @Override
      public String findSourceFileKeyByClassName(String className) {
        String name = className.replace('.', '/');
        return sourceFileCache.get(name);
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
      public void scanFile(JavaFileScannerContext context) {
        JavaFilesCache javaFilesCache = new JavaFilesCache();
        javaFilesCache.scanFile(context);
        for (String key : javaFilesCache.resourcesCache.keySet()){
          sourceFileCache.put(key, context.getFileKey());
        }
      }
    };
    squid = new JavaSquid(conf, null, measurer, javaResourceLocator, null, new CodeVisitor[0]);
    squid.scan(files, Collections.<File>emptyList());
  }

  private Map<String, Double> getMetrics() {
    Map<String, Double> metrics = new HashMap<>();
    for (InputFile inputFile : context.fileSystem().inputFiles()) {
      for (org.sonar.api.batch.sensor.measure.Measure measure : context.measures(inputFile.key())) {
        if (measure.value() != null) {
          String key = measure.metric().key();
          double value = 0;
          try {
            value = Double.parseDouble("" + measure.value());
          } catch (NumberFormatException nfe) {
            //do nothing
          }
          if (metrics.get(key) == null) {
            metrics.put(key, value);
          } else {
            metrics.put(key, metrics.get(key) + value);
          }
        }
      }
    }
    return metrics;
  }

  @Test
  public void measures_on_project() throws Exception {
    initAndScan();
    Map<String, Double> metrics = getMetrics();

    assertThat(metrics.get("classes").intValue()).isEqualTo(412);
    assertThat(metrics.get("ncloc").intValue()).isEqualTo(26323);
    assertThat(metrics.get("statements").intValue()).isEqualTo(12047);
    assertThat(metrics.get("comment_lines").intValue()).isEqualTo(17908);
    assertThat(metrics.get("functions").intValue()).isEqualTo(3762);
    assertThat(metrics.get("complexity").intValue()).isEqualTo(6721);
  }

}
