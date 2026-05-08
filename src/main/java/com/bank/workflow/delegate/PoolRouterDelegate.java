package com.bank.workflow.delegate;

import com.bank.workflow.strategy.KbsKontrolStrategy;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("poolRouterDelegate")
public class PoolRouterDelegate implements JavaDelegate {

  private static final Logger log = LoggerFactory.getLogger(PoolRouterDelegate.class);

  private final Map<String, KbsKontrolStrategy> strategyMap;

  public PoolRouterDelegate(List<KbsKontrolStrategy> strategies) {
    this.strategyMap =
        strategies.stream()
            .collect(Collectors.toMap(KbsKontrolStrategy::getKontrolAdi, Function.identity()));
  }

  @Override
  public void execute(DelegateExecution execution) {
    String segment = (String) execution.getVariable("segment");

    if ("VIP".equals(segment)) {
      List<String> kontroller =
          buildSortedKontroller(Set.of("TcknKontrol", "RiskKontrol", "AdresKontrol"));
      execution.setVariable("kbsRequired", true);
      execution.setVariable("calisacakKontroller", kontroller);
      execution.setVariable("lksRequired", true);
      execution.setVariable("lksKontrolleri", List.of("LimitSorgu"));
    } else {
      List<String> kontroller = buildSortedKontroller(Set.of("TcknKontrol"));
      execution.setVariable("kbsRequired", true);
      execution.setVariable("calisacakKontroller", kontroller);
      execution.setVariable("lksRequired", false);
    }

    execution.setVariable("tbhRequired", false);

    log.info(
        "Yönlendirme tamamlandı. Segment: {}, KBS kontrolleri: {}",
        segment,
        execution.getVariable("calisacakKontroller"));
  }

  private List<String> buildSortedKontroller(Set<String> istenenkontroller) {
    return strategyMap.values().stream()
        .filter(s -> istenenkontroller.contains(s.getKontrolAdi()))
        .sorted(Comparator.comparingInt(KbsKontrolStrategy::getOrder))
        .map(KbsKontrolStrategy::getKontrolAdi)
        .collect(Collectors.toList());
  }
}
