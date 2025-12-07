package com.msa.license.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name="licenses")
public class License {
    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="license_seq")
    @SequenceGenerator(name="license_seq", sequenceName="license_seq", allocationSize=1)
    private Long licenseId;
    private String licenseName;
    private LocalDate createdDate;

    @PrePersist
    protected void onCreate() {
        createdDate = LocalDate.now();
    }
}