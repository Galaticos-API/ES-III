package br.fatec.esiii.socagent.gui;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import br.fatec.esiii.socagent.command.CommandInvoker;
import br.fatec.esiii.socagent.command.SimulatedContainmentGateway;
import br.fatec.esiii.socagent.domain.TriageVerdict;
import br.fatec.esiii.socagent.gui.theme.UiTheme;
import br.fatec.esiii.socagent.gui.tree.EvidenceTreeBuilder;
import br.fatec.esiii.socagent.mitre.MitreRepository;
import br.fatec.esiii.socagent.observer.AgentEventBus;
import br.fatec.esiii.socagent.observer.AuditTrailListener;
import br.fatec.esiii.socagent.observer.ConsoleListener;
import br.fatec.esiii.socagent.observer.EventListenerRegistrar;
import br.fatec.esiii.socagent.service.AgentProperties;
import br.fatec.esiii.socagent.service.IncidentTriageService;
import br.fatec.esiii.socagent.service.PlannerRegistry;
import br.fatec.esiii.socagent.service.SampleAlertCatalog;
import br.fatec.esiii.socagent.state.Incident;
import br.fatec.esiii.socagent.strategy.CommandFactory;
import br.fatec.esiii.socagent.strategy.PlanThenExecutePlanner;
import br.fatec.esiii.socagent.strategy.ProposedAction;
import br.fatec.esiii.socagent.strategy.ThreatAnalyst;

/**
 * Utilitario de desenvolvimento: renderiza o painel em PNG para inspecao visual.
 *
 * <p>Monta a tela com um analista falso e deterministico, sem Spring e sem
 * modelo de linguagem, e grava a imagem em disco. Serve para revisar
 * alinhamento, contraste e espaçamento sem depender de captura de tela.
 *
 * <p>Nao faz parte da aplicacao: vive em src/test e nao vai para o artefato.
 */
public final class UiSnapshot {

    private UiSnapshot() {
    }

    /**
     * Renderiza UMA captura por processo.
     *
     * <p>Reinstalar o look and feel varias vezes no mesmo processo trava a EDT,
     * entao cada combinacao de tema e estado roda em uma JVM propria.
     *
     * @param args diretorio de saida, tema ({@code escuro} ou {@code claro}) e
     *             estado ({@code vazio}, {@code aguardando_aprovacao} ou
     *             {@code encerrado})
     */
    public static void main(String[] args) throws Exception {
        String outputDir = args.length > 0 ? args[0] : "target/ui-snapshots";
        boolean dark = args.length < 2 || !"claro".equalsIgnoreCase(args[1]);
        Estado estado = args.length < 3
                ? Estado.VAZIO
                : Estado.valueOf(args[2].toUpperCase());

        new File(outputDir).mkdirs();
        render(outputDir, dark, estado);

        // A EDT segue viva apos criar a janela; sem isto o processo nunca encerra.
        System.exit(0);
    }

    private enum Estado {
        VAZIO,
        AGUARDANDO_APROVACAO,
        ENCERRADO
    }

    private static void render(String outputDir, boolean dark, Estado estado) throws Exception {
        AgentEventBus bus = new AgentEventBus();
        new EventListenerRegistrar(bus, List.of(new AuditTrailListener(), new ConsoleListener()));

        var gateway = new SimulatedContainmentGateway();
        var mitre = new MitreRepository();
        var factory = new CommandFactory(gateway, mitre);
        var planner = new PlanThenExecutePlanner(new AnalistaFixo(), factory);
        var properties = new AgentProperties("plan-then-execute", 0.6, "analista.local");
        var registry = new PlannerRegistry(List.of(planner), properties);
        var invoker = new CommandInvoker(bus);
        var service = new IncidentTriageService(registry, invoker, bus, properties);
        var treeBuilder = new EvidenceTreeBuilder(mitre);
        var catalog = new SampleAlertCatalog();

        // Fase 1 (EDT): tema e montagem da janela
        var holder = new SocDashboardFrame[1];
        SwingUtilities.invokeAndWait(() -> {
            UiTheme.install(dark);
            SocDashboardFrame frame = new SocDashboardFrame(service, treeBuilder, catalog, bus, () -> {
            });
            frame.pack();
            frame.setSize(1280, 800);
            frame.validate();
            holder[0] = frame;
        });
        SocDashboardFrame frame = holder[0];

        // Fase 2 (fora da EDT): a triagem publica eventos que a janela consome via
        // invokeLater. Executar isto dentro da EDT enfileiraria o proprio consumo.
        Incident incident = null;
        if (estado != Estado.VAZIO) {
            incident = service.open(catalog.scenarios().getFirst().alerts());
            service.triage(incident);
            if (estado == Estado.ENCERRADO) {
                service.approve(incident, "analista.turno1");
            }
        }

        // Fase 3 (EDT): aplicar o incidente, deixar a fila de eventos drenar e pintar
        Incident rendered = incident;
        SwingUtilities.invokeAndWait(() -> {
            if (rendered != null) {
                frame.showIncident(rendered);
            }
        });
        SwingUtilities.invokeAndWait(() -> {
            frame.validate();
            frame.doLayout();
        });

        SwingUtilities.invokeAndWait(() -> {
            BufferedImage image = new BufferedImage(frame.getWidth(), frame.getHeight(),
                    BufferedImage.TYPE_INT_ARGB);
            var graphics = image.createGraphics();
            frame.getContentPane().printAll(graphics);
            graphics.dispose();
            try {
                File target = new File(outputDir, "%s-%s.png".formatted(
                        dark ? "escuro" : "claro", estado.name().toLowerCase()));
                ImageIO.write(image, "png", target);
                System.out.println("  " + target.getPath());
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        });
    }

    /** Analista deterministico: nenhuma chamada de rede, resultado sempre igual. */
    private static final class AnalistaFixo implements ThreatAnalyst {

        @Override
        public TriageVerdict classify(Incident incident) {
            return new TriageVerdict(TriageVerdict.Classification.TRUE_POSITIVE, 0.91,
                    "Volume de saida incompativel com o perfil do host e destino associado "
                            + "a infraestrutura de comando e controle.",
                    List.of("T1041", "T1071"));
        }

        @Override
        public List<ProposedAction> proposePlan(Incident incident) {
            return List.of(
                    new ProposedAction("collect_forensics", Map.of("hostname", "WKS-4471"),
                            "preservar memoria e conexoes antes de conter"),
                    new ProposedAction("block_ip", Map.of("ip_address", "185.220.101.7"),
                            "cortar o canal de comando e controle"),
                    new ProposedAction("isolate_host", Map.of("hostname", "WKS-4471"),
                            "impedir movimentacao lateral"));
        }

        @Override
        public Optional<ProposedAction> proposeNext(Incident incident, List<String> observations) {
            return Optional.empty();
        }
    }
}
