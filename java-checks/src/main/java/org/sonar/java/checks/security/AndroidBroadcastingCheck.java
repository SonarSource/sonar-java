/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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
package org.sonar.java.checks.security;

import java.util.Arrays;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.MethodMatcherCollection;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;

import static org.sonar.plugins.java.api.tree.Tree.Kind.NULL_LITERAL;

@Rule(key = "S5320")
public class AndroidBroadcastingCheck extends AbstractMethodDetection {

  private static final String MESSAGE = "Make sure that broadcasting intents is safe here.";

  private static final MethodMatcher SEND_BROADCAST = androidContext().name("sendBroadcast").withAnyParameters();
  private static final MethodMatcher SEND_BROADCAST_AS_USER = androidContext().name("sendBroadcastAsUser").withAnyParameters();
  private static final MethodMatcher SEND_ORDERED_BROADCAST = androidContext().name("sendOrderedBroadcast").withAnyParameters();
  private static final MethodMatcher SEND_ORDERED_BROADCAST_AS_USER = androidContext().name("sendOrderedBroadcastAsUser").withAnyParameters();
  private static final MethodMatcher SEND_STICKY_BROADCAST = androidContext().name("sendStickyBroadcast").withAnyParameters();
  private static final MethodMatcher SEND_STICKY_BROADCAST_AS_USER = androidContext().name("sendStickyBroadcastAsUser").withAnyParameters();
  private static final MethodMatcher SEND_STICKY_ORDERED_BROADCAST = androidContext().name("sendStickyOrderedBroadcast").withAnyParameters();
  private static final MethodMatcher SEND_STICKY_ORDERED_BROADCAST_AS_USER = androidContext().name("sendStickyOrderedBroadcastAsUser").withAnyParameters();
  private static final MethodMatcherCollection STICKY_BROADCAST = MethodMatcherCollection.create(SEND_STICKY_BROADCAST,
    SEND_STICKY_BROADCAST_AS_USER, SEND_STICKY_ORDERED_BROADCAST, SEND_STICKY_ORDERED_BROADCAST_AS_USER);

  private static MethodMatcher androidContext() {
    return MethodMatcher.create().typeDefinition("android.content.Context");
  }

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return Arrays.asList(
      SEND_BROADCAST,
      SEND_BROADCAST_AS_USER,
      SEND_ORDERED_BROADCAST,
      SEND_ORDERED_BROADCAST_AS_USER,
      SEND_STICKY_BROADCAST,
      SEND_STICKY_BROADCAST_AS_USER,
      SEND_STICKY_ORDERED_BROADCAST,
      SEND_STICKY_ORDERED_BROADCAST_AS_USER
    );
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    if (SEND_BROADCAST.matches(mit) && (mit.arguments().size() < 2 || mit.arguments().get(1).is(NULL_LITERAL))) {
      reportIssue(mit.methodSelect(), MESSAGE);
    } else if (SEND_BROADCAST_AS_USER.matches(mit) && (mit.arguments().size() < 3 || mit.arguments().get(2).is(NULL_LITERAL))) {
      reportIssue(mit.methodSelect(), MESSAGE);
    } else if (SEND_ORDERED_BROADCAST.matches(mit) && mit.arguments().size() > 1 && mit.arguments().get(1).is(NULL_LITERAL)) {
      reportIssue(mit.methodSelect(), MESSAGE);
    } else if (SEND_ORDERED_BROADCAST_AS_USER.matches(mit) && mit.arguments().size() > 2 && mit.arguments().get(2).is(NULL_LITERAL)) {
      reportIssue(mit.methodSelect(), MESSAGE);
    } else if (STICKY_BROADCAST.anyMatch(mit)) {
      reportIssue(mit.methodSelect(), MESSAGE);
    }
  }
}
