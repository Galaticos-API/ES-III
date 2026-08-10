package br.fatec.esiii.socagent.ai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import br.fatec.esiii.socagent.ai.AnalystResponses.ActionResponse;
import br.fatec.esiii.socagent.ai.AnalystResponses.NextStepResponse;
import br.fatec.esiii.socagent.ai.AnalystResponses.PlanResponse;
import br.fatec.esiii.socagent.ai.AnalystResponses.VerdictResponse;
import br.fatec.esiii.socagent.domain.Alert;
import br.fatec.esiii.socagent.domain.TriageVerdict;
import br.fatec.esiii.socagent.state.Incident;
import br.fatec.esiii.socagent.strategy.CommandFactory;
import br.fatec.esiii.socagent.strategy.ProposedAction;
import br.fatec.esiii.socagent.strategy.ThreatAnalyst;

/**
 * Implementacao da porta {@link ThreatAnalyst} sobre Spring AI e Ollama.
 *
 * <p>Papel deliberadamente restrito: o modelo classifica, justifica e sugere
 * nomes de ferramenta. Ele nao executa nada e nao decide se a acao acontece —
 * isso cabe ao State, ao Command e ao aprovador humano.
 *
 * <p>Toda falha do modelo degrada para revisao humana. Em seguranca, um agente
 * que nao consegue concluir precisa escalar, nunca adivinhar.
 */
@Component
public class LlmThreatAnalyst implements ThreatAnalyst {

    private static final Logger log = LoggerFactory.getLogger(LlmThreatAnalyst.class);

    private static final String SYSTEM_PROMPT = """
            Voce e um analista de seguranca de nivel 1 em um centro de operacoes (SOC).
            Sua funcao e classificar alertas e sugerir acoes de resposta.

            Regras invioláveis:
            - Voce NAO executa acoes. Voce apenas sugere.
            - Use exclusivamente as ferramentas desta lista: %s
            - Se as evidencias forem insuficientes, classifique como NEEDS_HUMAN_REVIEW.
            - Nunca invente nomes de host, enderecos IP ou identificadores de tecnica
              que nao estejam presentes no alerta.
            - Prefira coletar evidencia antes de propor contencao.
            """.formatted(String.join(", ", CommandFactory.allowedTools()));

    private final ChatClient chatClient;

    public LlmThreatAnalyst(ChatClient.Builder builder) {
        this.chatClient = builder.defaultSystem(SYSTEM_PROMPT).build();
    }

    @Override
    public TriageVerdict classify(Incident incident) {
        try {
            VerdictResponse response = chatClient.prompt()
                    .user(u -> u.text("""
                            Classifique o incidente abaixo.

                            Incidente: {id}
                            Host afetado: {host}
                            Severidade maxima: {severity}
                            Alertas:
                            {alerts}

                            Responda com:
                            - classification: TRUE_POSITIVE, FALSE_POSITIVE ou NEEDS_HUMAN_REVIEW
                            - confidence: numero entre 0.0 e 1.0
                            - rationale: uma frase justificando
                            - techniqueIds: identificadores MITRE ATT&CK aplicaveis (ex: T1041)
                            """)
                            .param("id", incident.id())
                            .param("host", incident.affectedHost())
                            .param("severity", incident.highestSeverity().name())
                            .param("alerts", formatAlerts(incident)))
                    .call()
                    .entity(VerdictResponse.class);

            return toVerdict(response);
        } catch (RuntimeException ex) {
            log.warn("Classificacao falhou para {}: {}", incident.id(), ex.getMessage());
            return TriageVerdict.needsReview(
                    "O modelo nao conseguiu classificar o incidente: " + ex.getMessage());
        }
    }

    @Override
    public List<ProposedAction> proposePlan(Incident incident) {
        try {
            PlanResponse response = chatClient.prompt()
                    .user(u -> u.text("""
                            Proponha o plano de resposta para o incidente abaixo.

                            Host afetado: {host}
                            Alertas:
                            {alerts}

                            Liste de 1 a 4 acoes, em ordem de execucao, usando apenas as
                            ferramentas permitidas. Preencha somente os campos relevantes
                            para cada ferramenta e deixe os demais vazios.
                            """)
                            .param("host", incident.affectedHost())
                            .param("alerts", formatAlerts(incident)))
                    .call()
                    .entity(PlanResponse.class);

            if (response == null || response.actions() == null) {
                return List.of();
            }
            return response.actions().stream()
                    .map(this::toProposedAction)
                    .flatMap(Optional::stream)
                    .toList();
        } catch (RuntimeException ex) {
            log.warn("Planejamento falhou para {}: {}", incident.id(), ex.getMessage());
            return List.of();
        }
    }

    @Override
    public Optional<ProposedAction> proposeNext(Incident incident, List<String> observations) {
        try {
            NextStepResponse response = chatClient.prompt()
                    .user(u -> u.text("""
                            Voce esta investigando o incidente de forma iterativa.

                            Host afetado: {host}
                            Alertas:
                            {alerts}

                            Observacoes ja coletadas:
                            {observations}

                            Decida a proxima acao. Se a investigacao ja estiver completa,
                            responda com done = true e nenhuma acao.
                            """)
                            .param("host", incident.affectedHost())
                            .param("alerts", formatAlerts(incident))
                            .param("observations", observations.isEmpty()
                                    ? "nenhuma ainda" : String.join("\n", observations)))
                    .call()
                    .entity(NextStepResponse.class);

            if (response == null || Boolean.TRUE.equals(response.done())) {
                return Optional.empty();
            }
            return toProposedAction(response.action());
        } catch (RuntimeException ex) {
            log.warn("Passo ReAct falhou para {}: {}", incident.id(), ex.getMessage());
            return Optional.empty();
        }
    }

    private String formatAlerts(Incident incident) {
        return incident.alerts().stream()
                .map(Alert::toPromptLine)
                .reduce((a, b) -> a + "\n" + b)
                .orElse("nenhum alerta");
    }

    private TriageVerdict toVerdict(VerdictResponse response) {
        if (response == null || response.classification() == null) {
            return TriageVerdict.needsReview("Resposta do modelo veio vazia");
        }
        TriageVerdict.Classification classification;
        try {
            classification = TriageVerdict.Classification
                    .valueOf(response.classification().trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            log.warn("Classificacao desconhecida '{}', escalando para revisao humana",
                    response.classification());
            return TriageVerdict.needsReview(
                    "Classificacao nao reconhecida: " + response.classification());
        }
        return new TriageVerdict(
                classification,
                response.confidence() == null ? 0.0 : response.confidence(),
                response.rationale() == null ? "sem justificativa" : response.rationale(),
                response.techniqueIds() == null ? List.of() : response.techniqueIds());
    }

    private Optional<ProposedAction> toProposedAction(ActionResponse action) {
        if (action == null || action.tool() == null || action.tool().isBlank()) {
            return Optional.empty();
        }
        Map<String, String> arguments = new HashMap<>();
        putIfPresent(arguments, "hostname", action.hostname());
        putIfPresent(arguments, "ip_address", action.ipAddress());
        putIfPresent(arguments, "technique_id", action.techniqueId());
        putIfPresent(arguments, "reason", action.reason());
        return Optional.of(new ProposedAction(
                action.tool().trim().toLowerCase(),
                arguments,
                action.reason() == null ? "sugerido pelo modelo" : action.reason()));
    }

    private void putIfPresent(Map<String, String> target, String key, String value) {
        if (value != null && !value.isBlank()) {
            target.put(key, value.trim());
        }
    }

    /** Exposto para diagnostico na GUI. */
    public List<String> allowedTools() {
        return new ArrayList<>(CommandFactory.allowedTools());
    }
}
