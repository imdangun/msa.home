package com.msa.license.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name="licenses")
public class License {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long licenseId;
    private String licenseName;
    @CreationTimestamp
    private LocalDate createdDate;
    private Long firmId;
}