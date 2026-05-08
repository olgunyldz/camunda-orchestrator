package com.bank.workflow.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("lksKontrolDelegate")
public class LksKontrolDelegate implements JavaDelegate {
    @Override
    public void execute(DelegateExecution execution) {
        System.out.println("--- LKS (Limit Kontrol Sistemi) Havuzu Çalıştı ---");
    }
}