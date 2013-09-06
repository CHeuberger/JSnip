package cfh.jsnip;

import java.awt.Image;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

public class ImageSelection implements Transferable, ClipboardOwner {
    
    private static final DataFlavor[] flavors = {
        DataFlavor.imageFlavor
    };
    
    private final Image image;
    
    public ImageSelection(Image image) {
        this.image = image;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return flavors.clone();
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        for (DataFlavor test : flavors) {
            if (flavor.equals(test))
                return true;
        }
        return false;
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
        return image;
    }
    
    @Override
    public void lostOwnership(Clipboard clipboard, Transferable contents) {
    }
}
