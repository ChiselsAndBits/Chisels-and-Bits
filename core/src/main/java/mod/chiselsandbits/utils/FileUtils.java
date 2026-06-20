package mod.chiselsandbits.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class FileUtils {
	private FileUtils()

	{
		throw new IllegalStateException("Can not instantiate an instance of: FileUtils. This is a utility class");
	}

	public static Path ensureFileWritable(Path filePath) throws IOException

	{
		final File file = filePath.toFile();

		if (!file.exists()) {
			file.getParentFile().mkdirs();
		}

		else

		{
			file.delete();
		}

		file.createNewFile();
		return filePath;
	}
}
