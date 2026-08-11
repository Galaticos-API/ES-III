package br.fatec.esiii.socagent.gui.theme;

import java.awt.Color;
import java.awt.Font;

import javax.swing.UIManager;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.fonts.inter.FlatInterFont;

import br.fatec.esiii.socagent.domain.Severity;

/**
 * Tokens visuais do painel: cores, tipografia e espacamento.
 *
 * <p>Centralizar isto evita valores magicos espalhados pelos componentes e
 * permite alternar entre tema claro e escuro sem tocar em cada tela.
 *
 * <p>As cores de severidade foram escolhidas com contraste suficiente nos dois
 * temas. Nenhuma informacao e transmitida apenas por cor: todo indicador
 * colorido vem acompanhado do rotulo textual da severidade, para nao excluir
 * quem tem daltonismo.
 */
public final class UiTheme {

    private static boolean dark;

    private UiTheme() {
    }

    // Espacamento em multiplos de 4, para alinhamento consistente
    public static final int GAP_XS = 4;
    public static final int GAP_SM = 8;
    public static final int GAP_MD = 16;
    public static final int GAP_LG = 24;

    public static void install(boolean useDark) {
        dark = useDark;
        FlatInterFont.installLazy();
        if (useDark) {
            FlatDarkLaf.setup();
        } else {
            FlatLightLaf.setup();
        }
        UIManager.put("defaultFont", new Font(FlatInterFont.FAMILY, Font.PLAIN, 13));
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Button.arc", 10);
        UIManager.put("Component.arc", 10);
        UIManager.put("ProgressBar.arc", 10);
        UIManager.put("TextComponent.arc", 8);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.thumbInsets", new java.awt.Insets(2, 2, 2, 2));
        UIManager.put("Tree.rowHeight", 26);
        UIManager.put("TitlePane.unifiedBackground", true);
    }

    public static boolean isDark() {
        return dark;
    }

    // ---------------------------------------------------------------
    // Tipografia
    // ---------------------------------------------------------------

    public static Font fontTitle() {
        return new Font(FlatInterFont.FAMILY, Font.BOLD, 16);
    }

    public static Font fontSubtitle() {
        return new Font(FlatInterFont.FAMILY, Font.PLAIN, 13);
    }

    public static Font fontLabel() {
        return new Font(FlatInterFont.FAMILY, Font.BOLD, 11);
    }

    public static Font fontBody() {
        return new Font(FlatInterFont.FAMILY, Font.PLAIN, 13);
    }

    public static Font fontMono() {
        return new Font(Font.MONOSPACED, Font.PLAIN, 12);
    }

    // ---------------------------------------------------------------
    // Cores semanticas
    // ---------------------------------------------------------------

    public static Color severityColor(Severity severity) {
        return switch (severity) {
            case CRITICAL -> dark ? new Color(0xFF6B6B) : new Color(0xC92A2A);
            case HIGH -> dark ? new Color(0xFF922B) : new Color(0xE8590C);
            case MEDIUM -> dark ? new Color(0xFFD43B) : new Color(0xB08900);
            case LOW -> dark ? new Color(0x4DABF7) : new Color(0x1971C2);
            case INFO -> dark ? new Color(0x909296) : new Color(0x868E96);
        };
    }

    public static Color accent() {
        return dark ? new Color(0x4DABF7) : new Color(0x1C7ED6);
    }

    public static Color success() {
        return dark ? new Color(0x51CF66) : new Color(0x2F9E44);
    }

    public static Color danger() {
        return dark ? new Color(0xFF6B6B) : new Color(0xC92A2A);
    }

    public static Color warning() {
        return dark ? new Color(0xFFD43B) : new Color(0xE8590C);
    }

    public static Color muted() {
        return dark ? new Color(0x909296) : new Color(0x868E96);
    }

    public static Color surface() {
        return dark ? new Color(0x2B2D30) : new Color(0xF8F9FA);
    }

    public static Color border() {
        return dark ? new Color(0x3A3D41) : new Color(0xDEE2E6);
    }

    /** Fundo tenue de uma cor semantica, para chips e faixas. */
    public static Color tint(Color base) {
        return new Color(base.getRed(), base.getGreen(), base.getBlue(), dark ? 38 : 28);
    }
}
