package br.fatec.esiii.socagent.command;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.fatec.esiii.socagent.domain.Alert;
import br.fatec.esiii.socagent.domain.Ioc;
import br.fatec.esiii.socagent.domain.Severity;
import br.fatec.esiii.socagent.domain.TriageVerdict;
import br.fatec.esiii.socagent.observer.AgentEventBus;
import br.fatec.esiii.socagent.state.Incident;

class CommandInvokerTest {

    private SimulatedContainmentGateway gateway;
    private CommandInvoker invoker;
    private AgentEventBus eventBus;

    @BeforeEach
    void setUp() {
        gateway = new SimulatedContainmentGateway();
        eventBus = new AgentEventBus();
        invoker = new CommandInvoker(eventBus);
    }

    private Incident incidenteEmContencao() {
        Incident incidente = novoIncidente();
        incidente.triage(new TriageVerdict(TriageVerdict.Classification.TRUE_POSITIVE, 0.92,
                "Exfiltracao confirmada", List.of("T1041")));
        incidente.correlate();
        incidente.requestApproval();
        incidente.approve("analista.turno1");
        return incidente;
    }

    private Incident novoIncidente() {
        Alert alerta = new Alert("ALR-1", "suricata", "WKS-4471",
                "Trafego de exfiltracao", Severity.CRITICAL, Instant.now(),
                List.of(Ioc.ip("185.220.101.7")));
        return new Incident("INC-1", List.of(alerta), eventBus);
    }

    @Test
    @DisplayName("recusa comando destrutivo antes da aprovacao humana")
    void recusaSemAprovacao() {
        Incident incidente = novoIncidente();
        AgentCommand isolar = new IsolateHostCommand(gateway, "WKS-4471", "exfiltracao");

        var registro = invoker.execute(incidente, isolar);

        assertThat(registro.result().success()).isFalse();
        assertThat(registro.result().output()).contains("exige aprovacao humana");
        assertThat(gateway.isIsolated("WKS-4471")).isFalse();
    }

    @Test
    @DisplayName("executa comando destrutivo apos aprovacao")
    void executaAposAprovacao() {
        Incident incidente = incidenteEmContencao();
        AgentCommand isolar = new IsolateHostCommand(gateway, "WKS-4471", "exfiltracao");

        var registro = invoker.execute(incidente, isolar);

        assertThat(registro.result().success()).isTrue();
        assertThat(gateway.isIsolated("WKS-4471")).isTrue();
    }

    @Test
    @DisplayName("comando somente de leitura dispensa aprovacao")
    void leituraDispensaAprovacao() {
        Incident incidente = novoIncidente();
        AgentCommand coleta = new CollectForensicsCommand(gateway, "WKS-4471");

        var registro = invoker.execute(incidente, coleta);

        assertThat(registro.result().success()).isTrue();
    }

    @Test
    @DisplayName("desfaz a ultima contencao aplicada")
    void desfazContencao() {
        Incident incidente = incidenteEmContencao();
        invoker.execute(incidente, new IsolateHostCommand(gateway, "WKS-4471", "exfiltracao"));
        assertThat(gateway.isIsolated("WKS-4471")).isTrue();

        var desfeito = invoker.undoLast(incidente);

        assertThat(desfeito.result().success()).isTrue();
        assertThat(gateway.isIsolated("WKS-4471")).isFalse();
        assertThat(invoker.undoableCount()).isZero();
    }

    @Test
    @DisplayName("processa a fila inteira preservando a ordem")
    void processaFila() {
        Incident incidente = incidenteEmContencao();
        invoker.enqueue(incidente, new CollectForensicsCommand(gateway, "WKS-4471"));
        invoker.enqueue(incidente, new IsolateHostCommand(gateway, "WKS-4471", "exfiltracao"));
        invoker.enqueue(incidente, new BlockIpCommand(gateway, "185.220.101.7", "C2"));

        var executados = invoker.executeQueue(incidente);

        assertThat(executados).hasSize(3);
        assertThat(executados).extracting(CommandInvoker.ExecutionRecord::commandName)
                .containsExactly("collect_forensics", "isolate_host", "block_ip");
        assertThat(invoker.pendingCount()).isZero();
        assertThat(gateway.blockedIps()).contains("185.220.101.7");
    }

    @Test
    @DisplayName("falha de um comando nao interrompe a fila")
    void falhaNaoInterrompeFila() {
        Incident incidente = incidenteEmContencao();
        AgentCommand quebrado = new AgentCommand() {
            @Override
            public String name() {
                return "comando_quebrado";
            }

            @Override
            public String description() {
                return "simula falha de integracao";
            }

            @Override
            public CommandResult execute() {
                throw new IllegalStateException("EDR indisponivel");
            }
        };

        invoker.enqueue(incidente, quebrado);
        invoker.enqueue(incidente, new CollectForensicsCommand(gateway, "WKS-4471"));
        var executados = invoker.executeQueue(incidente);

        assertThat(executados).hasSize(2);
        assertThat(executados.get(0).result().success()).isFalse();
        assertThat(executados.get(0).result().output()).contains("EDR indisponivel");
        assertThat(executados.get(1).result().success()).isTrue();
    }
}
