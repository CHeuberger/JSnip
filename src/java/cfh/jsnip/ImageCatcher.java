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
import java.util.Objects;

import javax.swing.JWindow;


class ImageCatcher extends JWindow {

    private final GraphicsDevice device;
    private final Listener listener;
    
    private BufferedImage background;
    
    private Point start;
    private Point end;
    
    private enum Change { END, STARTX, STARTY, ENDX, ENDY };
    private Change change = Change.END;
    
    private Rectangle rectangle;
    private BufferedImage image;

    ImageCatcher(GraphicsDevice device, Listener listener) throws AWTException {
        super(device.getDefaultConfiguration());
        
        this.device = Objects.requireNonNull(device);
        this.listener = Objects.requireNonNull(listener);
        
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

    private void capture() throws AWTException {
        GraphicsConfiguration configuration = device.getDefaultConfiguration();
        Robot robot = new Robot(device);
        Rectangle bounds = configuration.getBounds();
        bounds.x = 0;
        bounds.y = 0;
        background = robot.createScreenCapture(bounds);
    }

    private void doMousePressed(MouseEvent ev) {
//        System.out.println("pressed: " + ev);
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
//        System.out.println("released: " + ev);
        if (isButtonPressed(ev, 1)) {
            switch (change) {
                case END: end.setLocation(ev.getPoint()); break;
                case STARTX: start.x = ev.getPoint().x; break;
                case STARTY: start.y = ev.getPoint().y; break;
                case ENDX: end.x = ev.getPoint().x; break;
                case ENDY: end.y = ev.getPoint().y; break;
            }
            repaint();
        }
    }
    
    private void doMouseDragged(MouseEvent ev) {
//        System.out.println("dragged: " + ev);
        if (isButtonPressed(ev, 1)) {
            switch (change) {
                case END: end.setLocation(ev.getPoint()); break;
                case STARTX: start.x = ev.getPoint().x; break;
                case STARTY: start.y = ev.getPoint().y; break;
                case ENDX: end.x = ev.getPoint().x; break;
                case ENDY: end.y = ev.getPoint().y; break;
            }
            repaint();
        }
    }

    private void doMouseClicked(MouseEvent ev) {
//        System.out.println("clicked: " + ev);
        if (isButtonPressed(ev, 3)) {
            if (start != null) {
                int x1 = min(start.x, end.x);
                int x2 = max(start.x, end.x);
                int y1 = min(start.y, end.y);
                int y2 = max(start.y, end.y);
                if (x2 > x1 && y2 > y1) {
                    snip(x1, y1, x2, y2);
                    listener.catched(this);
                    return;
                }
            }
            listener.catched(null);
        }
    }
    
    private void snip(int x1, int y1, int x2, int y2) {
        if (x2 > x1 && y2 > y1) {
            Rectangle bounds = device.getDefaultConfiguration().getBounds();
            rectangle = new Rectangle(x1+bounds.x, y1+bounds.y, x2-x1, y2-y1);
            image = background.getSubimage(x1, y1, x2-x1, y2-y1);
            
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
    
    public void recapture() throws AWTException {
        capture();
        
        Rectangle bounds = device.getDefaultConfiguration().getBounds();
        int x1 = rectangle.x - bounds.x;
        int y1 = rectangle.y - bounds.y;
        int x2 = x1 + rectangle.width;
        int y2 = y1 + rectangle.height;
        snip(x1, y1, x2, y2);
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
