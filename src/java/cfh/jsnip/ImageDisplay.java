package cfh.jsnip;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.prefs.Preferences;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JWindow;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import cfh.jsnip.ImageCatcher.DiffMode;


@SuppressWarnings("serial")
class ImageDisplay extends JWindow {
    
    private static final String PREF_DIR = "image directory";
    private static final String PREF_SUFFIX = "image suffix";
    private final Preferences preferences = Preferences.userNodeForPackage(getClass());
    

    private static final int BORDER_X = 2;
    private static final int BORDER_Y = 2;
    
    private static int nextID = 1;

    private final int id = nextID++;
    
    private final ImageCatcher catcher;

    private Point pressed;
    private boolean border = true;
    
    private JPopupMenu popupMenu;
    private JCheckBoxMenuItem borderItem;
    
    private File savedAs = null;

    ImageDisplay(ImageCatcher catcher) {
        super(catcher.getDevice().getDefaultConfiguration());

        this.catcher = Objects.requireNonNull(catcher);
        Objects.requireNonNull(catcher.getImage());

        // TODO crop
        borderItem = new JCheckBoxMenuItem(new AbstractAction("Border") {
            @Override
            public void actionPerformed(ActionEvent ev) {
                doBorder(ev);
            }
        });
        borderItem.setState(border);
        JMenuItem save = new JMenuItem(new AbstractAction("Save") {
            @Override
            public void actionPerformed(ActionEvent ev) {
                doSave(ev);
            }
        });
        JMenuItem copy = new JMenuItem(new AbstractAction("Copy") {
            @Override
            public void actionPerformed(ActionEvent ev) {
                doCopy(ev);
            }
        });
        JMenuItem recapture = new JMenuItem(new AbstractAction("Recapture") {
            @Override
            public void actionPerformed(ActionEvent ev) {
                doRecapture(ev);
            }
        });
        JMenuItem clone = new JMenuItem(new AbstractAction("Clone") {
            @Override
            public void actionPerformed(ActionEvent ev) {
                doClone(ev);
            }
        });
        JMenu diff = new JMenu("Compare");
        for (DiffMode mode : DiffMode.values()) {
            final DiffMode finalMode = mode;
            JMenuItem item = new JMenuItem(new AbstractAction(mode.getName()) {
                @Override
                public void actionPerformed(ActionEvent ev) {
                    doDiff(ev, finalMode);
                }
            });
            diff.add(item);
        }
//        JMenuItem scan = new JMenuItem(new AbstractAction("Scan") {
//            @Override
//            public void actionPerformed(ActionEvent ev) {
//                doScan(ev);
//            }
//        });
        JMenuItem close = new JMenuItem(new AbstractAction("Close") {
            @Override
            public void actionPerformed(ActionEvent ev) {
                doClose(ev);
            }
        });
        
        popupMenu = new JPopupMenu();
        popupMenu.add(borderItem);
        popupMenu.add(save);
        popupMenu.add(copy);
        popupMenu.addSeparator();
        popupMenu.add(recapture);
        popupMenu.add(clone);
        popupMenu.add(diff);
//        popupMenu.add(scan);
        popupMenu.addSeparator();
        popupMenu.add(close);
        popupMenu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            }
            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                repaint();
            }
            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
                repaint();
            }
        });
        
        
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent ev) {
                doPressed(ev);
            }
            @Override
            public void mouseReleased(MouseEvent ev) {
                doReleased(ev);
            }
        });
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent ev) {
                doDragged(ev);
            }
        });
        
        setAlwaysOnTop(true);
        setFocusable(false);
        setLayout(null);
        Rectangle rectangle = catcher.getRectangle();
        setSize(rectangle.width+BORDER_X+BORDER_X, rectangle.height+BORDER_Y+BORDER_Y);
        setLocation(rectangle.x-BORDER_X, rectangle.y-BORDER_Y);
        validate();
        setVisible(true);
    }
    
    
    public int getId() {
        return id;
    }
    
    public File getSavedAs() {
        return savedAs;
    }
    
    public ImageCatcher getCatcher() {
        return catcher;
    }
    
    public int getImageHeight() {
        return catcher.getImage().getHeight();
    }
    
    public int getImageWidth() {
        return catcher.getImage().getWidth();
    }
    
    private void doSave(ActionEvent ev) {
        boolean alwaysOnTop = isAlwaysOnTop();
        setAlwaysOnTop(false);
        try {
            String dir = preferences.get(PREF_DIR, "");
            String suffix = preferences.get(PREF_SUFFIX, "");
            String[] suffixes = ImageIO.getWriterFileSuffixes();
            JFileChooser chooser = new JFileChooser(dir);
            FileNameExtensionFilter defaultFilter = null;
            for (int i = 0; i < suffixes.length; i += 1) {
                FileNameExtensionFilter filter = new FileNameExtensionFilter(suffixes[i], suffixes[i]);
                chooser.addChoosableFileFilter(filter);
                if (suffixes[i].equalsIgnoreCase(suffix)) {
                    defaultFilter = filter;
                }
            }
            if (defaultFilter != null) {
                chooser.setFileFilter(defaultFilter);
            }
            while (true) {
                if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                    dir = chooser.getCurrentDirectory().getAbsolutePath();
                    preferences.put(PREF_DIR, dir);
                    FileFilter filter = chooser.getFileFilter();
                    String defaultExtension = null;
                    if (filter instanceof FileNameExtensionFilter) {
                        defaultExtension = ((FileNameExtensionFilter) filter).getExtensions()[0];
                        preferences.put(PREF_SUFFIX, defaultExtension);
                    }
                    File file = chooser.getSelectedFile();
                    
                    String extension = null;
                    String name = file.getName();
                    int index = name.lastIndexOf('.');
                    if (index != -1) {
                        extension = name.substring(index + 1);
                        boolean ok = false;
                        for (int i = 0; i < suffixes.length; i++) {
                            if (extension.equalsIgnoreCase(suffixes[i])) {
                                ok = true;
                                break;
                            }
                        }
                        if (!ok) {
                            extension = null;
                        }
                    }
                    if (extension == null) {
                        if (!(filter instanceof FileNameExtensionFilter)) {
                            JOptionPane.showMessageDialog(this, "no extension", "JSnip - Error", JOptionPane.ERROR_MESSAGE);
                            continue;
                        } else {
                            extension = defaultExtension;
                            file = new File(file.getParentFile(), name + "." + extension);
                        }
                    }
                    if (file.exists()) {
                        if (JOptionPane.showConfirmDialog(this, "Overwrite?", "JSnip", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) 
                            continue;
                    }
                    try {
                        if (!ImageIO.write(catcher.getImage(), extension, file))
                            throw new IOException("no writer for image format \"" + extension + "\"");
                        savedAs = file;
                    } catch (IOException ex) {
                        ex.printStackTrace();
                        String[] msg = { ex.getMessage(), "writing " + file };
                        JOptionPane.showMessageDialog(this, msg, "JSnip - Error", JOptionPane.ERROR_MESSAGE);
                        continue;
                    }
                }
                break;
            }
        } finally {
            setAlwaysOnTop(alwaysOnTop);
        }
    }

    private void doCopy(ActionEvent ev) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        if (clipboard != null) {
            ImageSelection selection = new ImageSelection(catcher.getImage());
            clipboard.setContents(selection, selection);
        }
    }
    
    private void doBorder(ActionEvent ev) {
        Rectangle rectangle = catcher.getRectangle();
        int x = getX() + (getWidth() - rectangle.width) / 2;
        int y = getY() + (getHeight() - rectangle.height) / 2;
        debug("b %d, %d%n", x, y);
        debug("%s  %s%n", getLocation(), getLocationOnScreen());
        if (borderItem.getState() != border) {
            border = borderItem.getState();
            if (border) {
                setSize(rectangle.width+BORDER_X+BORDER_X, rectangle.height+BORDER_Y+BORDER_Y);
                setLocation(x-BORDER_X, y-BORDER_Y);
                validate();
            } else {
                setSize(rectangle.width, rectangle.height);
                setLocation(x, y);
                validate();
            }
        }
    }
    
    private void doRecapture(ActionEvent ev) {
        setVisible(false);
        try {
            catcher.recapture();
        } catch (AWTException ex) {
            ex.printStackTrace();
            String[] msg = { "recapture failed", ex.getMessage() };
            JOptionPane.showMessageDialog(this, msg, "JSnip - Error", JOptionPane.ERROR_MESSAGE);
            dispose();
            return;
        }
        if (catcher.getImage() == null) {
            JOptionPane.showMessageDialog(this, "recapture failed", "JSnip - Error", JOptionPane.ERROR_MESSAGE);
            dispose();
            return;
        }
        setVisible(true);
    }
    
    private void doClone(ActionEvent ev) {
        ImageCatcher clone;
        setVisible(false);
        try {
            clone = new ImageCatcher(catcher);
        } catch (AWTException ex) {
            ex.printStackTrace();
            String[] msg = { "clone failed", ex.getMessage() };
            JOptionPane.showMessageDialog(this, msg, "JSnip - Error", JOptionPane.ERROR_MESSAGE);
            return;
        } finally {
            setVisible(true);
        }
        if (clone.getImage() == null) {
            JOptionPane.showMessageDialog(this, "clone failed", "JSnip - Error", JOptionPane.ERROR_MESSAGE);
            clone.dispose();
            return;
        }
    }
    
    private void doDiff(ActionEvent ev, DiffMode mode) {
        ImageCatcher copy;
        setVisible(false);
        try {
            copy = new ImageCatcher(catcher);
        } catch (AWTException ex) {
            ex.printStackTrace();
            String[] msg = { "diff failed", ex.getMessage() };
            JOptionPane.showMessageDialog(this, msg, "JSnip - Error", JOptionPane.ERROR_MESSAGE);
            return;
        } finally {
            setVisible(true);
        }
        if (copy.getImage() == null) {
            JOptionPane.showMessageDialog(this, "diff failed", "JSnip - Error", JOptionPane.ERROR_MESSAGE);
            copy.dispose();
            return;
        }
        copy.diff(catcher.getImage(), mode);
    }
    
//    private void doScan(ActionEvent ev) {
//        LuminanceSource source = new BufferedImageLuminanceSource(catcher.getImage());
//        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
//        Reader reader = new MultiFormatReader();
//        
//        boolean alwaysTop = isAlwaysOnTop();
//        setAlwaysOnTop(false);
//        try {
//            Result result = reader.decode(bitmap);
//            Object[] message = { "Barcode: ", result, "Copy to clipboard?" };
//            int opt = JOptionPane.showConfirmDialog(this,  message, "JSnip - Barcode", JOptionPane.YES_NO_OPTION);
//            if (opt == JOptionPane.YES_OPTION) {
//                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
//                if (clipboard != null) {
//                    StringSelection selection = new StringSelection(result.getText());
//                    clipboard.setContents(selection, selection);
//                }
//            }
//        } catch (NotFoundException | ChecksumException | FormatException ex) {
//            JOptionPane.showMessageDialog(this, ex, "JSnip - Error", JOptionPane.ERROR_MESSAGE);
//        } finally {
//            setAlwaysOnTop(alwaysTop);
//        }
//
//    }
    
    private void doClose(ActionEvent ev) {
        dispose();
    }

    private void doPressed(MouseEvent ev) {
        debug("pressed %s, %s%n", ev, ev.getModifiersEx());
        if (ev.isPopupTrigger()) {
            popupMenu.show(ev.getComponent(), ev.getX(), ev.getY());
        } else if (ev.getModifiersEx() == MouseEvent.BUTTON1_DOWN_MASK) {
            pressed = ev.getPoint();
        }
    }
    
    private void doReleased(MouseEvent ev) {
        debug("released %s, %s%n", ev, ev.getModifiersEx());
        if (ev.isPopupTrigger()) {
            popupMenu.show(ev.getComponent(), ev.getX(), ev.getY());
        } else if (ev.getModifiersEx() == MouseEvent.BUTTON1_DOWN_MASK) {
            pressed = null;
        }
    }

    private void doDragged(MouseEvent ev) {
        debug("dragged %s, %s%n", ev, ev.getModifiersEx());
        if (ev.getModifiersEx() == MouseEvent.BUTTON1_DOWN_MASK && pressed != null) {
            Point p = ev.getLocationOnScreen();
            setLocation(p.x-pressed.x, p.y-pressed.y);
            repaint();
        }
    }
    
    @Override
    public void paint(Graphics g) {
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, getWidth(), getHeight());
        g.drawImage(catcher.getImage(), border ? BORDER_X : 0, border ? BORDER_Y : 0, this);
    }
    
    private static final void debug(String format, Object... args) {
//        System.out.printf(format, args);
    }
}
