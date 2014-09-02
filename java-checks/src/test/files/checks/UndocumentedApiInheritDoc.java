/**
 * Documented
 */
public class MyClass implements MyInterface {
   
   /**
    * {@inheritDoc}
    */
   public String javadocInheritDoc(String s){
       return s;
   }
   
   public void fault(){
       // To verify that the last javadoc error is this one ... and not the inheritDoc below (when inheritDoc is supported)
   }
   
}