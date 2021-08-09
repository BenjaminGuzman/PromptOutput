/*
 * Copyright (c) 2021. Benjamín Antonio Velasco Guzmán
 * Author: Benjamín Antonio Velasco Guzmán <bg@benjaminguzman.dev>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
