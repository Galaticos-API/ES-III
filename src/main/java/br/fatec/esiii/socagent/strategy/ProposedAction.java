package br.fatec.esiii.socagent.strategy;

import java.util.Map;
import java.util.Objects;

/**
 * Acao sugerida pelo modelo de linguagem.
 *
 * <p>E deliberadamente um dado inerte: nome de ferramenta e argumentos, sem
 * qualquer capacidade de execucao. Somente a {@code CommandFactory} converte
 * isso em um {@link br.fatec.esiii.socagent.command.AgentCommand}, e apenas
 * para nomes previstos na lista de permissao.
 */
public record ProposedAction(String tool, Map<String, String> arguments, String rationale) {

    public ProposedAction {
        Objects.requireNonNull(tool, "tool nao pode ser nulo");
        arguments = arguments == null ? Map.of() : Map.copyOf(arguments);
    }

    public String argument(String key) {
        return arguments.get(key);
    }

    public String argumentOr(String key, String fallback) {
        return arguments.getOrDefault(key, fallback);
    }
}
