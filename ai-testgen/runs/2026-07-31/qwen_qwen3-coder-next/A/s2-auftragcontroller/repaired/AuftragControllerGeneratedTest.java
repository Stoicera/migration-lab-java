package at.werkstatt.crm.gen;

import at.werkstatt.crm.controller.AuftragController;
import at.werkstatt.crm.model.Auftrag;
import at.werkstatt.crm.model.AuftragPosition;
import at.werkstatt.crm.service.WerkstattService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuftragControllerGeneratedTest {

    @Mock
    private WerkstattService werkstattService;

    @InjectMocks
    private AuftragController auftragController;

    private Auftrag auftrag;
    private AuftragPosition position;

    @BeforeEach
    void setUp() {
        auftrag = new Auftrag();
        auftrag.setId(1L);
        auftrag.setAuftragNr("A-2024-001");
        auftrag.setStatus(Auftrag.STATUS_ANGENOMMEN);
        auftrag.setKundeId(10L);
        auftrag.setFahrzeugId(20L);
        auftrag.setBeschreibung("Reparatur");
        auftrag.setKmStand(120000);

        position = new AuftragPosition();
        position.setId(100L);
        position.setTyp(AuftragPosition.TYP_ARBEIT);
        position.setBezeichnung("Bremsen gewechselt");
        position.setMenge(1.0);
        position.setEinzelpreis(150.0);
    }

    @Test
    void listeMitStatusFilter() {
        // Arrange
        String status = Auftrag.STATUS_IN_ARBEIT;
        when(werkstattService.getAuftraege(status)).thenReturn(Arrays.asList(auftrag));

        // Act
        var result = auftragController.liste(status);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(Auftrag.STATUS_ANGENOMMEN);
        verify(werkstattService).getAuftraege(status);
    }

    @Test
    void listeOhneStatusFilter() {
        // Arrange
        when(werkstattService.getAuftraege(null)).thenReturn(Arrays.asList(auftrag));

        // Act
        var result = auftragController.liste(null);

        // Assert
        assertThat(result).hasSize(1);
        verify(werkstattService).getAuftraege(null);
    }

    @Test
    void listeMitLeeremStatus() {
        // Arrange
        when(werkstattService.getAuftraege("")).thenReturn(Collections.emptyList());

        // Act
        var result = auftragController.liste("");

        // Assert
        assertThat(result).isEmpty();
        verify(werkstattService).getAuftraege("");
    }

    @Test
    void einzelnFound() {
        // Arrange
        when(werkstattService.getAuftrag(1L)).thenReturn(auftrag);

        // Act
        var result = auftragController.einzeln(1L);

        // Assert
        assertThat(result.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(result.getBody()).isEqualTo(auftrag);
        verify(werkstattService).getAuftrag(1L);
    }

    @Test
    void einzelnNotFound() {
        // Arrange
        when(werkstattService.getAuftrag(999L)).thenReturn(null);

        // Act
        var result = auftragController.einzeln(999L);

        // Assert
        assertThat(result.getStatusCode().is4xxClientError()).isTrue();
        assertThat(result.getBody()).isNull();
        verify(werkstattService).getAuftrag(999L);
    }

    @Test
    void anlegenSuccess() {
        // Arrange
        when(werkstattService.neuerAuftrag(any(Auftrag.class))).thenReturn(auftrag);

        // Act
        var result = auftragController.anlegen(auftrag);

        // Assert
        assertThat(result.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(result.getBody()).isEqualTo(auftrag);
        verify(werkstattService).neuerAuftrag(auftrag);
    }

    @Test
    void anlegenException() {
        // Arrange
        when(werkstattService.neuerAuftrag(any(Auftrag.class))).thenThrow(new RuntimeException("DB error"));

        // Act
        var result = auftragController.anlegen(auftrag);

        // Assert
        assertThat(result.getStatusCode().is5xxServerError()).isTrue();
        assertThat(result.getBody()).isEqualTo("DB error");
        verify(werkstattService).neuerAuftrag(auftrag);
    }

    @Test
    void statusSuccess() {
        // Arrange
        String neuerStatus = Auftrag.STATUS_IN_ARBEIT;
        when(werkstattService.setzeStatus(eq(1L), eq(neuerStatus))).thenReturn(auftrag);

        // Act
        var result = auftragController.status(1L, neuerStatus);

        // Assert
        assertThat(result.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(result.getBody()).isEqualTo(auftrag);
        verify(werkstattService).setzeStatus(1L, neuerStatus);
    }

    @Test
    void statusException() {
        // Arrange
        String neuerStatus = "UNGUELTIG";
        when(werkstattService.setzeStatus(eq(1L), eq(neuerStatus))).thenThrow(new IllegalArgumentException("Invalid status"));

        // Act
        var result = auftragController.status(1L, neuerStatus);

        // Assert
        assertThat(result.getStatusCode().is5xxServerError()).isTrue();
        assertThat(result.getBody()).isEqualTo("Invalid status");
        verify(werkstattService).setzeStatus(1L, neuerStatus);
    }

    @Test
    void positionSuccess() {
        // Arrange
        when(werkstattService.neuePosition(eq(1L), any(AuftragPosition.class))).thenReturn(position);

        // Act
        var result = auftragController.position(1L, position);

        // Assert
        assertThat(result.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(result.getBody()).isEqualTo(position);
        verify(werkstattService).neuePosition(1L, position);
    }

    @Test
    void positionException() {
        // Arrange
        when(werkstattService.neuePosition(eq(1L), any(AuftragPosition.class)))
                .thenThrow(new IllegalStateException("Position invalid"));

        // Act
        var result = auftragController.position(1L, position);

        // Assert
        assertThat(result.getStatusCode().is5xxServerError()).isTrue();
        assertThat(result.getBody()).isEqualTo("Position invalid");
        verify(werkstattService).neuePosition(1L, position);
    }

    @Test
    void positionLoeschenSuccess() {
        // Arrange
        doNothing().when(werkstattService).loeschePosition(100L);

        // Act
        var result = auftragController.positionLoeschen(100L);

        // Assert
        assertThat(result.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(result.getBody()).isNull();
        verify(werkstattService).loeschePosition(100L);
    }

    @Test
    void positionLoeschenException() {
        // Arrange
        doThrow(new RuntimeException("DB constraint violation")).when(werkstattService).loeschePosition(100L);

        // Act
        var result = auftragController.positionLoeschen(100L);

        // Assert
        assertThat(result.getStatusCode().is5xxServerError()).isTrue();
        assertThat(result.getBody()).isEqualTo("DB constraint violation");
        verify(werkstattService).loeschePosition(100L);
    }

    @Test
    void positionLoeschenWithZeroId() {
        // Arrange
        doNothing().when(werkstattService).loeschePosition(0L);

        // Act
        var result = auftragController.positionLoeschen(0L);

        // Assert
        assertThat(result.getStatusCode().is2xxSuccessful()).isTrue();
        verify(werkstattService).loeschePosition(0L);
    }

    @Test
    void anlegenWithNullAuftrag() {
        // Arrange
        when(werkstattService.neuerAuftrag(null)).thenReturn(auftrag);

        // Act
        var result = auftragController.anlegen(null);

        // Assert
        assertThat(result.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(result.getBody()).isEqualTo(auftrag);
        verify(werkstattService).neuerAuftrag(null);
    }

    @Test
    void statusWithEmptyNeuerStatus() {
        // Arrange
        when(werkstattService.setzeStatus(eq(1L), eq(""))).thenReturn(auftrag);

        // Act
        var result = auftragController.status(1L, "");

        // Assert
        assertThat(result.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(result.getBody()).isEqualTo(auftrag);
        verify(werkstattService).setzeStatus(1L, "");
    }

    @Test
    void positionWithNullPosition() {
        // Arrange
        when(werkstattService.neuePosition(1L, null)).thenReturn(position);

        // Act
        var result = auftragController.position(1L, null);

        // Assert
        assertThat(result.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(result.getBody()).isEqualTo(position);
        verify(werkstattService).neuePosition(1L, null);
    }

    @Test
    void einzelnWithNegativeId() {
        // Arrange
        when(werkstattService.getAuftrag(-1L)).thenReturn(null);

        // Act
        var result = auftragController.einzeln(-1L);

        // Assert
        assertThat(result.getStatusCode().is4xxClientError()).isTrue();
        verify(werkstattService).getAuftrag(-1L);
    }

    @Test
    void anlegenWithNullIdInAuftrag() {
        // Arrange
        Auftrag auftragWithoutId = new Auftrag();
        auftragWithoutId.setAuftragNr("A-2024-002");
        when(werkstattService.neuerAuftrag(auftragWithoutId)).thenReturn(auftrag);

        // Act
        var result = auftragController.anlegen(auftragWithoutId);

        // Assert
        assertThat(result.getStatusCode().is2xxSuccessful()).isTrue();
        verify(werkstattService).neuerAuftrag(auftragWithoutId);
    }

    @Test
    void positionWithNullIdInPosition() {
        // Arrange
        AuftragPosition positionWithoutId = new AuftragPosition();
        positionWithoutId.setTyp(AuftragPosition.TYP_MATERIAL);
        positionWithoutId.setBezeichnung("Schrauben");
        positionWithoutId.setMenge(10.0);
        positionWithoutId.setEinzelpreis(0.5);
        when(werkstattService.neuePosition(1L, positionWithoutId)).thenReturn(position);

        // Act
        var result = auftragController.position(1L, positionWithoutId);

        // Assert
        assertThat(result.getStatusCode().is2xxSuccessful()).isTrue();
        verify(werkstattService).neuePosition(1L, positionWithoutId);
    }

    @Test
    void statusWithNullNeuerStatus() {
        // Arrange
        when(werkstattService.setzeStatus(eq(1L), eq(null))).thenReturn(auftrag);

        // Act
        var result = auftragController.status(1L, null);

        // Assert
        assertThat(result.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(result.getBody()).isEqualTo(auftrag);
        verify(werkstattService).setzeStatus(1L, null);
    }

    @Test
    void positionLoeschenWithNegativeId() {
        // Arrange
        doNothing().when(werkstattService).loeschePosition(-100L);

        // Act
        var result = auftragController.positionLoeschen(-100L);

        // Assert
        assertThat(result.getStatusCode().is2xxSuccessful()).isTrue();
        verify(werkstattService).loeschePosition(-100L);
    }
}
