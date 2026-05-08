package com.bank.workflow.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("tbhKontrolDelegate")
public class TbhKontrolDelegate implements JavaDelegate {
  @Override
  public void execute(DelegateExecution execution) {
    System.out.println("--- TBH (Tahsisat) Havuzu Çalıştı ---");
  }
}
