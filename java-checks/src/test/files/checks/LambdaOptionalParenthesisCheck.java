class A{
  void method(){
    IntStream.range(1,5).map(a->a+1)
                        .map((a)->a+1) // Noncompliant [[sc=30;ec=31]] {{Remove the parentheses around the "a" parameter}}
                        .map((int x)->x+1)
    ;
    Collectors.groupingBy((Map<String, Object> page) -> Site.category(page), TreeMap::new, toList());
  }
}
