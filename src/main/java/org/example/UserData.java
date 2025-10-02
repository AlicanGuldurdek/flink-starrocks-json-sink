package org.example;

import java.io.Serializable;

public class UserData implements Serializable {
    public String name;
    public String surname;
    public String card;

    public UserData() {}

    public UserData(String name, String surname, String card) {
        this.name = name;
        this.surname = surname;
        this.card = card;
    }
}