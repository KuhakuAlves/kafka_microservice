package br.com.microservices.orchestrated.orchestratorservice.core.service;

import br.com.microservices.orchestrated.orchestratorservice.core.dto.Event;
import br.com.microservices.orchestrated.orchestratorservice.core.dto.History;
import br.com.microservices.orchestrated.orchestratorservice.core.enums.Etopics;
import br.com.microservices.orchestrated.orchestratorservice.core.producer.SagaOrchestratorProducer;
import br.com.microservices.orchestrated.orchestratorservice.core.saga.SagaExecutionController;
import br.com.microservices.orchestrated.orchestratorservice.core.utils.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static br.com.microservices.orchestrated.orchestratorservice.core.enums.EEventSource.ORCHESTRATOR;
import static br.com.microservices.orchestrated.orchestratorservice.core.enums.ESagaStatus.FAIL;
import static br.com.microservices.orchestrated.orchestratorservice.core.enums.ESagaStatus.SUCESS;
import static br.com.microservices.orchestrated.orchestratorservice.core.enums.Etopics.NOTIFY_ENDING;

@Slf4j
@AllArgsConstructor
@Service
public class OrchestratorService {

    private final SagaOrchestratorProducer producer;
    private final SagaExecutionController sagaExecutionController;
    private final JsonUtil jsonUtil;

    public void startSaga(Event event){
        event.setSource(ORCHESTRATOR);
        event.setStatus(SUCESS);
        var topic = getTopic(event);
        log.info("SAGA STARTED!!");
        addHistory(event,"Saga Started!" );
        sendEventToProducer(event, topic);
    }

    public void continueSaga(Event event){
        var topic = getTopic(event);
        log.info("SAGA CONTINUE FOR EVENT {}", event.getId());
        sendEventToProducer(event, topic);
    }

    public void finishSagaSuccess(Event event){
        event.setSource(ORCHESTRATOR);
        event.setStatus(SUCESS);
        log.info("SAGA FINISHED SUCCESSFULLY FOR EVENT!! {}", event.getId());
        addHistory(event,"Saga finished successfully!" );
        sendEventToProducer(event, NOTIFY_ENDING);
    }

    public void finishSagaFail(Event event){
        event.setSource(ORCHESTRATOR);
        event.setStatus(FAIL);
        log.info("SAGA FINISHED WITH ERROR FOR EVENT!! {}", event.getId());
        addHistory(event,"Saga finished with error!" );
        sendEventToProducer(event, NOTIFY_ENDING);
    }

    private Etopics getTopic(Event event){
        return sagaExecutionController.getNextTopic(event);
    }

    private void addHistory(Event event, String message) {
        var history = History
                .builder()
                .source(event.getSource())
                .status(event.getStatus())
                .message(message)
                .createAt(LocalDateTime.now())
                .build();
        event.addToHistory(history);
    }

    private void sendEventToProducer(Event event, Etopics topic){
        producer.sendEvent(jsonUtil.toJson(event), topic.getTopic() );
    }
}
