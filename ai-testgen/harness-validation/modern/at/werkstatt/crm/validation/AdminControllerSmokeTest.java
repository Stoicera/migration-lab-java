package at.werkstatt.crm.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import at.werkstatt.crm.controller.AdminController;
import at.werkstatt.crm.service.WerkstattService;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Harness validation, stratum shape S3 on the migrated counterpart (corpus B).
 *
 * <p>Honesty note carried into the report: this stratum is the one place where the two corpora are not
 * shape-identical. Stage 5 absorbed the JSP admin page (SD-2, ADR-0004), so corpus A tests a
 * JSP+gson @Controller and corpus B tests a JSON @RestController. That is a migration effect, not a
 * measurement error — but it means S3 numbers compare the corpora only with that caveat attached.
 */
@ExtendWith(MockitoExtension.class)
class AdminControllerSmokeTest {

  @Mock private WerkstattService werkstattService;

  @Test
  void statistik_liefert_firma_version_und_kennzahlen() {
    AdminController controller = new AdminController(werkstattService, "KFZ Moser GmbH", "1.4.2");
    Map<String, Object> statistik = Collections.<String, Object>singletonMap("kunden", 10);
    when(werkstattService.getAdminStatistik()).thenReturn(statistik);

    Map<String, Object> antwort = controller.statistik();

    assertThat(antwort)
        .containsEntry("firmaName", "KFZ Moser GmbH")
        .containsEntry("version", "1.4.2")
        .containsEntry("statistik", statistik);
  }
}
