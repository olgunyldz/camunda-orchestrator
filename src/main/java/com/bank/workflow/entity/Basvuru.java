package com.bank.workflow.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Data;

@Entity
@Table(name = "basvurular")
@Data
public class Basvuru {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(unique = true, nullable = false)
  private String basvuruNo;

  @Column(nullable = false)
  private String musteriTipi;

  @Column(nullable = false)
  private Double tutar;

  private String anlikHavuz;
  private String statu;
  private LocalDateTime baslangicZamani;
  private LocalDateTime bitisZamani;
  private Integer toplamTekrarSayisi = 0;
}
