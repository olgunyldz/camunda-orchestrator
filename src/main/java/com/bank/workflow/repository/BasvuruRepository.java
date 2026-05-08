package com.bank.workflow.repository;

import com.bank.workflow.entity.Basvuru;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BasvuruRepository extends JpaRepository<Basvuru, Long> {

  Optional<Basvuru> findByBasvuruNo(String basvuruNo);

  @Modifying
  @Query(
      "UPDATE Basvuru b SET b.toplamTekrarSayisi = b.toplamTekrarSayisi + 1 WHERE b.basvuruNo ="
          + " :basvuruNo")
  void incrementRetryCount(@Param("basvuruNo") String basvuruNo);
}
