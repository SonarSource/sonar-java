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

import org.sonar.api.CoreProperties;
import org.sonar.api.batch.DependedUpon;
import org.sonar.api.batch.DependsUpon;
import org.sonar.api.batch.Phase;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.checks.AnnotationCheckFactory;
import org.sonar.api.checks.NoSonarFilter;
import org.sonar.api.config.Settings;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.java.DefaultJavaResourceLocator;
import org.sonar.java.JavaClasspath;
import org.sonar.java.JavaConfiguration;
import org.sonar.java.JavaSquid;
import org.sonar.java.Measurer;
import org.sonar.java.SonarComponents;
import org.sonar.java.api.JavaUtils;
import org.sonar.java.checks.CheckList;
import org.sonar.squidbridge.api.CodeVisitor;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Phase(name = Phase.Name.PRE)
@DependsUpon(JavaUtils.BARRIER_BEFORE_SQUID)
@DependedUpon(value = JavaUtils.BARRIER_AFTER_SQUID)
public class JavaSquidSensor implements Sensor {

  private final JavaClasspath javaClasspath;
  private final SonarComponents sonarComponents;
  private final FileSystem fs;
  private final DefaultJavaResourceLocator javaResourceLocator;
  private final Settings settings;
  private final RulesProfile profile;
  private final NoSonarFilter noSonarFilter;

  public JavaSquidSensor(RulesProfile profile, JavaClasspath javaClasspath, SonarComponents sonarComponents, FileSystem fs,
                         DefaultJavaResourceLocator javaResourceLocator, Settings settings, org.sonar.api.checks.NoSonarFilter noSonarFilter) {
    this.profile = profile;
    this.noSonarFilter = noSonarFilter;
    this.javaClasspath = javaClasspath;
    this.sonarComponents = sonarComponents;
    this.fs = fs;
    this.javaResourceLocator = javaResourceLocator;
    this.settings = settings;
  }

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    return fs.hasFiles(fs.predicates().hasLanguage(Java.KEY));
  }

  @Override
  public void analyse(Project project, SensorContext context) {
    AnnotationCheckFactory annotationCheckFactory = AnnotationCheckFactory.create(profile, CheckList.REPOSITORY_KEY, CheckList.getChecks());
    Collection<CodeVisitor> checks = annotationCheckFactory.getChecks();
    JavaConfiguration configuration = createConfiguration();
    Measurer measurer = new Measurer(project, context, configuration.isAnalysePropertyAccessors());
    JavaSquid squid = new JavaSquid(configuration, sonarComponents, measurer, javaResourceLocator, checks.toArray(new CodeVisitor[checks.size()]));
    squid.scan(getSourceFiles(project), getTestFiles(project), getBytecodeFiles());
    new Bridges(squid, settings).save(context, project, annotationCheckFactory, javaResourceLocator.getResourceMapping(),
        sonarComponents.getResourcePerspectives(), noSonarFilter, profile);
  }

  private Iterable<InputFile> getSourceFiles(Project project) {
    return fs.inputFiles(fs.predicates().and(fs.predicates().hasLanguage(Java.KEY), fs.predicates().hasType(InputFile.Type.MAIN)));
  }

  private Iterable<InputFile> getTestFiles(Project project) {
    return fs.inputFiles(fs.predicates().and(fs.predicates().hasLanguage(Java.KEY), fs.predicates().hasType(InputFile.Type.TEST)));
  }

  private List<File> getBytecodeFiles() {
    if (settings.getBoolean(CoreProperties.DESIGN_SKIP_DESIGN_PROPERTY)) {
      return Collections.emptyList();
    }
    return javaClasspath.getElements();
  }

  private JavaConfiguration createConfiguration() {
    boolean analyzePropertyAccessors = settings.getBoolean(JavaPlugin.SQUID_ANALYSE_ACCESSORS_PROPERTY);
    Charset charset = fs.encoding();
    JavaConfiguration conf = new JavaConfiguration(charset);
    conf.setAnalyzePropertyAccessors(analyzePropertyAccessors);
    return conf;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }

}
