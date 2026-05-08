package com.bank.workflow.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RiskService {

  public int calculateRiskScore(String tckn) {
    log.info("RiskService: TCKN {} için skor hesaplanıyor...", tckn);
    // Gerçek dünyada burada bir KKB veya iç skorlama servisine RestClient ile gidilir.
    return (int) (Math.random() * 100);
  }
}
