package at.werkstatt.crm.gen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import at.werkstatt.crm.controller.KundenController;
import at.werkstatt.crm.model.Fahrzeug;
import at.werkstatt.crm.model.Kunde;
import at.werkstatt.crm.service.WerkstattService;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class KundenControllerGeneratedTest {

  @Mock
  private WerkstattService werkstattService;

  private KundenController controller;

  @BeforeEach
  void setUp() {
    controller = new KundenController(werkstattService);
  }

  @Test
  void liste_ohneSuche_gibtAlleKundenZurueck() {
    List<Kunde> alle = List.of(new Kunde(), new Kunde());
    when(werkstattService.getAlleKunden()).thenReturn(alle);

    List<Kunde> result = controller.liste(null);

    assertThat(result).isSameAs(alle);
    verify(werkstattService).getAlleKunden();
    verify(werkstattService, never()).sucheKunden(anyString());
  }

  @Test
  void liste_mitLeererSuche_gibtAlleKundenZurueck() {
    List<Kunde> alle = List.of(new Kunde());
    when(werkstattService.getAlleKunden()).thenReturn(alle);

    List<Kunde> result = controller.liste("   ");

    assertThat(result).isSameAs(alle);
    verify(werkstattService).getAlleKunden();
    verify(werkstattService, never()).sucheKunden(anyString());
  }

  @Test
  void liste_mitLeerstring_gibtAlleKundenZurueck() {
    List<Kunde> alle = List.of(new Kunde());
    when(werkstattService.getAlleKunden()).thenReturn(alle);

    List<Kunde> result = controller.liste("");

    assertThat(result).isSameAs(alle);
    verify(werkstattService).getAlleKunden();
    verify(werkstattService, never()).sucheKunden(anyString());
  }

  @Test
  void liste_mitSuchbegriff_gibtGefundeneKundenZurueck() {
    List<Kunde> gefunden = List.of(new Kunde());
    when(werkstattService.sucheKunden("Mueller")).thenReturn(gefunden);

    List<Kunde> result = controller.liste("  Mueller  ");

    assertThat(result).isSameAs(gefunden);
    verify(werkstattService).sucheKunden("Mueller");
    verify(werkstattService, never()).getAlleKunden();
  }

  @Test
  void einzeln_kundeVorhanden_gibtOkMitKundeZurueck() {
    Kunde kunde = new Kunde();
    kunde.setId(5L);
    when(werkstattService.getKunde(5L)).thenReturn(kunde);

    ResponseEntity<Kunde> response = controller.einzeln(5L);

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).isSameAs(kunde);
  }

  @Test
  void einzeln_kundeNichtVorhanden_gibtNotFoundZurueck() {
    when(werkstattService.getKunde(99L)).thenReturn(null);

    ResponseEntity<Kunde> response = controller.einzeln(99L);

    assertThat(response.getStatusCode().value()).isEqualTo(404);
    assertThat(response.getBody()).isNull();
  }

  @Test
  void fahrzeuge_gibtListeZurueck() {
    List<Fahrzeug> fahrzeuge = List.of(new Fahrzeug(), new Fahrzeug());
    when(werkstattService.getFahrzeugeZuKunde(7L)).thenReturn(fahrzeuge);

    List<Fahrzeug> result = controller.fahrzeuge(7L);

    assertThat(result).isSameAs(fahrzeuge);
    verify(werkstattService).getFahrzeugeZuKunde(7L);
  }

  @Test
  void fahrzeuge_leereListe() {
    when(werkstattService.getFahrzeugeZuKunde(1L)).thenReturn(Collections.emptyList());

    List<Fahrzeug> result = controller.fahrzeuge(1L);

    assertThat(result).isEmpty();
  }

  @Test
  void anlegen_setztIdAufNullUndSpeichert() {
    Kunde eingabe = new Kunde();
    eingabe.setId(123L);
    Kunde gespeichert = new Kunde();
    gespeichert.setId(1L);
    when(werkstattService.speichereKunde(any(Kunde.class))).thenReturn(gespeichert);

    Kunde result = controller.anlegen(eingabe);

    assertThat(eingabe.getId()).isNull();
    assertThat(result).isSameAs(gespeichert);
    verify(werkstattService).speichereKunde(eingabe);
  }

  @Test
  void aendern_erfolgreich_gibtOkMitGespeichertemKundeZurueck() {
    Kunde eingabe = new Kunde();
    Kunde gespeichert = new Kunde();
    gespeichert.setId(10L);
    when(werkstattService.speichereKunde(any(Kunde.class))).thenReturn(gespeichert);

    ResponseEntity<?> response = controller.aendern(10L, eingabe);

    assertThat(eingabe.getId()).isEqualTo(10L);
    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).isSameAs(gespeichert);
    verify(werkstattService).speichereKunde(eingabe);
  }

  @Test
  void aendern_serviceWirftException_gibtStatus500MitFehlermeldungZurueck() {
    Kunde eingabe = new Kunde();
    when(werkstattService.speichereKunde(any(Kunde.class)))
        .thenThrow(new RuntimeException("Speicherfehler"));

    ResponseEntity<?> response = controller.aendern(3L, eingabe);

    assertThat(response.getStatusCode().value()).isEqualTo(500);
    assertThat(response.getBody()).isEqualTo("Speicherfehler");
    assertThat(eingabe.getId()).isEqualTo(3L);
  }

  @Test
  void loeschen_erfolgreich_gibtOkZurueck() {
    doNothing().when(werkstattService).loescheKunde(4L);

    ResponseEntity<?> response = controller.loeschen(4L);

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    verify(werkstattService).loescheKunde(4L);
  }

  @Test
  void loeschen_serviceWirftException_gibtStatus500MitFehlermeldungZurueck() {
    doThrow(new RuntimeException("Kunde referenziert Fahrzeuge"))
        .when(werkstattService).loescheKunde(8L);

    ResponseEntity<?> response = controller.loeschen(8L);

    assertThat(response.getStatusCode().value()).isEqualTo(500);
    assertThat(response.getBody()).isEqualTo("Kunde referenziert Fahrzeuge");
  }
}
