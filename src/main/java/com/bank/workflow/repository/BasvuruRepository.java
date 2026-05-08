package com.bank.workflow.repository;

import com.bank.workflow.entity.Basvuru;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface BasvuruRepository extends JpaRepository<Basvuru, Long> {
    // Camunda'dan gelen Business Key ile kaydı bulmak için kritik metot
    Optional<Basvuru> findByBasvuruNo(String basvuruNo);
}