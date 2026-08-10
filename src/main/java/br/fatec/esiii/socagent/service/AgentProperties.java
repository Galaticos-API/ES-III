package br.fatec.esiii.socagent.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuracao do agente.
 *
 * @param planner           id da estrategia ativa na inicializacao
 * @param minimumConfidence confianca minima para considerar um veredito acionavel
 * @param defaultApprover   identificacao usada quando a aprovacao vem da GUI
 */
@ConfigurationProperties(prefix = "soc-agent")
public record AgentProperties(
        String planner,
        double minimumConfidence,
        String defaultApprover) {

    public AgentProperties {
        planner = planner == null || planner.isBlank() ? "human-in-the-loop" : planner;
        minimumConfidence = minimumConfidence <= 0 ? 0.6 : minimumConfidence;
        defaultApprover = defaultApprover == null || defaultApprover.isBlank()
                ? "analista.local" : defaultApprover;
    }
}
