package com.bank.workflow.strategy;

import org.camunda.bpm.engine.delegate.DelegateExecution;

public interface KbsKontrolStrategy {

  String getKontrolAdi();

  int getOrder();

  void kontroluYap(DelegateExecution execution) throws Exception;
}
