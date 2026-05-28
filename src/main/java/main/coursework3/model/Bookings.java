package main.coursework3.model;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class Bookings{

  private int idBooking;
  private int idClient;
  private int idRoom;
  private java.sql.Date dateOfArrival;
  private java.sql.Date departureDate;
  private double depositAmount;
  private String statusArmor;

    public Bookings() {

    }

  @Override
  public String toString() {
    return "Bookings{" +
            "idBooking=" + idBooking +
            ", idClient=" + idClient +
            ", idRoom=" + idRoom +
            ", dateOfArrival=" + dateOfArrival +
            ", departureDate=" + departureDate +
            ", depositAmount=" + depositAmount +
            ", statusArmor='" + statusArmor + '\'' +
            '}';
  }
}
