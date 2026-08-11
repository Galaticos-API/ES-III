package br.fatec.esiii.socagent.gui;

import java.awt.GraphicsEnvironment;

import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import br.fatec.esiii.socagent.gui.theme.UiTheme;
import br.fatec.esiii.socagent.gui.tree.EvidenceTreeBuilder;
import br.fatec.esiii.socagent.observer.AgentEventBus;
import br.fatec.esiii.socagent.service.IncidentTriageService;
import br.fatec.esiii.socagent.service.SampleAlertCatalog;

/**
 * Abre o painel apos a inicializacao do contexto e cuida da troca de tema.
 *
 * <p>Alternar tema em Swing exige reinstalar o look and feel. Em vez de tentar
 * atualizar cada componente ja criado — o que deixa cores antigas presas em
 * elementos desenhados manualmente — a janela e recriada. O incidente em
 * andamento nao e preservado, e a troca de tema e uma acao deliberada do
 * operador, nao algo que ocorra no meio de uma triagem.
 */
@Component
@Profile("!headless")
public class GuiLauncher implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(GuiLauncher.class);

    private final IncidentTriageService triageService;
    private final EvidenceTreeBuilder treeBuilder;
    private final SampleAlertCatalog catalog;
    private final AgentEventBus eventBus;

    private boolean darkTheme = true;
    private SocDashboardFrame frame;

    public GuiLauncher(IncidentTriageService triageService, EvidenceTreeBuilder treeBuilder,
            SampleAlertCatalog catalog, AgentEventBus eventBus) {
        this.triageService = triageService;
        this.treeBuilder = treeBuilder;
        this.catalog = catalog;
        this.eventBus = eventBus;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (GraphicsEnvironment.isHeadless()) {
            log.warn("Ambiente sem interface grafica: o painel nao sera aberto.");
            return;
        }
        if (args.containsOption("tema")) {
            darkTheme = !"claro".equalsIgnoreCase(args.getOptionValues("tema").getFirst());
        }
        SwingUtilities.invokeLater(this::openWindow);
    }

    private void openWindow() {
        try {
            UiTheme.install(darkTheme);
            if (frame != null) {
                frame.dispose();
            }
            frame = new SocDashboardFrame(triageService, treeBuilder, catalog, eventBus,
                    this::toggleTheme);
            frame.setVisible(true);
            log.info("Painel aberto no tema {}. Observadores registrados: {}",
                    darkTheme ? "escuro" : "claro", eventBus.listenerCount());
        } catch (RuntimeException ex) {
            // Em servidor sem display, forcar headless=false faz a criacao da janela
            // falhar aqui. Registrar e seguir e melhor que derrubar o agente.
            log.error("Nao foi possivel abrir o painel: {}. "
                    + "Use --spring.profiles.active=headless para rodar em terminal.",
                    ex.getMessage());
        }
    }

    private void toggleTheme() {
        darkTheme = !darkTheme;
        SwingUtilities.invokeLater(this::openWindow);
    }
}
