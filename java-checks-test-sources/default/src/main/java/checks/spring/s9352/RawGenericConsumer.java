package checks.spring.s9352;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

interface Shelf<T> {
}

class Book {
}

class Disc {
}

@Component
class BookShelf implements Shelf<Book> {
}

@Component
class DiscShelf implements Shelf<Disc> {
}

// A raw injection point carries no type argument to resolve, so Spring matches both beans and fails. The
// issue must still be reported, as it is today.
@Service
public class RawGenericConsumer {

  @Autowired
  private Shelf shelf;
}
