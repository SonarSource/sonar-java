public class SuppressWarningsAtFieldLevelWithRule {
  @SuppressWarnings({"java:S1197","java-extension:example"})
  int var[]; //java:S1197
  
  void method(@SuppressWarnings("java:S1197") int var[]) {
    
    @SuppressWarnings("java:S1197")
    int bar[];
  }
}
