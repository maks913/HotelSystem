package main.coursework3.model;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class Settlements {

  private int idSettlement;
  private int idClient;
  private int idRoom;
  private java.sql.Date factOfArrival;
  private java.sql.Date factOfLeaving;
  private double totalCost;
  private String paymentStatus;

    public Settlements() {

    }

    @Override
  public String toString() {
    return "Settlements{" +
            "idSettlement=" + idSettlement +
            ", idClient=" + idClient +
            ", idRoom=" + idRoom +
            ", factOfArrival=" + factOfArrival +
            ", factOfLeaving=" + factOfLeaving +
            ", totalCost=" + totalCost +
            ", paymentStatus='" + paymentStatus + '\'' +
            '}';
  }
}
