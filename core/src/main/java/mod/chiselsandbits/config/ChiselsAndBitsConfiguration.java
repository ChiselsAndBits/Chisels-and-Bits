package mod.chiselsandbits.config;

import com.communi.suggestu.scena.core.util.LanguageHandler;
import mod.chiselsandbits.api.config.IChiselsAndBitsConfiguration;
import mod.chiselsandbits.api.config.IClientConfiguration;
import mod.chiselsandbits.api.config.ICommonConfiguration;
import mod.chiselsandbits.api.config.IServerConfiguration;
import mod.chiselsandbits.api.util.constants.Constants;

public class ChiselsAndBitsConfiguration implements IChiselsAndBitsConfiguration {
	static {
	//Load the language file.
		final String fileLoc = "assets/" + Constants.MOD_ID + "/lang/%s.json";
		LanguageHandler.loadLangPath(fileLoc);
	}

	private final IClientConfiguration client = new ClientConfiguration();
	private final ICommonConfiguration common = new CommonConfiguration();
	private final IServerConfiguration server = new ServerConfiguration();

	@Override
	public IClientConfiguration getClient() {
		return client;
	}

	@Override
	public ICommonConfiguration getCommon() {
		return common;
	}

	@Override
	public IServerConfiguration getServer() {
		return server;
	}
}
