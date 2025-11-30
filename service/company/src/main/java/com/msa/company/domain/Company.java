package com.msa.company.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name="companies")
public class Company {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long companyId;
    private String companyName;
    @CreationTimestamp
    private LocalDate createdDate;

    @ElementCollection
    @CollectionTable(name="company_license", joinColumns=@JoinColumn(name="company_id"))
    @Column(name="license_id")
    private Set<Long> licenseIds = new HashSet<>();
}