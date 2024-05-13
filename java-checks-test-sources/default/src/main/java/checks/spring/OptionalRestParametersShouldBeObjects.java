package checks.spring;

import com.mongodb.lang.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

class OptionalRestParametersShouldBeObjects {

  @GetMapping(value = {"/article", "/article/{id}"})
  public Article getArticle(@PathVariable(required = false) int articleId, int unused) { // Noncompliant {{Convert this optional parameter to an Object type.}}
//                          ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    return new Article(articleId);
  }

  @GetMapping(value = {"/article", "/article/{id}"})
  public Article getArticleWithRequestParam(@RequestParam(required = false) int articleId, int unused) { // Noncompliant {{Convert this optional parameter to an Object type.}}
//                                          ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    return new Article(articleId);
  }

  @GetMapping(value = {"/article", "/article/{id}"})
  public Article getArticle(
    @PathVariable(required = false) float articleId, // Noncompliant
//  ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    @RequestParam(required = false) int someIssue, // Noncompliant
//  ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    @PathVariable(required = false) boolean anotherIssue // Noncompliant
//  ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
  ) {
    return new Article((int) (Math.floor(articleId)));
  }

  @GetMapping(value = {"/article", "/article/{id}"})
  public Article getArticle(@PathVariable(required = false) Integer articleId) { // Compliant
    return new Article(articleId);
  }

  @GetMapping(value = {"/article/{id}"})
  public Article getArticleButImplictlyRequired(@PathVariable int articleId) { // Compliant
    return new Article(articleId);
  }

  @GetMapping(value = {"/article/{id}"})
  public Article getArticleRequestParamButImplictlyRequired(@RequestParam int articleId) { // Compliant
    return new Article(articleId);
  }

  @GetMapping(value = {"/article/{id}"})
  public Article getArticleButExplicitlyRequired(@PathVariable(required = true) int articleId) { // Compliant
    return new Article(articleId);
  }

  @GetMapping(value = {"/article/{id}"})
  public Article getArticleRequestParamButExplicitlyRequired(@RequestParam(required = true) int articleId) { // Compliant
    return new Article(articleId);
  }

  @GetMapping(value = {"/article/{id}"})
  public Article getArticleButSettingADifferentValue(@PathVariable(name = "articleId") int articleId) { // Compliant
    return new Article(articleId);
  }

  @GetMapping(value = "{/article/id")
  public Article getArticleRequestParamWithDefaultValue(@RequestParam(required = false, defaultValue = "1") int articleId) { // Compliant
    return new Article(articleId);
  }

  @GetMapping(value = {"/article/{id}"})
  public Article getArticleButDifferentlyAnnotated(@Nullable int articleId) { // Compliant
    return new Article(articleId);
  }

  @GetMapping(value = {"/article/{id}"})
  public Article getArticlePathVariableWithValue(@PathVariable("id") int articleId) { // Compliant
    return new Article(articleId);
  }

  @GetMapping(value = {"/article/{id}"})
  public Article getArticleRequestParamWithValue(@RequestParam("id") int articleId) { // Compliant
    return new Article(articleId);
  }

  record Article(int id) {
  }
}
