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

  private String basvuruNo;
  private String havuzAdi; // KBS, LKS vs.

  private LocalDateTime girisZamani;
  private LocalDateTime cikisZamani;
  private Long gecenSureMs; // Havuzda ne kadar milisaniye harcadı?

  private String statu; // BASARILI, HATA_ALDI, RESTART_EDILDI
  private String hataDetayi;

  // Getter, Setter...
}
