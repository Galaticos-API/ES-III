package br.fatec.esiii.socagent.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Alerta bruto recebido de uma fonte de deteccao (Suricata, Zeek, EDR, regra Sigma).
 * E imutavel: o agente nunca altera o dado original, apenas o correlaciona.
 */
public record Alert(
        String id,
        String source,
        String hostname,
        String message,
        Severity severity,
        Instant detectedAt,
        List<Ioc> indicators) {

    public Alert {
        Objects.requireNonNull(id, "id nao pode ser nulo");
        Objects.requireNonNull(severity, "severity nao pode ser nula");
        Objects.requireNonNull(detectedAt, "detectedAt nao pode ser nulo");
        indicators = indicators == null ? List.of() : List.copyOf(indicators);
    }

    /** Representacao compacta usada no prompt enviado ao modelo. */
    public String toPromptLine() {
        String iocs = indicators.isEmpty()
                ? "nenhum"
                : indicators.stream().map(Ioc::toString).reduce((a, b) -> a + ", " + b).orElse("nenhum");
        return "[%s] severidade=%s host=%s fonte=%s | %s | IOCs: %s"
                .formatted(id, severity, hostname, source, message, iocs);
    }
}
