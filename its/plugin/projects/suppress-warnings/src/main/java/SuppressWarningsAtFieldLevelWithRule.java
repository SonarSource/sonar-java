public class SuppressWarningsAtFieldLevelWithRule {
  @SuppressWarnings({"squid:S1197","java-extension:example"})
  int var[]; //squid:S1197
  
  void method(@SuppressWarnings("squid:S1197") int var[]) {
    
    @SuppressWarnings("squid:S1197")
    int bar[];
  }
}
