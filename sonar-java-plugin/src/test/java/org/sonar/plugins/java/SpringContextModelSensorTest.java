package org.sonar.plugins.java;

import org.junit.jupiter.api.Test;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.java.model.springcontext.SpringContextModel;

import static org.assertj.core.api.Assertions.assertThat;

class SpringContextModelSensorTest {

  @Test
  void test_toString() {
    DefaultSensorDescriptor descriptor = new DefaultSensorDescriptor();
    SpringContextModelSensor sensor = new SpringContextModelSensor(new SpringContextModel());
    sensor.describe(descriptor);
    assertThat(descriptor.name()).isEqualTo("Java SpringContextModelSensor.");
    assertThat(descriptor.languages()).containsExactly("java", "jsp");
  }

}
