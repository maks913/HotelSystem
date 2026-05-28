package main.coursework3.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
@AllArgsConstructor
public class Rooms {
  private int idRoom;
  private String roomNumber;
  private int floor;
  private String roomClass;
  private double costPerDay;
  private int capacity;
  private String status;

  public Rooms() {

  }

  public Rooms(int idRoom, String roomNumber, int floor, String status, double price, int capacity) {
  }

  @Override
  public String toString() {
    return "Кімната: "  + roomNumber +
            " Класс: " + roomClass;
  }
}
