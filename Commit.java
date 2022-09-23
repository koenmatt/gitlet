package gitlet;



import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import static gitlet.Utils.*;

/** Represents a gitlet commit object.
 *  does at a high level.
 *
 *  @author Matthew Koen
 */
public class Commit implements Serializable {
    /**
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided  one example for `message`.
     */

    /** The message of this Commit. */

    private String parent;

    private HashMap<String, String> blobs;

    private String iD;

    private String time;

    private String message;


    public Commit(String message, String time) {
        this.message = message;
        this.time = time;
    }

    public Commit(String parent, String message, String time, HashMap<String, String> blobs) {
        this.message = message;
        this.time = time;
        this.blobs = blobs;
        this.parent = parent;

    }

    public String getParent() {
        return parent;
    }

    public String getID() {
        return iD;
    }

    public HashMap<String, String> getBlobs() {
        return blobs;
    }

    public void updateBlobs(HashMap<String, String> k) {
        blobs = k;
    }

    public static void saveBlobs(HashMap<String, String> blobs) {
        for (String fileName: blobs.keySet()) {
            File baseFile = join(Repository.CWD, fileName);
            File blob = join(Repository.BLOB_DIR, blobs.get(fileName));

            try {
                blob.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }

            byte[] content = readContents(baseFile);
            writeContents(blob, content);
        }

    }

    public void updateParent(String parentSHA) {
        parent = parentSHA;

    }

    public void updateID(String identifier) {
        this.iD = identifier;
    }

    public String getTime() {
        return time;
    }

    public String getMessage() {
        return message;
    }

}
