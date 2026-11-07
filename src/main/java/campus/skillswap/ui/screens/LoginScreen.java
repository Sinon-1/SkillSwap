package campus.skillswap.ui.screens;

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
import static campus.skillswap.ui.AppShell.*;
import static campus.skillswap.ui.Notifier.*;
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

/** Login, account creation and password reset. */
public class LoginScreen {

    // ====================== LOGIN ======================
    public static JComponent iconTile(int type) {
        return new JComponent() {
            { setPreferredSize(new Dimension(74, 74)); }
            protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0; aa(g);
                g.setColor(new Color(0x2A2A2A)); g.fillRoundRect(0, 0, 73, 73, 8, 8); g.setColor(Color.WHITE); g.setStroke(new BasicStroke(2f));
                if (type == 2) { g.drawOval(20, 16, 28, 28); g.drawLine(44, 42, 56, 56); }
                else { g.drawPolygon(new int[]{36, 62, 36, 10}, new int[]{18, 30, 42, 30}, 4); g.drawArc(22, 36, 28, 18, 180, 180); g.drawLine(62, 30, 62, 44); }
            }
        };
    }
    public static JComponent loginPanel() {
        JPanel bg = new JPanel(new GridBagLayout()); bg.setBackground(BG);
        JPanel c = vbox(); c.setBackground(PANEL); c.setOpaque(true); c.setBorder(new CompoundBorder(new LineBorder(PANEL2, 1, true), new EmptyBorder(18, 28, 14, 28)));
        JLabel title = lbl("LOGIN TO SKILLSWAP", Font.BOLD, 13, TXT); JPanel tp = new JPanel(new FlowLayout(FlowLayout.CENTER)); tp.setOpaque(false); tp.add(title); put(c, tp); gap(c, 6);
        JPanel icons = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0)); icons.setOpaque(false); for (int i = 0; i < 3; i++) icons.add(iconTile(i)); put(c, icons); gap(c, 14);
        JTextField u = field(); JPasswordField p = pass();
        put(c, lbl("Username or school email", Font.PLAIN, 11, MUTED)); put(c, u); gap(c, 6); put(c, lbl("Password", Font.PLAIN, 11, MUTED)); put(c, p); gap(c, 4);
        JButton forgot = ghost("<html><u>Forgot Password?</u></html>", LoginScreen::forgot); forgot.setBackground(PANEL); forgot.setForeground(TXT); forgot.setFont(f(Font.PLAIN, 10));
        JPanel fr = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0)); fr.setOpaque(false); fr.add(forgot); put(c, fr); gap(c, 6);
        Runnable go = () -> {
            String key = u.getText().trim(); String pw = new String(p.getPassword());
            User f = S.users.values().stream().filter(x -> x.username.equalsIgnoreCase(key) || x.email.equalsIgnoreCase(key)).findFirst().orElse(null);
            if (f == null || !f.passHash.equals(hash(f.username, pw))) { JOptionPane.showMessageDialog(frame, "Incorrect username/email or password.", "Sign in failed", JOptionPane.ERROR_MESSAGE); return; }
            u.setText(""); p.setText(""); enter(f);
        };
        p.addActionListener(e -> go.run()); u.addActionListener(e -> go.run());
        JButton si = btn("Sign in", go); si.setBorder(new EmptyBorder(9, 12, 9, 12)); si.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40)); put(c, si); gap(c, 6);
        JButton reg = ghost("Create account", LoginScreen::register); reg.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40)); put(c, reg); gap(c, 10);
        JLabel net = lbl(netText(), Font.PLAIN, 10, db == null ? GOLD : GREEN); JPanel nl = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0)); nl.setOpaque(false); nl.add(net); put(c, nl); gap(c, 4);
        JPanel nr = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0)); nr.setOpaque(false);
        nr.add(ghost("Database settings", () -> { changeConnection(); net.setText(netText()); }));
        nr.add(ghost(dark ? "Light mode" : "Dark mode", () -> switchTheme(!dark)));
        put(c, nr); gap(c, 8);
        JPanel ft = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0)); ft.setOpaque(false); ft.add(lbl("Contact us: SkillSwap support | Privacy | T&C", Font.PLAIN, 9, TXT)); put(c, ft);
        c.setPreferredSize(new Dimension(400, 560)); bg.add(c); return bg;
    }
    public static void enter(User u) {
        me = u; page = "Feed"; lastMsgSeen = System.currentTimeMillis(); draft = ""; chatWith = null; rootCards.show(root, "app"); show("Feed"); checkReminders();
        notifyUser("Welcome, " + name(u), "You have " + u.credits + " swap credits.");
    }
    public static void register() {
        JTextField em = field(), un = field(), rn = field(); JPasswordField p1 = pass(), p2 = pass();
        JCheckBox real = check("Show my real name publicly (otherwise username)", false), pub = check("Make my ratings public", true);
        JPanel f = form(new String[]{"School email", "Username", "Real name", "Password", "Confirm", "", ""}, new JComponent[]{em, un, rn, p1, p2, real, pub});
        f.setPreferredSize(new Dimension(420, 270));
        if (!ask("Create account", f)) return;
        String email = em.getText().trim().toLowerCase(), user = un.getText().trim(), pw = new String(p1.getPassword());
        String err = !email.matches("^[\\w.+-]+@[\\w.-]*edu[\\w.-]*$") ? "Please use your school email (a domain containing .edu)."
                : user.length() < 3 ? "Username must be at least 3 characters." : S.users.keySet().stream().anyMatch(x -> x.equalsIgnoreCase(user)) || S.users.values().stream().anyMatch(x -> x.email.equals(email)) ? "That username or email is already registered."
                : pw.length() < 6 ? "Password must be at least 6 characters." : !pw.equals(new String(p2.getPassword())) ? "Passwords do not match." : null;
        if (err != null) { JOptionPane.showMessageDialog(frame, err, "Cannot create account", JOptionPane.WARNING_MESSAGE); return; }
        String code = String.format("%06d", new Random().nextInt(1000000)); notifyUser("SkillSwap verification", "Your code is " + code);
        String in = JOptionPane.showInputDialog(frame, "Enter the 6-digit code sent to " + email + "\n(Demo: no mail server is configured - your code is " + code + ")");
        if (in == null || !in.trim().equals(code)) { info("Verification failed. Account not created."); return; }
        User u = new User(); u.username = user; u.email = email; u.realName = rn.getText().trim(); u.useRealName = real.isSelected() && !u.realName.isEmpty();
        u.publicRatings = pub.isSelected(); u.passHash = hash(user, pw); S.users.put(user, u); save(); info("Account created! Add the skills you teach and want to learn in My Profile."); enter(u); show("Profile");
    }
    public static void forgot() {
        JTextField em = field(); JPasswordField np = pass();
        if (!ask("Reset password", form(new String[]{"School email", "New password"}, new JComponent[]{em, np}))) return;
        User u = S.users.values().stream().filter(x -> x.email.equalsIgnoreCase(em.getText().trim())).findFirst().orElse(null);
        if (u == null || np.getPassword().length < 6) { info("Email not found, or password shorter than 6 characters."); return; }
        String code = String.format("%06d", new Random().nextInt(1000000));
        String in = JOptionPane.showInputDialog(frame, "Enter the code sent to your school email\n(Demo code: " + code + ")");
        if (in != null && in.trim().equals(code)) { u.passHash = hash(u.username, new String(np.getPassword())); save(); info("Password updated."); } else info("Wrong code.");
    }
}
