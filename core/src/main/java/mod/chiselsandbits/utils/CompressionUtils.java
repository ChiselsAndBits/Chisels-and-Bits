package mod.chiselsandbits.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CompressionUtils {
	private static final Logger LOG = LogManager.getLogger(CompressionUtils.class);

	public static byte[] compress(byte[] data) throws IOException
	{
		Deflater deflater = new Deflater();
		deflater.setInput(data);

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);

		deflater.finish();
		byte[] buffer = new byte[1024];
		while (!deflater.finished()) {
			int count = deflater.deflate(buffer); // returns the generated code... index
			outputStream.write(buffer, 0, count);
		}
		outputStream.close();
		byte[] output = outputStream.toByteArray();

		LOG.debug("Original: " + data.length / 1024 + " Kb");
		LOG.debug("Compressed: " + output.length / 1024 + " Kb");
		return output;
	}

	public static byte[] decompress(byte[] data) throws IOException, DataFormatException
	{
		Inflater inflater = new Inflater();
		inflater.setInput(data);

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);
		byte[] buffer = new byte[1024];
		while (!inflater.finished()) {
			int count = inflater.inflate(buffer);
			outputStream.write(buffer, 0, count);
		}
		outputStream.close();
		byte[] output = outputStream.toByteArray();

		LOG.debug("Original: " + data.length / 1024 + " Kb");
		LOG.debug("Decompressed: " + output.length / 1024 + " Kb");
		return output;
	}
}
