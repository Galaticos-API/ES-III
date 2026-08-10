package br.fatec.esiii.socagent.observer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Observador que espelha o progresso do agente no console, util durante a
 * apresentacao e para depuracao sem abrir a GUI.
 */
@Component
public class ConsoleListener implements AgentEventListener {

    private static final Logger log = LoggerFactory.getLogger(ConsoleListener.class);

    @Override
    public void onEvent(AgentEvent event) {
        log.info("[{}] {} {}", event.type(), event.title(),
                event.detail() == null ? "" : "-- " + event.detail());
    }
}
