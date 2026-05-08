package com.bank.workflow.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "basvurular")
@Data
public class Basvuru {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String basvuruNo; // Örn: BSV-2026-001
    private String musteriTipi;
    private Double tutar;

    // Anlık takip için durum kolonları
    private String anlikHavuz; // Örn: KBS, LKS, TBH
    private String statu; // Örn: ISLENIYOR, ONAYLANDI, REDDEDILDI
    private LocalDateTime baslangicZamani;
    private LocalDateTime bitisZamani;
    private Integer toplamTekrarSayisi = 0; // Toplam kaç kez retry yedi?

    // Getter, Setter ve Constructor'lar...
}