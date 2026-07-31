package at.werkstatt.crm.gen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import at.werkstatt.crm.controller.RechnungController;
import at.werkstatt.crm.model.Rechnung;
import at.werkstatt.crm.service.WerkstattService;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class RechnungControllerGeneratedTest {

  @Mock
  private WerkstattService werkstattService;

  private RechnungController controller;

  @BeforeEach
  void setUp() {
    controller = new RechnungController(werkstattService);
  }

  @Test
  void liste_gibtAlleRechnungenZurueck() {
    Rechnung r1 = new Rechnung();
    r1.setId(1L);
    Rechnung r2 = new Rechnung();
    r2.setId(2L);
    List<Rechnung> rechnungen = List.of(r1, r2);
    when(werkstattService.getAlleRechnungen()).thenReturn(rechnungen);

    List<Rechnung> result = controller.liste();

    assertThat(result).isEqualTo(rechnungen);
    verify(werkstattService).getAlleRechnungen();
  }

  @Test
  void liste_gibtLeereListeZurueck() {
    when(werkstattService.getAlleRechnungen()).thenReturn(Collections.emptyList());

    List<Rechnung> result = controller.liste();

    assertThat(result).isEmpty();
  }

  @Test
  void einzeln_gefunden_gibt200UndRechnungZurueck() {
    Rechnung rechnung = new Rechnung();
    rechnung.setId(5L);
    when(werkstattService.getRechnung(5L)).thenReturn(rechnung);

    ResponseEntity<Rechnung> response = controller.einzeln(5L);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo(rechnung);
  }

  @Test
  void einzeln_nichtGefunden_gibt404Zurueck() {
    when(werkstattService.getRechnung(99L)).thenReturn(null);

    ResponseEntity<Rechnung> response = controller.einzeln(99L);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody()).isNull();
  }

  @Test
  void erstellen_erfolgreich_gibt200UndRechnungZurueck() {
    Rechnung rechnung = new Rechnung();
    rechnung.setId(10L);
    rechnung.setAuftragId(3L);
    when(werkstattService.erstelleRechnung(3L)).thenReturn(rechnung);

    ResponseEntity<?> response = controller.erstellen(3L);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo(rechnung);
  }

  @Test
  void erstellen_wirftException_gibt500UndFehlermeldungZurueck() {
    when(werkstattService.erstelleRechnung(anyLong()))
        .thenThrow(new IllegalStateException("Auftrag nicht gefunden"));

    ResponseEntity<?> response = controller.erstellen(42L);

    assertThat(response.getStatusCode().value()).isEqualTo(500);
    assertThat(response.getBody()).isEqualTo("Auftrag nicht gefunden");
  }

  @Test
  void erstellen_wirftExceptionMitNullMessage_gibt500UndNullBodyZurueck() {
    when(werkstattService.erstelleRechnung(anyLong()))
        .thenThrow(new RuntimeException());

    ResponseEntity<?> response = controller.erstellen(1L);

    assertThat(response.getStatusCode().value()).isEqualTo(500);
    assertThat(response.getBody()).isNull();
  }

  @Test
  void bezahlt_erfolgreich_gibt200UndRechnungZurueck() {
    Rechnung rechnung = new Rechnung();
    rechnung.setId(7L);
    rechnung.setBezahlt(true);
    when(werkstattService.setzeBezahlt(7L)).thenReturn(rechnung);

    ResponseEntity<?> response = controller.bezahlt(7L);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo(rechnung);
  }

  @Test
  void bezahlt_wirftException_gibt500UndFehlermeldungZurueck() {
    when(werkstattService.setzeBezahlt(anyLong()))
        .thenThrow(new IllegalArgumentException("Rechnung nicht gefunden"));

    ResponseEntity<?> response = controller.bezahlt(123L);

    assertThat(response.getStatusCode().value()).isEqualTo(500);
    assertThat(response.getBody()).isEqualTo("Rechnung nicht gefunden");
  }

  @Test
  void bezahlt_ruftServiceMitKorrekterIdAuf() {
    Rechnung rechnung = new Rechnung();
    when(werkstattService.setzeBezahlt(eq(55L))).thenReturn(rechnung);

    controller.bezahlt(55L);

    verify(werkstattService).setzeBezahlt(55L);
  }
}
