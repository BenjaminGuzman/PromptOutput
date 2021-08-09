/*
 * MIT License
 *
 * Copyright (c) 2021. Benjamín Antonio Velasco Guzmán
 * Author: Benjamín Antonio Velasco Guzmán <bg@benjaminguzman.dev>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.benjaminguzman;

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
