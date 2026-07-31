package at.werkstatt.crm.gen;

import at.werkstatt.crm.controller.KundenController;
import at.werkstatt.crm.model.Fahrzeug;
import at.werkstatt.crm.model.Kunde;
import at.werkstatt.crm.service.WerkstattService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class KundenControllerGeneratedTest {

  @Mock
  private WerkstattService werkstattService;

  private KundenController controller;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    controller = new KundenController(werkstattService);
  }

  // --- @GetMapping (list) tests ---
  @Nested
  @DisplayName("GET /api/kunden - list")
  class ListTests {

    @Test
    @DisplayName("returns all customers when no search parameter is provided")
    void returnsAllKundenWhenNoSucheParam() {
      // Given
      List<Kunde> expected = createKundenList();
      when(werkstattService.getAlleKunden()).thenReturn(expected);

      // When
      List<Kunde> result = controller.liste(null);

      // Then
      verify(werkstattService, never()).sucheKunden(any());
      verify(werkstattService).getAlleKunden();
      assertThat(result).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("returns all customers when suche is null or empty")
    void returnsAllKundenWhenSucheIsEmpty(String suche) {
      // Given
      List<Kunde> expected = createKundenList();
      when(werkstattService.getAlleKunden()).thenReturn(expected);

      // When
      List<Kunde> result = controller.liste(suche);

      // Then
      verify(werkstattService, never()).sucheKunden(any());
      verify(werkstattService).getAlleKunden();
      assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("returns searched customers when suche is provided and non-empty")
    void returnsSearchedKundenWhenSucheProvided() {
      // Given
      String suche = "max";
      List<Kunde> expected = Collections.singletonList(createKunde(1L));
      when(werkstattService.sucheKunden("max")).thenReturn(expected);

      // When
      List<Kunde> result = controller.liste(suche);

      // Then
      verify(werkstattService).sucheKunden("max");
      verify(werkstattService, never()).getAlleKunden();
      assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("trims leading/trailing whitespace from suche parameter")
    void trimsSucheParameter() {
      // Given
      String suche = "  max  ";
      List<Kunde> expected = Collections.singletonList(createKunde(1L));
      when(werkstattService.sucheKunden("max")).thenReturn(expected);

      // When
      controller.liste(suche);

      // Then
      verify(werkstattService).sucheKunden("max");
    }
  }

  // --- @GetMapping("/{id}") tests ---
  @Nested
  @DisplayName("GET /api/kunden/{id} - single")
  class SingleTests {

    @Test
    @DisplayName("returns 200 OK with customer when found")
    void returnsKundeWhenFound() {
      // Given
      long id = 1L;
      Kunde expected = createKunde(id);
      when(werkstattService.getKunde(id)).thenReturn(expected);

      // When
      ResponseEntity<Kunde> response = controller.einzeln(id);

      // Then
      assertThat(response.getStatusCodeValue()).isEqualTo(200);
      assertThat(response.getBody()).isEqualTo(expected);
    }

    @Test
    @DisplayName("returns 404 Not Found when customer not found")
    void returnsNotFoundWhenKundeIsNull() {
      // Given
      long id = 2L;
      when(werkstattService.getKunde(id)).thenReturn(null);

      // When
      ResponseEntity<Kunde> response = controller.einzeln(id);

      // Then
      assertThat(response.getStatusCodeValue()).isEqualTo(404);
      assertThat(response.getBody()).isNull();
    }
  }

  // --- @GetMapping("/{id}/fahrzeuge") tests ---
  @Nested
  @DisplayName("GET /api/kunden/{id}/fahrzeuge - vehicles")
  class VehiclesTests {

    @Test
    @DisplayName("returns list of vehicles for given customer ID")
    void returnsVehiclesForKunde() {
      // Given
      long id = 1L;
      List<Fahrzeug> expected = Collections.singletonList(createFahrzeug(10L, id));
      when(werkstattService.getFahrzeugeZuKunde(id)).thenReturn(expected);

      // When
      List<Fahrzeug> result = controller.fahrzeuge(id);

      // Then
      verify(werkstattService).getFahrzeugeZuKunde(id);
      assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("returns empty list when no vehicles exist")
    void returnsEmptyListWhenNoVehicles() {
      // Given
      long id = 1L;
      when(werkstattService.getFahrzeugeZuKunde(id)).thenReturn(Collections.emptyList());

      // When
      List<Fahrzeug> result = controller.fahrzeuge(id);

      // Then
      verify(werkstattService).getFahrzeugeZuKunde(id);
      assertThat(result).isEmpty();
    }
  }

  // --- @PostMapping tests ---
  @Nested
  @DisplayName("POST /api/kunden - create")
  class CreateTests {

    @Test
    @DisplayName("sets ID to null and creates new customer")
    void createsNewKunde() {
      // Given
      Kunde kunde = createKunde(100L);
      kunde.setId(100L); // will be reset
      Kunde created = createKunde(1L);
      when(werkstattService.speichereKunde(any())).thenReturn(created);

      // When
      Kunde result = controller.anlegen(kunde);

      // Then
      assertThat(result).isEqualTo(created);
      verify(werkstattService).speichereKunde(any(kundeClass())); // verify object passed
      assertThat(kunde.getId()).isNull(); // Ensure passed-in ID was cleared
      // Verify that ID was set to null inside speichereKunde call
      ArgumentCaptor<Kunde> captor = ArgumentCaptor.forClass(Kunde.class);
      verify(werkstattService).speichereKunde(captor.capture());
      assertThat(captor.getValue().getId()).isNull();
    }

    private Class<Kunde> kundeClass() {
      return Kunde.class;
    }
  }

  // --- @PutMapping("/{id}") tests ---
  @Nested
  @DisplayName("PUT /api/kunden/{id} - update")
  class UpdateTests {

    @Test
    @DisplayName("returns updated customer when update succeeds")
    void returnsUpdatedKundeOnSuccess() {
      // Given
      long id = 1L;
      Kunde updatedKunde = createKunde(id);
      Kunde saved = createKunde(id);
      when(werkstattService.speichereKunde(any())).thenReturn(saved);

      // When
      ResponseEntity<?> response = controller.aendern(id, updatedKunde);

      // Then
      assertThat(response.getStatusCodeValue()).isEqualTo(200);
      assertThat(response.getBody()).isEqualTo(saved);
      assertThat(updatedKunde.getId()).isEqualTo(id);
    }

    @Test
    @DisplayName("returns 500 with exception message on failure")
    void returnsInternalServerErrorOnException() {
      // Given
      long id = 1L;
      Kunde kunde = createKunde(id);
      String errorMsg = "DB constraint violation";
      when(werkstattService.speichereKunde(any())).thenThrow(new RuntimeException(errorMsg));

      // When
      ResponseEntity<?> response = controller.aendern(id, kunde);

      // Then
      assertThat(response.getStatusCodeValue()).isEqualTo(500);
      assertThat(response.getBody()).isEqualTo(errorMsg);
    }

    @Test
    @DisplayName("sets customer ID before saving")
    void setsIdBeforeSaving() {
      // Given
      long id = 23L;
      Kunde kunde = createKunde(99L); // wrong ID
      Kunde saved = createKunde(id);
      when(werkstattService.speichereKunde(any())).thenReturn(saved);

      // When
      controller.aendern(id, kunde);

      // Then
      ArgumentCaptor<Kunde> captor = ArgumentCaptor.forClass(Kunde.class);
      verify(werkstattService).speichereKunde(captor.capture());
      assertThat(captor.getValue().getId()).isEqualTo(id);
    }
  }

  // --- @DeleteMapping("/{id}") tests ---
  @Nested
  @DisplayName("DELETE /api/kunden/{id} - delete")
  class DeleteTests {

    @Test
    @DisplayName("returns 200 OK when deletion succeeds")
    void returnsOkOnSuccess() {
      // Given
      long id = 1L;

      // When
      ResponseEntity<?> response = controller.loeschen(id);

      // Then
      assertThat(response.getStatusCodeValue()).isEqualTo(200);
      assertThat(response.getBody()).isNull();
      verify(werkstattService).loescheKunde(id);
    }

    @Test
    @DisplayName("returns 500 with exception message on failure")
    void returnsInternalServerErrorOnException() {
      // Given
      long id = 1L;
      String errorMsg = " foreign key constraint";
      doThrow(new RuntimeException(errorMsg)).when(werkstattService).loescheKunde(id);

      // When
      ResponseEntity<?> response = controller.loeschen(id);

      // Then
      assertThat(response.getStatusCodeValue()).isEqualTo(500);
      assertThat(response.getBody()).isEqualTo(errorMsg);
      verify(werkstattService).loescheKunde(id);
    }
  }

  // --- Helper methods ---

  private List<Kunde> createKundenList() {
    return Arrays.asList(
        createKunde(1L),
        createKunde(2L)
    );
  }

  private Kunde createKunde(Long id) {
    Kunde kunde = new Kunde();
    kunde.setId(id);
    kunde.setVorname("Max");
    kunde.setNachname("Mustermann");
    kunde.setAnrede("Herr");
    return kunde;
  }

  private Fahrzeug createFahrzeug(Long id, Long kundeId) {
    Fahrzeug fahrzeug = new Fahrzeug();
    fahrzeug.setId(id);
    fahrzeug.setKundeId(kundeId);
    fahrzeug.setKennzeichen("Wien AB 123");
    fahrzeug.setMarke("VW");
    fahrzeug.setModell("Golf");
    fahrzeug.setFahrgestellnr("12345678901234567");
    fahrzeug.setBaujahr(2020);
    fahrzeug.setKmStand(15000);
    fahrzeug.setPickerlDatum(new Date());
    fahrzeug.setAngelegtAm(new Date());
    return fahrzeug;
  }
}
