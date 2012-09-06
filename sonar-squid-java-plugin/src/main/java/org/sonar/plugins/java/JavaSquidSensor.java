/*
 * Sonar Java
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

import org.apache.commons.lang.StringUtils;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.ProjectClasspath;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.SquidUtils;
import org.sonar.api.checks.AnnotationCheckFactory;
import org.sonar.api.checks.NoSonarFilter;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.Project;
import org.sonar.java.JavaConfiguration;
import org.sonar.java.JavaSquid;
import org.sonar.java.checks.CheckList;
import org.sonar.squid.api.CodeVisitor;
import org.sonar.squid.api.SourceCode;
import org.sonar.squid.api.SourceFile;
import org.sonar.squid.indexer.QueryByType;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class JavaSquidSensor implements Sensor {

  private final AnnotationCheckFactory annotationCheckFactory;
  private final NoSonarFilter noSonarFilter;
  private final ProjectClasspath projectClasspath;
  private final FileLinesContextFactory fileLinesContextFactory;

  public JavaSquidSensor(RulesProfile profile, NoSonarFilter noSonarFilter, ProjectClasspath projectClasspath, FileLinesContextFactory fileLinesContextFactory) {
    this.annotationCheckFactory = AnnotationCheckFactory.create(profile, CheckList.REPOSITORY_KEY, CheckList.getChecks());
    this.noSonarFilter = noSonarFilter;
    this.projectClasspath = projectClasspath;
    this.fileLinesContextFactory = fileLinesContextFactory;
  }

  public boolean shouldExecuteOnProject(Project project) {
    return Java.KEY.equals(project.getLanguageKey());
  }

  public void analyse(Project project, SensorContext context) {
    Collection<CodeVisitor> checks = annotationCheckFactory.getChecks();

    JavaSquid squid = new JavaSquid(createConfiguration(project), fileLinesContextFactory, checks.toArray(new CodeVisitor[checks.size()]));
    squid.scan(project.getFileSystem().getSourceDirs(), getBytecodeFiles(project));

    new Bridges(squid).save(context, project, annotationCheckFactory);

    save(squid.getIndex().search(new QueryByType(SourceFile.class)));
  }

  private List<File> getBytecodeFiles(Project project) {
    if (project.getConfiguration().getBoolean(CoreProperties.DESIGN_SKIP_DESIGN_PROPERTY, CoreProperties.DESIGN_SKIP_DESIGN_DEFAULT_VALUE)) {
      return Collections.emptyList();
    }
    return projectClasspath.getElements();
  }

  private JavaConfiguration createConfiguration(Project project) {
    boolean analyzePropertyAccessors = project.getConfiguration().getBoolean(
        JavaSquidPlugin.SQUID_ANALYSE_ACCESSORS_PROPERTY,
        JavaSquidPlugin.SQUID_ANALYSE_ACCESSORS_DEFAULT_VALUE);
    String fieldNamesToExcludeFromLcom4Computation = project.getConfiguration().getString(
        JavaSquidPlugin.FIELDS_TO_EXCLUDE_FROM_LCOM4_COMPUTATION,
        JavaSquidPlugin.FIELDS_TO_EXCLUDE_FROM_LCOM4_COMPUTATION_DEFAULT_VALUE);
    Charset charset = project.getFileSystem().getSourceCharset();

    JavaConfiguration conf = new JavaConfiguration(charset);
    conf.setAnalyzePropertyAccessors(analyzePropertyAccessors);
    for (String fieldName : StringUtils.split(fieldNamesToExcludeFromLcom4Computation, ',')) {
      if (StringUtils.isNotBlank(fieldName)) {
        conf.addFieldToExcludeFromLcom4Calculation(fieldName);
      }
    }

    return conf;
  }

  // TODO replace by NoSonarBridge
  private void save(Collection<SourceCode> squidSourceFiles) {
    for (SourceCode squidSourceFile : squidSourceFiles) {
      SourceFile squidFile = (SourceFile) squidSourceFile;
      JavaFile sonarFile = SquidUtils.convertJavaFileKeyFromSquidFormat(squidFile.getKey());
      noSonarFilter.addResource(sonarFile, squidFile.getNoSonarTagLines());
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }

}
