package mod.chiselsandbits.client.util;

import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.communi.suggestu.scena.core.client.models.processing.BakedQuadBuilder;
import com.communi.suggestu.scena.core.client.models.processing.DeconstructedModelPartComponent;
import com.communi.suggestu.scena.core.client.models.processing.VertexData;
import mod.chiselsandbits.api.blockinformation.BlockInformation;
import mod.chiselsandbits.client.model.face.FaceManager;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public final class QuadGenerationUtils {
	private QuadGenerationUtils() {
		throw new IllegalStateException("Tried to instantiate: 'QuadGenerationUtils', but this is a utility class.");
	}

	public static void generateQuads(
			final BlockInformation blockInformation,
			@Nullable final Direction facingDirection,
			@Nullable final BlockAndTintGetter blockAndTintGetter,
			@Nullable final BlockPos pos,
			final Vector3f from,
			final Vector3f to,
			final Consumer<GeneratedQuad> target) {
		generateQuads(
				blockInformation,
				facingDirection,
				blockAndTintGetter,
				pos,
				from,
				to,
				($, $$) -> { },
				target
		);
	}

	public static void generateQuads(
			final BlockInformation blockInformation,
			@Nullable final Direction facingDirection,
			@Nullable final BlockAndTintGetter blockAndTintGetter,
			@Nullable final BlockPos pos,
			final Vector3f from,
			final Vector3f to,
			final BiConsumer<DeconstructedModelPartComponent, BakedQuadBuilder> quadAdapter,
			final Consumer<GeneratedQuad> target) {
		generateQuads(
				blockInformation,
				facingDirection,
				blockAndTintGetter,
				pos,
				from,
				to,
				quadAdapter,
				$ -> true,
				target
		);
	}

	public static boolean generateQuads(
			final BlockInformation blockInformation,
			@Nullable final Direction facingDirection,
			@Nullable final BlockAndTintGetter blockAndTintGetter,
			@Nullable final BlockPos pos,
			final Vector3f from,
			final Vector3f to,
			final Predicate<DeconstructedModelPartComponent> sourcePreflight,
			final Consumer<GeneratedQuad> target
	) {
		return generateQuads(
				blockInformation,
				facingDirection,
				blockAndTintGetter,
				pos,
				from,
				to,
				($, $$) -> { },
				sourcePreflight,
				target
		);
	}

	public static boolean generateQuads(
			final BlockInformation blockInformation,
			@Nullable final Direction facingDirection,
			@Nullable final BlockAndTintGetter blockAndTintGetter,
			@Nullable final BlockPos pos,
			final Vector3f from,
			final Vector3f to,
			final BiConsumer<DeconstructedModelPartComponent, BakedQuadBuilder> quadAdapter,
			final Predicate<DeconstructedModelPartComponent> sourcePreflight,
			final Consumer<GeneratedQuad> target) {
		return generateVertices(
				blockInformation,
				facingDirection,
				blockAndTintGetter,
				pos,
				from,
				to,
				sourcePreflight,
				vertices -> {
					final BakedQuadBuilder adapter = new BakedQuadBuilder(vertices.source().material());
					vertices.vertices().forEach(adapter);
					adapter.cullDirection(facingDirection);
					quadAdapter.accept(vertices.source(), adapter);
					target.accept(new GeneratedQuad(vertices.source(), adapter.build()));
				}
		);
	}

	public static boolean generateVertices(
			final BlockInformation blockInformation,
			@Nullable final Direction facingDirection,
			@Nullable final BlockAndTintGetter blockAndTintGetter,
			@Nullable final BlockPos pos,
			final Vector3f from,
			final Vector3f to,
			final Predicate<DeconstructedModelPartComponent> sourcePreflight,
			final Consumer<GeneratedVertices> target
	) {
		final boolean[] supported = {true};
		FaceManager.getInstance().extractQuads(
						blockInformation,
						facingDirection,
						blockAndTintGetter,
						pos,
						(layer) -> {
							if (!sourcePreflight.test(layer)) {
								supported[0] = false;
								return;
							}

							final Collection<VertexData> adaptedVertices;

							try {
								adaptedVertices = VertexDataUtils.adaptVertices(layer.vertexData(), layer.cullDirection(), from, to);
							} catch (IllegalStateException e) {
								supported[0] = false;
								return;
							}

							target.accept(new GeneratedVertices(layer, List.copyOf(adaptedVertices)));
						});
		return supported[0];
	}

	public record GeneratedVertices(DeconstructedModelPartComponent source, List<VertexData> vertices) {
	}

	public record GeneratedQuad(
			DeconstructedModelPartComponent source,
			BakedQuad quad) {
	}
}
