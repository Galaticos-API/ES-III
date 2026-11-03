package br.fatec.esiii.socagent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.fatec.esiii.socagent.command.*;
import br.fatec.esiii.socagent.domain.*;
import br.fatec.esiii.socagent.observer.AgentEventBus;
import br.fatec.esiii.socagent.state.Incident;
import br.fatec.esiii.socagent.state.IncidentPhase;
import br.fatec.esiii.socagent.strategy.TriagePlanner;

class IncidentTriageServiceTest {
    private final AgentEventBus bus = new AgentEventBus();
    private final CommandInvoker invoker = new CommandInvoker(bus);
    private final SimulatedContainmentGateway gateway = new SimulatedContainmentGateway();
    private final TriagePlanner planner = mock(TriagePlanner.class);
    private IncidentTriageService service;

    @BeforeEach
    void setup() {
        var properties = new AgentProperties("test", 0.6, "operador");
        when(planner.id()).thenReturn("test");
        when(planner.displayName()).thenReturn("Analista de teste");
        service = new IncidentTriageService(new PlannerRegistry(List.of(planner), properties),
                invoker, bus, properties);
    }

    private Incident open(String host) {
        return service.open(List.of(new Alert("A-" + host, "teste", host, "alerta",
                Severity.HIGH, Instant.now(), List.of())));
    }

    private TriageVerdict positive() {
        return new TriageVerdict(TriageVerdict.Classification.TRUE_POSITIVE, 0.9, "teste", List.of());
    }

    private void plan(TriageVerdict verdict, AgentCommand... commands) {
        when(planner.plan(any())).thenReturn(new TriagePlanner.Plan(verdict, List.of(commands), true));
    }

    @Test
    void negarNaoContaminaAprovacaoDoProximoIncidente() {
        Incident a = open("HOST-A");
        plan(positive(), new IsolateHostCommand(gateway, "HOST-A", "teste"));
        service.triage(a);
        service.deny(a, null, "atividade autorizada");
        assertThat(invoker.pendingCount(a)).isZero();

        Incident b = open("HOST-B");
        plan(positive(), new IsolateHostCommand(gateway, "HOST-B", "teste"));
        service.triage(b);
        service.approve(b, null);
        assertThat(gateway.isIsolated("HOST-A")).isFalse();
        assertThat(gateway.isIsolated("HOST-B")).isTrue();
        assertThat(invoker.history(a)).isEmpty();
        assertThat(invoker.history(b)).hasSize(1);
    }

    @Test
    void aprovarEDesfazerNaoConsomemFilaOuPilhaDeOutroIncidente() {
        Incident a = open("HOST-A");
        plan(positive(), new IsolateHostCommand(gateway, "HOST-A", "teste"));
        service.triage(a);
        Incident b = open("HOST-B");
        plan(positive(), new IsolateHostCommand(gateway, "HOST-B", "teste"));
        service.triage(b);
        service.approve(b, null);
        assertThat(invoker.pendingCount(a)).isEqualTo(1);
        assertThat(gateway.isIsolated("HOST-A")).isFalse();
        service.approve(a, null);
        service.undoLastContainment(b);
        assertThat(gateway.isIsolated("HOST-A")).isTrue();
        assertThat(gateway.isIsolated("HOST-B")).isFalse();
        assertThat(invoker.undoableCount(a)).isEqualTo(1);
    }

    @Test
    void revisaoSemPlanoAguardaDecisaoHumana() {
        Incident incident = open("HOST-A");
        plan(TriageVerdict.needsReview("modelo indisponivel"));
        service.triage(incident);
        assertThat(incident.phase()).isEqualTo(IncidentPhase.AWAITING_APPROVAL);
        assertThat(invoker.pendingCount(incident)).isZero();
        service.approve(incident, "analista");
        assertThat(incident.phase()).isEqualTo(IncidentPhase.CLOSED);
        assertThat(incident.closingSummary().orElseThrow()).contains("sem acoes");
        assertThat(incident.phaseHistory()).doesNotContain(IncidentPhase.CONTAINING);
        assertThat(incident.allowsContainmentCommands()).isFalse();
    }

    @Test
    void baixaConfiancaSemPlanoTambemAguardaRevisao() {
        Incident incident = open("HOST-A");
        plan(new TriageVerdict(TriageVerdict.Classification.TRUE_POSITIVE, 0.2, "incerto", List.of()));
        service.triage(incident);
        assertThat(incident.phase()).isEqualTo(IncidentPhase.AWAITING_APPROVAL);
        service.deny(incident, null, "sem evidencias");
        assertThat(incident.phase()).isEqualTo(IncidentPhase.CLOSED);
    }

    @Test
    void falsoPositivoEncerraSemExecutarPropostas() {
        Incident incident = open("HOST-A");
        plan(new TriageVerdict(TriageVerdict.Classification.FALSE_POSITIVE, 0.9, "backup", List.of()),
                new IsolateHostCommand(gateway, "HOST-A", "teste"));
        service.triage(incident);
        assertThat(incident.phase()).isEqualTo(IncidentPhase.CLOSED);
        assertThat(invoker.pendingCount(incident)).isZero();
        assertThat(gateway.isIsolated("HOST-A")).isFalse();
    }

    @Test
    void resultadoDistingueFalhaParcialETotal() {
        AgentCommand broken = new AgentCommand() {
            public String name() { return "falha"; }
            public String description() { return "gateway indisponivel"; }
            public CommandResult execute() { throw new IllegalStateException("offline"); }
        };
        Incident a = open("HOST-A");
        plan(positive(), broken, new CollectForensicsCommand(gateway, "HOST-A"));
        service.triage(a);
        service.approve(a, null);
        assertThat(a.closingSummary().orElseThrow()).contains("Execucao parcial", "1 sucesso(s), 1 falha(s)");
        Incident b = open("HOST-B");
        plan(positive(), broken);
        service.triage(b);
        service.approve(b, null);
        assertThat(b.closingSummary().orElseThrow()).contains("Execucao falhou");
    }

    @Test
    void leituraAutomaticaTemResultadoDeFalhaConferido() {
        Incident incident = open("HOST-A");
        AgentCommand broken = mock(AgentCommand.class);
        when(broken.name()).thenReturn("leitura");
        when(broken.description()).thenReturn("leitura indisponivel");
        when(broken.execute()).thenReturn(CommandResult.failure("offline", Duration.ZERO));
        when(planner.plan(any())).thenReturn(new TriagePlanner.Plan(positive(), List.of(broken), false));
        service.triage(incident);
        assertThat(incident.closingSummary().orElseThrow()).contains("Execucao falhou");
    }

    @Test
    void falhaAoDesfazerPreservaAcaoParaNovaTentativa() {
        Incident incident = open("HOST-A");
        AgentCommand command = mock(AgentCommand.class);
        when(command.name()).thenReturn("reversivel");
        when(command.description()).thenReturn("teste");
        when(command.undoable()).thenReturn(true);
        when(command.execute()).thenReturn(CommandResult.ok("ok", Duration.ZERO));
        when(command.undo()).thenThrow(new IllegalStateException("offline"))
                .thenReturn(CommandResult.ok("restaurado", Duration.ZERO));
        invoker.execute(incident, command);
        assertThat(invoker.undoLast(incident).result().success()).isFalse();
        assertThat(invoker.undoableCount(incident)).isEqualTo(1);
        assertThat(invoker.undoLast(incident).result().success()).isTrue();
        assertThat(invoker.undoableCount(incident)).isZero();
    }
}
