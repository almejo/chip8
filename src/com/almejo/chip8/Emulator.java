package com.almejo.chip8;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;

public class Emulator extends KeyAdapter {
	private Chip8 chip8 = new Chip8();
	private DisplayPane displayPane;

	public static void main(String[] args) throws IOException {
		new Emulator().run(args[0]);
	}

	private void run(String filename) throws IOException {
		setupInput();
		setupSound();
		chip8.initialize(true);
		setupGraphics(chip8);
		chip8.loadProgram(filename);
		while (true) {
			try {
				long millis = System.currentTimeMillis();
				chip8.emulateCycle();
				drawGraphics();
				long delta = System.currentTimeMillis() - millis;
				if (delta < 2) {
					Thread.sleep(2L - delta);
				}
			} catch (StopEmulationException e) {
				System.out.println("stopping emulation");
				break;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private void drawGraphics() {
		if (chip8.isDrawFlag()) {
			this.displayPane.repaint();
			chip8.setDrawFlag(false);
		}
	}

	private void setupGraphics(Chip8 chip8) {
		JFrame frame = new JFrame("Chip 8 Emulator");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		displayPane = new DisplayPane(chip8);
		frame.setLayout(new FlowLayout());
		frame.add(displayPane);
		frame.add(new DebuggerPanel(chip8));
		frame.addKeyListener(this);
		frame.pack();
		frame.setVisible(true);
	}

	private void setupInput() {
	}

	private void setupSound() {
	}

	private static class DisplayPane extends Container {
		private final Chip8 chip8;

		DisplayPane(Chip8 chip8) {
			this.chip8 = chip8;
			setPreferredSize(new Dimension(650, 330));
		}

		@Override
		public void paint(Graphics g) {
			Graphics2D graphics2D = (Graphics2D) g;
			int width = 10;
			int fillWidth = 10;
			int height = 10;
			int fillHeight = 10;
			for (int y = 0; y < 32; y++) {
				for (int x = 0; x < 64; x++) {
					graphics2D.setColor(chip8.isPainted(x, y) ? Color.WHITE : Color.DARK_GRAY);
					graphics2D.fillRect(x * width, y * height, fillWidth, fillHeight);
				}
			}
		}
	}

	@Override
	public void keyPressed(KeyEvent e) {
		char keyChar = e.getKeyChar();
		updateKeyboard(keyChar, 1);
	}

	private void updateKeyboard(char keyChar, int value) {
		if (keyChar == '1') chip8.setKey(0x1, value);
		else if (keyChar == '2') chip8.setKey(0x2, value);
		else if (keyChar == '3') chip8.setKey(0x3, value);
		else if (keyChar == '4') chip8.setKey(0xC, value);

		else if (keyChar == 'q') chip8.setKey(0x4, value);
		else if (keyChar == 'w') chip8.setKey(0x5, value);
		else if (keyChar == 'e') chip8.setKey(0x6, value);
		else if (keyChar == 'r') chip8.setKey(0xD, value);

		else if (keyChar == 'a') chip8.setKey(0x7, value);
		else if (keyChar == 's') chip8.setKey(0x8, value);
		else if (keyChar == 'd') chip8.setKey(0x9, value);
		else if (keyChar == 'f') chip8.setKey(0xE, value);

		else if (keyChar == 'z') chip8.setKey(0xA, value);
		else if (keyChar == 'x') chip8.setKey(0x0, value);
		else if (keyChar == 'c') chip8.setKey(0xB, value);
		else if (keyChar == 'v') chip8.setKey(0xF, value);
	}

	@Override
	public void keyReleased(KeyEvent e) {
		char keyChar = e.getKeyChar();
		System.out.println("---------------------------------------> " + keyChar);
		updateKeyboard(keyChar, 0);
	}

	private static class DebuggerPanel extends JPanel implements Chip8StateChangeLisener {
		JLabel[] registerLabel = new JLabel[16];

		DebuggerPanel(Chip8 chip8) {
			chip8.addStateChangedListener(this);
			setLayout(new BorderLayout());
			JPanel panel = new JPanel();
			panel.setLayout(new FlowLayout());
			add(panel, BorderLayout.NORTH);
			for (int i = 0; i < registerLabel.length; i++) {
				JPanel registerPanel = new JPanel();
				registerPanel.setLayout(new BoxLayout(registerPanel, BoxLayout.Y_AXIS));
				JLabel label = new JLabel("V" + i + ": ");
				registerPanel.add(label);
				registerLabel[i] = new JLabel();
				registerLabel[i].setPreferredSize(new Dimension(25, 40));
				registerPanel.add(registerLabel[i]);
				panel.add(registerPanel);
			}
			setPreferredSize(new Dimension(800, 330));
		}

		@Override
		public void onStateChanged(int[] V) {
			for (int i = 0; i < V.length; i++) {
				registerLabel[i].setText(V[i] + "");
			}
		}
	}
}
