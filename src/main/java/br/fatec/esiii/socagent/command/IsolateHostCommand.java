package br.fatec.esiii.socagent.command;

import java.time.Duration;
import java.time.Instant;

/**
 * Isola um host da rede. Acao destrutiva: exige aprovacao e e reversivel.
 */
public record IsolateHostCommand(ContainmentGateway gateway, String hostname, String reason)
        implements AgentCommand {

    @Override
    public String name() {
        return "isolate_host";
    }

    @Override
    public String description() {
        return "Isolar o host %s da rede corporativa (%s)".formatted(hostname, reason);
    }

    @Override
    public boolean requiresApproval() {
        return true;
    }

    @Override
    public boolean undoable() {
        return true;
    }

    @Override
    public CommandResult execute() {
        Instant start = Instant.now();
        String output = gateway.isolateHost(hostname, reason);
        return CommandResult.ok(output, Duration.between(start, Instant.now()));
    }

    @Override
    public CommandResult undo() {
        Instant start = Instant.now();
        String output = gateway.restoreHost(hostname);
        return CommandResult.ok(output, Duration.between(start, Instant.now()));
    }
}
