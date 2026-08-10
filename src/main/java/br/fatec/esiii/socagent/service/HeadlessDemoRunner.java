package br.fatec.esiii.socagent.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import br.fatec.esiii.socagent.command.CommandInvoker;
import br.fatec.esiii.socagent.gui.tree.EvidenceTreeBuilder;
import br.fatec.esiii.socagent.observer.AuditTrailListener;
import br.fatec.esiii.socagent.state.Incident;
import br.fatec.esiii.socagent.state.IncidentPhase;

/**
 * Executa um cenario completo sem interface grafica.
 *
 * <p>Serve para validar a integracao com o modelo em terminal e para gerar o
 * relatorio usado na apresentacao. Ative com
 * {@code --spring.profiles.active=headless}.
 */
@Component
@Profile("headless")
public class HeadlessDemoRunner implements ApplicationRunner {

    private final IncidentTriageService triageService;
    private final SampleAlertCatalog catalog;
    private final EvidenceTreeBuilder treeBuilder;
    private final AuditTrailListener auditTrail;
    private final CommandInvoker invoker;

    public HeadlessDemoRunner(IncidentTriageService triageService, SampleAlertCatalog catalog,
            EvidenceTreeBuilder treeBuilder, AuditTrailListener auditTrail, CommandInvoker invoker) {
        this.triageService = triageService;
        this.catalog = catalog;
        this.treeBuilder = treeBuilder;
        this.auditTrail = auditTrail;
        this.invoker = invoker;
    }

    @Override
    public void run(ApplicationArguments args) {
        String scenarioName = args.containsOption("cenario")
                ? args.getOptionValues("cenario").getFirst()
                : catalog.scenarios().getFirst().name();

        var scenario = catalog.byName(scenarioName);

        System.out.println("\n================ CENARIO ================");
        System.out.println(scenario.name() + " -- " + scenario.description());
        System.out.println("Estrategia ativa: " + triageService.planners().active().displayName());

        Incident incident = triageService.open(scenario.alerts());
        triageService.triage(incident);

        if (incident.phase() == IncidentPhase.AWAITING_APPROVAL) {
            System.out.println("\n--- APROVACAO HUMANA REQUERIDA ---");
            System.out.println("Comandos aguardando decisao: " + invoker.pendingCount());

            // Por padrao o agente para aqui, como pararia em producao. A aprovacao
            // automatica existe apenas para exercitar o fluxo completo em terminal
            // e precisa ser pedida explicitamente.
            if (args.containsOption("aprovar")) {
                System.out.println("Aprovacao simulada via --aprovar");
                triageService.approve(incident, "demo.operador");
            } else {
                System.out.println("Nenhuma acao executada. Use --aprovar para simular a decisao humana.");
            }
        }

        System.out.println("\n================ ARVORE DE EVIDENCIAS (Composite) ================");
        System.out.print(treeBuilder.build(incident).render(0));

        System.out.println("\n================ TRILHA DE AUDITORIA (Observer) ================");
        auditTrail.entries().forEach(System.out::println);

        System.out.println("\n================ COMANDOS EXECUTADOS (Command) ================");
        invoker.history().forEach(record -> System.out.println("  %s -> %s | %s".formatted(
                record.commandName(),
                record.result().success() ? "OK" : "RECUSADO/FALHOU",
                record.result().output())));

        System.out.println("\nFase final: " + incident.phase().label());
        incident.closingSummary().ifPresent(summary -> System.out.println("Conclusao: " + summary));
        System.out.println();
    }
}
