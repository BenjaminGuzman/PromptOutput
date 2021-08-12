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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * This class provided very similar functionality to {@link OutputStream}, the only differences is that you can
 * configure it to print a prompt after any new line is printed.
 * <p>
 * You can also print an icon/emoji to indicate the status of the application.
 * This may improve user experience
 * <p>
 * This class is intended to be used inside an application with interactive user input.
 * <p>
 * To use it, simply do something like this: {@code
 * PromptOutputStream pos = new PromptOutputStream(System.out);
 * System.setOut(new PrintStream(pos, true));
 * }
 * <p>
 * Note that, in this example the {@link PrintStream} we create contains a {@link PromptOutputStream} which contains
 * another {@link PrintStream} (because {@link System#out} is a {@link PrintStream}).
 * <p>
 * Even though this is not strictly bad, it may be cleaner to just extend {@link PrintStream}, but more code should
 * be written. Despite that, this may be a more general solution.
 * <p>
 * This class is thread-safe.
 * <p>
 * Even though some implementations of {@link OutputStream} or child classes (like {@link java.io.BufferedOutputStream})
 * are synchronized, this needs extra synchronization because this class has specific methods like
 * {@link #printPrompt()}
 */
public class PromptOutputStream extends OutputStream {
	private final OutputStream out;

	/**
	 * The prompt (as bytes) that should be printed
	 * <p>
	 * Examples:
	 * >>>
	 * <p>
	 * >
	 * <p>
	 * $
	 */
	private byte @NotNull [] prompt;

	/**
	 * The status symbol (as bytes) that should be printed
	 * <p>
	 * Examples:
	 * ⏳
	 * <p>
	 * ✔
	 * <p>
	 * ❌
	 */
	private byte @NotNull [] statusIcon;

	/**
	 * Flag to tell if the prompt should be deleted in a next call to any write method.
	 * <p>
	 * More formally this tells if the cursor should be moved to the beginning of the line using \r or \b.
	 * <p>
	 * If the cursor is moved to the beginning of the line, and you start writing text, the previous text will be
	 * overwritten, thus giving the impression that you've deleted the previous text
	 * <p>
	 * This may be set to true after printing the prompt, so the next line to be printed does not include the
	 * prompt
	 */
	private boolean should_delete_prompt;

	/**
	 * Creates a new object with no status icon and no prompt
	 * <p>
	 * In this state, the object is the same as a simple {@link OutputStream} so it is recommended you call
	 * {@link #setPrompt(String)} or {@link #setStatusIcon(String)}
	 *
	 * @param out actual output stream where data will be written
	 */
	public PromptOutputStream(@NotNull OutputStream out) {
		this.out = out;
		this.prompt = new byte[0]; // just ensure it is not null
		this.statusIcon = new byte[0]; // just ensure it is not null
	}

	/**
	 * Set the prompt to be used
	 * <p>
	 * This operation does not write to output
	 * <p>
	 * This method has the same problem described in {@link #setStatusIcon(String)} method documentation
	 *
	 * @param prompt the prompt to be used. If null, no prompt will be shown
	 * @return the same object (so you can use fluent pattern)
	 */
	public PromptOutputStream setPrompt(@Nullable String prompt) {
		if (prompt == null) {
			synchronized (out) {
				this.prompt = new byte[0];
			}
			return this;
		}

		byte[] promptBytes = prompt.getBytes(StandardCharsets.UTF_8);

		synchronized (out) {
			this.prompt = promptBytes;
		}

		return this;
	}

	/**
	 * @return the prompt bytes converted to a string. This is likely to equal the same prompt provided in the
	 * constructor or set with {@link #setPrompt(String)}.
	 * <p>
	 * It is "likely" to equal, because the bytes are decoded using {@link String} constructor
	 */
	@Nullable
	public String getPrompt() {
		return new String(prompt);
	}

	/**
	 * @return the prompt bytes
	 */
	public byte @NotNull [] getPromptBytes() {
		return prompt;
	}

	/**
	 * Set the "icon" (emoji preferably) to show alongside the prompt.
	 * If the icon does not end with a space, a space is added
	 * <p>
	 * This operation does not write to output
	 * <p>
	 * Even though, this is synchronized, that doesn't mean you won't get unexpected output.
	 * <p>
	 * Let's say you have 2 threads. One calling {@link #setStatusIcon(String)} and then {@link #write(byte[])}
	 * (or any write method via println). Suppose the other thread does the same. In such case, the following
	 * behaviour may result:
	 * <p>
	 * Thread 1: changes the status icon to 💥
	 * <p>
	 * Thread 2: changes the status icon to 💀
	 * <p>
	 * Thread 1: any write method is invoked. Then it prints the icon 💀 (wrong)
	 * <p>
	 * Thread 2: any write method is invoked. Then it prints the icon 💀 (good)
	 * <p>
	 * Therefore, you'll need to add extra synchronization or use another method like {@link #printPrompt(String)}
	 *
	 * @param icon the emoji to show. If null, no icon will be shown
	 * @return the same object (so you can use fluent pattern)
	 * @see #printPrompt(String)
	 */
	public PromptOutputStream setStatusIcon(@Nullable String icon) {
		if (icon == null) {
			synchronized (out) {
				statusIcon = new byte[0];
			}

			return this;
		}

		if (!icon.endsWith(" "))
			icon = icon + " ";

		byte[] iconBytes = icon.getBytes(StandardCharsets.UTF_8);

		synchronized (out) {
			statusIcon = iconBytes;
		}

		return this;
	}

	/**
	 * Changes the status icon and prints the prompt.
	 * <p>
	 * This solves the synchronization problem described in {@link #setStatusIcon(String)} method documentation
	 * <p>
	 * This method has very similar behaviour to {@link #printPrompt()}
	 *
	 * @return the same object (so you can use fluent pattern)
	 * @see #printPrompt()
	 */
	public PromptOutputStream printPrompt(@NotNull String icon) {
		try {
			synchronized (out) {
				this.setStatusIcon(icon);
				out.write('\r'); // start writing at the beginning
				out.write(statusIcon);
				out.write(prompt);
				out.flush();
			}
		} catch (IOException ignored) {
		} // just ignore the exception 🤞 it is nothing terribly bad

		return this;
	}

	/**
	 * Prints the prompt.
	 * <p>
	 * This will place the cursor at the beginning of the line and write the prompt (and status),
	 * so be careful as it may overwrite some bytes.
	 * Preferably call this after a line feed (\n) so that doesn't happen
	 *
	 * @return the same object (so you can use fluent pattern)
	 */
	public PromptOutputStream printPrompt() {
		try {
			synchronized (out) {
				out.write('\r'); // start writing at the beginning
				out.write(statusIcon);
				out.write(prompt);
				out.flush();
			}
		} catch (IOException ignored) {
		} // just ignore the exception 🤞 it is nothing terribly bad

		return this;
	}

	@Override
	public void write(int b) throws IOException {
		synchronized (out) {
			if (should_delete_prompt)
				out.write('\r'); // start writing at the beginning

			out.write(b);
		}

		if (b == '\n') {
			synchronized (out) {
				out.write(statusIcon);
				out.write(prompt);
				out.flush();
				should_delete_prompt = true;
			}
		} else
			// no synchronization is added because the worst thing that can happen is just writing an extra
			// \r to output
			should_delete_prompt = false;
	}

	@Override
	public void write(byte @NotNull [] b) throws IOException {
		synchronized (out) {
			if (should_delete_prompt)
				out.write('\r'); // start writing at the beginning

			out.write(b);
		}

		if (b[b.length - 1] == '\n') {
			synchronized (out) {
				out.write(statusIcon);
				out.write(prompt);
				out.flush();
				should_delete_prompt = true;
			}
		} else
			// no synchronization is added because the worst thing that can happen is just writing an extra
			// \r to output
			should_delete_prompt = false;
	}

	@Override
	public void write(byte @NotNull [] b, int off, int len) throws IOException {
		synchronized (out) {
			if (should_delete_prompt)
				out.write('\r'); // start writing at the beginning

			out.write(b, off, len);
		}

		if (b[off + len - 1] == '\n') {
			synchronized (out) {
				out.write(statusIcon);
				out.write(prompt);
				out.flush();
				should_delete_prompt = true;
			}
		} else
			// no synchronization is added because the worst thing that can happen is just writing an extra
			// \r to output
			should_delete_prompt = false;
	}

	@Override
	public void flush() throws IOException {
		synchronized (out) {
			out.flush();
		}
	}

	@Override
	public void close() throws IOException {
		synchronized (out) {
			out.close();
		}
	}
}
