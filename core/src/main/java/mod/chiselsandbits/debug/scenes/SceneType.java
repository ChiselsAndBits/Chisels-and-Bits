package mod.chiselsandbits.debug.scenes;

public enum SceneType {
	WORST_CASE("worst_case"),
	SHARDING("sharding"),
	TERRAIN_SUBSET("terrain_subset"),
	BUILDER_DETAIL("builder_detail"),
	TRANSLUCENCY("translucency"),
	LIGHTING("lighting"),
	CHURN("churn");

	private final String commandName;

	SceneType(final String commandName) {
		this.commandName = commandName;
	}

	public String getCommandName() {
		return commandName;
	}
}
