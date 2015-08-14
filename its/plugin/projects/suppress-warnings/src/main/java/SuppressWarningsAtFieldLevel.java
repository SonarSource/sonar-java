public class SuppressWarningsAtFieldLevel {
  @SuppressWarnings("all")
  int var[]; // Squid:S1197
  
  void method(@SuppressWarnings("all") int var[]) {
    
    @SuppressWarnings("all")
    int bar[];
  }
}
