package br.fatec.esiii.socagent.observer;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Inscreve no barramento todo observador declarado como bean.
 *
 * <p>Sem isto, um observador podia existir no contexto e nunca receber evento
 * algum: era exatamente o que acontecia com a trilha de auditoria, que subia
 * vazia porque ninguem a registrava. Centralizar o registro aqui garante que
 * acrescentar um observador novo baste declara-lo como bean.
 *
 * <p>Observadores que nao sao beans, como a janela do painel, continuam se
 * inscrevendo por conta propria.
 */
@Component
public class EventListenerRegistrar {

    private static final Logger log = LoggerFactory.getLogger(EventListenerRegistrar.class);

    public EventListenerRegistrar(AgentEventBus eventBus, List<AgentEventListener> listeners) {
        listeners.forEach(eventBus::subscribe);
        log.info("Observadores registrados no barramento: {}",
                listeners.stream().map(AgentEventListener::listenerName).toList());
    }
}
