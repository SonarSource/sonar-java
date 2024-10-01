package checks.jspecify;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.NullUnmarked;
import org.springframework.batch.item.ItemProcessor;

@NullMarked
class ReturnEmptyArrayNotNullCheckSampleA {

  public ReturnEmptyArrayNotNullCheckSampleA() {
    return;
  }

  public void f1() {     
    return;
  }

  public int[] f2() {
    return null; // Noncompliant {{Return an empty array instead of null.}}
//         ^^^^
  }

  public Object f3() {
    return null; // Compliant as not an array
  }

  public Object f4()[] {
    return null; // Noncompliant
  }

  public int[] f5(boolean cond) {
    new ReturnEmptyArrayNotNullCheckSampleB() {
      public Object g1() {
        return null;    
      }

      public int[] g2() {
        return null; // Noncompliant
      }
    };
    if (cond) {
      return new int[0];
    }
    return null; // Noncompliant
  }

  public List f6() {
    return null; // Noncompliant
  }

  public ArrayList f7() {
    return null; // Noncompliant
  }

  public Set<Integer> f8(boolean cond) {
    if (cond) {
      return Collections.EMPTY_SET;
    }
    return null; // Noncompliant {{Return an empty collection instead of null.}}
  }

  public <T> List<Integer>[] f9() {
    return null; // Noncompliant {{Return an empty array instead of null.}}
  }

  public java.util.Collection f10() {
    return null; // Noncompliant
  }
}

@NullMarked
interface ReturnEmptyArrayNotNullCheckSampleB{
  default int[] a(){
    return null; // Noncompliant
  }

  default int[] b(){
    return new int[4];
  }

  default List<String> c(){
    return null; // Noncompliant
  }

  default List<String> d(){
    return new ArrayList<String>();
  }

  default <T> int[] e(){
    return null; // Noncompliant
  }

  default <T> int[] f(){
    return new int[4];
  }
}

@NullMarked
class ReturnEmptyArrayNotNullCheckSampleC {
  @SuppressWarnings("Something")
  public int[] gul() {
    return null; // Noncompliant
  }

  @NullUnmarked
  public Object foo() {
    return null; 
  }
  
  @CheckForNull
  public Object bar() {
    return null; 
  }
  
  @NullUnmarked
  public int[] fool() {
    return null; 
  }
  
  @CheckForNull
  public int[] bark() {
    return null; 
  }

  int[] qix(){
    takeLambda(a -> {
      return null;
    });
    return new int[1];
  }

  static final Object CONSTANT = takeLambda(a->{return null;});

  private static Object takeLambda(Function<String, Object> o) {
    return o.apply("");
  }
}

@NullMarked
class ReturnEmptyArrayNotNullCheckSampleD implements ItemProcessor<Integer, List<String>> {
  @Override
  public List<String> process(Integer i) {
    return null; // Compliant: ItemProcessor requires to return null value to stop the processing
  }

  public List<String> process2(Integer i) {
    return null; // Noncompliant
  }
}

@NullMarked
interface ReturnEmptyArrayNotNullCheckSampleE {
  List<String> bar();
}

@NullMarked
class ReturnEmptyArrayNotNullCheckSampleF implements ReturnEmptyArrayNotNullCheckSampleE {
  @Override
  public List<String> bar() {
    return null; // Noncompliant
  }
}

@NullMarked
class ReturnEmptyArrayNotNullCheckSampleG implements ItemProcessor<Integer, List<String>>, ReturnEmptyArrayNotNullCheckSampleE {
  @Override
  public List<String> bar() {
    return null; // Noncompliant
  }

  @Override
  public List<String> process(Integer integer) throws Exception {
    return Collections.emptyList();
  }
}

@NullMarked
class ReturnEmptyArrayNotNullCheckSampleH implements ItemProcessor<Integer, Integer[]> {
  @Override
  public Integer[] process(Integer a) {
    return null; // Compliant
  }

  public int[] process(String a) {
    return null; // Noncompliant
  }
}
