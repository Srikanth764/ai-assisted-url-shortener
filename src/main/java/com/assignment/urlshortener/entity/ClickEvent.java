package com.assignment.urlshortener.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "click_events")
public class ClickEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String shortCode;

    @Column(nullable = false)
    private Instant clickedAt;

    @Column(nullable = false)
    private String country;

    @Column(nullable = false)
    private String browser;

    @Column
    private String device;

    @Column
    private String operatingSystem;

    @Column
    private String visitorKey;

    protected ClickEvent() {
        // required by JPA
    }

    public ClickEvent(String shortCode, Instant clickedAt, String country, String browser) {
        this(shortCode, clickedAt, country, browser, "Unknown", "Unknown", "Unknown");
    }

    public ClickEvent(String shortCode, Instant clickedAt, String country, String browser,
                      String device, String operatingSystem, String visitorKey) {
        this.shortCode = shortCode;
        this.clickedAt = clickedAt;
        this.country = country;
        this.browser = browser;
        this.device = device;
        this.operatingSystem = operatingSystem;
        this.visitorKey = visitorKey;
    }

    public Long getId() {
        return id;
    }

    public String getShortCode() {
        return shortCode;
    }

    public Instant getClickedAt() {
        return clickedAt;
    }

    public String getCountry() {
        return country;
    }

    public String getBrowser() {
        return browser;
    }

    public String getDevice() {
        return device;
    }

    public String getOperatingSystem() {
        return operatingSystem;
    }

    public String getVisitorKey() {
        return visitorKey;
    }
}
