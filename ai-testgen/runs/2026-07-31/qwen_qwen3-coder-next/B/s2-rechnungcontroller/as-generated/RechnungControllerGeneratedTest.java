package at.werkstatt.crm.gen;

import at.werkstatt.crm.controller.RechnungController;
import at.werkstatt.crm.model.Rechnung;
import at.werkstatt.crm.service.WerkstattService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class RechnungControllerGeneratedTest {

  private WerkstattService werkstattService;
  private RechnungController controller;

  @BeforeEach
  void setUp() {
    werkstattService = Mockito.mock(WerkstattService.class);
    controller = new RechnungController(werkstattService);
  }

  @Test
  void liste_returnsAllRechnungen() {
    // Arrange
    Rechnung r1 = new Rechnung();
    r1.setId(1L);
    Rechnung r2 = new Rechnung();
    r2.setId(2L);
    List<Rechnung> expected = Arrays.asList(r1, r2);
    when(werkstattService.getAlleRechnungen()).thenReturn(expected);

    // Act
    List<Rechnung> result = controller.liste();

    // Assert
    assertThat(result).isEqualTo(expected);
    verify(werkstattService).getAlleRechnungen();
  }

  @Test
  void einzeln_returnsRechnung_whenFound() {
    // Arrange
    Rechnung rechnung = new Rechnung();
    rechnung.setId(1L);
    when(werkstattService.getRechnung(1L)).thenReturn(rechnung);

    // Act
    ResponseEntity<Rechnung> response = controller.einzeln(1L);

    // Assert
    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(response.getBody()).isEqualTo(rechnung);
    verify(werkstattService).getRechnung(1L);
  }

  @Test
  void einzeln_returnsNotFound_whenRechnungIsNull() {
    // Arrange
    when(werkstattService.getRechnung(999L)).thenReturn(null);

    // Act
    ResponseEntity<Rechnung> response = controller.einzeln(999L);

    // Assert
    assertThat(response.getStatusCode().is4xxClientError()).isTrue();
    assertThat(response.getBody()).isNull();
    verify(werkstattService).getRechnung(999L);
  }

  @Test
  void erstellen_returnsCreatedRechnung_whenSuccessful() {
    // Arrange
    Rechnung created = new Rechnung();
    created.setId(10L);
    created.setRechnungNr("R-2025-001");
    when(werkstattService.erstelleRechnung(5L)).thenReturn(created);

    // Act
    ResponseEntity<?> response = controller.erstellen(5L);

    // Assert
    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(response.getBody()).isEqualTo(created);
    verify(werkstattService).erstelleRechnung(5L);
  }

  @Test
  void erstellen_returnsInternalServerError_whenExceptionThrown() {
    // Arrange
    String errorMsg = "Auftrag nicht gefunden";
    when(werkstattService.erstelleRechnung(999L)).thenThrow(new RuntimeException(errorMsg));

    // Act
    ResponseEntity<?> response = controller.erstellen(999L);

    // Assert
    assertThat(response.getStatusCode().is5xxServerError()).isTrue();
    assertThat(response.getBody()).isEqualTo(errorMsg);
    verify(werkstattService).erstelleRechnung(999L);
  }

  @Test
  void bezahlt_returnsUpdatedRechnung_whenSuccessful() {
    // Arrange
    Rechnung updated = new Rechnung();
    updated.setId(7L);
    updated.setBezahlt(true);
    updated.setBezahltAm(new Date());
    when(werkstattService.setzeBezahlt(7L)).thenReturn(updated);

    // Act
    ResponseEntity<?> response = controller.bezahlt(7L);

    // Assert
    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(response.getBody()).isEqualTo(updated);
    verify(werkstattService).setzeBezahlt(7L);
  }

  @Test
  void bezahlt_returnsInternalServerError_whenExceptionThrown() {
    // Arrange
    String errorMsg = "Rechnung nicht gefunden";
    when(werkstattService.setzeBezahlt(888L)).thenThrow(new RuntimeException(errorMsg));

    // Act
    ResponseEntity<?> response = controller.bezahlt(888L);

    // Assert
    assertThat(response.getStatusCode().is5xxServerError()).isTrue();
    assertThat(response.getBody()).isEqualTo(errorMsg);
    verify(werkstattService).setzeBezahlt(888L);
  }

  @Test
  void erstellen_handlesNullRechnungFromService() {
    // Arrange
    when(werkstattService.erstelleRechnung(100L)).thenReturn(null);

    // Act
    ResponseEntity<?> response = controller.erstellen(100L);

    // Assert
    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(response.getBody()).isNull();
    verify(werkstattService).erstelleRechnung(100L);
  }

  @Test
  void bezahlt_handlesNullRechnungFromService() {
    // Arrange
    when(werkstattService.setzeBezahlt(200L)).thenReturn(null);

    // Act
    ResponseEntity<?> response = controller.bezahlt(200L);

    // Assert
    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(response.getBody()).isNull();
    verify(werkstattService).setzeBezahlt(200L);
  }
}
