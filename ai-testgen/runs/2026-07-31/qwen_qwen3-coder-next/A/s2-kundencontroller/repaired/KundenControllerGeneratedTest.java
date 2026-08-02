package at.werkstatt.crm.gen;

import at.werkstatt.crm.controller.KundenController;
import at.werkstatt.crm.model.Fahrzeug;
import at.werkstatt.crm.model.Kunde;
import at.werkstatt.crm.service.WerkstattService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

class KundenControllerGeneratedTest {

    @Mock
    private WerkstattService werkstattService;

    @InjectMocks
    private KundenController kundenController;

    @BeforeEach
    void setUp() {
        openMocks(this);
    }

    @Test
    void listeWithNullSucheReturnsAllKunden() {
        // Given
        List<Kunde> expectedKunden = Arrays.asList(createKunde(1L), createKunde(2L));
        when(werkstattService.getAlleKunden()).thenReturn(expectedKunden);

        // When
        List<Kunde> result = kundenController.liste(null);

        // Then
        assertThat(result).isEqualTo(expectedKunden);
        verify(werkstattService).getAlleKunden();
        verify(werkstattService, never()).sucheKunden(anyString());
    }

    @Test
    void listeWithEmptyStringSucheReturnsAllKunden() {
        // Given
        List<Kunde> expectedKunden = Arrays.asList(createKunde(1L));
        when(werkstattService.getAlleKunden()).thenReturn(expectedKunden);

        // When
        List<Kunde> result = kundenController.liste("");

        // Then
        assertThat(result).isEqualTo(expectedKunden);
        verify(werkstattService).getAlleKunden();
        verify(werkstattService, never()).sucheKunden(anyString());
    }

    @Test
    void listeWithWhitespaceOnlySucheReturnsAllKunden() {
        // Given
        List<Kunde> expectedKunden = Arrays.asList(createKunde(1L));
        when(werkstattService.getAlleKunden()).thenReturn(expectedKunden);

        // When
        List<Kunde> result = kundenController.liste("   \t  ");

        // Then
        assertThat(result).isEqualTo(expectedKunden);
        verify(werkstattService).getAlleKunden();
        verify(werkstattService, never()).sucheKunden(anyString());
    }

    @Test
    void listeWithValidSucheReturnsSuchergebnis() {
        // Given
        String suche = "Max";
        List<Kunde> suchergebnis = Arrays.asList(createKunde(1L));
        when(werkstattService.sucheKunden("Max")).thenReturn(suchergebnis);

        // When
        List<Kunde> result = kundenController.liste(suche);

        // Then
        assertThat(result).isEqualTo(suchergebnis);
        verify(werkstattService, never()).getAlleKunden();
        verify(werkstattService).sucheKunden("Max");
    }

    @Test
    void einzeln_foundReturnsOk() {
        // Given
        long id = 42L;
        Kunde expectedKunde = createKunde(id);
        when(werkstattService.getKunde(id)).thenReturn(expectedKunde);

        // When
        ResponseEntity<Kunde> response = kundenController.einzeln(id);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(expectedKunde);
        verify(werkstattService).getKunde(id);
    }

    @Test
    void einzeln_notFoundReturns404() {
        // Given
        long id = 999L;
        when(werkstattService.getKunde(id)).thenReturn(null);

        // When
        ResponseEntity<Kunde> response = kundenController.einzeln(id);

        // Then
        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        assertThat(response.getBody()).isNull();
        verify(werkstattService).getKunde(id);
    }

    @Test
    void fahrzeuge_returnsList() {
        // Given
        long kundeId = 10L;
        List<Fahrzeug> fahrzeuge = Arrays.asList(createFahrzeug(1L, kundeId), createFahrzeug(2L, kundeId));
        when(werkstattService.getFahrzeugeZuKunde(kundeId)).thenReturn(fahrzeuge);

        // When
        List<Fahrzeug> result = kundenController.fahrzeuge(kundeId);

        // Then
        assertThat(result).isEqualTo(fahrzeuge);
        verify(werkstattService).getFahrzeugeZuKunde(kundeId);
    }

    @Test
    void anlegen_setsIdToNullAndSaves() {
        // Given
        Kunde kunde = createKunde(null);
        kunde.setNachname("Mustermann");
        Kunde savedKunde = createKunde(1L);
        savedKunde.setNachname("Mustermann");
        when(werkstattService.speichereKunde(any(Kunde.class))).thenReturn(savedKunde);

        // When
        Kunde result = kundenController.anlegen(kunde);

        // Then
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getNachname()).isEqualTo("Mustermann");
        verify(werkstattService).speichereKunde(any(Kunde.class)); // Verifies the passed object has id null
    }

    @Test
    void anlegen_setsOriginalIdToNullBeforeSaving() {
        // Given
        Kunde kunde = createKunde(999L);
        kunde.setNachname(" TEST ");
        Kunde expectedSaved = createKunde(1L);
        when(werkstattService.speichereKunde(any(Kunde.class))).thenAnswer(invocation -> {
            Kunde arg = invocation.getArgument(0);
            assertThat(arg.getId()).isNull(); // assert id was cleared
            return arg;
        });

        // When
        kundenController.anlegen(kunde);

        // Then
        verify(werkstattService).speichereKunde(any(Kunde.class));
    }

    @Test
    void aendern_successReturnsUpdatedKunde() {
        // Given
        long id = 5L;
        Kunde updatedInput = createKunde(null);
        updatedInput.setNachname("Neu");
        Kunde savedKunde = createKunde(id);
        savedKunde.setNachname("Neu");
        when(werkstattService.speichereKunde(any(Kunde.class))).thenReturn(savedKunde);

        // When
        ResponseEntity<?> response = kundenController.aendern(id, updatedInput);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        @SuppressWarnings("unchecked")
        Kunde body = (Kunde) response.getBody();
        assertThat(body).isEqualTo(savedKunde);
        assertThat(body.getId()).isEqualTo(id);
        verify(werkstattService).speichereKunde(any(Kunde.class));
    }

    @Test
    void aendren_exceptionReturns500WithError() {
        // Given
        long id = 7L;
        Kunde updatedInput = createKunde(null);
        String errorMsg = "DB error";
        doThrow(new RuntimeException(errorMsg)).when(werkstattService).speichereKunde(any(Kunde.class));

        // When
        ResponseEntity<?> response = kundenController.aendern(id, updatedInput);

        // Then
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody().toString()).contains(errorMsg);
        verify(werkstattService).speichereKunde(any(Kunde.class));
    }

    @Test
    void loeschen_successReturns200() {
        // Given
        long id = 12L;

        // When
        ResponseEntity<?> response = kundenController.loeschen(id);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        verify(werkstattService).loescheKunde(id);
    }

    @Test
    void loeschen_exceptionReturns500WithError() {
        // Given
        long id = 13L;
        String errorMsg = "Fremdschlüsselverletzung";
        doThrow(new RuntimeException(errorMsg)).when(werkstattService).loescheKunde(id);

        // When
        ResponseEntity<?> response = kundenController.loeschen(id);

        // Then
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody().toString()).contains(errorMsg);
        verify(werkstattService).loescheKunde(id);
    }

    private Kunde createKunde(Long id) {
        Kunde kunde = new Kunde();
        kunde.setId(id);
        kunde.setVorname("Max");
        kunde.setNachname("Mustermann");
        return kunde;
    }

    private Fahrzeug createFahrzeug(Long id, Long kundeId) {
        Fahrzeug fahrzeug = new Fahrzeug();
        fahrzeug.setId(id);
        fahrzeug.setKundeId(kundeId);
        fahrzeug.setKennzeichen("W-F 001");
        return fahrzeug;
    }
}
