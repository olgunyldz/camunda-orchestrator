package com.bank.workflow.aspect;

import com.bank.workflow.annotation.CamundaPoolStep;
import com.bank.workflow.entity.BasvuruTarihce;
import com.bank.workflow.repository.BasvuruRepository;
import com.bank.workflow.repository.BasvuruTarihceRepository;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Aspect
@Component
public class DelegateLoggingAspect {

    private final BasvuruRepository basvuruRepository;
    private final BasvuruTarihceRepository tarihceRepository;

    public DelegateLoggingAspect(BasvuruRepository repo, BasvuruTarihceRepository tarihceRepo) {
        this.basvuruRepository = repo;
        this.tarihceRepository = tarihceRepo;
    }

    @Around("execution(* org.camunda.bpm.engine.delegate.JavaDelegate.execute(..)) && @within(camundaPoolStep)")
    public Object profileDelegate(ProceedingJoinPoint joinPoint, CamundaPoolStep camundaPoolStep) throws Throwable {
        DelegateExecution execution = (DelegateExecution) joinPoint.getArgs()[0];
        String basvuruNo = execution.getBusinessKey();

        // Strateji adını al (Örn: TcknKontrol)
        String stepName = (String) execution.getVariable("aktifKontrolAdi");
        String poolAndStep = camundaPoolStep.poolName() + (stepName != null ? " - " + stepName : "");

        long start = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            logTarihce(basvuruNo, poolAndStep, "SUCCESS", System.currentTimeMillis() - start, null);
            return result;
        } catch (BpmnError be) {
            logTarihce(basvuruNo, poolAndStep, "BUSINESS_ERROR", System.currentTimeMillis() - start, be.getMessage());
            throw be;
        } catch (Throwable ex) {
            updateRetryCount(basvuruNo);
            logTarihce(basvuruNo, poolAndStep, "TECHNICAL_ERROR", System.currentTimeMillis() - start, ex.getMessage());
            throw ex;
        }
    }

    private void updateRetryCount(String basvuruNo) {
        basvuruRepository.findByBasvuruNo(basvuruNo).ifPresent(b -> {
            b.setToplamTekrarSayisi(b.getToplamTekrarSayisi() + 1);
            basvuruRepository.save(b);
        });
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