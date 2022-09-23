package gitlet;


/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author Matthew Koen
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */


    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.print("Must have at least one argument");
            return;
        }

        String firstArg = args[0];
        switch (firstArg) {
            case "init":
                Repository.init();
                break;
            case "add":
                Repository.add(args[1]);
                break;
            case "commit":
                if ((args.length < 2) || (args[1].equals(""))) {
                    System.out.print("Please enter a commit message.");
                    break;
                }
                Repository.commit(args[1]);
                break;
            case "rm":
                Repository.rm(args[1]);
                break;
            case "log":
                Repository.log();
                break;
            case "global-log":
                Repository.globalLog();
                break;
            case "find":
                Repository.find(args[1]);
                break;
            case "status":
                Repository.status();
                break;
            case "checkout":
                if (args.length == 3) {
                    Repository.checkout(args[2]);
                } else if (args.length == 4) {
                    if (!args[2].equals("--")) {
                        System.out.print("Incorrect operands.");
                        break;
                    }
                    Repository.checkout(args[3], args[1]);
                } else if (args.length == 2) {
                    Repository.checkoutBranch(args[1]);
                }
                break;
            case "branch":
                Repository.branch(args[1]);
                break;
            case "rm-branch":
                Repository.rmBranch(args[1]);
                break;
            case "reset":

                break;
            case "merge":

                break;
            default:
                System.out.print("Unknown argument.");
        }



    }




}
