/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks.security;

import java.util.List;
import java.util.Map;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodReferenceTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S7435")
public class AndroidPersistentUniqueIdentifierCheck extends AbstractMethodDetection {

  private static final String HARDWARE_ID_MESSAGE = "Using a hardware identifier puts user privacy at risk. Make sure it is safe here.";
  private static final String PHONE_NUMBER_MESSAGE = "Using a phone number puts user privacy at risk. Make sure it is safe here.";
  private static final String ADVERTISING_ID_MESSAGE = "Using Advertising ID puts user privacy at risk. Make sure it is safe here.";
  private static final String NON_RESETTABLE_PERSISTENT_ID_MESSAGE = "Using a non-resettable persistent identifier puts user privacy at risk. Make sure it is safe here.";

  private static final MethodMatchers STATIC_SETTINGS_SECURE_GET_STRING_MATCHER =
    MethodMatchers.create()
      .ofTypes("android.provider.Settings$Secure")
      .names("getString")
      .addParametersMatcher("android.content.ContentResolver", "java.lang.String")
      .build();

  private static final Map<MethodMatchers, String> MATCHERS = Map.of(
    MethodMatchers.create()
      .ofTypes("android.bluetooth.BluetoothAdapter")
      .names("getAddress")
      .withAnyParameters()
      .build(),
    HARDWARE_ID_MESSAGE,
    MethodMatchers.create()
      .ofSubTypes("android.net.wifi.WifiInfo")
      .names("getMacAddress")
      .withAnyParameters()
      .build(),
    HARDWARE_ID_MESSAGE,
    MethodMatchers.create()
      .ofSubTypes("android.telephony.TelephonyManager")
      .names("getSimSerialNumber", "getDeviceId", "getImei", "getMeid")
      .withAnyParameters()
      .build(),
    HARDWARE_ID_MESSAGE,
    MethodMatchers.create()
      .ofSubTypes("android.telephony.TelephonyManager")
      .names("getLine1Number")
      .withAnyParameters()
      .build(),
    PHONE_NUMBER_MESSAGE,
    MethodMatchers.create()
      .ofSubTypes("android.telephony.SubscriptionManager")
      .names("getPhoneNumber")
      .withAnyParameters()
      .build(),
    PHONE_NUMBER_MESSAGE,
    MethodMatchers.create()
      .ofSubTypes(
        "com.google.android.gms.ads.identifier.AdvertisingIdClient$Info",
        "androidx.ads.identifier.AdvertisingIdInfo",
        "com.huawei.hms.ads.identifier.AdvertisingIdClient$Info"
      )
      .names("getId")
      .withAnyParameters()
      .build(),
    ADVERTISING_ID_MESSAGE,
    STATIC_SETTINGS_SECURE_GET_STRING_MATCHER,
    NON_RESETTABLE_PERSISTENT_ID_MESSAGE
  );

  private static final MethodMatchers ALL_MATCHER = MethodMatchers.or(MATCHERS.keySet().toArray(MethodMatchers[]::new));

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.METHOD_INVOCATION, Tree.Kind.METHOD_REFERENCE);
  }

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return ALL_MATCHER;
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    MATCHERS.keySet().stream()
      .filter(matcher -> isInvocationCandidate(matcher, mit))
      .findFirst()
      .ifPresent(matcher -> {
        var tree = mit.methodSelect() instanceof MemberSelectExpressionTree mset ? mset.identifier() : mit.methodSelect();
        var message = MATCHERS.get(matcher);
        reportIssue(tree, message);
      });
  }

  @Override
  protected void onMethodReferenceFound(MethodReferenceTree methodReferenceTree) {
    MATCHERS.keySet().stream()
      .filter(matcher -> isReferenceCandidate(matcher, methodReferenceTree))
      .findFirst()
      .ifPresent(matcher -> reportIssue(methodReferenceTree.method(), MATCHERS.get(matcher)));
  }

  private static boolean isInvocationCandidate(MethodMatchers matcher, MethodInvocationTree mit) {
    return matcher.matches(mit) &&
      (matcher != STATIC_SETTINGS_SECURE_GET_STRING_MATCHER || hasAndroidIdArgument(mit));
  }

  private static boolean isReferenceCandidate(MethodMatchers matcher, MethodReferenceTree methodReferenceTree) {
    return matcher.matches(methodReferenceTree) && matcher != STATIC_SETTINGS_SECURE_GET_STRING_MATCHER;
  }

  private static boolean hasAndroidIdArgument(MethodInvocationTree mit) {
    return mit.arguments().get(1).asConstant().map("android_id"::equals).orElse(false);
  }
}
