package cfh.jsnip;

import static java.lang.Math.*;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JWindow;


class ImageCatcher extends JWindow {
    
    public static enum DiffMode { 
        BLACK_WHITE ("Black on white"),
        WHITE_BLACK ("White on black"),
        GRAY_WHITE ("Gray on white"),
        GRAY_BLACK ("Gray on black"),
        SUB_BLACK ("Subtract on black"),
        SUB_WHITE ("Subtract on white"),
        MIXED ("Mixed"),
        ;
        private final String name;
        DiffMode(String name) {
            this.name = name;
        }
        String getName() {
            return name;
        }
    }

    private final ImageCatcher original;
    
    private final GraphicsDevice device;
    private final Listener listener;
    
    private boolean prior18;
    
    private BufferedImage background;
    
    private Point start;
    private Point end;
    
    private enum Change { END, STARTX, STARTY, ENDX, ENDY }
    private Change change = Change.END;
    
    private Rectangle rectangle;
    private BufferedImage image;

    ImageCatcher(GraphicsDevice device, Listener listener) throws AWTException {
        super(device.getDefaultConfiguration());
        
        this.original = null;
        this.device = Objects.requireNonNull(device);
        this.listener = Objects.requireNonNull(listener);
        
        prior18 = checkVersionPrior18();
        
        capture();
        
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent ev) {
                doMouseClicked(ev);
            }
            @Override
            public void mousePressed(MouseEvent ev) {
                doMousePressed(ev);
            }
            @Override
            public void mouseReleased(MouseEvent ev) {
                doMouseReleased(ev);
            }
        });
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent ev) {
                doMouseDragged(ev);
            }
        });
        
        setAlwaysOnTop(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        setBounds(device.getDefaultConfiguration().getBounds());
        validate();
        setVisible(true);
    }
    
    ImageCatcher(ImageCatcher original) throws AWTException {
        super(original.device.getDefaultConfiguration());
        
        this.original = original;
        this.device = original.device;
        this.listener = original.listener;
        
        this.prior18 = original.prior18;
        
        this.start = original.start;
        this.end = original.end;
        this.rectangle = original.rectangle;
        
        recapture();
        dispose();
        listener.catched(this);
    }
    
    private boolean checkVersionPrior18() {
        try {
            String version = System.getProperty("java.version");
            Pattern pattern = Pattern.compile("(\\d++)(?:\\.(\\d++)(\\..*)?)?");
            Matcher matcher = pattern.matcher(version);
            if (matcher.matches()) {
                int major = Integer.parseInt(matcher.group(1));
                int minor = (matcher.groupCount()>1 && matcher.group(2)!=null) ? Integer.parseInt(matcher.group(2)) : 0;
                return (major < 1) || (major == 1 && minor < 8);
            } else {
                return false;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }
    
    private void capture() throws AWTException {
        GraphicsConfiguration configuration = device.getDefaultConfiguration();
        Robot robot = new Robot(device);
        Rectangle bounds = configuration.getBounds();
        if (prior18) {
            bounds.x = 0;
            bounds.y = 0;
        }
        background = robot.createScreenCapture(bounds);
    }

    private void doMousePressed(MouseEvent ev) {
        if (isButtonPressed(ev, 1)) {
            if (start == null) {
                start = ev.getPoint();
                end = new Point(start);
                change = Change.END;
            } else {
                int min = abs(ev.getPoint().x - start.x);
                change = Change.STARTX;
                int diff;
                diff = abs(ev.getPoint().y-start.y);
                if (diff < min) {
                    min = diff;
                    change = Change.STARTY;
                }
                diff = abs(ev.getPoint().x-end.x);
                if (diff < min) {
                    min = diff;
                    change = Change.ENDX;
                }
                diff = abs(ev.getPoint().y-end.y);
                if (diff < min) {
                    min = diff;
                    change = Change.ENDY;
                }
            }
        }
    }

    private void doMouseReleased(MouseEvent ev) {
        if (isButtonPressed(ev, 1)) {
            switch (change) {
                case END: end.setLocation(ev.getPoint()); break;
                case STARTX: start.x = ev.getPoint().x; break;
                case STARTY: start.y = ev.getPoint().y; break;
                case ENDX: end.x = ev.getPoint().x; break;
                case ENDY: end.y = ev.getPoint().y; break;
                default: throw new IllegalArgumentException("unhandled value: " + change);
            }
            repaint();
        }
    }
    
    private void doMouseDragged(MouseEvent ev) {
        if (isButtonPressed(ev, 1)) {
            switch (change) {
                case END: end.setLocation(ev.getPoint()); break;
                case STARTX: start.x = ev.getPoint().x; break;
                case STARTY: start.y = ev.getPoint().y; break;
                case ENDX: end.x = ev.getPoint().x; break;
                case ENDY: end.y = ev.getPoint().y; break;
                default: throw new IllegalArgumentException("unhandled value: " + change);
            }
            repaint();
        }
    }

    private void doMouseClicked(MouseEvent ev) {
        if (isButtonPressed(ev, 3)) {
            if (start != null) {
                int x1 = min(start.x, end.x);
                int x2 = max(start.x, end.x);
                int y1 = min(start.y, end.y);
                int y2 = max(start.y, end.y);
                if (x2 > x1 && y2 > y1) {
                    try {
                        snip(x1, y1, x2, y2);
                        listener.catched(this);
                        return;
                    } catch (Throwable ex) {
                        ex.printStackTrace();
                    }
                }
            }
            listener.catched(null);
        }
    }
    
    private void snip(int x1, int y1, int x2, int y2) {
        if (x2 > x1 && y2 > y1) {
            Rectangle bounds = device.getDefaultConfiguration().getBounds();
            rectangle = new Rectangle(x1+bounds.x, y1+bounds.y, x2-x1, y2-y1);
            rectangle = rectangle.intersection(bounds);
            
            image = background.getSubimage(rectangle.x-bounds.x, rectangle.y-bounds.y, rectangle.width, rectangle.height);
            
            ImageSelection selection = new ImageSelection(image);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(selection, selection);
        } else {
            image = null;
        }
    }
    
    private boolean isButtonPressed(MouseEvent ev, int button) {
        int mask;
        switch (button) {
            case 1: mask = MouseEvent.BUTTON1_MASK; break;
            case 2: mask = MouseEvent.BUTTON2_MASK; break;
            case 3: mask = MouseEvent.BUTTON3_MASK; break;
            default: throw new IllegalArgumentException("unknown button " + button);
        }
        return (ev.getModifiers() & mask) != 0;
    }
    
    public ImageCatcher getOriginal() {
        return original;
    }
    
    public void recapture() throws AWTException {
        capture();
        
        Rectangle bounds = device.getDefaultConfiguration().getBounds();
        int x1 = rectangle.x - bounds.x;
        int y1 = rectangle.y - bounds.y;
        int x2 = x1 + rectangle.width;
        int y2 = y1 + rectangle.height;
        snip(x1, y1, x2, y2);
    }
    
    public void diff(BufferedImage original, DiffMode mode) {
        assert original.getWidth() == image.getWidth() : original.getWidth() + " <> " + image.getWidth();
        assert original.getHeight() == image.getHeight() : original.getHeight() + " <> " + image.getHeight();
        
        ColorModel cm = ColorModel.getRGBdefault();
        switch (mode) {
            case BLACK_WHITE: 
            case WHITE_BLACK: {
                for (int y = 0; y < image.getHeight(); y += 1) {
                    for (int x = 0; x < image.getWidth(); x += 1) {
                        int o = original.getRGB(x, y);
                        int i = image.getRGB(x, y);
                        image.setRGB(x, y, (o == i ^ mode == DiffMode.WHITE_BLACK) ? 0x00ffffff : 0);
                    }
                }
                break;
            }
            case GRAY_WHITE: {
                int[] o = new int[cm.getNumComponents()];
                int[] i = new int[cm.getNumComponents()];
                for (int y = 0; y < image.getHeight(); y += 1) {
                    for (int x = 0; x < image.getWidth(); x += 1) {
                        o = cm.getComponents(original.getRGB(x, y), o, 0);
                        i = cm.getComponents(image.getRGB(x, y), i , 0);
                        double dist = 0.0;
                        for (int j = 0; j < cm.getNumColorComponents(); j++) {
                            int d = i[j] - o[j];
                            dist += d*d;
                        }
                        int n = max(0, min(255, 255 - (int) sqrt(dist)));
                        image.setRGB(x, y, 0x00010101 * n);
                    }
                }
                break;
            }
            case GRAY_BLACK: {
                int[] o = new int[cm.getNumComponents()];
                int[] i = new int[cm.getNumComponents()];
                for (int y = 0; y < image.getHeight(); y += 1) {
                    for (int x = 0; x < image.getWidth(); x += 1) {
                        o = cm.getComponents(original.getRGB(x, y), o, 0);
                        i = cm.getComponents(image.getRGB(x, y), i , 0);
                        double dist = 0.0;
                        for (int j = 0; j < cm.getNumColorComponents(); j++) {
                            int d = i[j] - o[j];
                            dist += d*d;
                        }
                        int n = max(0, min(255, (int) sqrt(dist)));
                        image.setRGB(x, y, 0x00010101 * n);
                    }
                }
                break;
            }
            case MIXED: {
                int[] o = new int[cm.getNumComponents()];
                int[] i = new int[cm.getNumComponents()];
                for (int y = 0; y < image.getHeight(); y += 1) {
                    for (int x = 0; x < image.getWidth(); x += 1) {
                        o = cm.getComponents(original.getRGB(x, y), o, 0);
                        i = cm.getComponents(image.getRGB(x, y), i , 0);
                        for (int j = 0; j < cm.getNumColorComponents(); j++) {
                            i[j] = (i[j] + o[j]) / 2;
                        }
                        image.setRGB(x, y, cm.getDataElement(i, 0));
                    }
                }
                break;
            }
            case SUB_BLACK: {
                int[] o = new int[cm.getNumComponents()];
                int[] i = new int[cm.getNumComponents()];
                for (int y = 0; y < image.getHeight(); y += 1) {
                    for (int x = 0; x < image.getWidth(); x += 1) {
                        o = cm.getComponents(original.getRGB(x, y), o, 0);
                        i = cm.getComponents(image.getRGB(x, y), i , 0);
                        for (int j = 0; j < cm.getNumColorComponents(); j++) {
                            i[j] = abs(i[j]-o[j]);
                        }
                        image.setRGB(x, y, cm.getDataElement(i, 0));
                    }
                }
                break;
            }
            case SUB_WHITE: {
                int[] o = new int[cm.getNumComponents()];
                int[] i = new int[cm.getNumComponents()];
                for (int y = 0; y < image.getHeight(); y += 1) {
                    for (int x = 0; x < image.getWidth(); x += 1) {
                        o = cm.getComponents(original.getRGB(x, y), o, 0);
                        i = cm.getComponents(image.getRGB(x, y), i , 0);
                        for (int j = 0; j < cm.getNumColorComponents(); j++) {
                            i[j] = 255 - abs(i[j]-o[j]);
                        }
                        image.setRGB(x, y, cm.getDataElement(i, 0));
                    }
                }
                break;
            }
            default: throw new IllegalArgumentException("unrecognized mode: " + mode);
        }
    }
    
    public GraphicsDevice getDevice() {
        return device;
    }
    
    public BufferedImage getImage() {
        return image;
    }
    
    public Rectangle getRectangle() {
        return rectangle;
    }
    
    @Override
    public void paint(Graphics g) {
        if (background != null) {
            Graphics2D gg = (Graphics2D) g;
            gg.drawImage(background, 0, 0, this);
            gg.setColor(new Color(0, 0, 0, 64));
            gg.fillRect(0, 0, getWidth(), getHeight());
            if (start != null) {
                int x1 = min(start.x, end.x);
                int x2 = max(start.x, end.x);
                int y1 = min(start.y, end.y);
                int y2 = max(start.y, end.y);
                gg.setClip(x1, y1, x2-x1, y2-y1);
                gg.drawImage(background, 0, 0, this);
            }
        }
    }
    
    ////////////////////////////////////////////////////////////////////////////////////////////////
    
    public static interface Listener {
        
        public void catched(ImageCatcher catcher);
    }
}
