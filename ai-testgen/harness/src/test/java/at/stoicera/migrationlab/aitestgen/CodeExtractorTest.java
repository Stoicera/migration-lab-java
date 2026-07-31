package at.stoicera.migrationlab.aitestgen;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * The extraction rule decides what counts as "generated output", so it decides the compile rate. It
 * is therefore mechanical, pinned here, and never adjusted after seeing a response.
 */
class CodeExtractorTest {

  @Test
  void nimmt_den_ersten_java_block() {
    String response =
        """
        Sure, here are the tests:

        ```java
        class A {}
        ```

        And an alternative:

        ```java
        class B {}
        ```
        """;

    assertThat(CodeExtractor.firstJavaBlock(response)).contains("class A {}\n");
  }

  @Test
  void ignoriert_bloecke_ohne_java_infostring() {
    String response =
        """
        ```
        class NotClaimedAsJava {}
        ```
        """;

    assertThat(CodeExtractor.firstJavaBlock(response)).isEmpty();
  }

  @Test
  void ein_nicht_geschlossener_block_ist_eine_extraktions_niederlage() {
    String response = "```java\nclass Truncated {\n";

    assertThat(CodeExtractor.firstJavaBlock(response)).isEmpty();
  }

  @Test
  void haelt_verschachtelte_backticks_im_javadoc_aus() {
    String response =
        """
        ````java
        /** uses ``` inside a comment */
        class C {}
        ````
        """;

    assertThat(CodeExtractor.firstJavaBlock(response)).get().asString().contains("class C {}");
  }

  @Test
  void keine_antwort_und_leerer_block_zaehlen_als_niederlage() {
    assertThat(CodeExtractor.firstJavaBlock(null)).isEmpty();
    assertThat(CodeExtractor.firstJavaBlock("no code at all, sorry")).isEmpty();
    assertThat(CodeExtractor.firstJavaBlock("```java\n\n```")).isEmpty();
  }
}
