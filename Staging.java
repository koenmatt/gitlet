package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import static gitlet.Utils.*;

public class Staging implements Serializable {

    HashMap<String, String> stageFiles;
    HashMap<String, String> rmFiles;

    ArrayList<String> orderedFiles = new ArrayList<String>();
    ArrayList<String> orderedRmFiles = new ArrayList<String>();

    public Staging() {
        this.stageFiles = new HashMap<String, String>();
        this.rmFiles = new HashMap<String, String>();
    }

    public void addFile(String fileName) {
        File baseFile = join(Repository.CWD, fileName);

        byte[] content = readContents(baseFile);
        String iD = sha1(content);


        Commit commitToCheck = Repository.loadLastCommit();

        HashMap<String, String> blobs = commitToCheck.getBlobs();

        boolean add = true;
        if (blobs == null) {
            stageFiles.put(fileName, iD);
            orderedFiles.add(fileName);
            save();
            add = false;
        } else {
            for (String blob : blobs.values()) {
                if (iD.equals(blob)) {
                    add = false;
                    if (stageFiles.get(fileName) != null) {
                        stageFiles.remove(fileName);
                        orderedFiles.remove(fileName);
                        save();
                    }
                    if (rmFiles.get(fileName) != null) {
                        rmFiles.remove(fileName);
                        orderedRmFiles.remove(fileName);
                        save();
                    }
                }
            }
        }

        if (add) {
            stageFiles.put(fileName, iD);
            orderedFiles.add(fileName);
            save();
        }

    }

    public void save() {
        writeObject(Repository.STAGING, this);
    }

    public HashMap<String, String> getStageFiles() {
        return stageFiles;
    }

    public HashMap<String, String> getRemoveFiles() {
        return rmFiles;
    }

    /** Returns false if the file is not in the staging area,
     * returns true if it is in the staging area */
    public boolean inStaging(String fileName) {
        return stageFiles.get(fileName) != null;
    }

    public static void clear() {
        try {
            restrictedDelete(Repository.STAGING);
        } catch (IllegalArgumentException e) {
            Staging emptyStage = new Staging();
            writeObject(Repository.STAGING, emptyStage);
        }

    }

    public void addRemoveFile(String fileName) {
        rmFiles.put(fileName, Repository.getFileSHA(fileName));
        orderedRmFiles.add(fileName);
    }

    public void addRemoveFile(String fileName, String fileSha) {
        rmFiles.put(fileName, fileSha);
        orderedRmFiles.add(fileName);
    }

    public ArrayList<String> getOrderedFiles() {
        return orderedFiles;
    }

    public ArrayList<String> getOrderedRmFiles() {
        return orderedRmFiles;
    }

    public void remove(String fileName) {
        stageFiles.remove(fileName);
        orderedFiles.remove(fileName);
    }

}
