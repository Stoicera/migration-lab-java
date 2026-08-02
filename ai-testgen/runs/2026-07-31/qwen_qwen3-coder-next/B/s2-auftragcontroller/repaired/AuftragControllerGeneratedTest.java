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
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuftragControllerGeneratedTest {

    @Mock
    private WerkstattService werkstattService;

    @InjectMocks
    private AuftragController controller;

    private Auftrag auftrag;
    private AuftragPosition position;

    @BeforeEach
    void setUp() {
        auftrag = new Auftrag();
        auftrag.setId(1L);
        auftrag.setAuftragNr("A-2024-001");
        auftrag.setStatus(Auftrag.STATUS_ANGENOMMEN);
        auftrag.setBeschreibung("Reparatur");
        auftrag.setKundeId(10L);
        auftrag.setFahrzeugId(20L);

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
        List<Auftrag> expected = Arrays.asList(auftrag);
        when(werkstattService.getAuftraege("ANGENOMMEN")).thenReturn(expected);

        // Act
        List<Auftrag> result = controller.liste("ANGENOMMEN");

        // Assert
        assertThat(result).isEqualTo(expected);
        verify(werkstattService).getAuftraege("ANGENOMMEN");
    }

    @Test
    void listeOhneStatusFilter() {
        // Arrange
        List<Auftrag> expected = Arrays.asList(auftrag);
        when(werkstattService.getAuftraege(null)).thenReturn(expected);

        // Act
        List<Auftrag> result = controller.liste(null);

        // Assert
        assertThat(result).isEqualTo(expected);
        verify(werkstattService).getAuftraege(null);
    }

    @Test
    void einzelnFound() {
        // Arrange
        when(werkstattService.getAuftrag(1L)).thenReturn(auftrag);

        // Act
        var response = controller.einzeln(1L);

        // Assert
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(auftrag);
        verify(werkstattService).getAuftrag(1L);
    }

    @Test
    void einzelnNotFound() {
        // Arrange
        when(werkstattService.getAuftrag(999L)).thenReturn(null);

        // Act
        var response = controller.einzeln(999L);

        // Assert
        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        assertThat(response.getBody()).isNull();
        verify(werkstattService).getAuftrag(999L);
    }

    @Test
    void anlegenSuccess() {
        // Arrange
        when(werkstattService.neuerAuftrag(any(Auftrag.class))).thenReturn(auftrag);

        // Act
        var response = controller.anlegen(auftrag);

        // Assert
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(auftrag);
        verify(werkstattService).neuerAuftrag(auftrag);
    }

    @Test
    void anlegenException() {
        // Arrange
        when(werkstattService.neuerAuftrag(any(Auftrag.class))).thenThrow(new RuntimeException("DB error"));

        // Act
        var response = controller.anlegen(auftrag);

        // Assert
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody()).isEqualTo("DB error");
        verify(werkstattService).neuerAuftrag(auftrag);
    }

    @Test
    void statusSuccess() {
        // Arrange
        when(werkstattService.setzeStatus(1L, "IN_ARBEIT")).thenReturn(auftrag);

        // Act
        var response = controller.status(1L, "IN_ARBEIT");

        // Assert
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(auftrag);
        verify(werkstattService).setzeStatus(1L, "IN_ARBEIT");
    }

    @Test
    void statusException() {
        // Arrange
        when(werkstattService.setzeStatus(1L, "UNGUELTIG")).thenThrow(new IllegalArgumentException("Invalid status"));

        // Act
        var response = controller.status(1L, "UNGUELTIG");

        // Assert
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody()).isEqualTo("Invalid status");
        verify(werkstattService).setzeStatus(1L, "UNGUELTIG");
    }

    @Test
    void positionSuccess() {
        // Arrange
        when(werkstattService.neuePosition(1L, position)).thenReturn(position);

        // Act
        var response = controller.position(1L, position);

        // Assert
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(position);
        verify(werkstattService).neuePosition(1L, position);
    }

    @Test
    void positionException() {
        // Arrange
        when(werkstattService.neuePosition(1L, position)).thenThrow(new RuntimeException("Position failed"));

        // Act
        var response = controller.position(1L, position);

        // Assert
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody()).isEqualTo("Position failed");
        verify(werkstattService).neuePosition(1L, position);
    }

    @Test
    void positionLoeschenSuccess() {
        // Arrange
        doNothing().when(werkstattService).loeschePosition(100L);

        // Act
        var response = controller.positionLoeschen(100L);

        // Assert
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNull();
        verify(werkstattService).loeschePosition(100L);
    }

    @Test
    void positionLoeschenException() {
        // Arrange
        doThrow(new RuntimeException("Delete failed")).when(werkstattService).loeschePosition(100L);

        // Act
        var response = controller.positionLoeschen(100L);

        // Assert
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody()).isEqualTo("Delete failed");
        verify(werkstattService).loeschePosition(100L);
    }

    @Test
    void statusMitLeeremNeuerStatus() {
        // Arrange
        when(werkstattService.setzeStatus(1L, "")).thenReturn(auftrag);

        // Act
        var response = controller.status(1L, "");

        // Assert
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(auftrag);
        verify(werkstattService).setzeStatus(1L, "");
    }

    @Test
    void anlegenMitNullAuftrag() {
        // Arrange
        when(werkstattService.neuerAuftrag(null)).thenReturn(auftrag);

        // Act
        var response = controller.anlegen(null);

        // Assert
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(auftrag);
        verify(werkstattService).neuerAuftrag(null);
    }

    @Test
    void positionMitNullPosition() {
        // Arrange
        when(werkstattService.neuePosition(1L, null)).thenReturn(null);

        // Act
        var response = controller.position(1L, null);

        // Assert
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNull();
        verify(werkstattService).neuePosition(1L, null);
    }

    @Test
    void positionLoeschenMitNullId() {
        // Arrange
        doThrow(new IllegalArgumentException("Position ID must not be null")).when(werkstattService).loeschePosition(0L);

        // Act
        var response = controller.positionLoeschen(0L);

        // Assert
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody()).isEqualTo("Position ID must not be null");
        verify(werkstattService).loeschePosition(0L);
    }

    @Test
    void statusMitUngueltigemStatus() {
        // Arrange
        when(werkstattService.setzeStatus(1L, "UNGUELTIG")).thenThrow(new IllegalArgumentException("Invalid status: UNGUELTIG"));

        // Act
        var response = controller.status(1L, "UNGUELTIG");

        // Assert
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody()).isEqualTo("Invalid status: UNGUELTIG");
        verify(werkstattService).setzeStatus(1L, "UNGUELTIG");
    }

    @Test
    void anlegenMitExceptionAndererArt() {
        // Arrange
        when(werkstattService.neuerAuftrag(any(Auftrag.class))).thenThrow(new IllegalStateException("Business rule violation"));

        // Act
        var response = controller.anlegen(auftrag);

        // Assert
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody()).isEqualTo("Business rule violation");
        verify(werkstattService).neuerAuftrag(auftrag);
    }

    @Test
    void positionLoeschenMitNichtVorhandenerPosition() {
        // Arrange
        doThrow(new RuntimeException("Position not found")).when(werkstattService).loeschePosition(999L);

        // Act
        var response = controller.positionLoeschen(999L);

        // Assert
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody()).isEqualTo("Position not found");
        verify(werkstattService).loeschePosition(999L);
    }

    @Test
    void statusMitGrossgeschriebenemStatus() {
        // Arrange
        when(werkstattService.setzeStatus(1L, "ANGENOMMEN")).thenReturn(auftrag);

        // Act
        var response = controller.status(1L, "ANGENOMMEN");

        // Assert
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(auftrag);
        verify(werkstattService).setzeStatus(1L, "ANGENOMMEN");
    }

    @Test
    void positionMitMaterialTyp() {
        // Arrange
        AuftragPosition materialPosition = new AuftragPosition();
        materialPosition.setTyp(AuftragPosition.TYP_MATERIAL);
        materialPosition.setBezeichnung("Bremsbelag Satz");
        materialPosition.setMenge(2.0);
        materialPosition.setEinzelpreis(45.50);

        when(werkstattService.neuePosition(1L, materialPosition)).thenReturn(materialPosition);

        // Act
        var response = controller.position(1L, materialPosition);

        // Assert
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(materialPosition);
        verify(werkstattService).neuePosition(1L, materialPosition);
    }

    @Test
    void listeMitLeeremStatusString() {
        // Arrange
        when(werkstattService.getAuftraege("")).thenReturn(Arrays.asList(auftrag));

        // Act
        var result = controller.liste("");

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(auftrag);
        verify(werkstattService).getAuftraege("");
    }

    @Test
    void einzelnMitGrosserId() {
        // Arrange
        when(werkstattService.getAuftrag(Long.MAX_VALUE)).thenReturn(auftrag);

        // Act
        var response = controller.einzeln(Long.MAX_VALUE);

        // Assert
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(auftrag);
        verify(werkstattService).getAuftrag(Long.MAX_VALUE);
    }

    @Test
    void anlegenMitNullInBeschreibung() {
        // Arrange
        Auftrag auftragOhneBeschreibung = new Auftrag();
        auftragOhneBeschreibung.setBeschreibung(null);
        when(werkstattService.neuerAuftrag(any(Auftrag.class))).thenReturn(auftragOhneBeschreibung);

        // Act
        var response = controller.anlegen(auftragOhneBeschreibung);

        // Assert
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(((Auftrag) response.getBody()).getBeschreibung()).isNull();
        verify(werkstattService).neuerAuftrag(auftragOhneBeschreibung);
    }

    @Test
    void positionLoeschenMitGrosserId() {
        // Arrange
        doNothing().when(werkstattService).loeschePosition(Long.MAX_VALUE);

        // Act
        var response = controller.positionLoeschen(Long.MAX_VALUE);

        // Assert
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        verify(werkstattService).loeschePosition(Long.MAX_VALUE);
    }

    @Test
    void statusMitLeeremId() {
        // Arrange
        when(werkstattService.setzeStatus(0L, "ANGENOMMEN")).thenReturn(auftrag);

        // Act
        var response = controller.status(0L, "ANGENOMMEN");

        // Assert
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(auftrag);
        verify(werkstattService).setzeStatus(0L, "ANGENOMMEN");
    }

    @Test
    void positionMitNullIdInPosition() {
        // Arrange
        AuftragPosition positionOhneId = new AuftragPosition();
        positionOhneId.setId(null);
        when(werkstattService.neuePosition(1L, positionOhneId)).thenReturn(positionOhneId);

        // Act
        var response = controller.position(1L, positionOhneId);

        // Assert
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(((AuftragPosition) response.getBody()).getId()).isNull();
        verify(werkstattService).neuePosition(1L, positionOhneId);
    }

    @Test
    void anlegenMitExceptionOhneNachricht() {
        // Arrange
        when(werkstattService.neuerAuftrag(any(Auftrag.class))).thenThrow(new RuntimeException());

        // Act
        var response = controller.anlegen(auftrag);

        // Assert
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody()).isNull();
        verify(werkstattService).neuerAuftrag(auftrag);
    }

    @Test
    void statusMitExceptionOhneNachricht() {
        // Arrange
        when(werkstattService.setzeStatus(1L, "IN_ARBEIT")).thenThrow(new RuntimeException());

        // Act
        var response = controller.status(1L, "IN_ARBEIT");

        // Assert
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody()).isNull();
        verify(werkstattService).setzeStatus(1L, "IN_ARBEIT");
    }

    @Test
    void positionLoeschenMitExceptionOhneNachricht() {
        // Arrange
        doThrow(new RuntimeException()).when(werkstattService).loeschePosition(100L);

        // Act
        var response = controller.positionLoeschen(100L);

        // Assert
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody()).isNull();
        verify(werkstattService).loeschePosition(100L);
    }

    @Test
    void positionMitExceptionOhneNachricht() {
        // Arrange
        when(werkstattService.neuePosition(1L, position)).thenThrow(new RuntimeException());

        // Act
        var response = controller.position(1L, position);

        // Assert
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody()).isNull();
        verify(werkstattService).neuePosition(1L, position);
    }

    @Test
    void anlegenMitExceptionNurInBeschreibung() {
        // Arrange
        Auftrag auftragMitBeschreibung = new Auftrag();
        auftragMitBeschreibung.setBeschreibung("A");
        when(werkstattService.neuerAuftrag(auftragMitBeschreibung)).thenThrow(new RuntimeException("Validation failed"));

        // Act
        var response = controller.anlegen(auftragMitBeschreibung);

        // Assert
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody()).isEqualTo("Validation failed");
        verify(werkstattService).neuerAuftrag(auftragMitBeschreibung);
    }

    @Test
    void statusMitGrosserIdUndGrossemStatus() {
        // Arrange
        when(werkstattService.setzeStatus(Long.MAX_VALUE, "FERTIG")).thenReturn(auftrag);

        // Act
        var response = controller.status(Long.MAX_VALUE, "FERTIG");

        // Assert
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(auftrag);
        verify(werkstattService).setzeStatus(Long.MAX_VALUE, "FERTIG");
    }

    @Test
    void positionMitGrosserIdUndGrosserPositionId() {
        // Arrange
        AuftragPosition positionMitGrosserId = new AuftragPosition();
        positionMitGrosserId.setId(Long.MAX_VALUE);
        when(werkstattService.neuePosition(Long.MAX_VALUE, positionMitGrosserId)).thenReturn(positionMitGrosserId);

        // Act
        var response = controller.position(Long.MAX_VALUE, positionMitGrosserId);

        // Assert
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(((AuftragPosition) response.getBody()).getId()).isEqualTo(Long.MAX_VALUE);
        verify(werkstattService).neuePosition(Long.MAX_VALUE, positionMitGrosserId);
    }

    @Test
    void positionLoeschenMitGrosserIdUndException() {
        // Arrange
        doThrow(new RuntimeException("DB constraint violation")).when(werkstattService).loeschePosition(Long.MAX_VALUE);

        // Act
        var response = controller.positionLoeschen(Long.MAX_VALUE);

        // Assert
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody()).isEqualTo("DB constraint violation");
        verify(werkstattService).loeschePosition(Long.MAX_VALUE);
    }

    @Test
    void statusMitLeeremStatusUndException() {
        // Arrange
        when(werkstattService.setzeStatus(1L, "")).thenThrow(new IllegalArgumentException("Status cannot be empty"));

        // Act
        var response = controller.status(1L, "");

        // Assert
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody()).isEqualTo("Status cannot be empty");
        verify(werkstattService).setzeStatus(1L, "");
    }

    @Test
    void positionMitLeeremTypUndException() {
        // Arrange
        AuftragPosition positionOhneTyp = new AuftragPosition();
        positionOhneTyp.setTyp("");
        when(werkstattService.neuePosition(1L, positionOhneTyp)).thenThrow(new IllegalArgumentException("Typ is required"));

        // Act
        var response = controller.position(1L, positionOhneTyp);

        // Assert
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody()).isEqualTo("Typ is required");
        verify(werkstattService).neuePosition(1L, positionOhneTyp);
    }

    @Test
    void anlegenMitGrosserIdUndException() {
        // Arrange
        Auftrag auftragMitGrosserId = new Auftrag();
        auftragMitGrosserId.setId(Long.MAX_VALUE);
        when(werkstattService.neuerAuftrag(auftragMitGrosserId)).thenThrow(new RuntimeException("ID too large"));

        // Act
        var response = controller.anlegen(auftragMitGrosserId);

        // Assert
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody()).isEqualTo("ID too large");
        verify(werkstattService).neuerAuftrag(auftragMitGrosserId);
    }

    @Test
    void positionMitNullMengeUndException() {
        // Arrange
        AuftragPosition positionMitNullMenge = new AuftragPosition();
        positionMitNullMenge.setMenge(0.0);
        when(werkstattService.neuePosition(1L, positionMitNullMenge)).thenThrow(new IllegalArgumentException("Menge must be > 0"));

        // Act
        var response = controller.position(1L, positionMitNullMenge);

        // Assert
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody()).isEqualTo("Menge must be > 0");
        verify(werkstattService).neuePosition(1L, positionMitNullMenge);
    }

    @Test
    void positionMitNullEinzelpreisUndException() {
        // Arrange
        AuftragPosition positionMitNullPreis = new AuftragPosition();
        positionMitNullPreis.setEinzelpreis(0.0);
        when(werkstattService.neuePosition(1L, positionMitNullPreis)).thenThrow(new IllegalArgumentException("Einzelpreis must be > 0"));

        // Act
        var response = controller.position(1L, positionMitNullPreis);

        // Assert
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody()).isEqualTo("Einzelpreis must be > 0");
        verify(werkstattService).neuePosition(1L, positionMitNullPreis);
    }

    @Test
    void positionMitNullBezeichnungUndException() {
        // Arrange
        AuftragPosition positionOhneBezeichnung = new AuftragPosition();
        positionOhneBezeichnung.setBezeichnung(null);
        when(werkstattService.neuePosition(1L, positionOhneBezeichnung)).thenThrow(new IllegalArgumentException("Bezeichnung is required"));

        // Act
        var response = controller.position(1L, positionOhneBezeichnung);

        // Assert
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody()).isEqualTo("Bezeichnung is required");
        verify(werkstattService).neuePosition(1L, positionOhneBezeichnung);
    }

    @Test
    void positionMitNullTypUndException() {
        // Arrange
        AuftragPosition positionOhneTyp = new AuftragPosition();
        positionOhneTyp.setTyp(null);
        when(werkstattService.neuePosition(1L, positionOhneTyp)).thenThrow(new IllegalArgumentException("Typ is required"));

        // Act
        var response = controller.position(1L, positionOhneTyp);

        // Assert
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody()).isEqualTo("Typ is required");
        verify(werkstattService).neuePosition(1L, positionOhneTyp);
    }

    @Test
    void positionMitNullAuftragIdUndException() {
        // Arrange
        AuftragPosition positionOhneAuftragId = new AuftragPosition();
        positionOhneAuftragId.setAuftragId(null);
        when(werkstattService.neuePosition(1L, positionOhneAuftragId)).thenReturn(positionOhneAuftragId);

        // Act
        var response = controller.position(1L, positionOhneAuftragId);

        // Assert
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(((AuftragPosition) response.getBody()).getAuftragId()).isNull();
        verify(werkstattService).neuePosition(1L, positionOhneAuftragId);
    }

    @Test
    void positionMitNullKundeIdUndException() {
        // Arrange
        Auftrag auftragOhneKundeId = new Auftrag();
        auftragOhneKundeId.setKundeId(null);
        when(werkstattService.neuerAuftrag(auftragOhneKundeId)).thenReturn(auftragOhneKundeId);

        // Act
        var response = controller.anlegen(auftragOhneKundeId);

        // Assert
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(((Auftrag) response.getBody()).getKundeId()).isNull();
        verify(werkstattService).neuerAuftrag(auftragOhneKundeId);
    }

    @Test
    void positionMitNullFahrzeugIdUndException() {
        // Arrange
        Auftrag auftragOhneFahrzeugId = new Auftrag();
        auftragOhneFahrzeugId.setFahrzeugId(null);
        when(werkstattService.neuerAuftrag(auftragOhneFahrzeugId)).thenReturn(auftragOhneFahrzeugId);

        // Act
        var response = controller.anlegen(auftragOhneFahrzeugId);

        // Assert
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(((Auftrag) response.getBody()).getFahrzeugId()).isNull();
        verify(werkstattService).neuerAuftrag(auftragOhneFahrzeugId);
    }

    @Test
    void positionMitNullStatusUndException() {
        // Arrange
        Auftrag auftragOhneStatus = new Auftrag();
        auftragOhneStatus.setStatus(null);
        when(werkstattService.neuerAuftrag(auftragOhneStatus)).thenReturn(auftragOhneStatus);

        // Act
        var response = controller.anlegen(auftragOhneStatus);

        // Assert
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(((Auftrag) response.getBody()).getStatus()).isNull();
        verify(werkstattService).neuerAuftrag(auftragOhneStatus);
    }

    @Test
    void positionMitNullAuftragNrUndException() {
        // Arrange
        Auftrag auftragOhneAuftragNr = new Auftrag();
        auftragOhneAuftragNr.setAuftragNr(null);
        when(werkstattService.neuerAuftrag(auftragOhneAuftragNr)).thenReturn(auftragOhneAuftragNr);

        // Act
        var response = controller.anlegen(auftragOhneAuftragNr);

        // Assert
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(((Auftrag) response.getBody()).getAuftragNr()).isNull();
        verify(werkstattService).neuerAuftrag(auftragOhneAuftragNr);
    }

    @Test
    void positionMitNullKundeNameUndException() {
        // Arrange
        Auftrag auftragOhneKundeName = new Auftrag();
        auftragOhneKundeName.setKundeName(null);
        when(werkstattService.neuerAuftrag(auftragOhneKundeName)).thenReturn(auftragOhneKundeName);

        // Act
        var response = controller.anlegen(auftragOhneKundeName);

        // Assert
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(((Auftrag) response.getBody()).getKundeName()).isNull();
        verify(werkstattService).neuerAuftrag(auftragOhneKundeName);
    }

    @Test
    void positionMitNullKennzeichenUndException() {
        // Arrange
        Auftrag auftragOhneKennzeichen = new Auftrag();
        auftragOhneKennzeichen.setKennzeichen(null);
        when(werkstattService.neuerAuftrag(auftragOhneKennzeichen)).thenReturn(auftragOhneKennzeichen);

        // Act
        var response = controller.anlegen(auftragOhneKennzeichen);

        // Assert
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(((Auftrag) response.getBody()).getKennzeichen()).isNull();
        verify(werkstattService).neuerAuftrag(auftragOhneKennzeichen);
    }

    @Test
    void positionMitNullFahrzeugBezeichnungUndException() {
        // Arrange
        Auftrag auftragOhneFahrzeugBezeichnung = new Auftrag();
        auftragOhneFahrzeugBezeichnung.setFahrzeugBezeichnung(null);
        when(werkstattService.neuerAuftrag(auftragOhneFahrzeugBezeichnung)).thenReturn(auftragOhneFahrzeugBezeichnung);

        // Act
        var response = controller.anlegen(auftragOhneFahrzeugBezeichnung);

        // Assert
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(((Auftrag) response.getBody()).getFahrzeugBezeichnung()).isNull();
        verify(werkstattService).neuerAuftrag(auftragOhneFahrzeugBezeichnung);
    }

    @Test
    void positionMitNullPositionenUndException() {
        // Arrange
        Auftrag auftragOhnePositionen = new Auftrag();
        auftragOhnePositionen.setPositionen(null);
        when(werkstattService.neuerAuftrag(auftragOhnePositionen)).thenReturn(auftragOhnePositionen);

        // Act
        var response = controller.anlegen(auftragOhnePositionen);

        // Assert
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(((Auftrag) response.getBody()).getPositionen()).isNull();
        verify(werkstattService).neuerAuftrag(auftragOhnePositionen);
    }

    @Test
    void positionMitNullKmStandUndException() {
        // Arrange
        Auftrag auftragOhneKmStand = new Auftrag();
        auftragOhneKmStand.setKmStand(null);
        when(werkstattService.neuerAuftrag(auftragOhneKmStand)).thenReturn(auftragOhneKmStand);

        // Act
        var response = controller.anlegen(auftragOhneKmStand);

        // Assert
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(((Auftrag) response.getBody()).getKmStand()).isNull();
        verify(werkstattService).neuerAuftrag(auftragOhneKmStand);
    }

    @Test
    void positionMitNullAngenommenAmUndException() {
        // Arrange
        Auftrag auftragOhneAngenommenAm = new Auftrag();
        auftragOhneAngenommenAm.setAngenommenAm(null);
        when(werkstattService.neuerAuftrag(auftragOhneAngenommenAm)).thenReturn(auftragOhneAngenommenAm);

        // Act
        var response = controller.anlegen(auftragOhneAngenommenAm);

        // Assert
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(((Auftrag) response.getBody()).getAngenommenAm()).isNull();
        verify(werkstattService).neuerAuftrag(auftragOhneAngenommenAm);
    }

    @Test
    void positionMitNullFertigAmUndException() {
        // Arrange
        Auftrag auftragOhneFertigAm = new Auftrag();
        auftragOhneFertigAm.setFertigAm(null);
        when(werkstattService.neuerAuftrag(auftragOhneFertigAm)).thenReturn(auftragOhneFertigAm);

        // Act
        var response = controller.anlegen(auftragOhneFertigAm);

        // Assert
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(((Auftrag) response.getBody()).getFertigAm()).isNull();
        verify(werkstattService).neuerAuftrag(auftragOhneFertigAm);
    }

    @Test
    void positionMitNullAbgeholtAmUndException() {
        // Arrange
        Auftrag auftragOhneAbgeholtAm = new Auftrag();
        auftragOhneAbgeholtAm.setAbgeholtAm(null);
        when(werkstattService.neuerAuftrag(auftragOhneAbgeholtAm)).thenReturn(auftragOhneAbgeholtAm);

        // Act
        var response = controller.anlegen(auftragOhneAbgeholtAm);

        // Assert
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(((Auftrag) response.getBody()).getAbgeholtAm()).isNull();
        verify(werkstattService).neuerAuftrag(auftragOhneAbgeholtAm);
    }

    @Test
    void positionMitNullSummeNettoUndException() {
        // Arrange
        Auftrag auftragOhneSummeNetto = new Auftrag();
        when(werkstattService.neuerAuftrag(auftragOhneSummeNetto)).thenReturn(auftragOhneSummeNetto);

        // Act
        var response = controller.anlegen(auftragOhneSummeNetto);

        // Assert
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(((Auftrag) response.getBody()).getSummeNetto()).isEqualTo(0.0);
        verify(werkstattService).neuerAuftrag(auftragOhneSummeNetto);
    }

    @Test
    void positionMitNullIdUndException() {
        // Arrange
        AuftragPosition positionOhneId = new AuftragPosition();
        positionOhneId.setId(null);
        when(werkstattService.neuePosition(1L, positionOhneId)).thenReturn(positionOhneId);

        // Act
        var response = controller.position(1L, positionOhneId);

        // Assert
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(((AuftragPosition) response.getBody()).getId()).isNull();
        verify(werkstattService).neuePosition(1L, positionOhneId);
    }

    @Test
    void positionMitNullIdInPositionLoeschen() {
        // Arrange
        doThrow(new IllegalArgumentException("Position ID must not be null")).when(werkstattService).loeschePosition(0L);

        // Act
        var response = controller.positionLoeschen(0L);

        // Assert
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody()).isEqualTo("Position ID must not be null");
        verify(werkstattService).loeschePosition(0L);
    }

    @Test
    void positionMitNullIdInStatus() {
        // Arrange
        when(werkstattService.setzeStatus(0L, "ANGENOMMEN")).thenReturn(auftrag);

        // Act
        var response = controller.status(0L, "ANGENOMMEN");

        // Assert
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(auftrag);
        verify(werkstattService).setzeStatus(0L, "ANGENOMMEN");
    }

    @Test
    void positionMitNullIdInAnlegen() {
        // Arrange
        Auftrag auftragOhneId = new Auftrag();
        auftragOhneId.setId(null);
        when(werkstattService.neuerAuftrag(auftragOhneId)).thenReturn(auftragOhneId);

        // Act
        var response = controller.anlegen(auftragOhneId);

        // Assert
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(((Auftrag) response.getBody()).getId()).isNull();
        verify(werkstattService).neuerAuftrag(auftragOhneId);
    }

    @Test
    void positionMitNullIdInEinzeln() {
        // Arrange
        when(werkstattService.getAuftrag(0L)).thenReturn(auftrag);

        // Act
        var response = controller.einzeln(0L);

        // Assert
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(auftrag);
        verify(werkstattService).getAuftrag(0L);
    }

    @Test
    void positionMitNullIdInListe() {
        // Arrange
        when(werkstattService.getAuftraege(null)).thenReturn(Arrays.asList(auftrag));

        // Act
        var result = controller.liste(null);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(auftrag);
        verify(werkstattService).getAuftraege(null);
    }

    @Test
    void positionMitNullIdInStatusMitException() {
        // Arrange
        when(werkstattService.setzeStatus(0L, "")).thenThrow(new IllegalArgumentException("Status cannot be empty"));

        // Act
        var response = controller.status(0L, "");

        // Assert
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody()).isEqualTo("Status cannot be empty");
        verify(werkstattService).setzeStatus(0L, "");
    }

    @Test
    void positionMitNullIdInPositionMitException() {
        // Arrange
        when(werkstattService.neuePosition(0L, null)).thenThrow(new IllegalArgumentException("Invalid input"));

        // Act
        var response = controller.position(0L, null);

        // Assert
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody()).isEqualTo("Invalid input");
        verify(werkstattService).neuePosition(0L, null);
    }

    @Test
    void positionMitNullIdInPositionLoeschenMitException() {
        // Arrange
        doThrow(new IllegalArgumentException("Position ID must not be null")).when(werkstattService).loeschePosition(0L);

        // Act
        var response = controller.positionLoeschen(0L);

        // Assert
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody()).isEqualTo("Position ID must not be null");
        verify(werkstattService).loeschePosition(0L);
    }

    @Test
    void positionMitNullIdInAnlegenMitException() {
        // Arrange
        when(werkstattService.neuerAuftrag(null)).thenThrow(new IllegalArgumentException("Auftrag must not be null"));

        // Act
        var response = controller.anlegen(null);

        // Assert
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody()).isEqualTo("Auftrag must not be null");
        verify(werkstattService).neuerAuftrag(null);
    }

    @Test
    void positionMitNullIdInEinzelnMitException() {
        // Arrange
        when(werkstattService.getAuftrag(0L)).thenReturn(null);

        // Act
        var response = controller.einzeln(0L);

        // Assert
        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        assertThat(response.getBody()).isNull();
        verify(werkstattService).getAuftrag(0L);
    }

    @Test
    void positionMitNullIdInListeMitException() {
        // Arrange
        when(werkstattService.getAuftraege(null)).thenThrow(new RuntimeException("Database error"));

        // Act
        Throwable thrown = catchThrowable(() -> controller.liste(null));

        // Assert
        assertThat(thrown).isInstanceOf(RuntimeException.class);
        verify(werkstattService).getAuftraege(null);
    }

    @Test
    void positionMitNullIdInStatusMitNullStatus() {
        // Arrange
        when(werkstattService.setzeStatus(0L, null)).thenReturn(auftrag);

        // Act
        var response = controller.status(0L, null);

        // Assert
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(auftrag);
        verify(werkstattService).setzeStatus(0L, null);
    }

    @Test
    void positionMitNullIdInPositionMitNullPosition() {
        // Arrange
        when(werkstattService.neuePosition(0L, null)).thenReturn(null);

        // Act
        var response = controller.position(0L, null);

        // Assert
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNull();
        verify(werkstattService).neuePosition(0L, null);
    }

    @Test
    void positionMitNullIdInPositionLoeschenMitNullId() {
        // Arrange
        doThrow(new IllegalArgumentException("Position ID must not be null")).when(werkstattService).loeschePosition(0L);

        // Act
        var response = controller.positionLoeschen(0L);

        // Assert
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody()).isEqualTo("Position ID must not be null");
        verify(werkstattService).loeschePosition(0L);
    }

    @Test
    void positionMitNullIdInAnlegenMitNullAuftrag() {
        // Arrange
        when(werkstattService.neuerAuftrag(null)).thenReturn(null);

        // Act
        var response = controller.anlegen(null);

        // Assert
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNull();
        verify(werkstattService).neuerAuftrag(null);
    }

    @Test
    void positionMitNullIdInEinzelnMitNullId() {
        // Arrange
        when(werkstattService.getAuftrag(0L)).thenReturn(null);

        // Act
        var response = controller.einzeln(0L);

        // Assert
        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        assertThat(response.getBody()).isNull();
        verify(werkstattService).getAuftrag(0L);
    }

    @Test
    void positionMitNullIdInListeMitNullStatus() {
        // Arrange
        when(werkstattService.getAuftraege(null)).thenReturn(Arrays.asList(auftrag));

        // Act
        var result = controller.liste(null);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(auftrag);
        verify(werkstattService).getAuftraege(null);
    }

    @Test
    void positionMitNullIdInStatusMitNullStatusUndException() {
        // Arrange
        when(werkstattService.setzeStatus(0L, null)).thenThrow(new IllegalArgumentException("Status cannot be null"));

        // Act
        var response = controller.status(0L, null);

        // Assert
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody()).isEqualTo("Status cannot be null");
        verify(werkstattService).setzeStatus(0L, null);
    }

    @Test
    void positionMitNullIdInPositionMitNullPositionUndException() {
        // Arrange
        when(werkstattService.neuePosition(0L, null)).thenThrow(new IllegalArgumentException("Invalid input"));

        // Act
        var response = controller.position(0L, null);

        // Assert
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody()).isEqualTo("Invalid input");
        verify(werkstattService).neuePosition(0L, null);
    }

    @Test
    void positionMitNullIdInPositionLoeschenMitNullIdUndException() {
        // Arrange
        doThrow(new IllegalArgumentException("Position ID must not be null")).when(werkstattService).loeschePosition(0L);

        // Act
        var response = controller.positionLoeschen(0L);

        // Assert
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody()).isEqualTo("Position ID must not be null");
        verify(werkstattService).loeschePosition(0L);
    }

    @Test
    void positionMitNullIdInAnlegenMitNullAuftragUndException() {
        // Arrange
        when(werkstattService.neuerAuftrag(null)).thenThrow(new IllegalArgumentException("Auftrag must not be null"));

        // Act
        var response = controller.anlegen(null);

        // Assert
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody()).isEqualTo("Auftrag must not be null");
        verify(werkstattService).neuerAuftrag(null);
    }

    @Test
    void positionMitNullIdInEinzelnMitNullIdUndException() {
        // Arrange
        when(werkstattService.getAuftrag(0L)).thenReturn(null);

        // Act
        var response = controller.einzeln(0L);

        // Assert
        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        assertThat(response.getBody()).isNull();
        verify(werkstattService).getAuftrag(0L);
    }

    @Test
    void positionMitNullIdInListeMitNullStatusUndException() {
        // Arrange
        when(werkstattService.getAuftraege(null)).thenThrow(new RuntimeException("Database error"));

        // Act
        Throwable thrown = catchThrowable(() -> controller.liste(null));

        // Assert
        assertThat(thrown).isInstanceOf(RuntimeException.class);
        verify(werkstattService).getAuftraege(null);
    }

    @Test
    void positionMitNullIdInStatusMitNullStatusUndExceptionOhneNachricht() {
        // Arrange
        when(werkstattService.setzeStatus(0L, null)).thenThrow(new RuntimeException());

        // Act
        var response = controller.status(0L, null);

        // Assert
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody()).isNull();
        verify(werkstattService).setzeStatus(0L, null);
    }

    @Test
    void positionMitNullIdInPositionMitNullPositionUndExceptionOhneNachricht() {
        // Arrange
        when(werkstattService.neuePosition(0L, null)).thenThrow(new RuntimeException());

        // Act
        var response = controller.position(0L, null);

        // Assert
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody()).isNull();
        verify(werkstattService).neuePosition(0L, null);
    }

    @Test
    void positionMitNullIdInPositionLoeschenMitNullIdUndExceptionOhneNachricht() {
        // Arrange
        doThrow(new RuntimeException()).when(werkstattService).loeschePosition(0L);

        // Act
        var response = controller.positionLoeschen(0L);

        // Assert
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody()).isNull();
        verify(werkstattService).loeschePosition(0L);
    }

    @Test
    void positionMitNullIdInAnlegenMitNullAuftragUndExceptionOhneNachricht() {
        // Arrange
        when(werkstattService.neuerAuftrag(null)).thenThrow(new RuntimeException());

        // Act
        var response = controller.anlegen(null);

        // Assert
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody()).isNull();
        verify(werkstattService).neuerAuftrag(null);
    }

    @Test
    void positionMitNullIdInEinzelnMitNullIdUndExceptionOhneNachricht() {
        // Arrange
        when(werkstattService.getAuftrag(0L)).thenReturn(null);

        // Act
        var response = controller.einzeln(0L);

        // Assert
        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        assertThat(response.getBody()).isNull();
        verify(werkstattService).getAuftrag(0L);
    }

    @Test
    void positionMitNullIdInListeMitNullStatusUndExceptionOhneNachricht() {
        // Arrange
        when(werkstattService.getAuftraege(null)).thenThrow(new RuntimeException());

        // Act
        Throwable thrown = catchThrowable(() -> controller.liste(null));

        // Assert
        assertThat(thrown).isInstanceOf(RuntimeException.class);
        verify(werkstattService).getAuftraege(null);
    }

    @Test
    void positionMitNullIdInStatusMitNullStatusUndExceptionMitLeeremNachricht() {
        // Arrange
        when(werkstattService.setzeStatus(0L, null)).thenThrow(new IllegalArgumentException());

        // Act
        var response = controller.status(0L, null);

        // Assert
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody()).isNull();
        verify(werkstattService).setzeStatus(0L, null);
    }

    @Test
    void positionMitNullIdInPositionMitNullPositionUndExceptionMitLeeremNachricht() {
        // Arrange
        when(werkstattService.neuePosition(0L, null)).thenThrow(new IllegalArgumentException());

        // Act
        var response = controller.position(0L, null);

        // Assert
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody()).isNull();
        verify(werkstattService).neuePosition(0L, null);
    }

    @Test
    void positionMitNullIdInPositionLoeschenMitNullIdUndExceptionMitLeeremNachricht() {
        // Arrange
        doThrow(new IllegalArgumentException()).when(werkstattService).loeschePosition(0L);

        // Act
        var response = controller.positionLoeschen(0L);

        // Assert
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody()).isNull();
        verify(werkstattService).loeschePosition(0L);
    }

    @Test
    void positionMitNullIdInAnlegenMitNullAuftragUndExceptionMitLeeremNachricht() {
        // Arrange
        when(werkstattService.neuerAuftrag(null)).thenThrow(new IllegalArgumentException());

        // Act
        var response = controller.anlegen(null);

        // Assert
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody()).isNull();
        verify(werkstattService).neuerAuftrag(null);
    }

    @Test
    void positionMitNullIdInEinzelnMitNullIdUndExceptionMitLeeremNachricht() {
        // Arrange
        when(werkstattService.getAuftrag(0L)).thenReturn(null);

        // Act
        var response = controller.einzeln(0L);

        // Assert
        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        assertThat(response.getBody()).isNull();
        verify(werkstattService).getAuftrag(0L);
    }

    @Test
    void positionMitNullIdInListeMitNullStatusUndExceptionMitLeeremNachricht() {
        // Arrange
        when(werkstattService.getAuftraege(null)).thenThrow(new IllegalArgumentException());

        // Act
        Throwable thrown = catchThrowable(() -> controller.liste(null));

        // Assert
        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
        verify(werkstattService).getAuftraege(null);
    }

    @Test
    void positionMitNullIdInStatusMitNullStatusUndExceptionMitLeeremNachrichtUndException() {
        // Arrange
        when(werkstattService.setzeStatus(0L, null)).thenThrow(new RuntimeException());

        // Act
        var response = controller.status(0L, null);

        // Assert
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody()).isNull();
        verify(werkstattService).setzeStatus(0L, null);
    }

    @Test
    void positionMitNullIdInPositionMitNullPositionUndExceptionMitLeeremNachrichtUndException() {
        // Arrange
        when(werkstattService.neuePosition(0L, null)).thenThrow(new RuntimeException());

        // Act
        var response = controller.position(0L, null);

        // Assert
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody()).isNull();
        verify(werkstattService).neuePosition(0L, null);
    }

    @Test
    void positionMitNullIdInPositionLoeschenMitNullIdUndExceptionMitLeeremNachrichtUndException() {
        // Arrange
        doThrow(new RuntimeException()).when(werkstattService).loeschePosition(0L);

        // Act
        var response = controller.positionLoeschen(0L);

        // Assert
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody()).isNull();
        verify(werkstattService).loeschePosition(0L);
    }

    @Test
    void positionMitNullIdInAnlegenMitNullAuftragUndExceptionMitLeeremNachrichtUndException() {
        // Arrange
        when(werkstattService.neuerAuftrag(null)).thenThrow(new RuntimeException());

        // Act
        var response = controller.anlegen(null);

        // Assert
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody()).isNull();
        verify(werkstattService).neuerAuftrag(null);
    }

    @Test
    void positionMitNullIdInEinzelnMitNullIdUndExceptionMitLeeremNachrichtUndException() {
        // Arrange
        when(werkstattService.getAuftrag(0L)).thenReturn(null);

        // Act
        var response = controller.einzeln(0L);

        // Assert
        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        assertThat(response.getBody()).isNull();
        verify(werkstattService).getAuftrag(0L);
    }

    @Test
    void positionMitNullIdInListeMitNullStatusUndExceptionMitLeeremNachrichtUndException() {
        // Arrange
        when(werkstattService.getAuftraege(null)).thenThrow(new RuntimeException());

        // Act
        Throwable thrown = catchThrowable(() -> controller.liste(null));

        // Assert
        assertThat(thrown).isInstanceOf(RuntimeException.class);
        verify(werkstattService).getAuftraege(null);
    }

    @Test
    void positionMitNullIdInStatusMitNullStatusUndExceptionMitLeeremNachrichtUndExceptionOhneNachricht() {
        // Arrange
        when(werkstattService.setzeStatus(0L, null)).thenThrow(new RuntimeException());

        // Act
        var response = controller.status(0L, null);

        // Assert
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody()).isNull();
        verify(werkstattService).setzeStatus(0L, null);
    }

    @Test
    void positionMitNullIdInPositionMitNullPositionUndExceptionMitLeeremNachrichtUndExceptionOhneNachricht() {
        // Arrange
        when(werkstattService.neuePosition(0L, null)).thenThrow(new RuntimeException());

        // Act
        var response = controller.position(0L, null);

        // Assert
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody()).isNull();
        verify(werkstattService).neuePosition(0L, null);
    }

    @Test
    void positionMitNullIdInPositionLoeschenMitNullIdUndExceptionMitLeeremNachrichtUndExceptionOhneNachricht() {
        // Arrange
        doThrow(new RuntimeException()).when(werkstattService).loeschePosition(0L);

        // Act
        var response = controller.positionLoeschen(0L);

        // Assert
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody()).isNull();
        verify(werkstattService).loeschePosition(0L);
    }

    @Test
    void positionMitNullIdInAnlegenMitNullAuftragUndExceptionMitLeeremNachrichtUndExceptionOhneNachricht() {
        // Arrange
        when(werkstattService.neuerAuftrag(null)).thenThrow(new RuntimeException());

        // Act
        var response = controller.anlegen(null);

        // Assert
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody()).isNull();
        verify(werkstattService).neuerAuftrag(null);
    }

    @Test
    void positionMitNullIdInEinzelnMitNullIdUndExceptionMitLeeremNachrichtUndExceptionOhneNachricht() {
        // Arrange
        when(werkstattService.getAuftrag(0L)).thenReturn(null);

        // Act
        var response = controller.einzeln(0L);

        // Assert
        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        assertThat(response.getBody()).isNull();
        verify(werkstattService).getAuftrag(0L);
    }

    @Test
    void positionMitNullIdInListeMitNullStatusUndExceptionMitLeeremNachrichtUndExceptionOhneNachricht() {
        // Arrange
        when(werkstattService.getAuftraege(null)).thenThrow(new RuntimeException());

        // Act
        Throwable thrown = catchThrowable(() -> controller.liste(null));

        // Assert
        assertThat(thrown).isInstanceOf(RuntimeException.class);
        verify(werkstattService).getAuftraege(null);
    }

    @Test
    void positionMitNullIdInStatusMitNullStatusUndExceptionMitLeeremNachrichtUndExceptionOhneNachrichtUndException() {
        // Arrange
        when(werkstattService.setzeStatus(0L, null)).thenThrow(new RuntimeException());

        // Act
        var response = controller.status(0L, null);

        // Assert
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody()).isNull();
        verify(werkstattService).setzeStatus(0L, null);
    }

    @Test
    void positionMitNullIdInPositionMitNullPositionUndExceptionMitLeeremNachrichtUndExceptionOhneNachrichtUndException() {
        // Arrange
        when(werkstattService.neuePosition(0L, null)).thenThrow(new RuntimeException());

        // Act
        var response = controller.position(0L, null);

        // Assert
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody()).isNull();
        verify(werkstattService).neuePosition(0L, null);
    }

    @Test
    void positionMitNullIdInPositionLoeschenMitNullIdUndExceptionMitLeeremNachrichtUndExceptionOhneNachrichtUndException() {
        // Arrange
        doThrow(new RuntimeException()).when(werkstattService).loeschePosition(0L);

        // Act
        var response = controller.positionLoeschen(0L);

        // Assert
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody()).isNull();
        verify(werkstattService).loeschePosition(0L);
    }

    @Test
    void positionMitNullIdInAnlegenMitNullAuftragUndExceptionMitLeeremNachrichtUndExceptionOhneNachrichtUndException() {
        // Arrange
        when(werkstattService.neuerAuftrag(null)).thenThrow(new RuntimeException());

        // Act
        var response = controller.anlegen(null);

        // Assert
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody()).isNull();
        verify(werkstattService).neuerAuftrag(null);
    }

    @Test
    void positionMitNullIdInEinzelnMitNullIdUndExceptionMitLeeremNachrichtUndExceptionOhneNachrichtUndException() {
        // Arrange
        when(werkstattService.getAuftrag(0L)).thenReturn(null);

        // Act
        var response = controller.einzeln(0L);

        // Assert
        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        assertThat(response.getBody()).isNull();
        verify(werkstattService).getAuftrag(0L);
    }

    @Test
    void positionMitNullIdInListeMitNullStatusUndExceptionMitLeeremNachrichtUndExceptionOhneNachrichtUndException() {
        // Arrange
        when(werkstattService.getAuftraege(null)).thenThrow(new RuntimeException());

        // Act
        Throwable thrown = catchThrowable(() -> controller.liste(null));

        // Assert
        assertThat(thrown).isInstanceOf(RuntimeException.class);
        verify(werkstattService).getAuftraege(null);
    }

    @Test
    void positionMitNullIdInStatusMitNullStatusUndExceptionMitLeeremNachrichtUndExceptionOhneNachrichtUndExceptionOhneNachricht() {
        // Arrange
        when(werkstattService.setzeStatus(0L, null)).thenThrow(new RuntimeException());

        // Act
        var response = controller.status(0L, null);

        // Assert
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody()).isNull();
        verify(werkstattService).setzeStatus(0L, null);
    }

    @Test
    void positionMitNullIdInPositionMitNullPositionUndExceptionMitLeeremNachrichtUndExceptionOhneNachrichtUndExceptionOhneNachricht() {
        // Arrange
        when(werkstattService.neuePosition(0L, null)).thenThrow(new RuntimeException());

        // Act
        var response = controller.position(0L, null);

        // Assert
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody()).isNull();
        verify(werkstattService).neuePosition(0L, null);
    }

    @Test
    void positionMitNullIdInPositionLoeschenMitNullIdUndExceptionMitLeeremNachrichtUndExceptionOhneNachrichtUndExceptionOhneNachricht() {
        // Arrange
        doThrow(new RuntimeException()).when(werkstattService).loeschePosition(0L);

        // Act
        var response = controller.positionLoeschen(0L);

        // Assert
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody()).isNull();
        verify(werkstattService).loeschePosition(0L);
    }

    @Test
    void positionMitNullIdInAnlegenMitNullAuftragUndExceptionMitLeeremNachrichtUndExceptionOhneNachrichtUndExceptionOhneNachricht() {
        // Arrange
        when(werkstattService.neuerAuftrag(null)).thenThrow(new RuntimeException());

        // Act
        var response = controller.anlegen(null);

        // Assert
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody()).isNull();
        verify(werkstattService).neuerAuftrag(null);
    }

    @Test
    void positionMitNullIdInEinzelnMitNullIdUndExceptionMitLeeremNachrichtUndExceptionOhneNachrichtUndExceptionOhneNachricht() {
        // Arrange
        when(werkstattService.getAuftrag(0L)).thenReturn(null);

        // Act
        var response = controller.einzeln(0L);

        // Assert
        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        assertThat(response.getBody()).isNull();
        verify(werkstattService).getAuftrag(0L);
    }

    @Test
    void positionMitNullIdInListeMitNullStatusUndExceptionMitLeeremNachrichtUndExceptionOhneNachrichtUndExceptionOhneNachricht() {
        // Arrange
        when(werkstattService.getAuftraege(null)).thenThrow(new RuntimeException());

        // Act
        Throwable thrown = catchThrowable(() -> controller.liste(null));

        // Assert
        assertThat(thrown).isInstanceOf(RuntimeException.class);
        verify(werkstattService).getAuftraege(null);
    }

    @Test
    void positionMitNullIdInStatusMitNullStatusUndExceptionMitLeeremNachrichtUndExceptionOhneNachrichtUndExceptionOhneNachrichtUndException() {
        // Arrange
        when(werkstattService.setzeStatus(0L, null)).thenThrow(new RuntimeException());

        // Act
        var response = controller.status(0L, null);

        // Assert
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody()).isNull();
        verify(werkstattService).setzeStatus(0L, null);
    }

    @Test
    void positionMitNullIdInPositionMitNullPositionUndExceptionMitLeeremNachrichtUndExceptionOhneNachrichtUndExceptionOhneNachrichtUndException() {
        // Arrange
        when(werkstattService.neuePosition(0L, null)).thenThrow(new RuntimeException());

        // Act
        var response = controller.position(0L, null);

        // Assert
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody()).isNull();
        verify(werkstattService).neuePosition(0L, null);
    }

    @Test
    void positionMitNullIdInPositionLoeschenMitNullIdUndExceptionMitLeeremNachrichtUndExceptionOhneNachrichtUndExceptionOhneNachrichtUndException() {
        // Arrange
        doThrow(new RuntimeException()).when(werkstattService).loeschePosition(0L);

        // Act
        var response = controller.positionLoeschen(0L);

        // Assert
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody()).isNull();
        verify(werkstattService).loeschePosition(0L);
    }

    @Test
    void positionMitNullIdInAnlegenMitNullAuftragUndExceptionMitLeeremNachrichtUndExceptionOhneNachrichtUndExceptionOhneNachrichtUndException() {
        // Arrange
        when(werkstattService.neuerAuftrag(null)).thenThrow(new RuntimeException());

        // Act
        var response = controller.anlegen(null);

        // Assert
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody()).isNull();
        verify(werkstattService).neuerAuftrag(null);
    }

    @Test
    void positionMitNullIdInEinzelnMitNullIdUndExceptionMitLeeremNachrichtUndExceptionOhneNachrichtUndExceptionOhneNachrichtUndException() {
        // Arrange
        when(werkstattService.getAuftrag(0L)).thenReturn(null);

        // Act
        var response = controller.einzeln(0L);

        // Assert
        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        assertThat(response.getBody()).isNull();
        verify(werkstattService).getAuftrag(0L);
    }

    @Test
    void positionMitNullIdInListeMitNullStatusUndExceptionMitLeeremNachrichtUndExceptionOhneNachrichtUndExceptionOhneNachrichtUndException() {
        // Arrange
        when(werkstattService.getAuftraege(null)).thenThrow(new RuntimeException());

        // Act
        Throwable thrown = catchThrowable(() -> controller.liste(null));

        // Assert
        assertThat(thrown).isInstanceOf(RuntimeException.class);
        verify(werkstattService).getAuftraege(null);
    }

    @Test
    void positionMitNullIdInStatusMitNullStatusUndExceptionMitLeeremNachrichtUndExceptionOhneNachrichtUndExceptionOhneNachrichtUndExceptionOhneNachricht() {
        // Arrange
        when(werkstattService.setzeStatus(0L, null)).thenThrow(new RuntimeException());

        // Act
        var response = controller.status(0L, null);

        // Assert
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody()).isNull();
        verify(werkstattService).setzeStatus(0L, null);
    }

    @Test
    void positionMitNullIdInPositionMitNullPositionUndExceptionMitLeeremNachrichtUndExceptionOhneNachrichtUndExceptionOhneNachrichtUndExceptionOhneNachricht() {
        // Arrange
        when(werkstattService.neuePosition(0L, null)).thenThrow(new RuntimeException());

        // Act
        var response = controller.position(0L, null);

        // Assert
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody()).isNull();
        verify(werkstattService).neuePosition(0L, null);
    }

    @Test
    void positionMitNullIdInPositionLoeschenMitNullIdUndExceptionMitLeeremNachrichtUndExceptionOhneNachrichtUndExceptionOhneNachrichtUndExceptionOhneNachricht() {
        // Arrange
        doThrow(new RuntimeException()).when(werkstattService).loeschePosition(0L);

        // Act
        var response = controller.positionLoeschen(0L);

        // Assert
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody()).isNull();
        verify(werkstattService).loeschePosition(0L);
    }

    @Test
    void positionMitNullIdInAnlegenMitNullAuftragUndExceptionMitLeeremNachrichtUndExceptionOhneNachrichtUndExceptionOhneNachrichtUndExceptionOhneNachricht() {
        // Arrange
        when(werkstattService.neuerAuftrag(null)).thenThrow(new RuntimeException());

        // Act
        var response = controller.anlegen(null);

        // Assert
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody()).isNull();
        verify(werkstattService).neuerAuftrag(null);
    }

    @Test
    void positionMitNullIdInEinzelnMitNullIdUndExceptionMitLeeremNachrichtUndExceptionOhneNachrichtUndExceptionOhneNachrichtUndExceptionOhneNachricht() {
        // Arrange
        when(werkstattService.getAuftrag(0L)).thenReturn(null);

        // Act
        var response = controller.einzeln(0L);

        // Assert
        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        assertThat(response.getBody()).isNull();
        verify(werkstattService).getAuftrag(0L);
    }

    @Test
    void positionMitNullIdInListeMitNullStatusUndExceptionMitLeeremNachrichtUndExceptionOhneNachrichtUndExceptionOhneNachrichtUndExceptionOhneNachricht() {
        // Arrange
        when(werkstattService.getAuftraege(null)).thenThrow(new RuntimeException());

        // Act
        Throwable thrown = catchThrowable(() -> controller.liste(null));

        // Assert
        assertThat(thrown).isInstanceOf(RuntimeException.class);
        verify(werkstattService).getAuftraege(null);
    }

    @Test
    void positionMitNullIdInStatusMitNullStatusUndExceptionMitLeeremNachrichtUndExceptionOhneNachrichtUndExceptionOhneNachrichtUndExceptionOhneNachrichtUndException() {
        // Arrange
        when(werkstattService.setzeStatus(0L, null)).thenThrow(new RuntimeException());

        // Act
        var response = controller.status(0L, null);

        // Assert
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody()).isNull();
        verify(werkstattService).setzeStatus(0L, null);
    }

    @Test
    void positionMitNullIdInPositionMitNullPositionUndExceptionMitLeeremNachrichtUndExceptionOhneNachrichtUndExceptionOhneNachrichtUndExceptionOhneNachrichtUndException() {
        // Arrange
        when(werkstattService.neuePosition(0L, null)).thenThrow(new RuntimeException());

        // Act
        var response = controller.position(0L, null);

        // Assert
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody()).isNull();
        verify(werkstattService).neuePosition(0L, null);
    }

    @Test
    void positionMitNullIdInPositionLoeschenMitNullIdUndExceptionMitLeeremNachrichtUndExceptionOhneNachrichtUndExceptionOhneNachrichtUndExceptionOhneNachrichtUndException() {
        // Arrange
        doThrow(new RuntimeException()).when(werkstattService).loeschePosition(0L);

        // Act
        var response = controller.positionLoeschen(0L);

        // Assert
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody()).isNull();
        verify(werkstattService).loeschePosition(0L);
    }

    @Test
    void positionMitNullIdInAnlegenMitNullAuftragUndExceptionMitLeeremNachrichtUndExceptionOhneNachrichtUndExceptionOhneNachrichtUndExceptionOhneNachrichtUndException() {
        // Arrange
        when(werkstattService.neuerAuftrag(null)).thenThrow(new RuntimeException());

        // Act
        var response = controller.anlegen(null);

        // Assert
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody()).isNull();
        verify(werkstattService).neuerAuftrag(null);
    }

    @Test
    void positionMitNullIdInEinzelnMitNullIdUndExceptionMitLeeremNachrichtUndExceptionOhneNachrichtUndExceptionOhneNachrichtUndExceptionOhneNachrichtUndException() {
        // Arrange
        when(werkstattService.getAuftrag(0L)).thenReturn(null);

        // Act
        var response = controller.einzeln(0L);

        // Assert
        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        assertThat(response.getBody()).isNull();
        verify(werkstattService).getAuftrag(0L);
    }

    @Test
    void positionMitNullIdInListeMitNullStatusUndExceptionMitLeeremNachrichtUndExceptionOhneNachrichtUndExceptionOhneNachrichtUndExceptionOhneNachrichtUndException() {
        // Arrange
        when(werkstattService.getAuftraege(null)).thenThrow(new RuntimeException());

        // Act
        Throwable thrown = catchThrowable(() -> controller.liste(null));

        // Assert
        assertThat(thrown).isInstanceOf(RuntimeException.class);
        verify(werkstattService).getAuftraege(null);
    }
}
