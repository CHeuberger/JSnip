/*
 * Copyright: Carlos F. Heuberger. All rights reserved.
 *
 */
package cfh.jsnip;

import static java.awt.event.InputEvent.*;

import java.awt.AWTException;
import java.awt.CheckboxMenuItem;
import java.awt.Color;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.Rectangle;
import java.awt.SplashScreen;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.Timer;


/**
 * @author Carlos F. Heuberger
 *
 */
public class Snipper {
    
    public static final String VERSION = "JSnip 1.00 by Carlos Heuberger";

    private static final String ICON_FILE = "tray.png";
    private static final String TIMER_FILE = "trayTimer.png";
    
    private static final int ALL_MODIFIERS = ALT_DOWN_MASK 
                                           | ALT_GRAPH_DOWN_MASK 
                                           | CTRL_DOWN_MASK 
                                           | SHIFT_DOWN_MASK;

    public static void main(String[] args) {
        Snipper main = new Snipper();
        main.init(args);
        SplashScreen splash = SplashScreen.getSplashScreen();
        if (splash != null) {
            splash.close();
        } else {
            System.out.println("no splash");
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    
    private PopupMenu popup;
    
    private MenuItem snipMenuItem;
    private CheckboxMenuItem ontopMenuItem;
    private CheckboxMenuItem hideMenuItem;
    private MenuItem chooseRedColorMenuItem;
    private MenuItem chooseColorMenuItem;
    private MenuItem clearMenuItem;
    private MenuItem helpMenuItem;
    private MenuItem quitMenuItem;
    
    private TrayIcon trayIcon;
    private BufferedImage trayImage;
    private BufferedImage trayTimerImage;
    
    private Color borderColor = Color.BLACK;
    
    private final List<ImageCatcher> catchers = new ArrayList<>();
    private final List<ImageDisplay> displays = new ArrayList<>();
    
    private final ImageCatcher.Listener catchListener = new ImageCatcher.Listener() {
        @Override
        public void catched(ImageCatcher catcher) {
            closeCatchers();
            if (catcher != null) {
                ImageDisplay display = new ImageDisplay(catcher, borderColor);
                display.setAlwaysOnTop(ontopMenuItem.getState());
                displays.add(display);
                display.addWindowListener(removeListener);
            }
        }
    };
    
    private final WindowListener removeListener = new WindowAdapter() {
        @Override
        public void windowClosed(java.awt.event.WindowEvent ev) {
            Object source = ev.getSource();
            if (source instanceof ImageDisplay) {
                ImageDisplay display = (ImageDisplay) source;
                if (displays.remove(display)) {
                    display.removeWindowListener(this);
                } else {
                    System.out.println("remove " + display);
                }
            }
        }
    };


    private Snipper() {
    }
    
    private void init(String[] args) {
        if (!SystemTray.isSupported()) {
            error("Sytem tray not supported!");
            return;
        }
        
        URL url;
        url = getClass().getResource(ICON_FILE);
        if (url == null) {
            error(ICON_FILE + " not found!");
            return;
        }
        try {
            trayImage = ImageIO.read(url);
        } catch (IOException ex) {
            error(ICON_FILE + " cannot be read!", ex);
            return;
        }
        
        url = getClass().getResource(TIMER_FILE);
        if (url == null) {
            error(TIMER_FILE + " not found!");
            trayTimerImage = trayImage;
        } else {
            try {
                trayTimerImage = ImageIO.read(getClass().getResource(TIMER_FILE));
            } catch (IOException ex) {
                error(TIMER_FILE + " cannot be read!", ex);
                trayTimerImage = trayImage;
            }
        }
        
        snipMenuItem = new MenuItem("Snip");
        snipMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ev) {
                doSnip(ev.getModifiers());
            }
        });
        
        ontopMenuItem = new CheckboxMenuItem("On Top");
        ontopMenuItem.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent ev) {
                doOnTop(ev);
            }
        });
        ontopMenuItem.setState(true);
        
        hideMenuItem = new CheckboxMenuItem("Hide");
        hideMenuItem.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent ev) {
                doHide(ev);
            }
        });
        
        clearMenuItem = new MenuItem("Clear");
        clearMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ev) {
                doClear(ev);
            }
        });
        
        chooseColorMenuItem = new MenuItem("Choose Color");
        chooseColorMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ev) {
                doChooseColor(ev);
            }
        });

        chooseRedColorMenuItem = new MenuItem("Choose Red");
        chooseRedColorMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ev) {
                doChooseRedColor(ev);
            }
        });
        
        helpMenuItem = new MenuItem("Help");
        helpMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ev) {
                doHelp(ev);
            }
        });
        
        quitMenuItem = new MenuItem("Quit");
        quitMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ev) {
                doQuit(ev);
            }
        });
        
        popup = new PopupMenu();
        popup.add(snipMenuItem);
        popup.addSeparator();
        popup.add(ontopMenuItem);
        popup.add(hideMenuItem);
        popup.add(clearMenuItem);
        popup.addSeparator();
        popup.add(chooseColorMenuItem);
        popup.add(chooseRedColorMenuItem);
        popup.addSeparator();
        popup.add(helpMenuItem);
        popup.add(quitMenuItem);
        
        String tooltip = VERSION 
                + "\nleft-click to take snapshot"
                + "\nright-click for menu";
        trayIcon = new TrayIcon(trayImage, tooltip, popup);
        trayIcon.setImageAutoSize(true);
        trayIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent ev) {
                doDefault(ev);
            }
        });
        
        
        try {
            SystemTray.getSystemTray().add(trayIcon);
        } catch (AWTException ex) {
            error("Tray Icon", ex);
            return;
        }
    }

    private void doDefault(MouseEvent ev) {
        if (ev.getButton() == MouseEvent.BUTTON1) {
            doSnip(ev.getModifiersEx());
        }
    }
    
    private void doSnip(int modifiers) {
        closeCatchers();
        int delay;
        switch (modifiers & ALL_MODIFIERS) {
            case CTRL_DOWN_MASK: delay = 3_000; break;
            case SHIFT_DOWN_MASK+CTRL_DOWN_MASK: delay = 10_000; break;
            default: delay = -1;
        }
        if (delay > 0) {
            trayIcon.setImage(trayTimerImage);
            Timer timer = new Timer(delay, this::snip);
            timer.setRepeats(false);
            timer.start();
        } else {
            snip(null);
        }
    }
    
    private void snip(ActionEvent ignored) {
        trayIcon.setImage(trayTimerImage);
        GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
        for (GraphicsDevice device : environment.getScreenDevices()) {
            try {
                ImageCatcher catcher = new ImageCatcher(device, catchListener);
                catchers.add(catcher);
            } catch (AWTException ex) {
                error("creating catcher for " + device, ex);
            }
        }
    }
    
    private void doOnTop(ItemEvent ev) {
        setAlwaysOnTop(ontopMenuItem.getState());
    }
    
    private void doHide(ItemEvent ev) {
        boolean visible = ! hideMenuItem.getState();
        for (ImageDisplay display : displays) {
            display.setVisible(visible);
        }
    }
    
    private void doClear(ActionEvent ev) {
        if (displays.isEmpty())
            return;
        setAlwaysOnTop(false);
        try {
            int opt = JOptionPane.showConfirmDialog(null, "Clear all?", VERSION, JOptionPane.YES_NO_OPTION);
            if (opt == JOptionPane.YES_OPTION) {
                for (ImageDisplay display : new ArrayList<>(displays)) {
                    display.dispose();
                }
            }
        } finally {
            setAlwaysOnTop(ontopMenuItem.getState());
        }
    }
    
    private void doChooseColor(ActionEvent ev) {
        JColorChooser colorChooser = new JColorChooser(borderColor);
        int opt = JOptionPane.showConfirmDialog(null, colorChooser, "Border Color", JOptionPane.OK_CANCEL_OPTION);
        if (opt == JOptionPane.OK_OPTION) {
            borderColor = colorChooser.getColor();
        }
    }
    
    private void doChooseRedColor(ActionEvent ev) {
        borderColor = Color.RED;
    }
    
    private void doHelp(ActionEvent ev) {
        String text = "<html><body>\n"
                + "<h1><center>" + VERSION + "</center></h1>\n"
                + "<h1><center>Help</center></h1>\n"
                + "<h2>Snip</h2>\n"
                + "<tt>Right-click</tt> the tray icon for menu and select <tt>Snip</tt>; or<br>\n"
                + "<tt>left-click</tt> the tray icon to start a new snip.<p>\n"
                + "additionally hold:<br/>\n"
                + "<tt>CTRL</tt> to delay about 3 seconds, <br/>\n"
                + "<tt>CTRL-SHIFT</tt> for 10 seconds delay<p>\n"
                + "Select region:<br/>"
                + "Start: <tt>Left-press</tt> and <tt>drag</tt> to select the screen region.<br>\n"
                + "Adjust: <tt>Left-press</tt> and <tt>drag</tt> borders to adjust the region if needed.<p>\n"
                + "End: <tt>right-click</tt> to complete the snip when ready; a window will be created with the screenshot.<br>\n"
                + "<h2>Screenshot Window</h2>\n"
                + "<tt>Left-press</tt> and <tt>drag</tt> to move the image.<br>\n"
                + "<tt>Right-click</tt> for menu.<br>\n"
                + "<h2>Images</h2>\n";
        if (displays.isEmpty()) {
            text += "Screenshot data will be displayed here, if some screenshot is open.\n";
        } else {
            text += "<table border=\"1\">\n"
                + "<tr>\n"
                + "  <th>ID</th>\n"
                + "  <th>Current<br>Postion</th>\n"
                + "  <th>Image<br>Size</th>\n"
                + "  <th>Snip<br>from</th>\n"
                + "  <th>Snip<br>to</th>\n"
                + "  <th>Saved as</th>\n"
                + "  <th>Original</th>\n"
                + "</tr>\n";
            for (ImageDisplay display : displays) {
                Rectangle r = display.getCatcher().getRectangle();
                File saved = display.getSavedAs();
                ImageCatcher original = display.getCatcher().getOriginal();
                ImageDisplay copy = null;
                if (original != null) {
                    for (ImageDisplay d : displays) {
                        if (original == d.getCatcher()) {
                            copy = d;
                            break;
                        }
                    }
                }
                text += String.format("<tr>\n"
                        + "  <td>%d</td>\n"
                        + "  <td>%d,%d</td>\n"
                        + "  <td>%dx%d</td>\n"
                        + "  <td>%d,%d</td>\n"
                        + "  <td>%d,%d</td>\n"
                        + "  <td>%s</td>\n"
                        + "  <td>%s</td>\n"
                        + "</tr>\n",
                        display.getId(),
                        display.getLocationOnScreen().x, display.getLocationOnScreen().y,
                        display.getImageWidth(), display.getImageHeight(), 
                        r.x, r.y, 
                        r.x + r.width, r.y + r.height,
                        saved == null ? "" : saved.getAbsolutePath(),
                        copy == null ? "" : copy.getId());
            }
            text += "</table>\n";
        }
        text += "</body></html>";
        JComponent message = new JScrollPane(new JEditorPane("text/html", text));
        setAlwaysOnTop(false);
        for (ImageDisplay display : displays) {
            display.setShowID(true);
        }
        try {
            JOptionPane.showMessageDialog(null, message, VERSION, JOptionPane.PLAIN_MESSAGE);
        } finally {
            for (ImageDisplay display : displays) {
                display.setShowID(false);
            }
            setAlwaysOnTop(ontopMenuItem.getState());
        }
    }
    
    private void doQuit(ActionEvent ev) {
        setAlwaysOnTop(false);
        if (!displays.isEmpty()) {
            int opt = JOptionPane.showConfirmDialog(null, "Quit?", VERSION, JOptionPane.YES_NO_OPTION);
            if (opt != JOptionPane.YES_OPTION) {
                setAlwaysOnTop(ontopMenuItem.getState());
                return;
            }
        }
        SystemTray.getSystemTray().remove(trayIcon);
        System.exit(0);
    }

    private void setAlwaysOnTop(boolean alwaysOnTop) {
        for (ImageDisplay display : displays) {
            display.setAlwaysOnTop(alwaysOnTop);
        }
    }
    
    private void closeCatchers() {
        for (ImageCatcher ic : catchers) {
            ic.dispose();
        }
        catchers.clear();
        trayIcon.setImage(trayImage);
    }
    
    private void error(String message) {
        System.err.println(message);
        JOptionPane.showMessageDialog(null, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
    
    private void error(String message, Throwable ex) {
        System.err.println(message);
        ex.printStackTrace();
        String[] msg = { message, ex.getMessage() };
        JOptionPane.showMessageDialog(null, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
