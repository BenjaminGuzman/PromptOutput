package net.benjaminguzman;

import org.junit.jupiter.api.*;

import java.io.*;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class PromptOutputStreamTest {
	PrintStream originalOut;

	@BeforeEach()
	void saveOut() {
		originalOut = System.out;
	}

	@AfterEach()
	void restoreOut() {
		System.setOut(originalOut);
	}

	/**
	 * Generate an ascii string.
	 * <p>
	 * Control characters are not included in the string
	 *
	 * @param length length of the string generated
	 * @return the generated string
	 */
	String randomAsciiString(int length) {
		int left_limit = 32; // space
		int right_limit = 126; // ~

		return new Random()
			.ints(left_limit, right_limit + 1)
			.limit(length)
			.collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
			.toString();
	}

	@Test()
	@DisplayName("Prompt should be written after new line")
	void singleThread() throws IOException {
		// initialize Output
		PipedOutputStream outputStream = new PipedOutputStream();
		PromptOutputStream promptOutputStream = new PromptOutputStream(outputStream);

		// change default stdout
		System.setOut(new PrintStream(promptOutputStream, true));

		// generate some random strings
		int N_TEST_STRINGS = 1_000;
		List<String> strings = new Random()
			.ints(5, 1_000) // length of the generated string
			.limit(N_TEST_STRINGS)
			.parallel()
			.mapToObj(this::randomAsciiString)
			.collect(Collectors.toList());

		Thread writerThread = new Thread(() -> {
			strings.forEach(System.out::println);
			try {
				outputStream.flush();
				outputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
		writerThread.start();

		// initialize Input
		BufferedReader reader = new BufferedReader(new InputStreamReader(new PipedInputStream(outputStream)));

		String expectedPrompt = promptOutputStream.getPrompt();

		// check everything is working as expected
		assertEquals(strings.get(0), reader.readLine()); // check the string was written
		strings.subList(1, strings.size()).forEach(expected -> {
			try {
				// check prompt was printed and then cursor position was reset (with \r)
				assertEquals(expectedPrompt, reader.readLine()); // actual written string is <prompt>\r
				assertEquals(expected, reader.readLine()); // check the expected string was written
				// (this should overwrite the prompt printed before)
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}

	@Test()
	@DisplayName("Prompt should be re-written and should contain the status icon")
	void rewritePrompt() throws IOException {
		// initialize Output
		PipedOutputStream outputStream = new PipedOutputStream();
		PromptOutputStream promptOutputStream = new PromptOutputStream(outputStream);

		List<String> statusIcons = List.of("💀", "☠", "⏳", "💥", "🔥", "♥", "🇲🇽", "🇮🇱", "🇨🇱", "😍",
			"🥰");

		Thread writerThread = new Thread(() -> {
			for (int i = 0; i < statusIcons.size(); ++i)
				promptOutputStream.printPrompt(i + " " + statusIcons.get(i));

			try {
				outputStream.flush();
				outputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
		writerThread.start();

		// initialize Input
		BufferedReader reader = new BufferedReader(new InputStreamReader(new PipedInputStream(outputStream)));

		String line;
		while (reader.readLine() != null) { // skip first \r written
			// first line should contain the index of the prompt and status icon
			line = reader.readLine();
			String[] splittedLine = line.split(" ", 2);
			int idx = Integer.parseInt(splittedLine[0]);
			// originalOut.println(line); // uncomment if you don't have clear what is this doing

			String expectedPrompt = statusIcons.get(idx) + " " + PromptOutputStream.DEFAULT_PROMPT;
			String actualPrompt = splittedLine[1];

			assertEquals(expectedPrompt, actualPrompt);
		}
	}

	@Test()
	@DisplayName("Testing with multiple threads")
	void multiThread() throws IOException {
		// initialize Output
		PipedOutputStream outputStream = new PipedOutputStream();
		PromptOutputStream promptOutputStream = new PromptOutputStream(outputStream, "123> ");

		// change default stdout
		System.setOut(new PrintStream(promptOutputStream, true));

		// generate some random strings
		int N_TEST_STRINGS = 1_000;
		List<String> strings = new Random()
			.ints(5, 100) // length of the generated string
			.limit(N_TEST_STRINGS)
			.parallel()
			.mapToObj(this::randomAsciiString)
			.collect(Collectors.toList());

		int N_THREADS = 5;
		ExecutorService writerExecutorService = Executors.newFixedThreadPool(N_THREADS);
		for (int i = 0; i < N_THREADS; ++i)
			// writing to PipedOutputStream from different threads may not be a good idea, but let's 🤞
			// https://techtavern.wordpress.com/2008/07/16/whats-this-ioexception-write-end-dead/
			writerExecutorService.submit(() -> strings.forEach(System.out::println));
		writerExecutorService.submit(() -> {
			try {
				boolean timed_out = writerExecutorService.awaitTermination(15, TimeUnit.SECONDS);
				if (timed_out)
					System.err.println("executorService took too long to finish");
			} catch (InterruptedException e) {
				e.printStackTrace();
			} finally {
				try {
					outputStream.flush();
					outputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
		writerExecutorService.shutdown();

		// initialize Input
		BufferedReader reader = new BufferedReader(new InputStreamReader(new PipedInputStream(outputStream)));
		String line;
		String prompt = promptOutputStream.getPrompt();
		for (int i = 0; i < N_TEST_STRINGS * N_THREADS; ++i) {
			if ((line = reader.readLine()) == null) // check EOF has not been reached
				break;
			assertFalse(line.contains(prompt)); // check the output does not contain the prompt (meaning
			// it has been corrupted)

			if ((line = reader.readLine()) == null) // check EOF has not been reached
				break;
			assertEquals(prompt, line); // output should be ordered, first the line, then the prompt

			++i;
		}
	}

	@Test()
	@DisplayName("Example using stdout")
	void example() {
		// initialize IO
		PromptOutputStream pos = new PromptOutputStream(System.out);

		// change default stdout
		System.setOut(new PrintStream(pos, true));

		// write some stuff to stdout
		System.out.println("Just an example");
		System.out.println("Check output is not corrupted");
		System.out.println("The prompt should appear below this line");
	}

	@Test()
	@DisplayName("Performance with PipedOutputStream")
	void performance() {
		long start_time;
		long end_time;
		long time1, time2;

		PipedOutputStream outputStream = new PipedOutputStream();

		// generate some random strings
		int N_TEST_STRINGS = 10_000;
		List<String> strings = new Random()
			.ints(5, 1_000) // length of the generated string
			.limit(N_TEST_STRINGS)
			.parallel()
			.mapToObj(this::randomAsciiString)
			.collect(Collectors.toList());

		//// Test without custom out
		// change default stdout
		System.setOut(new PrintStream(outputStream, true));

		start_time = System.currentTimeMillis();
		strings.forEach(System.out::println);
		end_time = System.currentTimeMillis();
		time1 = end_time - start_time;
		originalOut.println("Without PromptOutputStream and " + N_TEST_STRINGS + " strings, execution took: " + time1 + "ms");

		//// Test with custom out
		// change default stdout
		System.setOut(new PrintStream(new PromptOutputStream(outputStream), true));

		start_time = System.currentTimeMillis();
		strings.forEach(System.out::println);
		end_time = System.currentTimeMillis();
		time2 = (end_time - start_time);
		originalOut.println("With PromptOutputStream and " + N_TEST_STRINGS + " strings, execution took: " + time2 + "ms");

		originalOut.println("Difference is: " + (time2 - time1) + "ms = " + Math.abs(time2 - time1) / 1_000f + "s");
		Assertions.assertTrue(Math.abs(time2 - time1) / 1_000f <= 2); // time difference should be very low
	}

	@Test()
	@DisplayName("Performance with real stdout output")
	void performanceNoPipe() {
		long start_time;
		long end_time;
		long time1, time2;

		originalOut.println();
		originalOut.println();
		originalOut.println();

		// generate some random strings
		int N_TEST_STRINGS = 1_000;
		List<String> strings = new Random()
			.ints(5, 100) // length of the generated string
			.limit(N_TEST_STRINGS)
			.parallel()
			.mapToObj(this::randomAsciiString)
			.collect(Collectors.toList());

		//// Test without custom out
		start_time = System.currentTimeMillis();
		strings.forEach(System.out::println);
		end_time = System.currentTimeMillis();
		time1 = end_time - start_time;
		originalOut.println("Without PromptOutputStream and " + N_TEST_STRINGS + " strings, execution took: " + time1 + "ms");

		//// Test with custom out
		// change default stdout
		System.setErr(new PrintStream(new PromptOutputStream(System.out), true));

		start_time = System.currentTimeMillis();
		strings.forEach(System.out::println);
		end_time = System.currentTimeMillis();
		time2 = (end_time - start_time);
		originalOut.println("With PromptOutputStream and " + N_TEST_STRINGS + " strings, execution took: " + time2 + "ms");

		originalOut.println("Difference is: " + (time2 - time1) + "ms = " + Math.abs(time2 - time1) / 1_000f + "s");
		Assertions.assertTrue(Math.abs(time2 - time1) / 1_000f <= 2); // time difference should be very low
	}
}