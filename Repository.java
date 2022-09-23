package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;

import static gitlet.Utils.*;


/** Represents a gitlet repository.
 *  does at a high level.
 *
 *  @author Matthew Koen
 */
public class Repository implements Serializable {
    /** List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */

    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    /** The commit directory. */
    public static final File COMMIT_DIR = join(GITLET_DIR, "commits");
    /** The blob directory. */
    public static final File BLOB_DIR = join(GITLET_DIR, "blobs");
    /** The HEAD directory. */
    public static final File HEAD = join(GITLET_DIR, "HEAD.txt");
    /** A file for the branches */
    public static final File BRANCHES = join(GITLET_DIR, "branches");
    /** The current starting commit for the current branch. */
    public static final File CURRENT_BRANCH = join(GITLET_DIR, "currentBranch.txt");
    /** The current staging area */
    public static final File STAGING = join(GITLET_DIR, "stage");
    /** The master */
    public static final File MASTER = join(GITLET_DIR, "master.txt");


    /** The init function call, initializes all directories needed,creates a new commit */
    public static void init() {

        if (GITLET_DIR.exists()) {
            System.out.print("A Gitlet version-control system already "
                    + "exists in the current directory.");
        }

        createInitialDirectories();
        createInitialFiles();

        String initialCommitSha = createInitialCommit();

        writeContents(HEAD, initialCommitSha);
        writeContents(CURRENT_BRANCH, "main");
        writeContents(MASTER, initialCommitSha);

        HashMap<String, String> branches = new HashMap<String, String>();
        branches.put("main", initialCommitSha);
        writeObject(BRANCHES, branches);
    }

    /** Adds a file to the current staging area */
    public static void add(String file) {
        Staging stage;
        File baseFile = join(CWD, file);

        if (!baseFile.exists()) {
            System.out.print("File does not exist.");
            return;
        }

        if (STAGING.exists()) {
            stage = readObject(STAGING, Staging.class);
        } else {
            stage = newStage();
        }

        stage.addFile(file);
    }

    /** commits the files in the staging area to a new commit */
    public static void commit(String message) {

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("EEE MMM d kk:mm:ss uuuu "
                + "-0800");
        LocalDateTime now = LocalDateTime.now();
        String time = dtf.format(now);

        Commit currentCommit = new Commit(message, time);


        Staging currentStage;
        Commit lastCommit = loadLastCommit();
        HashMap<String, String> blobs = lastCommit.getBlobs();

        if (STAGING.exists()) {
            currentStage = readObject(STAGING, Staging.class);
        } else {
            currentStage = new Staging();
            System.out.print("No changes added to the commit.");
        }

        if (currentStage.getStageFiles() == null) {
            System.out.print("No changes added to the commit.");
        }

        HashMap<String, String> currentStageFiles = currentStage.getStageFiles();

        Commit.saveBlobs(currentStageFiles);

        if (lastCommit.getBlobs() != null) {
            blobs.putAll(currentStageFiles);
            currentCommit.updateBlobs(blobs);
        } else {
            currentCommit.updateBlobs(currentStageFiles);
        }

        currentCommit.updateParent(lastCommit.getID());

        updateBranch(sha1(serialize(currentCommit)));

        createCommit(currentCommit);

        Staging.clear();
    }

    public static void checkout(String fileName) {
        Commit lastCommit = loadLastCommit();
        String blobSHA = lastCommit.getBlobs().get(fileName);
        if (blobSHA == null) {
            throw new RuntimeException("File does not exist in that commit.");
        }

        byte[] blob = readContents(join(BLOB_DIR, blobSHA));
        writeContents(join(CWD, fileName), blob);
    }

    public static void checkout(String fileName, String commitId) {

        Commit c = null;

        for (String commit : plainFilenamesIn(COMMIT_DIR)) {
            if (commit.equals(commitId)) {
                c = readObject(join(COMMIT_DIR, commitId), Commit.class);
            }
        }
        if (c == null) {
            System.out.print("No commit with that id exists.");
        } else {
            String blobSHA = c.getBlobs().get(fileName);
            if (blobSHA == null) {
                System.out.print("File does not exist in that commit.");
                return;
            }
            byte[] blob = readContents(join(BLOB_DIR, blobSHA));
            writeContents(join(CWD, fileName), blob);
        }

    }

    public static void checkoutBranch(String branchName) {

        if (branchName.equals(readContentsAsString(CURRENT_BRANCH))) {
            System.out.print("No need to checkout the current branch.");
            return;
        }

        HashMap<String, String> branches = readObject(BRANCHES, HashMap.class);
        if (branches.get(branchName) == null) {
            System.out.print("No such branch exists.");
            return;
        }
        Commit c = loadCommit(branches.get(branchName));
        HashMap<String, String> blobs = c.getBlobs();

        Commit currentBranchCommit = loadLastCommit();
        HashMap<String, String> currentBlobs = currentBranchCommit.getBlobs();


        if (blobs != null) {

            for (String fileName: plainFilenamesIn(CWD)) {

                File baseFile = join(Repository.CWD, fileName);

                byte[] content = readContents(baseFile);
                String iD = sha1(content);

                if (((currentBlobs.get(fileName) == null)
                        || (currentBlobs.get(fileName) != iD))
                        && (blobs.get(fileName) != null)) {
                    System.out.print(("There is an untracked file in the way; delete it, "
                            + "or add and commit it first."));
                    return;
                }
            }

            for (String fileName : currentBlobs.keySet()) {
                if (blobs.get(fileName) == null) {
                    restrictedDelete(join(CWD, fileName));
                }
            }
        }



        if (blobs != null) {
            for (String blobName : blobs.keySet()) {
                byte [] content = readContents(join(BLOB_DIR, blobs.get(blobName)));
                writeContents(join(CWD, blobName), content);
            }
        }
        //Overwrite files to CWD


        writeContents(CURRENT_BRANCH, branchName);

    }

    public static void rmBranch(String branchName) {
        HashMap<String, String> branches = readObject(BRANCHES, HashMap.class);
        if (branches.get(branchName) == null) {
            System.out.print("A branch with that name does not exist.");
            return;
        }

        if (readContentsAsString(CURRENT_BRANCH).equals(branchName)) {
            System.out.print("Cannot remove the current branch.");
            return;
        }

        branches.remove(branchName);
        writeObject(BRANCHES, branches);
    }



    public static void branch(String branchName) {
        HashMap<String, String> branches = readObject(BRANCHES, HashMap.class);
        if (branches.get(branchName) != null) {
            System.out.print("A branch with that name already exists.");
            return;
        }

        branches.put(branchName, readContentsAsString(HEAD));
        writeObject(BRANCHES, branches);
    }


    public static void globalLog() {

        Commit start = loadLastCommit();

        while (start.getParent() != null) {
            System.out.println("===");
            System.out.println("commit " + start.getID());
            System.out.println("Date: " + start.getTime());
            System.out.println(start.getMessage());
            System.out.println("");

            start = loadCommit(start.getParent());

        }

        System.out.println("===");
        System.out.println("commit " + start.getID());
        System.out.println("Date: " + start.getTime());
        System.out.println(start.getMessage());
        System.out.println("");
    }

    public static void rm(String fileName) {
        Staging stage;

        boolean possibleError = false;

        if (STAGING.exists()) {
            stage = readObject(STAGING, Staging.class);
            if (stage.inStaging(fileName)) {
                stage.remove(fileName);
                stage.save();
            } else {
                possibleError = true;
            }
        } else {
            possibleError = true;
            stage = newStage();
        }

        Commit c = loadLastCommit();
        HashMap<String, String> blobs = c.getBlobs();

        if (blobs != null) {
            if (blobs.get(fileName) != null) {
                possibleError = false;
                File f = join(CWD, fileName);
                if (f.exists()) {
                    stage.addRemoveFile(fileName);
                } else {
                    stage.addRemoveFile(fileName, blobs.get(fileName));
                }

                restrictedDelete(f);
            }
        }

        if (possibleError) {
            System.out.print("No reason to remove the file.");
            return;
        }
        stage.save();

    }


    public static void log() {
        Commit start = loadLastCommit();

        while (start.getParent() != null) {
            System.out.println("===");
            System.out.println("commit " + start.getID());
            System.out.println("Date: " + start.getTime());
            System.out.println(start.getMessage());
            System.out.println("");

            start = loadCommit(start.getParent());

        }

        System.out.println("===");
        System.out.println("commit " + start.getID());
        System.out.println("Date: " + start.getTime());
        System.out.println(start.getMessage());
        System.out.println("");
    }

    public static void find(String commitMessage) {

        boolean found = false;
        for (String fileName: plainFilenamesIn(COMMIT_DIR)) {
            Commit c = loadCommit(fileName);
            if (commitMessage.equals(c.getMessage())) {
                System.out.println(fileName);
                found = true;
            }
        }
        if (!found) {
            System.out.print("Found no commit with that message.");
        }
    }

    public static ArrayList<String> reverse(ArrayList<String> list) {
        for (int i = 0, j = list.size() - 1; i < j; i++) {
            list.add(i, list.remove(j));
        }
        return list;
    }


    public static void status() {
        String currentBranch = readContentsAsString(CURRENT_BRANCH);
        ArrayList<String> stagedFiles;
        ArrayList<String> removedFiles;

        HashMap<String, String> branches = getBranches();
        if (getStagedFiles() == null) {
            stagedFiles = null;
        } else {
            stagedFiles = reverse(getStagedFiles());
        }

        if (getRemovedFiles() == null) {
            removedFiles = null;
        } else {
            removedFiles = reverse(getRemovedFiles());
        }

        System.out.println("=== Branches ===");

        System.out.println("*" + currentBranch);
        for (String branchName : branches.keySet()) {
            if (branchName.equals(currentBranch)) {
                continue;
            } else {
                System.out.println(branchName);
            }
        }
        System.out.println("");

        System.out.println("=== Staged Files ===");
        if (stagedFiles != null) {
            for (String fileName : stagedFiles) {
                System.out.println(fileName);
            }
        }
        System.out.println("");

        System.out.println("=== Removed Files ===");
        if (removedFiles != null) {
            for (String fileName : removedFiles) {
                System.out.println(fileName);
            }
        }
        System.out.println("");
        System.out.println("=== Modifications Not Staged For Commit ===");
        System.out.println("");
        System.out.println("=== Untracked Files ===");

    }



    public static void reset() {

    }




    ///HELPER FUNCTIONS



    /** points the main branch title to the commit with this SHA */
    public static void updateBranch(String commitSha) {
        HashMap<String, String> s = readObject(BRANCHES, HashMap.class);
        String currentBranch = readContentsAsString(CURRENT_BRANCH);
        s.put(currentBranch, commitSha);
        writeObject(BRANCHES, s);

    }




    /** returns a hashmap of the current branches */
    public static HashMap<String, String> getBranches() {
        return readObject(BRANCHES, HashMap.class);
    }


    /** creates a new stage and returns that staging object*/
    public static Staging newStage() {
        try {
            STAGING.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new Staging();
    }

    /** returns a hashmap of the current files staged */
    public static ArrayList<String> getStagedFiles() {
        if (!STAGING.exists()) {
            return null;
        }
        Staging x = readObject(STAGING, Staging.class);
        return x.getOrderedFiles();
    }


    /** returns a hashmap of the current files removed */
    public static ArrayList<String> getRemovedFiles() {
        if (!STAGING.exists()) {
            return null;
        }
        Staging x = readObject(STAGING, Staging.class);
        return x.getOrderedRmFiles();
    }

    /** returns the fileSHA of file fileName*/
    public static String getFileSHA(String fileName) {
        File baseFile = join(CWD, fileName);

        byte[] content = readContents(baseFile);
        return sha1(content);
    }


    public static String createInitialCommit() {
        Commit initialCommit = new Commit("initial commit",
                "Thu Jan 1 00:00:00 1970 -0800");
        String initialCommitSha = sha1(serialize(initialCommit));
        File commit0 = join(COMMIT_DIR, initialCommitSha);
        initialCommit.updateID(initialCommitSha);
        initialCommit.updateBlobs(null);
        initialCommit.updateParent(null);

        try {
            commit0.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        writeObject(commit0, initialCommit);

        return initialCommitSha;
    }

    public static void createCommit(Commit c) {
        String currentCommitSHA = sha1(serialize(c));
        updateHEAD(currentCommitSHA);
        File newCommit = join(COMMIT_DIR, currentCommitSHA);
        c.updateID(currentCommitSHA);

        try {
            newCommit.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        writeObject(newCommit, c);
    }

    public static void createInitialDirectories() {
        GITLET_DIR.mkdir();
        COMMIT_DIR.mkdir();
        BLOB_DIR.mkdir();
    }

    public static void createInitialFiles() {
        try {
            HEAD.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            CURRENT_BRANCH.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            MASTER.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            BRANCHES.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }



    }

    public static void updateHEAD(String newHeadSHA) {
        writeContents(HEAD, newHeadSHA);
    }

    /** Loads the last commit
     */
    public static Commit loadLastCommit() {

        File lastCommit = join(Repository.GITLET_DIR, "HEAD.txt");
        String head = readContentsAsString(lastCommit);

        File commitPath = join(Repository.COMMIT_DIR, head);
        return readObject(commitPath, Commit.class);
    }

    public static Commit loadCommit(String fileName) {

        File commitPath = join(Repository.COMMIT_DIR, fileName);
        return readObject(commitPath, Commit.class);
    }

}
