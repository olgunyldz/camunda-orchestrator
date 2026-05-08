package com.bank.workflow.strategy;

import org.camunda.bpm.engine.delegate.DelegateExecution;


public interface KbsKontrolStrategy {
    // Bu kontrolün DMN'den gelecek adı (Örn: "TcknKontrol")
    String getKontrolAdi();

    // Gerçek işin yapılacağı metod
    void kontroluYap(DelegateExecution execution) throws Exception;
}