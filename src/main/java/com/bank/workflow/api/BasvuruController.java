package com.bank.workflow.api;

import com.bank.workflow.entity.Basvuru;
import com.bank.workflow.repository.BasvuruRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.Map;
import java.util.UUID;
import org.camunda.bpm.engine.RuntimeService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/basvuru")
public class BasvuruController {

  private final BasvuruRepository basvuruRepository;
  private final RuntimeService runtimeService;

  public BasvuruController(BasvuruRepository repo, RuntimeService runtimeService) {
    this.basvuruRepository = repo;
    this.runtimeService = runtimeService;
  }

  public record BasvuruRequest(
      @NotBlank String musteriTipi,
      @NotNull @Positive Double tutar,
      @NotBlank String segment,
      @NotBlank String tckn,
      @NotNull Long addressId) {}

  @PostMapping("/olustur")
  @Transactional
  public String basvuruOlustur(@Valid @RequestBody BasvuruRequest request) {
    Basvuru basvuru = new Basvuru();
    basvuru.setBasvuruNo("BSV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    basvuru.setMusteriTipi(request.musteriTipi());
    basvuru.setTutar(request.tutar());
    basvuru.setStatu("YENI_BASLADI");
    basvuru.setAnlikHavuz("YONLENDIRILIYOR");

    basvuruRepository.save(basvuru);

    runtimeService.startProcessInstanceByKey(
        "MainPoolProcess",
        basvuru.getBasvuruNo(),
        Map.of(
            "basvuruId", basvuru.getId(),
            "musteriTipi", basvuru.getMusteriTipi(),
            "segment", request.segment(),
            "tckn", request.tckn(),
            "addressId", request.addressId()));

    return "Başvuru oluşturuldu. No: " + basvuru.getBasvuruNo();
  }
}
