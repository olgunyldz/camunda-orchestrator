package com.bank.workflow.delegate;

import com.bank.workflow.annotation.CamundaPoolStep;
import com.bank.workflow.strategy.KbsKontrolStrategy;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("kbsDinamikKontrolDelegate")
@CamundaPoolStep(poolName = "KBS") // Aspect bu sınıfı izleyecek
public class KbsDinamikKontrolDelegate implements JavaDelegate {

  private final Map<String, KbsKontrolStrategy> strategies;

  // Spring tüm implementasyonları otomatik enjekte eder
  public KbsDinamikKontrolDelegate(List<KbsKontrolStrategy> strategyList) {
    this.strategies =
        strategyList.stream()
            .collect(Collectors.toMap(KbsKontrolStrategy::getKontrolAdi, Function.identity()));
  }

  @Override
  public void execute(DelegateExecution execution) throws Exception {
    // 1. DMN'den veya Multi-instance koleksiyonundan gelen aktif adım adını al
    String targetStep = (String) execution.getVariable("aktifKontrolAdi");

    // 2. İlgili stratejiyi bul ve çalıştır
    KbsKontrolStrategy strategy =
        Optional.ofNullable(strategies.get(targetStep))
            .orElseThrow(() -> new RuntimeException("Strateji bulunamadı: " + targetStep));

    // Aspect buradaki çağrıyı çevreler ve hata/süre yönetimini yapar
    strategy.kontroluYap(execution);
  }
}
