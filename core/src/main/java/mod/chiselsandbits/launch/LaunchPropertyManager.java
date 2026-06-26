package mod.chiselsandbits.launch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import mod.chiselsandbits.api.launch.ILaunchPropertyManager;

public final class LaunchPropertyManager implements ILaunchPropertyManager {
	private static final LaunchPropertyManager INSTANCE = new LaunchPropertyManager();

	public static LaunchPropertyManager getInstance() {
		return INSTANCE;
	}

	private LaunchPropertyManager() {
	}

	@Override
	public String get(String key, String defaultValue) {
		final String sysProValue = System.getProperty("chiselsandbits.launch.prop.%s".formatted(key));

		if (sysProValue == null) {
			return getFromLaunchPropFile(key, defaultValue);
		}

		return sysProValue;
	}

	private String getFromLaunchPropFile(final String key, final String defaultValue) {
		try {
			final Path launchPropertiesFilePath = Path.of("config", "chiselsandbits.launchproperties");

			if (!Files.exists(launchPropertiesFilePath)) {
				final List<String> launchPropLines = new ArrayList<>();
				launchPropLines.add("%s = %s".formatted(key, defaultValue));
				Files.write(launchPropertiesFilePath, launchPropLines);
				return defaultValue;
			}

			final List<String> launchPropLines = new ArrayList<>(Files.readAllLines(launchPropertiesFilePath));
			final Optional<String> value = launchPropLines.stream()
					.filter(l -> !l.startsWith("#"))
					.filter(l -> l.replace(" ", "").startsWith("%s = ".formatted(key)))
					.findFirst()
					.map(l -> l.replace(" ", "").replace("%s = ".formatted(key), ""));

			if (value.isEmpty()) {
				launchPropLines.add("%s = %s".formatted(key, defaultValue));
				Files.write(launchPropertiesFilePath, launchPropLines);
				return defaultValue;
			}

			return value.get();
		} catch (IOException e) {
			return defaultValue;
		}
	}
}
