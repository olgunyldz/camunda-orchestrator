package com.bank.workflow.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("poolRouterDelegate")
public class PoolRouterDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(PoolRouterDelegate.class);

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String musteriTipi = (String) execution.getVariable("musteriTipi");
        // İş Kuralları: Müşteri segmentine göre havuzları ve içindeki adımları belirle
        String segment = (String) execution.getVariable("segment");

        if ("VIP".equals(segment)) {
            execution.setVariable("kbsRequired", true);
            execution.setVariable("calisacakKontroller", List.of("TcknKontrol", "AdresKontrol"));

            execution.setVariable("lksRequired", true);
            execution.setVariable("lksKontrolleri", List.of("LimitSorgu"));
        } else {
            execution.setVariable("kbsRequired", true);
            execution.setVariable("calisacakKontroller", List.of("TcknKontrol"));
            execution.setVariable("lksRequired", false);
        }

        execution.setVariable("tbhRequired", false); // Örnek olarak kapalı
    }
}