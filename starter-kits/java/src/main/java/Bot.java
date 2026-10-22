import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Persistent Cyber-Grid bot. Replace the empty actions with your strategy. */
public final class Bot {
    public static void main(String[] args) throws Exception {
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        System.out.println("READY");
        System.out.flush();
        while (input.readLine() != null) {
            System.out.println("{\"actions\":[]}");
            System.out.flush();
        }
    }
}
