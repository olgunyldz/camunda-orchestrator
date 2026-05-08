package com.bank.workflow.repository;

import com.bank.workflow.entity.BasvuruTarihce;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BasvuruTarihceRepository extends JpaRepository<BasvuruTarihce, Long> {}
