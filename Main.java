package gitlet;

import java.io.File;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Rayan Taghizadeh
 */

public class Main {

    /** Repo variable. */
    private static WRepo repo = new WRepo();

    /** Current working directory. */
    static final File CWD = new File(System.getProperty("user.dir"));

    /** Creation of .gitlet directory. */
    static final File GITLET = Utils.join(CWD, ".gitlet");

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            return;
        } else {
            Main.mainHelper(args);
        }
    }

    public static void checkoutHelp(String[] args) {
        if (args.length == 2) {
            WRepo.checkoutBranch(args[1]);
        }
        if (args.length == 3) {
            WRepo.checkoutHead(args[2]);
        }
        if (args.length == 4) {
            if (args[2].equals("--")) {
                WRepo.checkoutCommit(args[1], args[3]);
            } else {
                System.out.println("Incorrect operands.");
            }
        }
    }

    public static void mainHelper(String[] args) {
        for (String s: args) {
            if (s.equals("init")) {
                WRepo.init();
                return;
            } else if (s.equals("commit")) {
                WRepo.commit(args[1]);
                return;
            } else if (s.equals("add")) {
                WRepo.add(args[1]);
                return;
            } else if (s.equals("log")) {
                WRepo.log();
                return;
            } else if (s.equals("checkout")) {
                Main.checkoutHelp(args);
                return;
            } else if (s.equals("rm")) {
                WRepo.rm(args[1]);
                return;
            } else if (s.equals("find")) {
                WRepo.find(args[1]);
                return;
            } else if (s.equals("global-log")) {
                WRepo.globallog();
                return;
            } else if (s.equals("branch")) {
                WRepo.branch(args[1]);
                return;
            } else if (s.equals("rm-branch")) {
                WRepo.removeBranch(args[1]);
                return;
            } else if (s.equals("status")) {
                if (!GITLET.exists()) {
                    System.out.println("Not in an initialized Gitlet "
                            + "directory.");
                    return;
                } else {
                    WRepo.status();
                    return;
                }
            } else if (s.equals("merge")) {
                WRepo.merge(args[1]);
                return;
            } else if (s.equals("reset")) {
                WRepo.reset(args[1]);
                return;
            }
        }
        System.out.println("No command with that name exists.");
    }
}
