package main.coursework3.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class Appliances {
    private int idAppliance;
    private String name;
    private String technicalCondition;

    @Override
    public String toString() {
        return "Ім'я: " + name + " Технічний стан: " + technicalCondition;
    }
}
