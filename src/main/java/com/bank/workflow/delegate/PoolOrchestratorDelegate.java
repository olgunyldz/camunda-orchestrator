package com.bank.workflow.delegate;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Slf4j
@Component("poolOrchestratorDelegate")
public class PoolOrchestratorDelegate implements JavaDelegate {

  private static final int KBS_MAX_RETRY = 3;

  private final PoolRouterDelegate poolRouterDelegate;
  private final KbsDinamikKontrolDelegate kbsDelegate;
  private final LksKontrolDelegate lksDelegate;
  private final TbhKontrolDelegate tbhDelegate;

  public PoolOrchestratorDelegate(
      PoolRouterDelegate poolRouterDelegate,
      KbsDinamikKontrolDelegate kbsDelegate,
      LksKontrolDelegate lksDelegate,
      TbhKontrolDelegate tbhDelegate) {
    this.poolRouterDelegate = poolRouterDelegate;
    this.kbsDelegate = kbsDelegate;
    this.lksDelegate = lksDelegate;
    this.tbhDelegate = tbhDelegate;
  }

  @Override
  public void execute(DelegateExecution execution) throws Exception {
    poolRouterDelegate.execute(execution);

    if (isRequired(execution, "kbsRequired")) {
      runKbs(execution);
    }

    if (!isHata(execution) && isRequired(execution, "lksRequired")) {
      runPool("LKS", execution, lksDelegate, "LKS_HATA");
    }

    if (!isHata(execution) && isRequired(execution, "tbhRequired")) {
      runPool("TBH", execution, tbhDelegate, "TBH_HATA");
    }

    if (!isHata(execution)) {
      execution.setVariable("status", "DONE");
    }
  }

  private void runKbs(DelegateExecution execution) throws Exception {
    List<String> kontroller = (List<String>) execution.getVariable("calisacakKontroller");
    int retryCount = 0;

    while (retryCount < KBS_MAX_RETRY) {
      try {
        for (String kontrol : kontroller) {
          execution.setVariable("aktifKontrolAdi", kontrol);
          kbsDelegate.execute(execution);
        }
        return;
      } catch (Exception e) {
        retryCount++;
        execution.setVariable("kbsRetryCount", retryCount);
        log.warn("KBS hata, deneme {}/{}: {}", retryCount, KBS_MAX_RETRY, e.getMessage());
      }
    }

    log.error("KBS max retry aşıldı. BusinessKey: {}", execution.getBusinessKey());
    execution.setVariable("status", "KBS_HATA");
  }

  private void runPool(
      String poolName, DelegateExecution execution, JavaDelegate delegate, String hataStatus)
      throws Exception {
    try {
      delegate.execute(execution);
    } catch (BpmnError e) {
      log.warn("{} havuzu başarısız. BusinessKey: {}", poolName, execution.getBusinessKey());
      execution.setVariable("status", hataStatus);
    }
  }

  private boolean isRequired(DelegateExecution execution, String variable) {
    return Boolean.TRUE.equals(execution.getVariable(variable));
  }

  private boolean isHata(DelegateExecution execution) {
    String status = (String) execution.getVariable("status");
    return status != null && status.endsWith("_HATA");
  }
}
