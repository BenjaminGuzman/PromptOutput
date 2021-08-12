# PromptOutput

Simple Java utility to print a prompt after any new line. Intended to be used within applications with interactive
input.

## Usage

## Maven

Add the maven dependency

```xml
<dependency>
    <groupId>net.benjaminguzman</groupId>
    <artifactId>PromptOutput</artifactId>
    <version>1.1.1</version>
</dependency>
```

## Code

Usage is very easy:

```Java
// initialization
PromptOutputStream promptOutStream = new PromptOutputStream(System.out, "$ ");

// change default stdout
System.setOut(new PrintStream(promptOutStream, true));
// setting autoflush (second argument) to true is important to flush internal PrintStream buffer after a new line is found
```

### Full code example

```Java
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

public class Example {
    public static void main(String... args) throws IOException {
        // use the prompt output stream
        PromptOutputStream promptOutStream = new PromptOutputStream(System.out)
            .setPrompt("$ ")
            .setStatusIcon("🧪");
        System.setOut(new PrintStream(promptOutStream, true));

        System.out.println("Input multiple lines of text and see how prompt and icon changes");
        System.out.println("Enter \"quit\" to exit");

        Deque<String> prompts = new LinkedList<>(List.of(">>> ", "# ", "$ ", "> "));
        Deque<String> statusIcons = new LinkedList<>(List.of("⏳", "💀", "🙈"));

        // read from stdin
        BufferedReader bufferedReader = new BufferedReader( // add buffer for optimization
            new InputStreamReader( // bridge bytes -> chars (using default encoding)
                System.in // Input stream (bytes)
            )
        );

        String line;
        while ((line = bufferedReader.readLine()) != null && !line.equalsIgnoreCase("quit")) {
            // change status icon and prompt in a circular manner
            String statusIcon = statusIcons.poll();
            String prompt = prompts.poll();
            promptOutStream.setStatusIcon(statusIcon);
            promptOutStream.setPrompt(prompt);
            statusIcons.add(statusIcon);
            prompts.add(prompt);

            System.out.println("Text entered: " + line);

            // if you just want to print the prompt again and nothing else, use
            // promptOutStream.printPrompt();
        }
    }
}
```

You can find this full example in
[src/test/java/net/benjaminguzman/Example.java](src/test/java/net/benjaminguzman/Example.java)

## Test

Simply run

```shell
mvn clean test
```

to run tests

## Efficiency

Tests include a simple stress test, if you run it, you'll see there is no significant difference between using this
custom prompt output and not using it. They both run in about the same time.

Likewise, you shouldn't be worried about memory or CPU consumption.

## License

[MIT license](./LICENSE)

Copyright © 2021 Benjamín Antonio Velasco Guzmán