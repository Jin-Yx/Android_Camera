package com.hsj.camera.bean;

public class FrameRate {

	private final int numerator;
	private final int denominator;

	public FrameRate(int numerator, int denominator) {
		this.numerator = numerator;
		this.denominator = denominator;
	}

	public int getNumerator() {
		return numerator;
	}

	public int getDenominator() {
		return denominator;
	}

	@Override
	public String toString() {
		return "FrameRate{" +
			"numerator=" + numerator +
			", denominator=" + denominator +
			'}';
	}
}
