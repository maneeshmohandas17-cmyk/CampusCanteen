package com.canteen.campuscanteen.model;

import jakarta.persistence.*;

@Entity
@Table(name = "canteens")
public class Canteen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean active = true;

    public Canteen() {
    }

    public Canteen(String name) {
        this.name = name;
        this.active = true;
    }

    public Canteen(String name, boolean active) {
        this.name = name;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name != null ? name.trim() : null;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
