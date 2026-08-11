package br.fatec.esiii.socagent;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Ponto de entrada do agente de triagem de incidentes de seguranca.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class SocAgentApplication {

    public static void main(String[] args) {
        // O Spring Boot forca java.awt.headless=true por padrao, e o faz antes de
        // ler o application.yml -- definir spring.main.headless la chega tarde
        // demais e o painel Swing nunca abre. Por isso a configuracao vem pelo
        // builder, que aplica o valor antes da inicializacao.
        new SpringApplicationBuilder(SocAgentApplication.class)
                .headless(false)
                .run(args);
    }
}
