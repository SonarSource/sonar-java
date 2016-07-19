import java.time.Clock;
import java.time.LocalDateTime;
import java.time.chrono.HijrahDate;
import java.util.Optional;
import java.util.List;
import java.util.Map;

class A  {

  // value-based: false
  String attr0;

  // value-based: true
  Optional<String> attr1;

  // value-based: true
  HijrahDate attr2;

  // value-based: false
  String[] attr3;
  
  // value-based: true
  LocalDateTime[][] attr4;
  
  // value-based: false
  List<String> attr5;
  
  // value-based: false
  Map<String, LocalDateTime> attr6;
  
  // value-based: false
  Clock attr7;
  
}
