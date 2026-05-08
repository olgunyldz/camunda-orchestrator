package com.bank.workflow.aspect;

import com.bank.workflow.annotation.CamundaPoolStep;
import com.bank.workflow.entity.BasvuruTarihce;
import com.bank.workflow.repository.BasvuruRepository;
import com.bank.workflow.repository.BasvuruTarihceRepository;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Aspect
@Component
public class DelegateLoggingAspect {

  private final BasvuruRepository basvuruRepository;
  private final BasvuruTarihceRepository tarihceRepository;

  public DelegateLoggingAspect(BasvuruRepository repo, BasvuruTarihceRepository tarihceRepo) {
    this.basvuruRepository = repo;
    this.tarihceRepository = tarihceRepo;
  }

  @Around(
      "execution(* org.camunda.bpm.engine.delegate.JavaDelegate.execute(..)) && @within(camundaPoolStep)")
  public Object profileDelegate(ProceedingJoinPoint joinPoint, CamundaPoolStep camundaPoolStep)
      throws Throwable {

    Object[] args = joinPoint.getArgs();
    if (args.length == 0 || !(args[0] instanceof DelegateExecution execution)) {
      return joinPoint.proceed();
    }

    String basvuruNo = execution.getBusinessKey();
    if (basvuruNo == null) {
      log.warn(
          "Business key bulunamadı, delegate: {}", joinPoint.getSignature().getDeclaringTypeName());
      return joinPoint.proceed();
    }

    String stepName = (String) execution.getVariable("aktifKontrolAdi");
    String poolAndStep = camundaPoolStep.poolName() + (stepName != null ? " - " + stepName : "");

    long start = System.currentTimeMillis();

    try {
      Object result = joinPoint.proceed();
      logTarihce(basvuruNo, poolAndStep, "SUCCESS", System.currentTimeMillis() - start, null);
      return result;
    } catch (BpmnError be) {
      logTarihce(
          basvuruNo,
          poolAndStep,
          "BUSINESS_ERROR",
          System.currentTimeMillis() - start,
          be.getMessage());
      throw be;
    } catch (Throwable ex) {
      incrementRetryCount(basvuruNo);
      logTarihce(
          basvuruNo,
          poolAndStep,
          "TECHNICAL_ERROR",
          System.currentTimeMillis() - start,
          ex.getMessage());
      throw ex;
    }
  }

  @Transactional
  protected void incrementRetryCount(String basvuruNo) {
    basvuruRepository.incrementRetryCount(basvuruNo);
  }

  private void logTarihce(String bNo, String poolStep, String status, long duration, String error) {
    BasvuruTarihce t = new BasvuruTarihce();
    t.setBasvuruNo(bNo);
    t.setHavuzAdi(poolStep);
    t.setStatu(status);
    t.setGirisZamani(LocalDateTime.now());
    t.setGecenSureMs(duration);
    t.setHataDetayi(error);
    tarihceRepository.save(t);
  }
}
