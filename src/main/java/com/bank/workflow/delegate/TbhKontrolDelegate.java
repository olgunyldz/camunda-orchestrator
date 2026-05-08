package com.bank.workflow.delegate;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Slf4j
@Component("tbhKontrolDelegate")
public class TbhKontrolDelegate implements JavaDelegate {
  @Override
  public void execute(DelegateExecution execution) {
    log.info("TBH (Tahsisat) Havuzu çalıştı. BusinessKey: {}", execution.getBusinessKey());
  }
}
