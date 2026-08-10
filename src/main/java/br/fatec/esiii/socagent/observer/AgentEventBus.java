package br.fatec.esiii.socagent.observer;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Sujeito (Subject) do padrao Observer: mantem os observadores registrados e
 * difunde os eventos do agente.
 *
 * <p>Usa {@link CopyOnWriteArrayList} porque a leitura (publicacao) e muito mais
 * frequente que a escrita (registro) e porque a GUI se registra a partir da
 * Event Dispatch Thread do Swing enquanto o agente publica de outra thread.
 *
 * <p>Falha de um observador nunca interrompe a difusao para os demais: em um
 * contexto de seguranca, perder a trilha de auditoria por causa de um erro de
 * renderizacao na GUI seria inaceitavel.
 */
@Component
public class AgentEventBus {

    private static final Logger log = LoggerFactory.getLogger(AgentEventBus.class);

    private final List<AgentEventListener> listeners = new CopyOnWriteArrayList<>();

    public void subscribe(AgentEventListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void unsubscribe(AgentEventListener listener) {
        listeners.remove(listener);
    }

    public void publish(AgentEvent event) {
        for (AgentEventListener listener : listeners) {
            try {
                if (listener.supports(event)) {
                    listener.onEvent(event);
                }
            } catch (RuntimeException ex) {
                log.warn("Observador {} falhou ao tratar {}: {}",
                        listener.listenerName(), event.type(), ex.getMessage());
            }
        }
    }

    public int listenerCount() {
        return listeners.size();
    }
}
