package br.fatec.esiii.socagent.command;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Function;

import br.fatec.esiii.socagent.domain.MitreTechnique;

/**
 * Consulta uma tecnica MITRE ATT&CK na base local. Comando somente de leitura.
 */
public record LookupMitreCommand(Function<String, MitreTechnique> repository, String techniqueId)
        implements AgentCommand {

    @Override
    public String name() {
        return "lookup_mitre";
    }

    @Override
    public String description() {
        return "Consultar a tecnica MITRE %s".formatted(techniqueId);
    }

    @Override
    public CommandResult execute() {
        Instant start = Instant.now();
        MitreTechnique technique = repository.apply(techniqueId);
        return CommandResult.ok(
                "%s | %s".formatted(technique, technique.description()),
                Duration.between(start, Instant.now()));
    }
}
