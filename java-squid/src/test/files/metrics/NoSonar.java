class A {


  @Override
  public boolean equals(Object object) {
    if (object instanceof SquidMetricFromSonarMetric) {
      SquidMetricFromSonarMetric otherMetric = (SquidMetricFromSonarMetric) object;
      return otherMetric.sonarMetric.getKey().equals(sonarMetric); // NOSONAR This is intentional to compare a SquidMetricFromSonarMetric
      // class and a MetricDef class
    }
    return false;
  }
}