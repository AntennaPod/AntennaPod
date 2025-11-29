package de.danoeh.antennapod.ui.preferences.screen.downloads;

public class MoveRequest {
    public final String newPath;
    public final boolean moveFiles;
    
    public MoveRequest(String newPath, boolean moveFiles) {
        this.newPath = newPath;
        this.moveFiles = moveFiles;
    }
}
