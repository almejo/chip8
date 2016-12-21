package com.almejo.chip8;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class HexToAsm {
	public static void main(String[] args) throws IOException {
		try (BufferedReader br = new BufferedReader(new FileReader("/home/alejo/git/chip8/src/com/almejo/chip8/pong.asm"))) {
			String line = br.readLine();
			int i = 0x200;
			while (line != null) {
				String[] words = line.split("\\s+");
				for (String word : words) {
					String first = "" + word.charAt(0) + word.charAt(1);
					String second = "" + word.charAt(2) + word.charAt(3);

					int opcode = Integer.decode("0x" + first + second);
					System.out.print("0x" + Integer.toHexString(i) + ": ");
					System.out.print(word + " ");
					System.out.println(tranlsateCode(opcode));
					i += 2;
				}
				line = br.readLine();
			}
		}
	}

	private static String tranlsateCode(int opcode) {
		switch (opcode & 0xF000) {
			case 0x0000:
				return translate0000(opcode);
//			case 0x1000:
//				return translate1000(opcode);
			case 0x2000:
				return translate2000(opcode);
//			case 0x3000:
//				return translate3000(opcode);
//			case 0x4000:
//				return translate4000(opcode);
			case 0x6000:
				return translate6000(opcode);
//			case 0x8000:
//				return translate8000(opcode);
//			case 0x7000:
//				return translate7000(opcode);
			case 0xA000:
				return translateA000(opcode);
			case 0xC000:
				return translateC000(opcode);
			case 0xD000:
				return translateD000(opcode);
//			case 0xE000:
//				return translateE000(opcode);
//			case 0xF000:
//				return translateF000(opcode);
			default:
				return "Unknown opcode: " + Integer.toHexString(opcode);
		}
	}

	private static String translateD000(int opcode) {
		return "DXYN: DRAW V[" + getX(opcode) + "], V[" + getY(opcode) + "], " + formattedHex(getN(opcode));
	}

	private static String translateC000(int opcode) {
		return "CXNN: RAND V[" + getX(opcode) + "], " + formattedHex(getNN(opcode));
	}

	private static String translate2000(int opcode) {
		return "2NNN: CALL, " + formattedHex(getNNN(opcode));
	}

	private static String translateA000(int opcode) {
		return "ANNN: SET_I " + formattedHex(getNNN(opcode));
	}

	private static String translate0000(int opcode) {
		switch (opcode & 0x000F) {
			case 0x0000: // 0x00E0: Clears the screen
				return "00E0: CLS";
			case 0x000E: // 0x00EE: Returns from subroutine
				return "00EE: RETURN";
			default:
				System.out.println("Unknown opcode  [0x0000]: " + Integer.toHexString(opcode));
				throw new StopEmulationException();
		}
	}

	private static String translate6000(int opcode) {
		return "6XNN: SET V[" + getX(opcode) + "], " + formattedHex(getNN(opcode));
	}

	private static int getY(int opcode) {
		return (opcode & 0x00F0) >> 4;
	}

	private static int getX(int opcode) {
		return (opcode & 0x0F00) >> 8;
	}

	private static int getN(int opcode) {
		return opcode & 0x000F;
	}

	private static int getNN(int opcode) {
		return opcode & 0x00FF;
	}

	private static int getNNN(int opcode) {
		return opcode & 0x0FFF;
	}

	private static String formattedHex(int value) {
		return value > 0xF ? Integer.toHexString(value) : "0" + Integer.toHexString(value);
	}
}
