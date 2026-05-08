package com.bank.workflow.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AddressService {
  public boolean verifyAddress(Long addressId, int riskLevel) {
    log.info("AddressService: Adres {} kontrol ediliyor. Risk Seviyesi: {}", addressId, riskLevel);
    // Risk seviyesi 80'den büyükse MERNIS kontrolü yap, değilse sadece format kontrolü yap gibi...
    return true;
  }
}
