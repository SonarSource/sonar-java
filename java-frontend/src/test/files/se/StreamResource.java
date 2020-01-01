package org.sonar.trial;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

// All are compliant because Stream operations
public class StreamResource {

  private static String useList(List<String> words) {
    return Arrays.asList(words).stream().map(word -> "\"" + word + "\"").collect(Collectors.joining(" and "));
  }

  private static String useArray(String[] words) {
    return Arrays.stream(words).map(word -> "\"" + word + "\"").collect(Collectors.joining(" and "));
  }

  private static int useArray(int[] numbers) {
    int sum = 0;
    Arrays.stream(numbers).map(word -> sum + word);
    return sum;
  }

  private static long useArray(long[] numbers) {
    long sum = 0;
    Arrays.stream(numbers).map(word -> sum + word);
    return sum;
  }

  private static double useArray(double[] numbers) {
    double sum = 0;
    Arrays.stream(numbers).map(word -> sum + word);
    return sum;
  }

  private static void constantStream(String[] words) {
    Stream<String> str = Stream.of(words);
    str.count();
  }
}
