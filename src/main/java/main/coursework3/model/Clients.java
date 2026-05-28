package main.coursework3.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class Clients {

  private int idClient;
  private String pib;
  private String pasportSeria;
  private String pasportNumber;
  private String phoneNumber;
  private java.sql.Date dateOfBirth;

  public Clients() {

  }

  @Override
  public String toString() {
    return "Clients{" +
            "idClient=" + idClient +
            ", pib='" + pib + '\'' +
            ", pasportSeria='" + pasportSeria + '\'' +
            ", pasportNumber='" + pasportNumber + '\'' +
            ", phoneNumber='" + phoneNumber + '\'' +
            ", dateOfBirth=" + dateOfBirth +
            '}';
  }
}
