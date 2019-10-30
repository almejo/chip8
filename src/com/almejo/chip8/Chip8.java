/**
 * Created by alejo on 20-12-16.
 */

package com.almejo.chip8;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class Chip8 {

	private int[] memory = new int[4096];
	private int[] V = new int[16];
	private int I;
	private int pc;
	private int[] gfx = new int[64 * 32];
	private int delayTimer;
	private int soundTimer;
	private int[] stack = new int[16];
	private int sp;
	private int[] key = new int[16];
	private int[] fonts = {
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
	private boolean debug;
	private List<Chip8StateChangeLisener> stateChangeLisenerList = new LinkedList<>();

	void emulateCycle() {
		int opcode = fetch();
		decode(opcode);
		updateTimers();
		if (debug) {
			printState();
		}
	}

	private void printState() {
		System.out.print("I=" + formattedHex(I) + " ");
		System.out.print("PC=" + formattedHex(pc) + " ");
		System.out.print("C=" + formattedHex(V[0xF]) + " ");
		System.out.print(" [");
		for (int i = 0; i < 0xF; i++) {
			System.out.print(" " + formattedHex(i) + ":" + formattedHex(V[i]));
		}
		System.out.print(" ]");
		System.out.print(" [");
		for (int i = 0; i < key.length; i++) {
			System.out.print(" " + i + ":" + key[i]);
		}
		System.out.print(" ]");
		System.out.println();
	}

	private void updateTimers() {
		if (delayTimer > 0) {
			--delayTimer;
		}

		if (soundTimer > 0) {
			if (soundTimer == 1) {
				//System.out.println("BEEP!");
			}
			--soundTimer;
		}
	}

	private void decode(int opcode) {
		//System.out.print(formattedHex(opcode) + " -> ");
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
			case 0x4000:
				execute4000(opcode);
				break;
			case 0x6000:
				execute6000(opcode);
				break;
			case 0x7000:
				execute7000(opcode);
				break;
			case 0x8000:
				execute8000(opcode);
				break;
			case 0xA000:
				executeA000(opcode);
				break;
			case 0xC000:
				executeC000(opcode);
				break;
			case 0xD000:
				executeD000(opcode);
				break;
			case 0xE000:
				executeE000(opcode);
				break;
			case 0xF000:
				executeF000(opcode);
				break;
			default:
				System.out.println("Unknown opcode: " + Integer.toHexString(opcode));
				throw new StopEmulationException();
		}
		stateChanged();
	}

	private void executeD000(int opcode) {
		if (debug) {
			System.out.println("[0xDXYN]:"
					+ " X= " + getX(opcode) + " (" + formattedHex(getX(opcode)) + ")"
					+ " Y= " + getY(opcode) + " (" + formattedHex(getY(opcode)) + ")"
					+ " N= " + getN(opcode) + " (" + formattedHex(getN(opcode)) + ")");
		}
		int x = V[getX(opcode)];
		int y = V[getY(opcode)];
		int height = getN(opcode);
		int pixel;

		V[0xF] = 0;
		for (int yline = 0; yline < height; yline++) {
			pixel = memory[I + yline];
			for (int xline = 0; xline < 8; xline++) {
				if ((pixel & (0x80 >> xline)) != 0) {
					if (gfx[(x + xline + ((y + yline) * 64))] == 1) {
						V[0xF] = 1;
					}
					gfx[x + xline + ((y + yline) * 64)] ^= 1;
				}
			}
		}

		drawFlag = true;
		pc += 2;
	}

	private void execute2000(int opcode) {
		if (debug) {
			System.out.println("[0x2NNN]: NNN= " + getNNN(opcode) + " (" + formattedHex(getNNN(opcode)) + ")");
		}
		stack[sp] = pc;
		++sp;
		pc = getNNN(opcode);
	}

	private void executeE000(int opcode) {
		switch (opcode & 0x00FF) {
			case 0x00A1:
				if (debug) {
					System.out.println("[0xEXA1]: X= " + getX(opcode) + " (" + formattedHex(getX(opcode)) + ") V" + getX(opcode) + "=" + V[getX(opcode)] + " key[VX]= " + key[V[getX(opcode)]]);
				}
				if (key[V[getX(opcode)]] == 0) {
					pc += 4;
				} else {
					pc += 2;
				}
				break;
			default:
				System.out.println("Unknown opcode  [0xE000]: " + Integer.toHexString(opcode));
				throw new StopEmulationException();
		}
	}

	private void executeF000(int opcode) {
		switch (opcode & 0x00FF) {
			case 0x0007: // FX07: Sets VX to the value of the delay timer.
				if (debug) {
					System.out.println("[0x0007]: X= " + getX(opcode) + " (" + formattedHex(getX(opcode)) + ")");
				}
				V[getX(opcode)] = delayTimer;
				pc += 2;
				break;
			case 0x0015: // FX15: Sets the delay timer to VX.
				if (debug) {
					System.out.println("[0x0015]: X= " + getX(opcode) + " (" + formattedHex(getX(opcode)) + ")");
				}
				delayTimer = V[getX(opcode)];
				pc += 2;
				break;
			case 0x001E: // FX1E: Adds VX to I
				if (debug) {
					System.out.println("[0x001E]: X= " + getX(opcode) + " (" + formattedHex(getX(opcode)) + ")");
				}
				I += V[getX(opcode)];
				pc += 2;
				break;
			case 0x0018: // FX15: Sets the sound timer to VX.
				if (debug) {
					System.out.println("[0x0018]: X= " + getX(opcode) + " (" + formattedHex(getX(opcode)) + ")");
				}
				soundTimer = V[getX(opcode)];
				pc += 2;
				break;
			case 0x0029: // FX29: Sets I to the location of the sprite for the character in VX. Characters 0-F (in hexadecimal) are represented by a 4x5 font
				if (debug) {
					System.out.println("[0xFX29]: X= " + getX(opcode) + " (" + formattedHex(getX(opcode)) + ")");
				}
				I = V[getX(opcode)] * 0x5;
				pc += 2;
				break;
			case 0x0033:
				if (debug) {
					System.out.println("[0xFX33]: X= " + getX(opcode) + " (" + formattedHex(getX(opcode)) + ")");
				}
				memory[I] = V[getX(opcode)] / 100;
				memory[I + 1] = (V[getX(opcode)] / 10) % 10;
				memory[I + 2] = (V[getX(opcode)] % 100) % 10;
				pc += 2;
				break;
			case 0x0065:
				if (debug) {
					System.out.println("[0xFX65]: X= " + getX(opcode) + " (" + formattedHex(getX(opcode)) + ")");
				}
				System.arraycopy(memory, I, V, 0, getX(opcode) + 1);
				// On the original interpreter, when the operation is done, I = I + X + 1.
				I += getX(opcode) + 1;
				pc += 2;
				break;
			default:
				System.out.println("Unknown opcode  [0xF000]: " + Integer.toHexString(opcode));
				throw new StopEmulationException();
		}
	}

	/**
	 * Skips the next instruction if VX equals NN. (Usually the next instruction is a jump to skip a code block)
	 *
	 * @param opcode the operation code
	 */
	private void execute3000(int opcode) {
		if (debug) {
			System.out.println("[0x3XNN]: X= " + getX(opcode) + ", (" + formattedHex(getNN(opcode)) + ")");
		}
		if (V[getX(opcode)] == getNN(opcode)) {
			pc += 4;
			return;
		}
		pc += 2;
	}

	/**
	 * Skips the next instruction if VX doesn't equal NN. (Usually the next instruction is a jump to skip a code block)
	 *
	 * @param opcode the operation code
	 */
	private void execute4000(int opcode) {
		if (debug) {
			System.out.println("[0x4XNN]: X= " + getX(opcode) + ", (" + formattedHex(getNN(opcode)) + ") NN= " + getNN(opcode) + ", (" + formattedHex(getNN(opcode)) + ")");
		}
		if (V[getX(opcode)] != getNN(opcode)) {
			pc += 4;
			return;
		}
		pc += 2;
	}

	private void execute7000(int opcode) {
		if (debug) {
			System.out.println("[0x7XNN]: X= " + getX(opcode) + ", NN= " + getNN(opcode));
		}
		updateSumOverflow(getNN(opcode), V[getX(opcode)]);
		V[getX(opcode)] += getNN(opcode);
		pc += 2;
	}

	private void updateSumOverflow(int x, int y) {
		V[0xF] = x > (0xFF - y) ? 1 : 0;
	}

	private void execute8000(int opcode) {
		switch (opcode & 0x000F) {
			case 0x0000:
				if (debug) {
					System.out.println("[0F8XY0]: X= " + getX(opcode) + " (" + formattedHex(getX(opcode)) + ") Y= " + getY(opcode) + " (" + formattedHex(getY(opcode)) + ")");
				}
				V[getX(opcode)] = V[getY(opcode)];
				pc += 2;
				break;
			case 0x0002:
				if (debug) {
					System.out.println("[0x8XY2]: X= " + getX(opcode) + " (" + formattedHex(getX(opcode)) + ") Y= " + getY(opcode) + " (" + formattedHex(getY(opcode)) + ")");
				}
				V[getX(opcode)] = V[getX(opcode)] & V[getY(opcode)];
				pc += 2;
				break;
			case 0x0004:
				if (debug) {
					System.out.println("[0x8XY4]: X= " + getX(opcode) + ", Y= " + getY(opcode));
				}
				updateSumOverflow(V[getY(opcode)], V[getX(opcode)]);
				V[getX(opcode)] += V[getY(opcode)];
				pc += 2;
				break;
			case 0x0005:
				if (debug) {
					System.out.println("[0x8XY5]: X= " + getX(opcode) + ", Y= " + getY(opcode));
				}
				updateRestBorrow(opcode);
				V[getX(opcode)] -= V[getY(opcode)];
				pc += 2;
				break;
			case 0x000E:
				if (debug) {
					System.out.println("[0x8XYE]: X= " + getX(opcode));
				}
				V[0xF] = V[getX(opcode)] >> 7;
				V[getX(opcode)] <<= 1;
				V[getX(opcode)] = 0xFF & V[getX(opcode)];
				pc += 2;
				break;
			default:
				System.out.println("Unknown opcode  [0x8000]: " + Integer.toHexString(opcode));
				throw new StopEmulationException();
		}
	}

	private void updateRestBorrow(int opcode) {
		V[0xF] = V[getY(opcode)] > V[getX(opcode)] ? 0 : 1;
	}

	private void execute6000(int opcode) {
		if (debug) {
			System.out.println("[0x6XNN]: X= " + getX(opcode) + ", NN= " + getNN(opcode));
		}
		V[getX(opcode)] = getNN(opcode);
		pc += 2;
	}

	private void execute0000(int opcode) {
		switch (opcode & 0x000F) {
			case 0x0000: // 0x00E0: Clears the screen
				if (debug) {
					System.out.println("[0x00E0]: " + Integer.toHexString(opcode));
				}
				Arrays.fill(gfx, 0);
				drawFlag = true;
				pc += 2;
				break;
			case 0x000E: // 0x00EE: Returns from subroutine
				if (debug) {
					System.out.println(" [0x000E]: " + Integer.toHexString(opcode));
				}
				--sp;           // 16 levels of stack, decrease stack pointer to prevent overwrite
				pc = stack[sp]; // Put the stored return address from the stack back into the program counter
				pc += 2;        // Don't forget to increase the program counter!
				break;
			default:
				System.out.println("Unknown opcode  [0x0000]: " + Integer.toHexString(opcode));
				throw new StopEmulationException();
		}
	}

	private void execute1000(int opcode) {
		if (debug) {
			System.out.println("[0x1NNN]: NNN= " + getNNN(opcode));
		}
		pc = getNNN(opcode);
	}

	/**
	 * ANNN: Sets I to the address NNN
	 *
	 * @param opcode operation code
	 */
	private void executeA000(int opcode) {
		if (debug) {
			System.out.println("[0xANNN]: NNN= " + getNNN(opcode) + " (" + formattedHex(getNNN(opcode)) + ")");
		}
		I = getNNN(opcode);
		pc += 2;
	}

	private void executeC000(int opcode) {
		int random = new Random().nextInt(256) & getNN(opcode);
		if (debug) {
			System.out.println("[0xCXNN]: random=" + random + " X= " + getX(opcode) + " (" + formattedHex(getX(opcode)) + ")  NN= " + getNN(opcode) + " (" + formattedHex(getNN(opcode)) + ")");
		}
		V[getX(opcode)] = random;
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

	void initialize(boolean debug) {
		this.debug = debug;
		pc = 0x200;  // Program counter starts at 0x200
		I = 0;       // Reset index register
		sp = 0;      // Reset stack pointer

		Arrays.fill(gfx, 0);
		Arrays.fill(stack, 0);
		Arrays.fill(V, 0);
		Arrays.fill(memory, 0);

		System.arraycopy(fonts, 0, memory, 0, 80);

	}

	void loadProgram(String filename) throws IOException {
		try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
			String line = br.readLine();
			int i = 0;
			while (line != null) {
				if (line.contains(";")) {
					line = line.substring(0, line.indexOf(";"));
				}
				if (line.contains(":")) {
					line = line.substring(line.indexOf(":") + 1);
				}
				line = line.trim();
				System.out.println(line);
				String[] words = line.split("\\s+");
				for (String word : words) {
					System.out.print("word: " + word + " ");
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
		for (int i = 0x200 - 2; i < 1450; i++) {
			if (i % 0x2 == 0) {
				System.out.print("\n0x" + formattedHex(i) + " - " + i + ": ");
			}
			System.out.print(formattedHex(memory[i]) + " ");
		}
		System.out.println("\n*******************");
	}

	private String formattedHex(int value) {
		return value > 0xF ? Integer.toHexString(value) : "0" + Integer.toHexString(value);
	}

	void drawGraphics() {
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
	}

	boolean isDrawFlag() {
		return drawFlag;
	}
	void setDrawFlag(boolean drawFlag) {
		this.drawFlag = drawFlag;
	}

	void setKey(int key, int value) {
		this.key[key] = value;
	}

	private void stateChanged() {
		stateChangeLisenerList.forEach(listener -> listener.onStateChanged(V));
	}

	void addStateChangedListener(Chip8StateChangeLisener listener) {
		stateChangeLisenerList.add(listener);
	}

	boolean isPainted(int x, int y) {
		return gfx[y * 64 + x] > 0;
	}
}