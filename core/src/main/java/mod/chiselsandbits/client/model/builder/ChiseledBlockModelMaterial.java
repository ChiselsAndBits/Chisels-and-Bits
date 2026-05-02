package mod.chiselsandbits.client.model.builder;

import mod.chiselsandbits.api.blockinformation.BlockInformation;

public record ChiseledBlockModelMaterial(
    BlockInformation blockInformation,
    int tintIndex) {}
