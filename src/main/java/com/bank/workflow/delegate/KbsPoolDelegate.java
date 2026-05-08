package com.bank.workflow.delegate;

import com.bank.workflow.annotation.CamundaPoolStep;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Slf4j
@Component("kbsPoolDelegate")
@CamundaPoolStep(poolName = "KBS")
@RequiredArgsConstructor
public class KbsPoolDelegate implements JavaDelegate {

  private final KbsDinamikKontrolDelegate kbsDinamikKontrolDelegate;

  @Override
  public void execute(DelegateExecution execution) throws Exception {
    if (!Boolean.TRUE.equals(execution.getVariable("kbsRequired"))) {
      return;
    }

    String basvuruNo = execution.getBusinessKey();
    log.info("KBS Havuzu çalışıyor. BusinessKey: {}", basvuruNo);

    @SuppressWarnings("unchecked")
    List<String> kontroller = (List<String>) execution.getVariable("calisacakKontroller");

    for (String kontrol : kontroller) {
      execution.setVariable("aktifKontrolAdi", kontrol);
      kbsDinamikKontrolDelegate.execute(execution);
    }

    log.info("KBS Havuzu tamamlandı. BusinessKey: {}", basvuruNo);
  }
}
