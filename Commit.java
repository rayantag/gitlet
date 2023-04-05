package gitlet;

import java.io.Serializable;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;

public class Commit implements Serializable {

    /** Represents date. */
    private Date date;

    /** Represents message. */
    private String message;

    /** Represents timeStamp. */
    private String timeStamp;

    /** Represents parent. */
    private String parent;

    /** Represents report. */
    private String report;

    /** Represents blobs as HashMap: String FileName, String SHA-1 ID. */
    private HashMap<String, String> blobs;

    public Commit(String m, String parents, HashMap<String, String> blobss) {
        message = m;
        parent = parents;
        if (blobss == null) {
            blobs = new HashMap<String, String>();
        } else {
            blobs = blobss;
        }
        this.report = Utils.sha1(Utils.serialize(this));
        if (parent == null) {
            this.timeStamp = "Wed Dec 31 16:00:00 1969 -0800";
        } else {
            DateTimeFormatter dtf
                    = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
            LocalDateTime now = LocalDateTime.now();
            date = new Date();
            this.timeStamp = "Wed Dec 31 16:00:00 1969 -0800";
        }
    }

    public String getMessage() {
        return message;
    }
    public String getTimeStamp() {
        return timeStamp;
    }
    public String getDate() {
        return date.toString();
    }
    public String getParent() {
        return parent;
    }
    public String getID() {
        return report;
    }
    public HashMap<String, String> getBlobs() {
        return blobs;
    }
}
