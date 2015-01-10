package com.github.davidmoten.ar;

import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;

public class Audio {

	private static final int BUFFER_SIZE = 1024;

	public static void play(InputStream is) {

		// Load the Audio Input Stream from the file
		AudioInputStream audioInputStream = null;
		try {
			audioInputStream = AudioSystem.getAudioInputStream(is);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}

		// Get Audio Format information
		AudioFormat audioFormat = audioInputStream.getFormat();

		// Handle opening the line
		SourceDataLine line = null;
		DataLine.Info info = new DataLine.Info(SourceDataLine.class,
				audioFormat);
		try {
			line = (SourceDataLine) AudioSystem.getLine(info);
			line.open(audioFormat);
		} catch (LineUnavailableException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}

		// Start playing the sound
		line.start();

		// Write the sound to an array of bytes
		int nBytesRead = 0;
		byte[] abData = new byte[BUFFER_SIZE];
		while (nBytesRead != -1) {
			try {
				nBytesRead = audioInputStream.read(abData, 0, abData.length);
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (nBytesRead >= 0) {
				line.write(abData, 0, nBytesRead);
			}
		}

		// close the line
		line.drain();
		line.close();

	}

	public static Observable<Integer> read(final InputStream is) {

		return Observable.create(new OnSubscribe<Integer>() {

			@Override
			public void call(Subscriber<? super Integer> sub) {
				// Load the Audio Input Stream from the file
				AudioInputStream audioInputStream = null;
				try {
					audioInputStream = AudioSystem.getAudioInputStream(is);
				} catch (UnsupportedAudioFileException e) {
					sub.onError(e);
					return;
				} catch (IOException e) {
					sub.onError(e);
					return;
				}

				// Get Audio Format information
				AudioFormat audioFormat = audioInputStream.getFormat();

				printAudioDetails(audioInputStream, audioFormat);

				// Write the sound to an array of bytes
				int nBytesRead = 0;
				byte[] abData = new byte[8192];
				while (nBytesRead != -1 && !sub.isUnsubscribed()) {
					try {
						nBytesRead = audioInputStream.read(abData, 0,
								abData.length);
					} catch (IOException e) {
						sub.onError(e);
						return;
					}
					if (nBytesRead > 0) {

						// Determine the original Endian encoding format
						boolean isBigEndian = audioFormat.isBigEndian();

						int n = nBytesRead / 2;

						// convert each pair of byte values from the byte array
						// to an
						// Endian value
						for (int i = 0; i < n * 2; i += 2) {
							int b1 = abData[i];
							int b2 = abData[i + 1];
							if (b1 < 0)
								b1 += 0x100;
							if (b2 < 0)
								b2 += 0x100;

							int value;

							// Store the data based on the original Endian
							// encoding
							// format
							if (!isBigEndian)
								value = (b1 << 8) + b2;
							else
								value = b1 + (b2 << 8);
							if (sub.isUnsubscribed())
								return;
							else
								sub.onNext(value);
						}
					}
				}
				sub.onCompleted();
			}

		});

	}

	private static void printAudioDetails(AudioInputStream audioInputStream,
			AudioFormat audioFormat) {
		// Calculate the sample rate
		float sample_rate = audioFormat.getSampleRate();
		System.out.println("sample rate = " + sample_rate);

		// Calculate the length in seconds of the sample
		float T = audioInputStream.getFrameLength()
				/ audioFormat.getFrameRate();
		System.out
				.println("T = " + T + " (length of sampled sound in seconds)");

		// Calculate the number of equidistant points in time
		int num = (int) (T * sample_rate) / 2;
		System.out.println("n = " + num + " (number of equidistant points)");

		// Calculate the time interval at each equidistant point
		float h = (T / num);
		System.out.println("h = " + h
				+ " (length of each time interval in seconds)");
	}

}
