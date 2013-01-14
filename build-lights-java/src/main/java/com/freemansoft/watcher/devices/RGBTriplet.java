package com.freemansoft.watcher.devices;

public class RGBTriplet {

	private final int red;

	public int getRed() {
		return red;
	}

	public int getGreen() {
		return green;
	}

	public int getBlue() {
		return blue;
	}

	private final int green;
	private final int blue;

	public RGBTriplet(int red, int green, int blue) {
		this.red = red;
		this.blue = blue;
		this.green = green;
	}
}
