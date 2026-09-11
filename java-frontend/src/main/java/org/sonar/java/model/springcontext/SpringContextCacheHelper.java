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
package org.sonar.java.model.springcontext;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.caching.JsonCacheFormat;
import org.sonar.java.model.springcontext.BeanDefinitionGatherer.BeanData;

/**
 * JSON mapping of the data the Spring context gatherers cache per file.
 *
 * <p>The version is shared by all gatherers: bumping it whenever any entry's shape changes invalidates
 * all previously cached entries, which are then recomputed on the next analysis. The generic document,
 * validation and span mechanics live in {@link JsonCacheFormat}.
 */
final class SpringContextCacheHelper {

  private static final int CACHE_FORMAT_VERSION = 1;

  private static final String BEANS = "beans";
  private static final String PACKAGES = "packages";
  private static final String NAME = "name";
  private static final String TYPE = "type";
  private static final String PACKAGE = "package";
  private static final String SPAN = "span";
  private static final String PRIMARY = "primary";
  private static final String PROFILES = "profiles";
  private static final String DEPENDENCIES = "dependencies";
  private static final String INJECTION_POINTS = "injectionPoints";
  private static final String TYPE_HIERARCHY = "typeHierarchy";

  private SpringContextCacheHelper() {
  }

  static byte[] serializeBeans(List<BeanData> beans) {
    var document = JsonCacheFormat.newDocument(CACHE_FORMAT_VERSION);
    var serializedBeans = new JsonArray();
    beans.stream().map(SpringContextCacheHelper::serializeBean).forEach(serializedBeans::add);
    document.add(BEANS, serializedBeans);
    return JsonCacheFormat.toBytes(document);
  }

  /**
   * Restores bean definitions from their JSON representation, associating every location with the current file.
   */
  static List<BeanData> deserializeBeans(byte[] data, InputFile inputFile) {
    var beans = JsonCacheFormat.requiredArray(JsonCacheFormat.parseDocument(data, CACHE_FORMAT_VERSION), BEANS);
    List<BeanData> result = new ArrayList<>();
    for (JsonElement bean : beans) {
      result.add(deserializeBean(JsonCacheFormat.requiredObject(bean), inputFile));
    }
    return result;
  }

  static byte[] serializeComponentScanPackages(Collection<String> packages) {
    var document = JsonCacheFormat.newDocument(CACHE_FORMAT_VERSION);
    document.add(PACKAGES, JsonCacheFormat.strings(packages));
    return JsonCacheFormat.toBytes(document);
  }

  static Set<String> deserializeComponentScanPackages(byte[] data) {
    var document = JsonCacheFormat.parseDocument(data, CACHE_FORMAT_VERSION);
    return JsonCacheFormat.stringSet(JsonCacheFormat.requiredArray(document, PACKAGES));
  }

  private static JsonObject serializeBean(BeanData bean) {
    var serializedBean = new JsonObject();
    serializedBean.addProperty(NAME, bean.beanName());
    serializedBean.addProperty(TYPE, bean.type());
    serializedBean.addProperty(PACKAGE, bean.beanPackage());
    serializedBean.add(SPAN, JsonCacheFormat.spanToJson(bean.textSpan()));
    serializedBean.addProperty(PRIMARY, bean.isPrimary());
    serializedBean.addProperty(PROFILES, bean.profiles());
    var dependencies = new JsonArray();
    bean.dependencyInjectionPoints().forEach((type, points) -> dependencies.add(serializeDependency(type, points)));
    serializedBean.add(DEPENDENCIES, dependencies);
    serializedBean.add(TYPE_HIERARCHY, JsonCacheFormat.strings(bean.typeHierarchy()));
    return serializedBean;
  }

  private static JsonObject serializeDependency(String type, Set<InjectionPoint> points) {
    var serializedDependency = new JsonObject();
    serializedDependency.addProperty(TYPE, type);
    var injectionPoints = new JsonArray();
    points.stream().map(SpringContextCacheHelper::serializeInjectionPoint).forEach(injectionPoints::add);
    serializedDependency.add(INJECTION_POINTS, injectionPoints);
    return serializedDependency;
  }

  private static JsonObject serializeInjectionPoint(InjectionPoint injectionPoint) {
    var serializedInjectionPoint = new JsonObject();
    serializedInjectionPoint.addProperty(NAME, injectionPoint.name());
    serializedInjectionPoint.add(SPAN, JsonCacheFormat.spanToJson(injectionPoint.location().mainLocation()));
    return serializedInjectionPoint;
  }

  private static BeanData deserializeBean(JsonObject serializedBean, InputFile inputFile) {
    String beanName = JsonCacheFormat.requiredString(serializedBean, NAME);
    String type = JsonCacheFormat.requiredString(serializedBean, TYPE);
    String beanPackage = JsonCacheFormat.requiredString(serializedBean, PACKAGE);
    var span = JsonCacheFormat.spanFromJson(JsonCacheFormat.requiredObject(serializedBean, SPAN));
    boolean isPrimary = JsonCacheFormat.requiredBoolean(serializedBean, PRIMARY);
    String profiles = JsonCacheFormat.nullableString(serializedBean, PROFILES);
    Map<String, Set<InjectionPoint>> injectionPoints = deserializeDependencies(JsonCacheFormat.requiredArray(serializedBean, DEPENDENCIES), inputFile);
    Set<String> typeHierarchy = JsonCacheFormat.stringSet(JsonCacheFormat.requiredArray(serializedBean, TYPE_HIERARCHY));
    return new BeanData(beanName, type, beanPackage, inputFile, span, isPrimary, profiles,
      BeanDefinitionGatherer.projectToNames(injectionPoints), injectionPoints, typeHierarchy);
  }

  private static Map<String, Set<InjectionPoint>> deserializeDependencies(JsonArray dependencies, InputFile inputFile) {
    Map<String, Set<InjectionPoint>> result = new LinkedHashMap<>();
    for (JsonElement dependency : dependencies) {
      JsonObject serializedDependency = JsonCacheFormat.requiredObject(dependency);
      String type = JsonCacheFormat.requiredString(serializedDependency, TYPE);
      result.put(type, deserializeInjectionPoints(JsonCacheFormat.requiredArray(serializedDependency, INJECTION_POINTS), inputFile));
    }
    return result;
  }

  private static Set<InjectionPoint> deserializeInjectionPoints(JsonArray injectionPoints, InputFile inputFile) {
    Set<InjectionPoint> result = new LinkedHashSet<>();
    for (JsonElement injectionPoint : injectionPoints) {
      JsonObject serializedInjectionPoint = JsonCacheFormat.requiredObject(injectionPoint);
      result.add(new InjectionPoint(
        JsonCacheFormat.requiredString(serializedInjectionPoint, NAME),
        new BeanLocation(inputFile, JsonCacheFormat.spanFromJson(JsonCacheFormat.requiredObject(serializedInjectionPoint, SPAN)))));
    }
    return result;
  }
}
