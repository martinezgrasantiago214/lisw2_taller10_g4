package co.unicauca.saberpro.frontend.ui;

import co.unicauca.saberpro.frontend.AppContext;
import co.unicauca.saberpro.frontend.client.ApiException;
import co.unicauca.saberpro.frontend.model.Models.QuestionVersionView;
import co.unicauca.saberpro.frontend.model.Models.QuestionView;
import co.unicauca.saberpro.frontend.model.Models.ReviewHistoryView;
import co.unicauca.saberpro.frontend.model.Models.RoundView;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.List;

/**
 * HU06: historial de revisiones de una pregunta. Una seccion por ronda (una
 * por version), con sus revisores, el veredicto y las observaciones, y una
 * pestana con el contenido de las versiones anteriores. Si la pregunta esta
 * Rechazada, desde aqui el autor crea la version mejorada.
 */
public class ReviewHistoryDialog extends JDialog {

    private final AppContext context;
    private final QuestionView question;
    private ReviewHistoryView history;

    public ReviewHistoryDialog(Window owner, AppContext context, QuestionView question) {
        super(owner, "Historial de revisiones", ModalityType.APPLICATION_MODAL);
        this.context = context;
        this.question = question;
        buildUi();
    }

    private void buildUi() {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(720, 620);
        setMinimumSize(new Dimension(520, 420));
        setLocationRelativeTo(getOwner());

        JPanel root = new JPanel(new BorderLayout());
        UiTheme.applyBackground(root);
        root.add(UiTheme.header("Historial de revisiones"), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Revisiones", new JScrollPane(roundsPanel()));
        tabs.addTab("Versiones anteriores", new JScrollPane(versionsPanel()));
        JPanel center = new JPanel(new BorderLayout());
        center.setBorder(new EmptyBorder(10, 12, 6, 12));
        UiTheme.applyBackground(center);
        center.add(tabs, BorderLayout.CENTER);
        root.add(center, BorderLayout.CENTER);

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 10));
        UiTheme.applyBackground(bar);
        if (question.revisable()) {
            JButton btnRevise = UiTheme.primaryButton("Crear version mejorada");
            btnRevise.addActionListener(e -> {
                String observations = history == null ? null
                        : ReviewHistoryFormatter.lastRejectionObservations(history).orElse(null);
                dispose();
                CreateQuestionFrame.forRevision(context, question, observations).setVisible(true);
            });
            bar.add(btnRevise);
        }
        JButton btnClose = UiTheme.secondaryButton("Cerrar");
        btnClose.addActionListener(e -> dispose());
        bar.add(btnClose);
        root.add(bar, BorderLayout.SOUTH);
        setContentPane(root);
    }

    private JPanel roundsPanel() {
        JPanel panel = verticalPanel();
        try {
            history = context.getFacade().reviewHistory(question.id());
        } catch (ApiException ex) {
            panel.add(message(ex.getStatus() == 404
                    ? "Esta pregunta aun no se ha enviado a revision, asi que no tiene historial."
                    : "No fue posible cargar el historial: " + String.join(" ", ex.getMessages())));
            return panel;
        }
        panel.add(message(ReviewHistoryFormatter.summary(history)));
        List<RoundView> rounds = history.rounds();
        for (int i = rounds.size() - 1; i >= 0; i--) {
            RoundView round = rounds.get(i);
            JPanel section = new JPanel(new BorderLayout());
            section.setOpaque(false);
            section.setAlignmentX(Component.LEFT_ALIGNMENT);
            section.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createTitledBorder(BorderFactory.createLineBorder(UiTheme.PRIMARY),
                            ReviewHistoryFormatter.roundTitle(round), TitledBorder.LEFT, TitledBorder.TOP, null,
                            UiTheme.PRIMARY),
                    new EmptyBorder(4, 8, 8, 8)));
            section.add(readOnly(ReviewHistoryFormatter.roundDetail(round)), BorderLayout.CENTER);
            panel.add(section);
            panel.add(Box.createVerticalStrut(8));
        }
        return panel;
    }

    private JPanel versionsPanel() {
        JPanel panel = verticalPanel();
        List<QuestionVersionView> versions;
        try {
            versions = context.getFacade().listQuestionVersions(question.id());
        } catch (ApiException ex) {
            panel.add(message("No fue posible cargar las versiones: " + String.join(" ", ex.getMessages())));
            return panel;
        }
        if (versions.isEmpty()) {
            panel.add(message("La pregunta esta en su primera version (version " + question.version() + ")."));
        }
        for (int i = versions.size() - 1; i >= 0; i--) {
            panel.add(readOnly(ReviewHistoryFormatter.version(versions.get(i))));
            panel.add(Box.createVerticalStrut(10));
        }
        return panel;
    }

    private static JPanel verticalPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(8, 8, 8, 8));
        UiTheme.applyBackground(panel);
        return panel;
    }

    private static JComponent message(String text) {
        JLabel label = new JLabel("<html>" + text + "</html>");
        label.setForeground(UiTheme.TEXT);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(new EmptyBorder(0, 0, 8, 0));
        return label;
    }

    private static JTextArea readOnly(String text) {
        JTextArea area = new JTextArea(text);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setAlignmentX(Component.LEFT_ALIGNMENT);
        area.setBorder(new EmptyBorder(4, 4, 4, 4));
        return area;
    }
}
