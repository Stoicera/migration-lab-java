package at.werkstatt.crm.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import at.werkstatt.crm.controller.KundenController;
import at.werkstatt.crm.model.Kunde;
import at.werkstatt.crm.service.WerkstattService;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

/** Harness validation, stratum shape S2 (REST controller). See WerkstattServiceSmokeTest. */
@ExtendWith(MockitoExtension.class)
class KundenControllerSmokeTest {

  @Mock private WerkstattService werkstattService;

  @InjectMocks private KundenController controller;

  @Test
  void liste_ohne_suchbegriff_holt_alle_kunden() {
    when(werkstattService.getAlleKunden()).thenReturn(Collections.<Kunde>emptyList());

    List<Kunde> kunden = controller.liste(null);

    assertThat(kunden).isEmpty();
    verify(werkstattService).getAlleKunden();
  }

  @Test
  void einzeln_liefert_404_wenn_der_service_null_meldet() {
    when(werkstattService.getKunde(99L)).thenReturn(null);

    ResponseEntity<Kunde> antwort = controller.einzeln(99L);

    assertThat(antwort.getStatusCode().value()).isEqualTo(404);
  }
}
