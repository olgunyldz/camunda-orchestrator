package com.bank.workflow.delegate;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Slf4j
@Component("lksKontrolDelegate")
public class LksKontrolDelegate implements JavaDelegate {
  @Override
  public void execute(DelegateExecution execution) {
    log.info(
        "LKS (Limit Kontrol Sistemi) Havuzu çalıştı. BusinessKey: {}", execution.getBusinessKey());
  }
}
