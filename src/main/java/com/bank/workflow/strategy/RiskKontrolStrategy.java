package com.bank.workflow.strategy;

import com.bank.workflow.service.RiskService;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RiskKontrolStrategy implements KbsKontrolStrategy {
  private final RiskService riskService;

  @Override
  public String getKontrolAdi() {
    return "RiskKontrol";
  }

  @Override
  public int getOrder() {
    return 2;
  }

  @Override
  public void kontroluYap(DelegateExecution execution) {
    String tckn = (String) execution.getVariable("tckn");
    int score = riskService.calculateRiskScore(tckn);

    // Veriyi context'e yazıyoruz. 'kbs_' prefix'i ile havuz bazlı izole ediyoruz.
    execution.setVariable("kbs_riskScore", score);
  }
}
