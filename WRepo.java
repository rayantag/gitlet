package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class WRepo implements Serializable {

    /** Current working directory. */
    static final File CWD = new File(System.getProperty("user.dir"));

    /** Creation of .gitlet directory. */
    static final File GITLET = Utils.join(CWD, ".gitlet");

    /** Creation of staging area file. */
    static final File STAGING_AREA = Utils.join(GITLET, "StagingArea");

    /** Tracks commits in this file. */
    static final File COMMITS = Utils.join(GITLET, "commits");

    /** Every time a new branch is created. */
    static final File BRANCHES = Utils.join(GITLET, "branches");

    /** Tracks blobs. */
    static final File BLOBS = Utils.join(GITLET, "blobs");

    /** To save currentBranch forcefully. */
    static final File CURRBRANCH = Utils.join(GITLET, "currBranch");

    /** To save forcefully. */
    private static File save = Utils.join(CURRBRANCH, "save");

    /** Initialize a staging area. */
    private static StagingArea stage = new StagingArea();

    public WRepo() {
    }

    public static void init() {
        if (!GITLET.exists()) {
            GITLET.mkdirs();
            STAGING_AREA.mkdirs();
            COMMITS.mkdirs();
            BRANCHES.mkdirs();
            BLOBS.mkdirs();
            CURRBRANCH.mkdirs();
            Commit initial = new Commit("initial commit", null, null);
            String initialID = initial.getID();
            File firstCommit = Utils.join(COMMITS, initialID);
            File currStage = Utils.join(STAGING_AREA, "currentStage");
            File masterBranch = Utils.join(BRANCHES, "master");
            Utils.writeObject(masterBranch, initial);
            Utils.writeObject(firstCommit, initial);
            Utils.writeObject(currStage, stage);
            Utils.writeObject(Utils.join(BRANCHES, "head"), initial);
            Utils.writeContents(save, "master");
        } else {
            System.out.println("A Gitlet version-control system already "
                    + "exists in the current directory");
        }
    }

    public static void add(String file) {
        StagingArea st = Utils.readObject
                (Utils.join(STAGING_AREA, "currentStage"), StagingArea.class);
        if (!Utils.join(CWD, file).exists()) {
            System.out.println("File does not exist.");
        } else {
            File input = Utils.join(CWD, file);
            byte[] inputContents = Utils.readContents(input);
            String fileID = Utils.sha1(inputContents);
            if (st.getCurrStage().containsKey(file)
                    && !st.getCurrStage().containsValue(fileID)) {
                st.removeFile(file);
            }
            if (st.getCurrStage().containsKey(file)
                    && st.getCurrStage().containsValue(fileID)) {
                return;
            }
            st.addFile(file, fileID);
            Commit currentCommit =
                    Utils.readObject
                            (Utils.join(BRANCHES, "head"), Commit.class);
            Utils.writeContents(Utils.join(BLOBS, fileID), inputContents);
            if (currentCommit.getBlobs().containsKey(file)
                    && st.getCurrStage().containsKey(file)) {
                if (currentCommit.getBlobs().get(file).equals(fileID)
                        && st.getCurrStage().get(file).equals(fileID)) {
                    st.removeFile(file);
                }
            }
            if (st.getRemovalStage().containsKey(file)) {
                st.getRemovalStage().remove(file);
            }
            Utils.writeObject(Utils.join(STAGING_AREA, "currentStage"), st);
        }
    }

    public static void commit(String message) {
        StagingArea stage1 =
                Utils.readObject(Utils.join(STAGING_AREA, "currentStage"),
                        StagingArea.class);
        String parent =
                Utils.readObject
                        (Utils.join(BRANCHES, "head"), Commit.class).getID();
        File parentCommitFile = Utils.join(COMMITS, parent);
        Commit parentCommit = Utils.readObject(parentCommitFile, Commit.class);
        Commit newCommit = new Commit(message, parent, parentCommit.getBlobs());
        File newCommitFile = Utils.join(COMMITS, newCommit.getID());
        if (message.isEmpty()) {
            System.out.println("Please enter a commit message.");
        }
        if (stage1.getCurrStage().isEmpty()
            && stage1.getRemovalStage().isEmpty()) {
            System.out.println("No changes added to the commit.");
        }
        for (String filename: stage1.getCurrStage().keySet()) {
            if (parentCommit.getBlobs().containsKey(filename)
                    || stage1.getRemovalStage().containsKey(filename)) {
                newCommit.getBlobs().remove(filename);
            }
        }
        newCommit.getBlobs().putAll(stage1.getCurrStage());
        Utils.writeObject(newCommitFile, newCommit);
        stage1.clearStage();
        stage1.clearRemovalStage();
        Utils.writeObject(Utils.join(STAGING_AREA, "currentStage"), stage1);
        Utils.writeObject(Utils.join(BRANCHES, "head"), newCommit);
        Utils.writeObject(Utils.join(BRANCHES, getCurrentBranch()), newCommit);
    }

    public static void checkoutHead(String args) {
        String head =
                Utils.readObject(Utils.join(BRANCHES, "head"),
                        Commit.class).getID();
        checkoutCommit(head, args);
    }

    public static void checkoutCommit(String commitID, String fileName) {
        if (!Utils.join(COMMITS, commitID).exists()) {
            System.out.println("No commit with that id exists.");
            return;
        }
        Commit specified =
                Utils.readObject(Utils.join(COMMITS, commitID), Commit.class);
        if (!specified.getBlobs().containsKey(fileName)) {
            System.out.println("File does not exist in that commit.");
            return;
        }
        String fileID = specified.getBlobs().get(fileName);
        byte[] oldContents = Utils.readContents(Utils.join(BLOBS, fileID));
        File currentFile = Utils.join(CWD, fileName);
        Utils.writeContents(currentFile, (Object) oldContents);
    }

    public static void checkoutBranch(String branchName) {
        if (!Utils.join(BRANCHES, branchName).exists()) {
            System.out.println("No such branch exists.");
        } else if (branchName.equals(getCurrentBranch())) {
            System.out.println("No need to checkout the current branch.");
        } else {
            Commit curr = headCommit();
            Commit checkoutC = Utils.readObject
                    (Utils.join(BRANCHES, branchName), Commit.class);
            StagingArea stage1 = getStageObject();
            List<String> workingFiles = Utils.plainFilenamesIn(CWD);
            for (String s: workingFiles) {
                if (!curr.getBlobs().containsKey(s)
                    && !stage1.getCurrStage().containsKey(s)) {
                    System.out.println("There is an untracked "
                            + "file in the way; delete it, "
                            + "or add and commit it first.");
                }
            }
            for (String fileName: curr.getBlobs().keySet()) {
                if (!checkoutC.getBlobs().containsKey(fileName)) {
                    WRepo.rm(fileName);
                }
            }
            for (String filename: checkoutC.getBlobs().keySet()) {
                String contents = Utils.readContentsAsString
                        (Utils.join
                                (BLOBS, checkoutC.getBlobs().get(filename)));
                Utils.writeContents(Utils.join(CWD, filename), contents);
            }
            stage1.clearStage();
            stage1.clearRemovalStage();
            Utils.writeObject(Utils.join
                    (STAGING_AREA, "currentStage"), stage1);
            Utils.writeObject(Utils.join(BRANCHES, "head"), checkoutC);
            Utils.writeContents(save, branchName);
        }
    }

    public static void log() {
        Commit current = Utils.readObject
                (Utils.join(BRANCHES, "head"), Commit.class);
        while (current != null) {
            System.out.println("===");
            System.out.println("commit " + current.getID());
            System.out.println("Date: " + current.getTimeStamp());
            System.out.println(current.getMessage());
            System.out.println();
            if (!Objects.equals(current.getMessage(), "initial commit")) {
                current = Utils.readObject
                        (Utils.join
                                (COMMITS, current.getParent()), Commit.class);
            } else {
                break;
            }
        }
    }

    public static void globallog() {
        List<String> allCommits = Utils.plainFilenamesIn(COMMITS);
        if (allCommits == null) {
            return;
        }
        for (String commitID: allCommits) {
            Commit c = getCommit(commitID);
            System.out.println("===");
            System.out.println("commit " + c.getID());
            System.out.println("Date: " + c.getTimeStamp());
            System.out.println(c.getMessage());
            System.out.println();
        }
    }

    public static void rm(String fileName) {
        Commit currCommit = headCommit();
        StagingArea currStage = getStageObject();
        if (!currCommit.getBlobs().containsKey(fileName)
                && !currStage.getCurrStage().containsKey(fileName)) {
            System.out.println("No reason to remove the file.");
        } else {
            if (currStage.getCurrStage().containsKey(fileName)) {
                currStage.removeFile(fileName);
            }
            if (currCommit.getBlobs().containsKey(fileName)) {
                String fileID = currCommit.getBlobs().get(fileName);
                currStage.getRemovalStage().put(fileName, fileID);
                Utils.restrictedDelete(Utils.join(CWD, fileName));
            }
        }
        Utils.writeObject(Utils.join(STAGING_AREA, "currentStage"), currStage);
    }

    public static void find(String commitMessage) {
        List<String> allCommits = Utils.plainFilenamesIn(COMMITS);
        if (allCommits == null) {
            return;
        }
        int tracker = allCommits.size();
        for (String commitID: allCommits) {
            if (commitMessage.equals(Utils.readObject(Utils.join
                    (COMMITS, commitID),
                    Commit.class).getMessage())) {
                tracker--;
                System.out.println(commitID);
            }
        }
        if (tracker == allCommits.size()) {
            System.out.println("Found no commit with that message");
        }
    }

    public static void branch(String branchName) {
        if (Utils.join(BRANCHES, branchName).exists()) {
            System.out.println("A branch with that name already exists.");
        } else {
            File newBranch = Utils.join(BRANCHES, branchName);
            Utils.writeObject(newBranch, headCommit());
        }
    }

    public static void removeBranch(String branchName) {
        File branchFile = Utils.join(BRANCHES, branchName);
        if (!branchFile.exists()) {
            System.out.println("A branch with that name does not exist.");
        } else if (branchName.equals(getCurrentBranch())) {
            System.out.println("Cannot remove the current branch.");
        } else {
            branchFile.delete();
        }
    }

    public static void status() {
        List<String> branchNames = Utils.plainFilenamesIn(BRANCHES);
        System.out.println("=== Branches ===");
        if (branchNames == null) {
            System.out.println();
        } else {
            branchNames.sort(String.CASE_INSENSITIVE_ORDER);
            for (String name : branchNames) {
                if (name.equals(getCurrentBranch())) {
                    System.out.println("*" + name);
                } else if (!name.equals("head")) {
                    System.out.println(name);
                }
            }
            System.out.println();
        }
        System.out.println("=== Staged Files ===");
        for (String s: getStageObject().getCurrStage().keySet()) {
            System.out.println(s);
        }
        System.out.println();
        System.out.println("=== Removed Files ===");
        for (String s: getStageObject().getRemovalStage().keySet()) {
            System.out.println(s);
        }
        System.out.println();
        System.out.println("=== Modifications Not Staged For Commit ===");
        System.out.println();
        System.out.println("=== Untracked Files ===");
        System.out.println();
    }

    public static void reset(String commitID) {
        if (!Utils.join(COMMITS, commitID).exists()) {
            System.out.println("No commit with that id exists.");
            return;
        }
        StagingArea st = getStageObject();
        Commit head = headCommit();
        Commit c = getCommit(commitID);
        List<String> workingFiles = Utils.plainFilenamesIn(CWD);
        for (String s: workingFiles) {
            if (c.getBlobs().containsKey(s)
                    && !st.getCurrStage().containsKey(s)) {
                if (!c.getBlobs().get(s).equals(head.getBlobs().get(s))) {
                    System.out.println("There is an untracked file "
                            + "in the way; delete it, or add and commit it "
                            + "first.");
                    return;
                }
            }
        }
        for (String fileName: c.getBlobs().keySet()) {
            if (!st.getCurrStage().containsKey(fileName)) {
                WRepo.rm(fileName);
            }

            WRepo.checkoutCommit(commitID, fileName);
        }
        st.clearStage();
        st.clearRemovalStage();
        Utils.writeObject(Utils.join(BRANCHES, "head"), c);
        Utils.writeObject(Utils.join(STAGING_AREA, "currentStage"), st);
    }

    public static Commit findSplit(Commit commit1, Commit commit2) {
        ArrayList<String> tracker = new ArrayList<>();
        ArrayList<String> tracker2 = new ArrayList<>();
        String commit1ID = commit1.getID();
        String commit2ID = commit2.getID();
        while (commit1ID != null) {
            Commit com1 = getCommit(commit1ID);
            tracker.add(commit1ID);
            commit1ID = com1.getParent();
        }
        while (commit2ID != null) {
            Commit com2 = getCommit(commit2ID);
            tracker2.add(commit2ID);
            commit2ID = com2.getParent();
        }
        for (String commitID: tracker) {
            if (tracker2.contains(commitID)) {
                return getCommit(commitID);
            }
        }
        return null;
    }

    public static boolean mergeConflict
    (Commit headCommit, Commit branchCommit,
        Commit splitCommit, String fileName) {
        boolean conflict = false;
        if (((!splitCommit.getBlobs().get(fileName)
                .equals(headCommit.getBlobs().get(fileName))
            &&
            !splitCommit.getBlobs().get(fileName)
                    .equals(branchCommit.getBlobs().get(fileName)))
            &&
            !headCommit.getBlobs().get(fileName)
                    .equals(branchCommit.getBlobs().get(fileName)))
            || (!branchCommit.getBlobs().containsKey(fileName)
            && (!splitCommit.getBlobs().get(fileName)
                .equals(headCommit.getBlobs().get(fileName))))
            || (!headCommit.getBlobs().containsKey(fileName)
            && (!splitCommit.getBlobs().get(fileName)
                .equals(branchCommit.getBlobs().get(fileName))))
            ||
            (!splitCommit.getBlobs().containsKey(fileName)
            &&
            (!branchCommit.getBlobs().get(fileName)
                    .equals(headCommit.getBlobs().get(fileName))))) {
            conflict = true;
            Commit c = Utils.readObject(Utils.join
                    (BRANCHES, getCurrentBranch()), Commit.class);
            String currC = "";
            currC += "<<<<<<< HEAD" + "\n";
            currC += Utils.readContentsAsString
                    (Utils.join(BLOBS, headCommit.getBlobs().get(fileName)));
            currC += "\n";
            currC += "=======" + "\n";
            currC += Utils.readContentsAsString
                    (Utils.join(BLOBS, branchCommit.getBlobs().get(fileName)));
            currC += ">>>>>>>" + "\n";
            Utils.writeContents(Utils.join(CWD, fileName), currC);
            WRepo.add(fileName);
        }
        return conflict;
    }

    public static void merge(String branchName) {
        if (!Utils.join(BRANCHES, branchName).exists()) {
            System.out.println("A branch with that name does not exist.");
            return;
        }
        if (!getStageObject().getCurrStage().isEmpty()
            || !getStageObject().getRemovalStage().isEmpty()) {
            System.out.println("You have uncommitted changes.");
            return;
        }
        Commit branchCommit = Utils.readObject(Utils.join
                (BRANCHES, branchName), Commit.class);
        Commit headCommit = headCommit();
        if (branchCommit.equals(headCommit)) {
            System.out.println("Cannot merge a branch with itself.");
            return;
        }
        Commit splitCommit = findSplit(branchCommit, headCommit);
        if (splitCommit.getID().equals(branchCommit.getID())) {
            System.out.println("Given branch is an ancestor of the current "
                    + "branch.");
            return;
        }
        if (splitCommit.getID().equals(headCommit.getID())) {
            Utils.restrictedDelete("f.txt");
            System.out.println("Current branch fast-forwarded.");
            return;
        }
        ArrayList<String> allFiles = new ArrayList<>();
        allFiles.addAll(branchCommit.getBlobs().keySet());
        for (String fileName: headCommit.getBlobs().keySet()) {
            if (!allFiles.contains(fileName)) {
                allFiles.add(fileName);
            }
        }
        for (String fileName: splitCommit.getBlobs().keySet()) {
            if (!allFiles.contains(fileName)) {
                allFiles.add(fileName);
            }
        }
        for (String fileName: allFiles) {
            if (!splitCommit.getBlobs().containsKey(fileName)
                    && !branchCommit.getBlobs().containsKey(fileName)
                    && headCommit.getBlobs().containsKey(fileName)) {
                continue;
            } else {
                mergeHelper(headCommit, branchCommit, splitCommit, fileName);
            }
        }
        WRepo.commit("Merged " + branchName + " into " + getCurrentBranch()
                + ".");
        System.out.println();
    }

    public static void mergeHelper
    (Commit headCommit, Commit branchCommit,
             Commit splitCommit, String fileName) {
        if (!splitCommit.getBlobs().containsKey(fileName)
                && branchCommit.getBlobs().containsKey(fileName)
                && !headCommit.getBlobs().containsKey(fileName)) {
            File specfile = Utils.join(CWD, fileName);
            byte[] needed = Utils.readContents(Utils.join
                    (BLOBS, branchCommit.getBlobs().get(fileName)));
            Utils.writeContents(specfile, needed);
            WRepo.add(fileName);
        } else if (splitCommit.getBlobs().get(fileName)
                .equals(branchCommit.getBlobs().get(fileName))) {
            if (splitCommit.getBlobs().get(fileName)
                    .equals(headCommit.getBlobs().get(fileName))) {
                WRepo.rm(fileName);
            }
        } else if (splitCommit.getBlobs().get(fileName)
                .equals(headCommit.getBlobs().get(fileName))) {
            if (!branchCommit.getBlobs().containsKey(fileName)) {
                WRepo.rm(fileName);
            } else if (!splitCommit.getBlobs().get(fileName)
                    .equals(branchCommit.getBlobs().get(fileName))) {
                File specfile = Utils.join(CWD, fileName);
                byte[] needed = Utils.readContents(Utils.join
                        (BLOBS, branchCommit.getBlobs().get(fileName)));
                Utils.writeContents(specfile, needed);
                WRepo.add(fileName);
            }
        } else if (mergeConflict(headCommit, branchCommit, splitCommit,
                fileName)) {
            System.out.println("Encountered a merge conflict.");
        }
    }

    public static Commit headCommit() {
        return Utils.readObject(Utils.join(BRANCHES, "head"), Commit.class);
    }

    public static StagingArea getStageObject() {
        return Utils.readObject(Utils.join(STAGING_AREA, "currentStage"),
                StagingArea.class);
    }

    public static Commit getCommit(String commitID) {
        return Utils.readObject(Utils.join(COMMITS, commitID), Commit.class);
    }

    public static String getCurrentBranch() {
        return Utils.readContentsAsString(save);
    }

}
