package br.fatec.esiii.socagent.command;

import java.time.Duration;
import java.time.Instant;

/**
 * Coleta artefatos forenses do host. Nao altera o ativo, portanto dispensa
 * aprovacao e nao possui acao compensatoria.
 */
public record CollectForensicsCommand(ContainmentGateway gateway, String hostname)
        implements AgentCommand {

    @Override
    public String name() {
        return "collect_forensics";
    }

    @Override
    public String description() {
        return "Coletar evidencias forenses do host %s".formatted(hostname);
    }

    @Override
    public CommandResult execute() {
        Instant start = Instant.now();
        String output = gateway.collectForensics(hostname);
        return CommandResult.ok(output, Duration.between(start, Instant.now()));
    }
}
