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
    private RechnungController rechnungController;

    private Rechnung rechnung1;
    private Rechnung rechnung2;

    @BeforeEach
    void setUp() {
        werkstattService = Mockito.mock(WerkstattService.class);
        rechnungController = new RechnungController();
        
        // Inject mock via ReflectionTestUtils since no constructor injection
        org.springframework.test.util.ReflectionTestUtils.setField(rechnungController, "werkstattService", werkstattService);

        // Prepare test data
        rechnung1 = new Rechnung();
        rechnung1.setId(1L);
        rechnung1.setRechnungNr("R-2024-001");
        rechnung1.setAuftragId(10L);
        rechnung1.setAusgestelltAm(new Date());
        rechnung1.setSummeNetto(100.0);
        rechnung1.setUst(20.0);
        rechnung1.setSummeBrutto(120.0);
        rechnung1.setBezahlt(false);
        rechnung1.setAuftragNr("A-2024-001");
        rechnung1.setKundeName("Max Mustermann");

        rechnung2 = new Rechnung();
        rechnung2.setId(2L);
        rechnung2.setRechnungNr("R-2024-002");
        rechnung2.setAuftragId(11L);
        rechnung2.setAusgestelltAm(new Date());
        rechnung2.setSummeNetto(200.0);
        rechnung2.setUst(40.0);
        rechnung2.setSummeBrutto(240.0);
        rechnung2.setBezahlt(true);
        rechnung2.setBezahltAm(new Date());
        rechnung2.setAuftragNr("A-2024-002");
        rechnung2.setKundeName("Erika Musterfrau");
    }

    @Test
    void liste_returnsAllRechnungen() {
        // Arrange
        List<Rechnung> expected = Arrays.asList(rechnung1, rechnung2);
        when(werkstattService.getAlleRechnungen()).thenReturn(expected);

        // Act
        List<Rechnung> result = rechnungController.liste();

        // Assert
        assertThat(result).isEqualTo(expected);
        verify(werkstattService).getAlleRechnungen();
    }

    @Test
    void liste_returnsEmptyList_whenNoRechnungen() {
        // Arrange
        List<Rechnung> expected = List.of();
        when(werkstattService.getAlleRechnungen()).thenReturn(expected);

        // Act
        List<Rechnung> result = rechnungController.liste();

        // Assert
        assertThat(result).isEmpty();
        verify(werkstattService).getAlleRechnungen();
    }

    @Test
    void einzeln_returnsRechnung_whenFound() {
        // Arrange
        long id = 1L;
        when(werkstattService.getRechnung(id)).thenReturn(rechnung1);

        // Act
        var response = rechnungController.einzeln(id);

        // Assert
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(rechnung1);
        verify(werkstattService).getRechnung(id);
    }

    @Test
    void einzeln_returnsNotFound_whenRechnungNotFound() {
        // Arrange
        long id = 999L;
        when(werkstattService.getRechnung(id)).thenReturn(null);

        // Act
        var response = rechnungController.einzeln(id);

        // Assert
        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isNull();
        verify(werkstattService).getRechnung(id);
    }

    @Test
    void erstellen_returnsCreatedRechnung_whenSuccessful() {
        // Arrange
        long auftragId = 10L;
        when(werkstattService.erstelleRechnung(auftragId)).thenReturn(rechnung1);

        // Act
        var response = rechnungController.erstellen(auftragId);

        // Assert
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(rechnung1);
        verify(werkstattService).erstelleRechnung(auftragId);
    }

    @Test
    void erstellen_returnsInternalServerError_whenExceptionThrown() {
        // Arrange
        long auftragId = 10L;
        String errorMsg = "Auftrag nicht gefunden";
        when(werkstattService.erstelleRechnung(auftragId)).thenThrow(new RuntimeException(errorMsg));

        // Act
        var response = rechnungController.erstellen(auftragId);

        // Assert
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).isEqualTo(errorMsg);
        verify(werkstattService).erstelleRechnung(auftragId);
    }

    @Test
    void bezahlt_returnsUpdatedRechnung_whenSuccessful() {
        // Arrange
        long id = 1L;
        Rechnung updatedRechnung = new Rechnung();
        updatedRechnung.setId(id);
        updatedRechnung.setBezahlt(true);
        updatedRechnung.setBezahltAm(new Date());
        when(werkstattService.setzeBezahlt(id)).thenReturn(updatedRechnung);

        // Act
        var response = rechnungController.bezahlt(id);

        // Assert
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(updatedRechnung);
        verify(werkstattService).setzeBezahlt(id);
    }

    @Test
    void bezahlt_returnsInternalServerError_whenExceptionThrown() {
        // Arrange
        long id = 1L;
        String errorMsg = "Rechnung nicht gefunden";
        when(werkstattService.setzeBezahlt(id)).thenThrow(new RuntimeException(errorMsg));

        // Act
        var response = rechnungController.bezahlt(id);

        // Assert
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).isEqualTo(errorMsg);
        verify(werkstattService).setzeBezahlt(id);
    }

    @Test
    void erstellen_throwsExceptionWithNullMessage_handledGracefully() {
        // Arrange
        long auftragId = 10L;
        when(werkstattService.erstelleRechnung(auftragId)).thenThrow(new RuntimeException((String) null));

        // Act
        var response = rechnungController.erstellen(auftragId);

        // Assert
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).isNull(); // Spring handles null message as null body
        verify(werkstattService).erstelleRechnung(auftragId);
    }

    @Test
    void bezahlt_throwsExceptionWithNullMessage_handledGracefully() {
        // Arrange
        long id = 1L;
        when(werkstattService.setzeBezahlt(id)).thenThrow(new RuntimeException((String) null));

        // Act
        var response = rechnungController.bezahlt(id);

        // Assert
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).isNull();
        verify(werkstattService).setzeBezahlt(id);
    }
}
