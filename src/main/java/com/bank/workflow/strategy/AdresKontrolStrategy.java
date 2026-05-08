package com.bank.workflow.strategy;

import com.bank.workflow.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdresKontrolStrategy implements KbsKontrolStrategy {

  private final AddressService addressService;

  @Override
  public String getKontrolAdi() {
    return "AdresKontrol";
  }

  @Override
  public int getOrder() {
    return 3;
  }

  @Override
  public void kontroluYap(DelegateExecution execution) {
    Integer riskScore = (Integer) execution.getVariable("kbs_riskScore");

    if (riskScore == null) {
      throw new RuntimeException("Kritik Hata: RiskKontrol verisi bulunamadı!");
    }

    Long addrId = (Long) execution.getVariable("addressId");
    addressService.verifyAddress(addrId, riskScore);
  }
}
