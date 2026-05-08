package com.bank.workflow.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Data;

@Entity
@Table(name = "basvuru_tarihce")
@Data
public class BasvuruTarihce {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String basvuruNo;

  @Column(nullable = false)
  private String havuzAdi;

  @Column(nullable = false)
  private LocalDateTime girisZamani;

  private LocalDateTime cikisZamani;
  private Long gecenSureMs;

  @Column(nullable = false)
  private String statu;

  @Column(length = 1000)
  private String hataDetayi;
}
