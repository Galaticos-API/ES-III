package br.fatec.esiii.socagent.strategy;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.fatec.esiii.socagent.command.SimulatedContainmentGateway;
import br.fatec.esiii.socagent.domain.Alert;
import br.fatec.esiii.socagent.domain.Ioc;
import br.fatec.esiii.socagent.domain.Severity;
import br.fatec.esiii.socagent.domain.TriageVerdict;
import br.fatec.esiii.socagent.mitre.MitreRepository;
import br.fatec.esiii.socagent.observer.AgentEventBus;
import br.fatec.esiii.socagent.state.Incident;

class TriagePlannerTest {

    private CommandFactory commandFactory;

    @BeforeEach
    void setUp() {
        commandFactory = new CommandFactory(new SimulatedContainmentGateway(), new MitreRepository());
    }

    private Incident incidente() {
        Alert alerta = new Alert("ALR-1", "suricata", "WKS-4471",
                "Trafego de exfiltracao", Severity.CRITICAL, Instant.now(),
                List.of(Ioc.ip("185.220.101.7")));
        return new Incident("INC-1", List.of(alerta), new AgentEventBus());
    }

    /** Analista falso e deterministico: isola os planejadores do modelo real. */
    private static class AnalistaFalso implements ThreatAnalyst {
        private final TriageVerdict verdict;
        private final List<ProposedAction> plano;
        private final List<ProposedAction> passos;
        private int indice;

        AnalistaFalso(TriageVerdict verdict, List<ProposedAction> plano, List<ProposedAction> passos) {
            this.verdict = verdict;
            this.plano = plano;
            this.passos = new ArrayList<>(passos);
        }

        @Override
        public TriageVerdict classify(Incident incident) {
            return verdict;
        }

        @Override
        public List<ProposedAction> proposePlan(Incident incident) {
            return plano;
        }

        @Override
        public Optional<ProposedAction> proposeNext(Incident incident, List<String> observations) {
            return indice < passos.size() ? Optional.of(passos.get(indice++)) : Optional.empty();
        }
    }

    private TriageVerdict positivo() {
        return new TriageVerdict(TriageVerdict.Classification.TRUE_POSITIVE, 0.9,
                "Exfiltracao confirmada", List.of("T1041"));
    }

    @Test
    @DisplayName("plan-then-execute traduz o plano inteiro e exige aprovacao se houver acao destrutiva")
    void planoCompleto() {
        var analista = new AnalistaFalso(positivo(), List.of(
                new ProposedAction("collect_forensics", Map.of("hostname", "WKS-4471"), "preservar evidencia"),
                new ProposedAction("isolate_host", Map.of("hostname", "WKS-4471"), "conter exfiltracao")),
                List.of());

        var plano = new PlanThenExecutePlanner(analista, commandFactory).plan(incidente());

        assertThat(plano.commands()).hasSize(2);
        assertThat(plano.requiresApproval()).isTrue();
    }

    @Test
    @DisplayName("falso positivo nao gera nenhum comando")
    void falsoPositivoNaoAge() {
        var analista = new AnalistaFalso(
                new TriageVerdict(TriageVerdict.Classification.FALSE_POSITIVE, 0.95,
                        "Backup legitimo", List.of()),
                List.of(new ProposedAction("isolate_host", Map.of(), "nao deveria acontecer")),
                List.of());

        var plano = new PlanThenExecutePlanner(analista, commandFactory).plan(incidente());

        assertThat(plano.commands()).isEmpty();
        assertThat(plano.requiresApproval()).isFalse();
    }

    @Test
    @DisplayName("ferramenta fora da lista de permissao e descartada")
    void ferramentaNaoPermitida() {
        var analista = new AnalistaFalso(positivo(), List.of(
                new ProposedAction("delete_all_logs", Map.of(), "acao alucinada pelo modelo"),
                new ProposedAction("collect_forensics", Map.of("hostname", "WKS-4471"), "legitima")),
                List.of());

        var plano = new PlanThenExecutePlanner(analista, commandFactory).plan(incidente());

        assertThat(plano.commands()).hasSize(1);
        assertThat(plano.commands().getFirst().name()).isEqualTo("collect_forensics");
    }

    @Test
    @DisplayName("ReAct executa leitura no ciclo e acumula contencao para aprovacao")
    void reactSeparaLeituraDeContencao() {
        var analista = new AnalistaFalso(positivo(), List.of(), List.of(
                new ProposedAction("lookup_mitre", Map.of("technique_id", "T1041"), "entender a tecnica"),
                new ProposedAction("collect_forensics", Map.of("hostname", "WKS-4471"), "coletar"),
                new ProposedAction("isolate_host", Map.of("hostname", "WKS-4471"), "conter")));

        var plano = new ReActPlanner(analista, commandFactory).plan(incidente());

        assertThat(plano.commands()).hasSize(1);
        assertThat(plano.commands().getFirst().name()).isEqualTo("isolate_host");
        assertThat(plano.requiresApproval()).isTrue();
    }

    @Test
    @DisplayName("human-in-the-loop forca aprovacao mesmo para plano so de leitura")
    void humanoNoCircuitoForcaAprovacao() {
        var analista = new AnalistaFalso(positivo(), List.of(
                new ProposedAction("collect_forensics", Map.of("hostname", "WKS-4471"), "coletar")),
                List.of());
        var delegado = new PlanThenExecutePlanner(analista, commandFactory);

        var planoDireto = delegado.plan(incidente());
        assertThat(planoDireto.requiresApproval()).isFalse();

        var planoComGovernanca = new HumanInTheLoopPlanner(delegado).plan(incidente());
        assertThat(planoComGovernanca.requiresApproval()).isTrue();
        assertThat(planoComGovernanca.commands()).hasSize(1);
    }
}
