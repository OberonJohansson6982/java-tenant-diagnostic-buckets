package devtools;

public final class App {
  public static void main(String[] args) {
    if (args.length < 2 || args.length > 3 || (args.length == 3 && !args[2].equals("--temporary"))) throw new IllegalArgumentException("usage: App <tenant-id> <build-id> [--temporary]");
    String result = new DiagnosticService(new InfraiStorageClient(ServiceConfig.fromEnvironment())).recordBuild(args[0], args[1], args.length == 3);
    System.out.println("Completed diagnostics at " + result);
  }
}
