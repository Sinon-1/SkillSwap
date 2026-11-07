package campus.skillswap.ui;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import java.util.stream.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import javax.swing.filechooser.FileNameExtensionFilter;
import campus.skillswap.model.*;
import static campus.skillswap.config.AppConfig.*;
import static campus.skillswap.core.AppState.*;
import static campus.skillswap.data.Database.*;
import static campus.skillswap.data.Seed.*;
import static campus.skillswap.service.Skills.*;
import static campus.skillswap.service.Stats.*;
import static campus.skillswap.service.Security.*;
import static campus.skillswap.ui.AppShell.*;
import static campus.skillswap.ui.Notifier.*;
import static campus.skillswap.ui.screens.LoginScreen.*;
import static campus.skillswap.ui.screens.ConnectionScreen.*;
import static campus.skillswap.ui.screens.SettingsScreen.*;
import static campus.skillswap.ui.screens.FeedScreen.*;
import static campus.skillswap.ui.screens.SearchScreen.*;
import static campus.skillswap.ui.screens.CertsScreen.*;
import static campus.skillswap.ui.screens.BookingsScreen.*;
import static campus.skillswap.ui.screens.NearbyScreen.*;
import static campus.skillswap.ui.screens.MessagesScreen.*;
import static campus.skillswap.ui.screens.ProfileScreen.*;
import static campus.skillswap.ui.media.Videos.*;
import static campus.skillswap.ui.media.Reels.*;

/** Colors (dark/light), buttons, cards, dialogs and other reusable Swing widgets. */
public class Theme {

    // ====================== THEME (dark / light) ======================
    public static final Color PINK = new Color(0xE91E63), ONPINK = Color.WHITE;
    public static Color BG, PANEL, PANEL2, TXT, MUTED, GREEN, GOLD, BLUE;
    public static boolean dark = true;
    public static void applyTheme(boolean d) {
        dark = d;
        if (d) { BG = new Color(0x0B1026); PANEL = new Color(0x141B3B); PANEL2 = new Color(0x1E2755); TXT = Color.WHITE; MUTED = new Color(0x8D96BD);
            GREEN = new Color(0x3DDC97); GOLD = new Color(0xFFC107); BLUE = new Color(0x6C8CFF); }
        else { BG = new Color(0xF1F3FA); PANEL = Color.WHITE; PANEL2 = new Color(0xE3E8F5); TXT = new Color(0x1B2140); MUTED = new Color(0x5C668C);
            GREEN = new Color(0x12A06B); GOLD = new Color(0xD08F00); BLUE = new Color(0x3858D6); }
        UIManager.put("OptionPane.background", PANEL); UIManager.put("Panel.background", PANEL); UIManager.put("OptionPane.messageForeground", TXT);
        UIManager.put("Label.foreground", TXT); UIManager.put("ComboBox.selectionBackground", PINK);
    }
    public static void switchTheme(boolean d) {
        applyTheme(d); cfg.setProperty("theme", d ? "dark" : "light"); saveCfg();
        root.removeAll(); root.add(loginPanel(), "login"); root.add(appPanel(), "app");
        if (me != null) { rootCards.show(root, "app"); show(page); } else rootCards.show(root, "login");
        root.revalidate(); root.repaint();
    }
    public static Color leftBg() { return dark ? new Color(0x090E24) : PANEL; }
    public static Color divider() { return dark ? new Color(0x7F89B5) : new Color(0xC3CAE0); }

    // ====================== UI HELPERS ======================
    public static Font f(int st, int sz) { return new Font("SansSerif", st, sz); }
    public static JLabel lbl(String t, int st, int sz, Color c) { JLabel l = new JLabel(t); l.setFont(f(st, sz)); l.setForeground(c); return l; }
    public static JPanel vbox() { JPanel p = new JPanel(); p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS)); p.setOpaque(false); return p; }
    public static JPanel row() { JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4)); p.setOpaque(false); return p; }
    public static void put(JPanel box, JComponent c) { c.setAlignmentX(0f); box.add(c); }
    public static void gap(JPanel box, int h) { box.add(Box.createVerticalStrut(h)); }
    public static void aa(Graphics2D g) { g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); }

    /** Rounded button: fills getBackground() with rounded corners, so every colour follows the theme. */
    public static class RBtn extends JButton {
        int arc;
        public RBtn(String t, int arc) { super(t); this.arc = arc; setContentAreaFilled(false); setOpaque(false); setBorderPainted(false); setFocusPainted(false); }
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create(); aa(g); g.setColor(getBackground()); g.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc); g.dispose(); super.paintComponent(g0);
        }
    }
    public static JButton btn(String t, Runnable r) {
        RBtn b = new RBtn(t, 18); b.setFont(f(Font.BOLD, 11)); b.setForeground(ONPINK); b.setBackground(PINK);
        b.setBorder(new EmptyBorder(6, 14, 6, 14)); b.setCursor(new Cursor(Cursor.HAND_CURSOR)); b.addActionListener(e -> r.run()); return b;
    }
    public static JButton ghost(String t, Runnable r) { JButton b = btn(t, r); b.setBackground(PANEL2); b.setForeground(TXT); return b; }
    public static JButton chip(String t, boolean on, Runnable r) { JButton b = on ? btn(t, r) : ghost(t, r); if (!on) b.setForeground(MUTED); return b; }
    public static JLabel tag(String t, Color c) {
        JLabel l = lbl(t, Font.BOLD, 10, c); l.setBorder(new CompoundBorder(new LineBorder(c, 1, true), new EmptyBorder(1, 6, 1, 6))); return l;
    }
    public static JPanel card() {
        JPanel p = new JPanel(new BorderLayout(10, 4)) {
            public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, getPreferredSize().height); }
        };
        p.setBackground(PANEL); p.setBorder(new CompoundBorder(new LineBorder(PANEL2, 1, true), new EmptyBorder(10, 12, 10, 12)));
        return p;
    }
    public static JTextField field() {
        JTextField t = new JTextField(); t.setBackground(PANEL2); t.setForeground(TXT); t.setCaretColor(TXT);
        t.setBorder(new CompoundBorder(new LineBorder(PINK.darker(), 1), new EmptyBorder(6, 8, 6, 8))); return t;
    }
    public static JPasswordField pass() {
        JPasswordField t = new JPasswordField(); t.setBackground(PANEL2); t.setForeground(TXT); t.setCaretColor(TXT);
        t.setBorder(new CompoundBorder(new LineBorder(PINK.darker(), 1), new EmptyBorder(6, 8, 6, 8))); return t;
    }
    public static JCheckBox check(String t, boolean v) { JCheckBox c = new JCheckBox(t, v); c.setOpaque(false); c.setForeground(TXT); c.setFont(f(Font.PLAIN, 12)); return c; }
    public static <T> JComboBox<T> combo(T[] items) { JComboBox<T> c = new JComboBox<>(items); c.setBackground(PANEL2); c.setForeground(TXT); return c; }
    public static JComponent avatar(String n, int sz) {
        String[] w = n.trim().split("\\s+"); String s = w[w.length - 1].isEmpty() ? "?" : w[w.length - 1].substring(0, 1).toUpperCase();
        return new JComponent() {
            { Dimension d = new Dimension(sz, sz); setPreferredSize(d); setMinimumSize(d); setMaximumSize(d); }
            protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0; aa(g);
                g.setColor(PANEL2); g.fillOval(0, 0, sz, sz); g.setColor(PINK); g.drawOval(0, 0, sz - 1, sz - 1);
                g.setColor(TXT); g.setFont(f(Font.BOLD, sz / 2)); FontMetrics m = g.getFontMetrics();
                g.drawString(s, (sz - m.stringWidth(s)) / 2, (sz + m.getAscent() - m.getDescent()) / 2);
            }
        };
    }
    public static JPanel avatarWrap(String n, int sz) { JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)); p.setOpaque(false); p.add(avatar(n, sz)); return p; }
    public static JPanel form(String[] labels, JComponent[] comps) {
        JPanel p = new JPanel(new GridBagLayout()); p.setBackground(PANEL); GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4); c.fill = GridBagConstraints.HORIZONTAL;
        for (int i = 0; i < labels.length; i++) {
            c.gridy = i; c.gridx = 0; c.weightx = 0; if (!labels[i].isEmpty()) p.add(lbl(labels[i], Font.PLAIN, 12, MUTED), c);
            c.gridx = 1; c.weightx = 1; p.add(comps[i], c);
        }
        return p;
    }
    public static boolean ask(String title, JComponent body) {
        busy++; try { return JOptionPane.showConfirmDialog(frame, body, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION; } finally { busy--; }
    }
    public static void info(String m) { busy++; try { JOptionPane.showMessageDialog(frame, m, "SkillSwap Campus", JOptionPane.INFORMATION_MESSAGE); } finally { busy--; } }
    public static String esc(String s) { return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"); }
    public static String ago(long t) {
        long m = (System.currentTimeMillis() - t) / 60000; return m < 1 ? "just now" : m < 60 ? m + "m ago" : m < 1440 ? m / 60 + "h ago" : m / 1440 + "d ago";
    }
    public static String tshort(long t) { long m = (System.currentTimeMillis() - t) / 60000; return m < 1 ? "now" : m < 60 ? m + "m" : m < 1440 ? m / 60 + "h" : m / 1440 + "d"; }
    public static class Pg { public JPanel root = new JPanel(new BorderLayout()), body = vbox(); }
    public static Pg pg(String title, String sub, JComponent action) {
        Pg p = new Pg(); p.root.setBackground(BG);
        JPanel h = new JPanel(new BorderLayout()); h.setOpaque(false); h.setBorder(new EmptyBorder(14, 16, 6, 16));
        JPanel t = vbox(); put(t, lbl(title, Font.BOLD, 18, TXT)); put(t, lbl(sub, Font.PLAIN, 12, MUTED)); h.add(t, BorderLayout.CENTER);
        if (action != null) h.add(action, BorderLayout.EAST);
        JPanel wrap = new JPanel(new BorderLayout()); wrap.setBackground(BG); wrap.setBorder(new EmptyBorder(4, 16, 16, 16)); wrap.add(p.body, BorderLayout.NORTH);
        JScrollPane sp = new JScrollPane(wrap, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER); sp.setBorder(null); sp.getViewport().setBackground(BG); sp.getVerticalScrollBar().setUnitIncrement(18);
        p.root.add(h, BorderLayout.NORTH); p.root.add(sp, BorderLayout.CENTER); return p;
    }
    public static Color catColor(String c) {
        switch (c) { case "Tech": return BLUE; case "Music": return PINK; case "Arts": return new Color(0xFF9800); case "Academics": return GREEN;
            case "Languages": return new Color(0xAB47BC); case "Cooking": return GOLD; default: return new Color(0x607D8B); }
    }
    public static JComponent swatch(Color c, int sz) {
        return new JComponent() {
            { Dimension d = new Dimension(sz, sz); setPreferredSize(d); setMinimumSize(d); setMaximumSize(d); }
            protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0; aa(g);
                g.setColor(c.darker().darker()); g.fillRoundRect(0, 0, sz, sz, 10, 10); g.setColor(c); g.drawRoundRect(0, 0, sz - 1, sz - 1, 10, 10); g.fillRoundRect(sz / 4, sz / 4, sz / 2, sz / 2, 6, 6);
            }
        };
    }
    public static JPanel swatchWrap(Color c, int sz) { JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)); p.setOpaque(false); p.add(swatch(c, sz)); return p; }
    public static JTextField searchField(String hint) {
        JTextField t = new JTextField() { protected void paintComponent(Graphics g) { super.paintComponent(g);
            if (getText().isEmpty()) { g.setColor(MUTED); g.setFont(getFont()); g.drawString(hint, 12, (getHeight() + g.getFontMetrics().getAscent() - g.getFontMetrics().getDescent()) / 2); } } };
        t.setBackground(PANEL); t.setForeground(TXT); t.setCaretColor(TXT); t.setFont(f(Font.PLAIN, 13));
        t.setBorder(new CompoundBorder(new LineBorder(PANEL2, 1, true), new EmptyBorder(9, 12, 9, 12))); return t;
    }
}
