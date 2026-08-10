package br.fatec.esiii.socagent.command;

import java.time.Duration;
import java.time.Instant;

/**
 * Resultado da execucao de um comando. Alimenta a trilha de auditoria e serve
 * de observacao devolvida ao modelo no ciclo do agente.
 */
public record CommandResult(
        boolean success,
        String output,
        Instant executedAt,
        Duration elapsed) {

    public static CommandResult ok(String output, Duration elapsed) {
        return new CommandResult(true, output, Instant.now(), elapsed);
    }

    public static CommandResult failure(String reason, Duration elapsed) {
        return new CommandResult(false, reason, Instant.now(), elapsed);
    }

    public static CommandResult refused(String reason) {
        return new CommandResult(false, reason, Instant.now(), Duration.ZERO);
    }
}
