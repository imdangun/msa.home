package com.msa.license.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenerationTime;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name="licenses")
public class License {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column
    private Long licenseId;

    @Column(length=30)
    private String licenseName;

    @Column
    private LocalDate createdDate;
}