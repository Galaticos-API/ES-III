package br.fatec.esiii.socagent.strategy;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import br.fatec.esiii.socagent.command.AgentCommand;
import br.fatec.esiii.socagent.command.BlockIpCommand;
import br.fatec.esiii.socagent.command.CollectForensicsCommand;
import br.fatec.esiii.socagent.command.ContainmentGateway;
import br.fatec.esiii.socagent.command.IsolateHostCommand;
import br.fatec.esiii.socagent.command.LookupMitreCommand;
import br.fatec.esiii.socagent.mitre.MitreRepository;

/**
 * Converte a sugestao do modelo em um comando executavel.
 *
 * <p>Este e o unico ponto do sistema onde a saida do modelo vira acao, e ele
 * opera por lista de permissao: nome de ferramenta desconhecido e descartado
 * com registro em log, nunca interpretado. Um modelo que alucine
 * {@code delete_all_logs} nao encontra tradutor.
 */
@Component
public class CommandFactory {

    private static final Logger log = LoggerFactory.getLogger(CommandFactory.class);

    private static final Set<String> ALLOWED_TOOLS =
            Set.of("isolate_host", "block_ip", "collect_forensics", "lookup_mitre");

    private final ContainmentGateway gateway;
    private final MitreRepository mitreRepository;

    public CommandFactory(ContainmentGateway gateway, MitreRepository mitreRepository) {
        this.gateway = gateway;
        this.mitreRepository = mitreRepository;
    }

    public Optional<AgentCommand> create(ProposedAction action, String defaultHostname) {
        if (action == null || !ALLOWED_TOOLS.contains(action.tool())) {
            log.warn("Acao descartada: ferramenta '{}' fora da lista de permissao",
                    action == null ? "null" : action.tool());
            return Optional.empty();
        }

        String reason = action.argumentOr("reason", action.rationale() == null
                ? "sugerido pela triagem automatizada" : action.rationale());

        return switch (action.tool()) {
            case "isolate_host" -> Optional.of(new IsolateHostCommand(
                    gateway, action.argumentOr("hostname", defaultHostname), reason));
            case "block_ip" -> {
                String ip = action.argument("ip_address");
                yield ip == null || ip.isBlank()
                        ? Optional.empty()
                        : Optional.of(new BlockIpCommand(gateway, ip, reason));
            }
            case "collect_forensics" -> Optional.of(new CollectForensicsCommand(
                    gateway, action.argumentOr("hostname", defaultHostname)));
            case "lookup_mitre" -> {
                String id = action.argument("technique_id");
                yield id == null || id.isBlank()
                        ? Optional.empty()
                        : Optional.of(new LookupMitreCommand(mitreRepository::find, id));
            }
            default -> Optional.empty();
        };
    }

    public List<AgentCommand> createAll(List<ProposedAction> actions, String defaultHostname) {
        return actions.stream()
                .map(action -> create(action, defaultHostname))
                .flatMap(Optional::stream)
                .toList();
    }

    public static Set<String> allowedTools() {
        return ALLOWED_TOOLS;
    }
}
