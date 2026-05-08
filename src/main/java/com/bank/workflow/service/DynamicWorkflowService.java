package com.bank.workflow.service;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.builder.AbstractFlowNodeBuilder;
import org.camunda.bpm.model.bpmn.builder.SubProcessBuilder;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DynamicWorkflowService {

  private final RepositoryService repositoryService;

  public DynamicWorkflowService(RepositoryService repositoryService) {
    this.repositoryService = repositoryService;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void deployProcess() {

    try {

      AbstractFlowNodeBuilder<?, ?> builder =
          Bpmn.createExecutableProcess("MainPoolProcess")
              .name("Kredi Başvuru Havuz Orkestratörü")
              .startEvent("start")
              .serviceTask("routeTask")
              .camundaDelegateExpression("${poolRouterDelegate}")
              .exclusiveGateway("gateway");

      // =====================================================
      // KBS FLOW
      // =====================================================

      builder = builder.condition("KBS_FLOW", "${kbsRequired}");

      SubProcessBuilder kbsSubBuilder = builder.subProcess("Sub_KbsHavuzu").name("KBS Havuzu");

      builder =
          kbsSubBuilder
              .embeddedSubProcess()
              .startEvent()
              .serviceTask("kbsTask")
              .camundaDelegateExpression("${kbsDinamikKontrolDelegate}")
              .multiInstance()
              .sequential()
              .camundaCollection("${calisacakKontroller}")
              .camundaElementVariable("aktifKontrolAdi")
              .multiInstanceDone()
              .endEvent()
              .subProcessDone();

      // =====================================================
      // LKS FLOW
      // =====================================================

      builder = builder.exclusiveGateway("lksGateway");

      SubProcessBuilder lksSubBuilder =
          builder
              .condition("LKS_FLOW", "${lksRequired}")
              .subProcess("Sub_LksHavuzu")
              .name("LKS Havuzu");

      builder =
          lksSubBuilder
              .embeddedSubProcess()
              .startEvent()
              .serviceTask("lksTask")
              .camundaDelegateExpression("${lksKontrolDelegate}")
              .endEvent()
              .subProcessDone();

      // =====================================================
      // TBH FLOW
      // =====================================================

      builder = builder.exclusiveGateway("tbhGateway");

      SubProcessBuilder tbhSubBuilder =
          builder
              .condition("TBH_FLOW", "${tbhRequired}")
              .subProcess("Sub_TbhHavuzu")
              .name("TBH Havuzu");

      builder =
          tbhSubBuilder
              .embeddedSubProcess()
              .startEvent()
              .serviceTask("tbhTask")
              .camundaDelegateExpression("${tbhKontrolDelegate}")
              .endEvent()
              .subProcessDone();

      // =====================================================
      // MAIN FLOW — finishTask tüm boundary event'lerden önce tanımlanmalı
      // =====================================================

      builder =
          builder
              .serviceTask("finishTask")
              .camundaExpression("${execution.setVariable('status','DONE')}")
              .endEvent("end");

      // KBS bypass: gateway → lksGateway
      builder
          .moveToNode("gateway")
          .condition("BYPASS_FLOW", "${!kbsRequired}")
          .connectTo("lksGateway");

      // LKS bypass: lksGateway → tbhGateway
      builder
          .moveToNode("lksGateway")
          .condition("LKS_BYPASS", "${!lksRequired}")
          .connectTo("tbhGateway");

      // TBH bypass: tbhGateway → finishTask
      builder
          .moveToNode("tbhGateway")
          .condition("TBH_BYPASS", "${!tbhRequired}")
          .connectTo("finishTask");

      // =====================================================
      // BOUNDARY EVENTS — finishTask modele eklendikten sonra bağlanabilir
      // =====================================================

      // KBS: max 3 retry, sonra KBS_HATA
      kbsSubBuilder
          .boundaryEvent("Evt_KbsHata")
          .error("ERR_KBS_RESTART")
          .serviceTask("retryCountTask")
          .camundaExpression(
              "${execution.setVariable('kbsRetryCount', kbsRetryCount == null ? 1 : kbsRetryCount + 1)}")
          .exclusiveGateway("retryGateway")
          .condition("RETRY_ALLOWED", "${kbsRetryCount < 3}")
          .connectTo("Sub_KbsHavuzu")
          .moveToNode("retryGateway")
          .condition("MAX_RETRY_EXCEEDED", "${kbsRetryCount >= 3}")
          .serviceTask("kbsHataTask")
          .camundaExpression("${execution.setVariable('status', 'KBS_HATA')}")
          .connectTo("finishTask");

      // LKS: hata → LKS_HATA → finishTask
      lksSubBuilder
          .boundaryEvent("Evt_LksHata")
          .error("ERR_LKS_FAIL")
          .serviceTask("lksHataTask")
          .camundaExpression("${execution.setVariable('status', 'LKS_HATA')}")
          .connectTo("finishTask");

      // TBH: hata → TBH_HATA → finishTask
      tbhSubBuilder
          .boundaryEvent("Evt_TbhHata")
          .error("ERR_TBH_FAIL")
          .serviceTask("tbhHataTask")
          .camundaExpression("${execution.setVariable('status', 'TBH_HATA')}")
          .connectTo("finishTask");

      BpmnModelInstance modelInstance = builder.done();

      modelInstance.getModelElementById("gateway").setAttributeValue("default", "BYPASS_FLOW");
      modelInstance.getModelElementById("lksGateway").setAttributeValue("default", "LKS_BYPASS");
      modelInstance.getModelElementById("tbhGateway").setAttributeValue("default", "TBH_BYPASS");

      repositoryService
          .createDeployment()
          .name("Dynamic Deployment")
          .addModelInstance("dynamic-process.bpmn", modelInstance)
          .deploy();

      log.info("BPMN deploy edildi.");

    } catch (Exception e) {
      log.error("BPMN deploy edilemedi: {}", e.getMessage(), e);
      throw new IllegalStateException("BPMN deploy başarısız", e);
    }
  }
}
