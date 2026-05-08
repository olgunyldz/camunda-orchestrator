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

      /*
       * =====================================================
       * KBS FLOW
       * =====================================================
       */

      builder = builder.condition("KBS_FLOW", "${kbsRequired}");

      /*
       * subprocess builder ayrı tutulmalı
       */
      SubProcessBuilder subProcessBuilder = builder.subProcess("Sub_KbsHavuzu").name("KBS Havuzu");

      /*
       * subprocess iç akışı
       */
      builder =
          subProcessBuilder
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

      /*
       * subprocess boundary event — max 3 deneme, sonra hata akışına geçer
       */
      subProcessBuilder
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

      /*
       * =====================================================
       * MAIN FLOW
       * =====================================================
       */

      builder =
          builder
              .serviceTask("finishTask")
              .camundaExpression("${execution.setVariable('status','DONE')}")
              .endEvent("end");

      /*
       * bypass flow
       */
      builder
          .moveToNode("gateway")
          .condition("BYPASS_FLOW", "${!kbsRequired}")
          .connectTo("finishTask");

      BpmnModelInstance modelInstance = builder.done();

      /*
       * default flow
       */
      modelInstance.getModelElementById("gateway").setAttributeValue("default", "BYPASS_FLOW");

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
