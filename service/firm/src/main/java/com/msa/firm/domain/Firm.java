package com.msa.firm.domain;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name="firms")
public class Firm {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long firmId;
    private String firmName;
    @CreationTimestamp
    private LocalDate foundedDate;
}
