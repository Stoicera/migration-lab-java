package at.werkstatt.crm;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

// The modern stand's security.txt (RFC 9116) travels a long way before a browser sees it:
// modern/frontend/public/.well-known/ -> Angular asset copy (glob with dot: true) ->
// frontend/dist/.../browser -> maven-resources-plugin -> classpath:/static/ -> Spring Boot's
// default resource handler at /.well-known/security.txt. Every hop is a place where a
// dot-directory can be dropped silently (an added asset ignore, a default-excludes change, a
// resource-handler customisation). This test pins the one fact that proves all hops worked: the
// file is on the classpath exactly where Boot serves it from, and it is the real file, not the
// SPA shell.
//
// Deliberately a plain JUnit test, not a @SpringBootTest: the question is "did the build ship
// the file", which needs no context, no database and no container. The live half - "does the
// edge leave it public" - is asserted by deploy/verify-live.sh, where it belongs.
class SecurityTxtTest {

  private static final String RESOURCE = "/static/.well-known/security.txt";
  private static final String CANONICAL =
      "Canonical: https://migration-lab.stoicera.cyou/.well-known/security.txt";

  @Test
  void security_txt_liegt_im_classpath_und_ist_die_echte_datei() throws IOException {
    try (InputStream in = SecurityTxtTest.class.getResourceAsStream(RESOURCE)) {
      assertNotNull(in, RESOURCE + " missing: the frontend asset copy dropped .well-known");
      String body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
      assertTrue(body.contains("\nContact: "), "no Contact field: not the real security.txt");
      assertTrue(body.contains("\nExpires: "), "no Expires field: RFC 9116 requires one");
      assertTrue(body.contains(CANONICAL), "Canonical must name the modern stand's own URL");
    }
  }
}
