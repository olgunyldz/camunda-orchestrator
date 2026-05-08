package com.bank.workflow.api;

import com.bank.workflow.entity.Basvuru;
import com.bank.workflow.repository.BasvuruRepository;
import org.camunda.bpm.engine.RuntimeService;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/basvuru")
public class BasvuruController {

    private final BasvuruRepository basvuruRepository;
    private final RuntimeService runtimeService;

    public BasvuruController(BasvuruRepository repo, RuntimeService runtimeService) {
        this.basvuruRepository = repo;
        this.runtimeService = runtimeService;
    }

    public record BasvuruRequest(String musteriTipi, Double tutar, String segment, String tckn, Long addressId) {}

    @PostMapping("/olustur")
    public String basvuruOlustur(@RequestBody BasvuruRequest request) {
        // 1. Kendi DB'mize kaydet (Single Source of Truth)
        Basvuru basvuru = new Basvuru();
        basvuru.setBasvuruNo("BSV-" + UUID.randomUUID().toString().substring(0,8).toUpperCase());
        basvuru.setMusteriTipi(request.musteriTipi());
        basvuru.setTutar(request.tutar());
        basvuru.setStatu("YENI_BASLADI");
        basvuru.setAnlikHavuz("YONLENDIRILIYOR");

        basvuruRepository.save(basvuru);

        // 2. Camunda'yı Business Key ile başlat
        runtimeService.startProcessInstanceByKey(
                "MainPoolProcess",
                basvuru.getBasvuruNo(), // Business Key: DB ve BPMN arasındaki bağ
                Map.of(
                        "basvuruId", basvuru.getId(),
                        "musteriTipi", basvuru.getMusteriTipi(),
                        "segment", request.segment(),
                        "tckn", request.tckn(),
                        "addressId", request.addressId()
                )
        );

        return "Başvuru oluşturuldu. No: " + basvuru.getBasvuruNo();
    }
}