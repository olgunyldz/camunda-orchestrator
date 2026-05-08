package com.bank.workflow.delegate;

import com.bank.workflow.annotation.CamundaPoolStep;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Slf4j
@Component("lksKontrolDelegate")
@CamundaPoolStep(poolName = "LKS")
public class LksKontrolDelegate implements JavaDelegate {

  @Override
  public void execute(DelegateExecution execution) {
    if (!Boolean.TRUE.equals(execution.getVariable("lksRequired"))) {
      return;
    }

    String basvuruNo = execution.getBusinessKey();
    log.info("LKS Havuzu çalışıyor. BusinessKey: {}", basvuruNo);

    boolean basarili = Math.random() > 0.5;

    if (!basarili) {
      log.warn("LKS kontrolü başarısız. BusinessKey: {}", basvuruNo);
      throw new RuntimeException("LKS limit kontrolü başarısız");
    }

    execution.setVariable("lksOnaylandi", true);
    log.info("LKS kontrolü başarılı. BusinessKey: {}", basvuruNo);
  }
}
