package de.danoeh.antennapod.ui.preferences.screen.downloads;

public class MoveRequest {
    public final String newPath;
    public final boolean moveFiles;
    public final boolean forceMoveInsufficientSpace;
    
    public MoveRequest(String newPath, boolean moveFiles, boolean forceMoveInsufficientSpace) {
        this.newPath = newPath;
        this.moveFiles = moveFiles;
        this.forceMoveInsufficientSpace = forceMoveInsufficientSpace;
    }
}
