/**
 * Created by alejo on 20-12-16.
 */

package com.almejo.chip8;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

public class Chip8 {

	private int memory[] = new int[4096];
	private int V[] = new int[16];
	private int I;
	private int pc;
	private int gfx[] = new int[64 * 32];
	private int delay_timer;
	private int sound_timer;
	private int stack[] = new int[16];
	private int sp;
	private int key[] = new int[16];
	private int[] fontset = {
			0xF0, 0x90, 0x90, 0x90, 0xF0, // 0
			0x20, 0x60, 0x20, 0x20, 0x70, // 1
			0xF0, 0x10, 0xF0, 0x80, 0xF0, // 2
			0xF0, 0x10, 0xF0, 0x10, 0xF0, // 3
			0x90, 0x90, 0xF0, 0x10, 0x10, // 4
			0xF0, 0x80, 0xF0, 0x10, 0xF0, // 5
			0xF0, 0x80, 0xF0, 0x90, 0xF0, // 6
			0xF0, 0x10, 0x20, 0x40, 0x40, // 7
			0xF0, 0x90, 0xF0, 0x90, 0xF0, // 8
			0xF0, 0x90, 0xF0, 0x10, 0xF0, // 9
			0xF0, 0x90, 0xF0, 0x90, 0x90, // A
			0xE0, 0x90, 0xE0, 0x90, 0xE0, // B
			0xF0, 0x80, 0x80, 0x80, 0xF0, // C
			0xE0, 0x90, 0x90, 0x90, 0xE0, // D
			0xF0, 0x80, 0xF0, 0x80, 0xF0, // E
			0xF0, 0x80, 0xF0, 0x80, 0x80  // F
	};
	private boolean drawFlag;

	void emulateCycle() {
		int opcode = fetch();
		decode(opcode);
		updateTimers();
		printState();
	}

	private void printState() {
		System.out.print("I=" + formatedHex(I) + " ");
		System.out.print("PC=" + formatedHex(pc) + " ");
		System.out.print("C=" + formatedHex(V[0xF]) + " ");
		System.out.print(" [");
		for (int i = 0; i < 0xF; i++) {
			System.out.print(" " + i + ":" + formatedHex(V[i]));
		}
		System.out.print(" ]");
		System.out.println();
	}

	private void updateTimers() {
		if (delay_timer > 0) {
			--delay_timer;
		}

		if (sound_timer > 0) {
			if (sound_timer == 1) {
				System.out.println("BEEP!");
			}
			--sound_timer;
		}
	}

	private void decode(int opcode) {
		System.out.print(formatedHex(opcode) + " -> ");
		switch (opcode & 0xF000) {
			case 0x0000:
				execute0000(opcode);
				break;
			case 0x1000:
				execute1000(opcode);
				break;
			case 0x2000:
				execute2000(opcode);
				break;
			case 0x3000:
				execute3000(opcode);
				break;
			case 0x6000:
				execute6000(opcode);
				break;
			case 0x8000:
				execute8000(opcode);
				break;
			case 0x7000:
				execute7000(opcode);
				break;
			case 0xA000:
				executeA000(opcode);
				break;
			case 0xD000:
				executeD000(opcode);
				break;
			default:
				System.out.println("Unknown opcode: " + Integer.toHexString(opcode));
				throw new StopEmulationException();
		}
	}

	private void executeD000(int opcode) {
		System.out.println("[0xD000]:"
								   + " X= " + getX(opcode) + " (" + formatedHex(getX(opcode)) + ")"
								   + " Y= " + getY(opcode) + " (" + formatedHex(getY(opcode)) + ") "
								   + " N= " + getN(opcode) + " (" + formatedHex(getN(opcode)) + ")");
		int x = getX(opcode);
		int y = getY(opcode);
		int height = getN(opcode);
		int pixel;

		V[0xF] = 0;
		for (int yline = 0; yline < height; yline++) {
			pixel = memory[I + yline];
			for (int xline = 0; xline < 8; xline++) {
				if ((pixel & (0x80 >> xline)) != 0) {
					if (gfx[(x + xline + ((y + yline) * 64))] == 1)
						V[0xF] = 1;
					gfx[x + xline + ((y + yline) * 64)] ^= 1;
				}
			}
		}

		drawFlag = true;
		pc += 2;
	}

	private void execute2000(int opcode) {
		System.out.println("[0x2NNN]: NNN= " + getNNN(opcode) + " (" + formatedHex(getNNN(opcode)) + ")");
		stack[sp] = pc;
		++sp;
		pc = getNNN(opcode);
	}

	private void execute3000(int opcode) {
		System.out.println("[0x3XNN]: X= " + getX(opcode) + ", (" + formatedHex(getNN(opcode)) + ")");
		if (V[getX(opcode)] == getNN(opcode)) {
			pc += 4;
			return;
		}
		pc += 2;
	}

	private void execute7000(int opcode) {
		System.out.println("[0x7XNN]: X= " + getX(opcode) + ", NN= " + getNN(opcode));
		updateSumOverflow(getNN(opcode), V[getX(opcode)]);
		V[getX(opcode)] += getNN(opcode);
		pc += 2;
	}

	private void updateSumOverflow(int x, int y) {
		V[0xF] = x > (0xFF - y) ? 1 : 0;
	}

	private void execute8000(int opcode) {
		switch (opcode & 0x000F) {
			case 0x0004:
				System.out.println("[0x8XY4]: X= " + getX(opcode) + ", Y= " + getY(opcode));
				updateSumOverflow(V[getY(opcode)], V[getX(opcode)]);
				V[getX(opcode)] += V[getY(opcode)];
				pc += 2;
				break;
			default:
				System.out.println("Unknown opcode  [0x8000]: " + Integer.toHexString(opcode));
		}
	}

	private void execute6000(int opcode) {
		System.out.println("[0x6XNN]: X= " + getX(opcode) + ", NN= " + getNN(opcode));
		V[getX(opcode)] = getNN(opcode);
		pc += 2;
	}

	private void execute0000(int opcode) {
		switch (opcode & 0x000F) {
			case 0x0000: // 0x00E0: Clears the screen
				System.out.println("[0x00E0]: " + Integer.toHexString(opcode));
				Arrays.fill(gfx, 0);
				drawFlag = true;
				pc += 2;
				break;
			case 0x000E: // 0x00EE: Returns from subroutine
				System.out.println(" [0x000E]: " + Integer.toHexString(opcode));
				break;
			default:
				System.out.println("Unknown opcode  [0x0000]: " + Integer.toHexString(opcode));
				throw new StopEmulationException();
		}
	}

	private void execute1000(int opcode) {
		System.out.println("[0x1NNN]: NNN= " + getNNN(opcode));
		pc = getNNN(opcode);
	}

	/**
	 * ANNN: Sets I to the address NNN
	 *
	 * @param opcode operation code
	 */
	private void executeA000(int opcode) {
		System.out.println("[0xANNN]: NNN= " + getNNN(opcode) + " (" + formatedHex(getNNN(opcode)) + ")");
		I = getNNN(opcode);
		pc += 2;
	}

	private int fetch() {
		return memory[pc] << 8 | memory[pc + 1];
	}

	private int getY(int opcode) {
		return (opcode & 0x00F0) >> 4;
	}

	private int getN(int opcode) {
		return opcode & 0x000F;
	}

	private int getNN(int opcode) {
		return opcode & 0x00FF;
	}

	private int getNNN(int opcode) {
		return opcode & 0x0FFF;
	}

	private int getX(int opcode) {
		return (opcode & 0x0F00) >> 8;
	}

	void initialize() {
		pc = 0x200;  // Program counter starts at 0x200
		I = 0;      // Reset index register
		sp = 0;      // Reset stack pointer

		Arrays.fill(gfx, 0);
		Arrays.fill(stack, 0);
		Arrays.fill(V, 0);
		Arrays.fill(memory, 0);

		System.arraycopy(fontset, 0, memory, 0, 80);

	}

	void loadProgram(String filename) throws IOException {
		try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
			String line = br.readLine();
			int i = 0;
			while (line != null) {
				String[] words = line.split("\\s+");
				for (String word : words) {
					System.out.print(word + " ");
					String first = "" + word.charAt(0) + word.charAt(1);
					String second = "" + word.charAt(2) + word.charAt(3);

					System.out.print(first + " ");
					System.out.print(second + " ");
					System.out.print("1: " + Integer.decode("0x" + first) + " ");
					System.out.print("2: " + Integer.decode("0x" + second) + " ");
					memory[0x200 + i] = Integer.decode("0x" + first);
					memory[0x200 + i + 1] = Integer.decode("0x" + second);
					i += 2;
				}
				System.out.println();
				line = br.readLine();
			}
		}
		System.out.println("\n*******************");
		for (int i = 0x200 - 2; i < 840; i++) {
			if (i % 0xF == 0) {
				System.out.print("\n" + i + ": ");
			}
			System.out.print(formatedHex(memory[i]) + " ");
		}
		System.out.println("\n*******************");
	}

	private String formatedHex(int value) {
		return value > 0xF ? Integer.toHexString(value) : "0" + Integer.toHexString(value);
	}

	void drawGraphics() {
		if (drawFlag) {
			System.out.println();
			for (int x = 0; x < 66; x++) {
				System.out.print("*");
			}
			System.out.println();
			for (int y = 0; y < 32; y++) {
				System.out.print("*");
				for (int x = 0; x < 64; x++) {
					System.out.print(gfx[y * 64 + x] > 0 ? "1" : " ");
				}
				System.out.print("*");
				System.out.println();
			}
			for (int x = 0; x < 66; x++) {
				System.out.print("*");
			}
			System.out.println();
			drawFlag = false;
		}
	}
}
