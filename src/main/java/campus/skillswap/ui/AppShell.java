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
import static campus.skillswap.ui.Theme.*;
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

/** Main window layout: sidebar, right-hand panel, page switching. */
public class AppShell {

    // ====================== APP STATE / SHELL ======================
    public static JFrame frame; public static JPanel root, content, sidebar, rightPanel; public static JScrollPane rightScroll; public static CardLayout rootCards = new CardLayout(); public static JLabel statusBar;
    public static String page = "Feed", feedFilter = "All", searchQ = "", searchCat = "All", bookTab = "Upcoming", chatWith = null, board = "Most Active", chatQ = "";
    public static TrayIcon tray; public static String feedQ = "", certFilter = "All", draft = ""; public static LocalDate dayFilter; public static final LinkedList<String> recent = new LinkedList<>();
    public static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("EEE, MMM d \u2022 h:mm a");
    // ====================== MAIN APP SHELL ======================
    public static final String[][] NAV = {{"Feed", "Skill Feed"}, {"Search", "Search Topics"}, {"Certs", "Verified Certificates"}, {"Bookings", "Bookings & Sessions"},
            {"Nearby", "Nearby Now"}, {"Messages", "Messages"}, {"Profile", "My Profile & Rating"}, {"Settings", "Settings"}};
    public static JComponent appPanel() {
        JPanel p = new JPanel(new BorderLayout()); p.setBackground(BG);
        sidebar = new JPanel(); sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS)); sidebar.setBackground(PANEL); sidebar.setPreferredSize(new Dimension(215, 0));
        content = new JPanel(new BorderLayout()); content.setBackground(BG);
        rightPanel = new JPanel(); rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS)); rightPanel.setBackground(PANEL);
        rightScroll = new JScrollPane(rightPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER); rightScroll.setBorder(null);
        rightScroll.setPreferredSize(new Dimension(270, 0)); rightScroll.getViewport().setBackground(PANEL);
        statusBar = lbl(" ", Font.PLAIN, 11, MUTED); statusBar.setBorder(new EmptyBorder(3, 10, 3, 10)); JPanel sb = new JPanel(new BorderLayout()); sb.setBackground(PANEL); sb.add(statusBar);
        p.add(sidebar, BorderLayout.WEST); p.add(content, BorderLayout.CENTER); p.add(rightScroll, BorderLayout.EAST); p.add(sb, BorderLayout.SOUTH); return p;
    }
    public static void show(String pgName) {
        stopInline(); page = pgName; content.removeAll();
        JComponent c;
        switch (pgName) {
            case "Search": c = searchPage(); break; case "Certs": c = certsPage(); break; case "Bookings": c = bookingsPage(); break;
            case "Nearby": c = nearbyPage(); break; case "Messages": c = messagesPage(); break; case "Profile": c = profilePage(); break;
            case "Settings": c = settingsPage(); break; default: c = feedPage();
        }
        content.add(c); content.revalidate(); content.repaint(); buildSidebar();
        boolean side = !(pgName.equals("Messages") || pgName.equals("Settings"));   // chat & settings use the full width
        rightScroll.setVisible(side); if (side) buildRight(); rightScroll.getParent().revalidate();
        statusBar.setText(netText() + "   |   Signed in as " + name(me) + "   |   Credits: " + me.credits + "/" + MAX_CREDITS + "   |   Last online sync: " + S.lastSync);
    }
    public static void buildSidebar() {
        sidebar.removeAll(); sidebar.setBorder(new EmptyBorder(14, 10, 12, 10));
        JPanel logo = new JPanel(new BorderLayout(8, 0)); logo.setOpaque(false); JPanel sq = new JPanel(); sq.setBackground(new Color(0xA03CFF)); sq.setPreferredSize(new Dimension(16, 16)); JPanel sqw = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 3)); sqw.setOpaque(false); sqw.add(sq); JPanel tx = vbox(); put(tx, lbl("Skillswap", Font.BOLD, 14, TXT)); put(tx, lbl("Peer Learning", Font.PLAIN, 10, PINK)); logo.add(sqw, BorderLayout.WEST); logo.add(tx); logo.setBorder(new EmptyBorder(0, 4, 16, 0)); logo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50)); put(sidebar, logo);
        for (String[] n : NAV) {
            boolean on = n[0].equals(page);
            RBtn b = new RBtn(n[1], 26); b.setHorizontalAlignment(SwingConstants.LEFT); b.setFont(f(Font.BOLD, 12)); b.setForeground(on ? ONPINK : MUTED);
            b.setBackground(on ? PINK : PANEL); b.setBorder(new EmptyBorder(8, 12, 8, 12));
            b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36)); b.setCursor(new Cursor(Cursor.HAND_CURSOR));
            if (n[0].equals("Certs")) {
                long earned = badges(me).stream().filter(x -> x[2].equals("Earned")).count();
                if (earned > 0) {
                    JLabel bd = new JLabel("" + earned, SwingConstants.CENTER) { protected void paintComponent(Graphics g0) { Graphics2D g = (Graphics2D) g0.create(); aa(g); g.setColor(getBackground()); g.fillOval(0, 0, getWidth(), getHeight()); g.dispose(); super.paintComponent(g0); } };
                    bd.setOpaque(false); bd.setBackground(new Color(0x7B3FB0)); bd.setForeground(Color.WHITE); bd.setFont(f(Font.BOLD, 10)); bd.setPreferredSize(new Dimension(20, 20));
                    JPanel w = new JPanel(new GridBagLayout()); w.setOpaque(false); w.add(bd); b.setLayout(new BorderLayout()); b.add(w, BorderLayout.EAST);
                }
            }
            String t = n[0]; b.addActionListener(e -> show(t)); put(sidebar, b); gap(sidebar, 4);
        }
        sidebar.add(Box.createVerticalGlue());
        JButton out = ghost("Log Out", () -> { stopInline(); closeReels(); save(); me = null; rootCards.show(root, "login"); }); out.setBackground(PANEL); out.setForeground(MUTED); put(sidebar, out);
        sidebar.revalidate(); sidebar.repaint();
    }
    public static void buildRight() {
        rightPanel.removeAll(); rightPanel.setBorder(new EmptyBorder(14, 12, 12, 12));
        if (page.equals("Profile")) { buildRightProfile(); rightPanel.add(Box.createVerticalGlue()); rightPanel.revalidate(); rightPanel.repaint(); return; }
        put(rightPanel, lbl("TOP TUTORS", Font.BOLD, 12, MUTED)); gap(rightPanel, 4);
        JPanel chips = row();
        for (String bd : new String[]{"Most Active", "Most Learned"}) chips.add(chip(bd, bd.equals(board), () -> { board = bd; buildRight(); }));
        put(rightPanel, chips);
        List<User> us = new ArrayList<>(S.users.values());
        us.sort((x, y) -> board.equals("Most Active") ? Long.compare(taught(y), taught(x)) : Long.compare(learnedN(y), learnedN(x)));
        int rank = 1;
        for (User u : us.subList(0, Math.min(5, us.size()))) {
            JPanel r = card(); r.setBorder(new EmptyBorder(6, 8, 6, 8)); JPanel inf = vbox();
            JPanel nm = row(); nm.add(lbl(rank++ + ". " + name(u), Font.BOLD, 12, TXT)); if (verified(u)) nm.add(tag("\u2713", GREEN)); put(inf, nm);
            put(inf, lbl((board.equals("Most Active") ? taught(u) + " sessions taught" : learnedN(u) + " skills learned") + (u.publicRatings && !reviews(u).isEmpty() ? "  " + String.format("%.1f", avg(u)) + "\u2605" : ""), Font.PLAIN, 11, MUTED));
            r.add(inf, BorderLayout.CENTER);
            if (u != me && !u.teach.isEmpty()) r.add(btn("Book", () -> book(u.username, me.username, u.teach.get(0))), BorderLayout.EAST);
            put(rightPanel, r); gap(rightPanel, 5);
        }
        gap(rightPanel, 10); put(rightPanel, lbl("CAMPUS BADGES", Font.BOLD, 12, MUTED)); gap(rightPanel, 4);
        JPanel cr = card(); cr.setBorder(new EmptyBorder(6, 8, 6, 8)); JPanel ci = vbox();
        put(ci, lbl("Swap credits: " + me.credits + " / " + MAX_CREDITS, Font.BOLD, 12, TXT));
        JProgressBar pb = new JProgressBar(0, MAX_CREDITS); pb.setValue(me.credits); pb.setForeground(PINK); pb.setBackground(PANEL2); pb.setBorderPainted(false); put(ci, pb);
        put(ci, lbl("Learning costs 1, teaching earns 1.", Font.PLAIN, 10, MUTED)); cr.add(ci); put(rightPanel, cr); gap(rightPanel, 5);
        for (String[] b : badges(me)) if (b[2].equals("Earned")) { JPanel br = card(); br.setBorder(new EmptyBorder(6, 8, 6, 8)); br.add(lbl("\u2611 " + b[0], Font.BOLD, 11, GOLD)); put(rightPanel, br); gap(rightPanel, 4); }
        rightPanel.add(Box.createVerticalGlue()); rightPanel.revalidate(); rightPanel.repaint();
    }
    public static void buildRightProfile() {
        put(rightPanel, lbl("MY RATING", Font.BOLD, 12, MUTED)); gap(rightPanel, 6); List<Sess> rs = reviews(me);
        JPanel big = row(); big.add(lbl(rs.isEmpty() ? "-" : String.format("%.1f", avg(me)), Font.BOLD, 28, TXT)); big.add(lbl(stars(avg(me)) + "  (" + rs.size() + ")", Font.PLAIN, 12, GOLD)); put(rightPanel, big);
        if (!me.publicRatings) put(rightPanel, lbl("Private - only you can see this", Font.PLAIN, 10, MUTED));
        for (int st = 5; st >= 1; st--) { final int s2 = st; long n = rs.stream().filter(x -> x.rating == s2).count(); JPanel r = new JPanel(new BorderLayout(8, 0)); r.setOpaque(false);
            r.add(lbl(st + " \u2605", Font.PLAIN, 11, GOLD), BorderLayout.WEST); JProgressBar pb = new JProgressBar(0, Math.max(1, rs.size())); pb.setValue((int) n); pb.setForeground(PINK); pb.setBackground(PANEL2); pb.setBorderPainted(false);
            r.add(pb); r.add(lbl("" + n, Font.PLAIN, 11, MUTED), BorderLayout.EAST); put(rightPanel, r); gap(rightPanel, 3); }
        gap(rightPanel, 12); put(rightPanel, lbl("MY BADGES", Font.BOLD, 12, MUTED)); gap(rightPanel, 4);
        for (String[] b : badges(me)) { JPanel br = card(); br.setBorder(new EmptyBorder(6, 8, 6, 8)); boolean ok = b[2].equals("Earned"); br.add(lbl((ok ? "\u2605 " : "\u2606 ") + b[0], Font.BOLD, 11, ok ? GOLD : MUTED)); put(rightPanel, br); gap(rightPanel, 4); }
        gap(rightPanel, 8); JButton sh = btn("Share Profile", AppShell::shareProfile); sh.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38)); put(rightPanel, sh);
    }
    public static void shareProfile() {
        List<Sess> rs = reviews(me);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection("SkillSwap Campus - " + name(me) + " | Teaches: " + String.join(", ", me.teach) + " | Wants: " + String.join(", ", me.learn)
                + (me.publicRatings && !rs.isEmpty() ? " | Rating " + String.format("%.1f", avg(me)) : "")), null);
        toast("Profile summary copied to clipboard");
    }
}
