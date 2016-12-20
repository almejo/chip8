package com.almejo.chip8;

import java.io.IOException;

public class Emulator {
	private Chip8 chip8 = new Chip8();

	public static void main(String[] args) throws IOException {
		new Emulator().run();
	}

	private void run() throws IOException {
		setupGraphics();
		setupInput();
		setupSound();
		chip8.initialize();
		chip8.loadProgram("/home/alejo/git/chip8/src/com/almejo/chip8/pong.asm");
		while (true) {
			try {
				chip8.emulateCycle();
				chip8.drawGraphics();
			} catch (StopEmulationException e) {
				System.out.println("stopping emulation");
				break;
			}
		}
	}

	private void setupGraphics() {
	}

	private void setupInput() {
	}

	private void setupSound() {
	}
}
