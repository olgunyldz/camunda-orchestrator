package com.bank.workflow.strategy;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TcknKontrolStrategy implements KbsKontrolStrategy {

  @Override
  public String getKontrolAdi() {
    return "TcknKontrol";
  }

  @Override
  public int getOrder() {
    return 1;
  }

  @Override
  public void kontroluYap(DelegateExecution execution) throws Exception {
    log.info("TCKN Kontrolü yapılıyor...");
    // API çağrısı ve mantık burada...
  }
} // Eksik parantez eklendi
