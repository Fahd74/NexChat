package ui;

import javax.swing.*;
import java.awt.*;

/**
 * Phase 1: UI Only - Custom ListCellRenderer for the online users panel.
 * Renders each user with a green online dot indicator and their username.
 */
public class UserListRenderer extends DefaultListCellRenderer {
    private static final Color BG_COLOR = new Color(0x13151F);
    private static final Color HOVER_COLOR = new Color(0x2A2D3E);
    private static final Color TEXT_COLOR = Color.WHITE;
    private static final Color DOT_COLOR = new Color(0x4CAF50); // Green dot

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 8));
        panel.setBackground(isSelected ? HOVER_COLOR : BG_COLOR);

        // Custom painting for the green online indicator dot
        JPanel dot = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(DOT_COLOR);
                g2.fillOval(0, 0, 10, 10);
            }
        };
        dot.setPreferredSize(new Dimension(10, 10));
        dot.setOpaque(false);

        JLabel label = new JLabel(value.toString());
        label.setForeground(TEXT_COLOR);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        panel.add(dot);
        panel.add(label);
        
        // Add some padding to the cell
        panel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));

        return panel;
    }
}
