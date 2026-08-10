package br.fatec.esiii.socagent.gui;

import java.awt.GraphicsEnvironment;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import br.fatec.esiii.socagent.gui.tree.EvidenceTreeBuilder;
import br.fatec.esiii.socagent.observer.AgentEventBus;
import br.fatec.esiii.socagent.service.IncidentTriageService;
import br.fatec.esiii.socagent.service.SampleAlertCatalog;

/**
 * Abre o painel apos a inicializacao do contexto.
 *
 * <p>Em ambiente sem interface grafica a aplicacao segue funcionando: o
 * lancador apenas registra o aviso e nao abre janela alguma.
 */
@Component
@Profile("!headless")
public class GuiLauncher implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(GuiLauncher.class);

    private final IncidentTriageService triageService;
    private final EvidenceTreeBuilder treeBuilder;
    private final SampleAlertCatalog catalog;
    private final AgentEventBus eventBus;

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
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) {
                log.debug("Look and feel do sistema indisponivel, usando o padrao.");
            }
            new SocDashboardFrame(triageService, treeBuilder, catalog, eventBus).setVisible(true);
            log.info("Painel aberto. Observadores registrados: {}", eventBus.listenerCount());
        });
    }
}
