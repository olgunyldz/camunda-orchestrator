package com.bank.workflow.delegate;

import com.bank.workflow.annotation.CamundaPoolStep;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Slf4j
@Component("tbhKontrolDelegate")
@CamundaPoolStep(poolName = "TBH")
public class TbhKontrolDelegate implements JavaDelegate {

  @Override
  public void execute(DelegateExecution execution) {
    if (!Boolean.TRUE.equals(execution.getVariable("tbhRequired"))) {
      return;
    }

    String basvuruNo = execution.getBusinessKey();
    log.info("TBH Havuzu çalışıyor. BusinessKey: {}", basvuruNo);

    boolean basarili = Math.random() > 0.5;

    if (!basarili) {
      log.warn("TBH kontrolü başarısız. BusinessKey: {}", basvuruNo);
      throw new RuntimeException("TBH tahsisat kontrolü başarısız");
    }

    execution.setVariable("tbhOnaylandi", true);
    log.info("TBH kontrolü başarılı. BusinessKey: {}", basvuruNo);
  }
}
