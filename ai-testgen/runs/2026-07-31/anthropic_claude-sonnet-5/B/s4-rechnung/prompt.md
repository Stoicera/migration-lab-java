# Prompt instance — at.werkstatt.crm.model.Rechnung (corpus B, stratum S4)

Rendered mechanically by the harness from the frozen templates. Do not edit.

## System

````text
You are generating JUnit 5 unit tests for a Java 25 / Spring Boot 4.1 codebase.
Constraints:
- JUnit 5.14.x, Mockito (with mockito-junit-jupiter), AssertJ. From spring-test, only
  org.springframework.test.util.ReflectionTestUtils is available. No other libraries.
- Unit tests only: no Spring context, no MockMvc, no real database, no network, no file I/O.
- Mock all dependencies of the class under test and instantiate it as it is — the class under
  test must not be modified.
- Output exactly one complete, compilable test class in a single ```java code block,
  package at.werkstatt.crm.gen, class name RechnungGeneratedTest.
- Aim for behavioural coverage: happy paths, edge cases, error/rejection paths.
- Do not invent methods that do not exist in the provided source.
````

## User

````text
Class under test (full source):
package at.werkstatt.crm.model;

import java.util.Date;

public class Rechnung {

  private Long id;
  private String rechnungNr;
  private Long auftragId;
  private Date ausgestelltAm;
  // Betraege als double, gerundet wird beim Erstellen (siehe WerkstattService)
  private double summeNetto;
  private double ust;
  private double summeBrutto;
  private boolean bezahlt;
  private Date bezahltAm;

  // Anzeige
  private String auftragNr;
  private String kundeName;

  public Rechnung() {}

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getRechnungNr() {
    return rechnungNr;
  }

  public void setRechnungNr(String rechnungNr) {
    this.rechnungNr = rechnungNr;
  }

  public Long getAuftragId() {
    return auftragId;
  }

  public void setAuftragId(Long auftragId) {
    this.auftragId = auftragId;
  }

  public Date getAusgestelltAm() {
    return ausgestelltAm;
  }

  public void setAusgestelltAm(Date ausgestelltAm) {
    this.ausgestelltAm = ausgestelltAm;
  }

  public double getSummeNetto() {
    return summeNetto;
  }

  public void setSummeNetto(double summeNetto) {
    this.summeNetto = summeNetto;
  }

  public double getUst() {
    return ust;
  }

  public void setUst(double ust) {
    this.ust = ust;
  }

  public double getSummeBrutto() {
    return summeBrutto;
  }

  public void setSummeBrutto(double summeBrutto) {
    this.summeBrutto = summeBrutto;
  }

  public boolean isBezahlt() {
    return bezahlt;
  }

  public void setBezahlt(boolean bezahlt) {
    this.bezahlt = bezahlt;
  }

  public Date getBezahltAm() {
    return bezahltAm;
  }

  public void setBezahltAm(Date bezahltAm) {
    this.bezahltAm = bezahltAm;
  }

  public String getAuftragNr() {
    return auftragNr;
  }

  public void setAuftragNr(String auftragNr) {
    this.auftragNr = auftragNr;
  }

  public String getKundeName() {
    return kundeName;
  }

  public void setKundeName(String kundeName) {
    this.kundeName = kundeName;
  }
}

Direct dependency types visible to the class (signatures only):


Database schema excerpt referenced by the SQL in this class (DDL, if any):
(none — this class contains no SQL)

Write the test class now.
````
