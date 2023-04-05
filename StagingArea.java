package gitlet;

import java.io.Serializable;
import java.util.HashMap;

public class StagingArea implements Serializable {

    /** Represents the current stage with a HashMap: fileName, ID. */
    private HashMap<String, String> currStage;

    /** Represents removal stage. */
    private HashMap<String, String> removalStage;

    public StagingArea() {
        currStage = new HashMap<>();
        removalStage = new HashMap<>();
    }

    public void addFile(String filename, String id) {
        currStage.put(filename, id);
    }
    public void addRemovalFile(String fileName, String id) {
        removalStage.put(fileName, id);
    }

    public HashMap<String, String> getCurrStage() {
        return currStage;
    }
    public HashMap<String, String> getRemovalStage() {
        return removalStage;
    }

    public void removeFile(String fileName) {
        currStage.remove(fileName);
    }
    public void clearStage() {
        currStage.clear();
    }
    public void clearRemovalStage() {
        removalStage.clear();
    }
}
