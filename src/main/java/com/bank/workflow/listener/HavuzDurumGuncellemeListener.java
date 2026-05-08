package com.bank.workflow.listener;

import com.bank.workflow.entity.Basvuru;
import com.bank.workflow.repository.BasvuruRepository;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component("havuzDurumGuncellemeListener")
public class HavuzDurumGuncellemeListener implements ExecutionListener {

    private final BasvuruRepository basvuruRepository;

    public HavuzDurumGuncellemeListener(BasvuruRepository basvuruRepository) {
        this.basvuruRepository = basvuruRepository;
    }

    @Override
    public void notify(DelegateExecution execution) throws Exception {
        String basvuruNo = execution.getBusinessKey();
        String havuzAdi = (String) execution.getVariableLocal("havuzAdi");
        String eventName = execution.getEventName();

        Basvuru basvuru = basvuruRepository.findByBasvuruNo(basvuruNo)
                .orElseThrow(() -> new RuntimeException("Başvuru bulunamadı: " + basvuruNo));

        if ("start".equals(eventName)) {
            basvuru.setAnlikHavuz(havuzAdi);
            basvuru.setStatu("ISLENIYOR");
            if (basvuru.getBaslangicZamani() == null) {
                basvuru.setBaslangicZamani(LocalDateTime.now());
            }
        } else if ("end".equals(eventName)) {
            basvuru.setAnlikHavuz("BEKLEMEDE");
        }

        basvuruRepository.save(basvuru);
    }
}