package br.fatec.esiii.socagent.observer;

/**
 * Observador do ciclo de vida do agente.
 *
 * <p>Implementacoes nao devem lancar excecoes nem bloquear: o barramento isola
 * falhas, mas um observador lento atrasa todos os demais.
 */
@FunctionalInterface
public interface AgentEventListener {

    void onEvent(AgentEvent event);

    /** Nome usado em log de diagnostico quando o observador falha. */
    default String listenerName() {
        return getClass().getSimpleName();
    }

    /** Permite que o observador ignore tipos que nao lhe interessam. */
    default boolean supports(AgentEvent event) {
        return true;
    }
}
