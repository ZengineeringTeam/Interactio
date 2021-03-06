package dev.maxneedssnacks.interactio;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import dev.maxneedssnacks.interactio.recipe.ingredient.RecipeIngredient;
import dev.maxneedssnacks.interactio.recipe.ingredient.WeightedOutput;
import dev.maxneedssnacks.interactio.recipe.util.IEntrySerializer;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.particles.IParticleData;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.*;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public final class Utils {

    public static boolean isItem(Entity e) {
        return e instanceof ItemEntity;
    }

    // region recipe
    public static boolean compareStacks(List<ItemEntity> entities, List<RecipeIngredient> ingredients) {
        return compareStacks(entities, new Object2IntOpenHashMap<>(), ingredients);
    }

    public static boolean compareStacks(List<ItemEntity> entities, Object2IntMap<ItemEntity> used, List<RecipeIngredient> ingredients) {

        List<RecipeIngredient> required = ingredients.stream().map(RecipeIngredient::copy).collect(Collectors.toList());

        for (ItemEntity entity : entities) {
            ItemStack item = entity.getItem();

            if (!entity.isAlive()) return false;

            for (RecipeIngredient req : required) {
                Ingredient ingredient = req.getIngredient();
                int available = Math.min(req.getCount(), item.getCount());

                if (ingredient.test(item)) {
                    used.mergeInt(entity, available - req.roll(available), Integer::sum);
                    req.shrink(item.getCount());
                    break;
                }
            }

            required.removeIf(RecipeIngredient::isEmpty);
        }

        return required.isEmpty();
    }

    public static void shrinkAndUpdate(Object2IntMap<ItemEntity> entities) {
        entities.forEach((entity, count) -> {
            entity.setInfinitePickupDelay();

            ItemStack item = entity.getItem().copy();
            item.shrink(count);

            if (item.isEmpty()) {
                entity.remove();
            } else {
                entity.setItem(item);
            }

            entity.setDefaultPickupDelay();
        });
    }

    public static <T> WeightedOutput<T> singleOrWeighted(JsonObject json, IEntrySerializer<T> serializer) {
        WeightedOutput<T> output = new WeightedOutput<>(0);
        try {
            output.add(serializer.read(json), 1);
        } catch (Exception e) {
            output = WeightedOutput.deserialize(json, serializer);
        }
        return output;
    }
    //endregion recipe

    // region network
    public static void sendParticle(IParticleData particle, World world, Vec3d pos) {
        if (world instanceof ServerWorld) {
            Random rand = world.rand;

            double dx = rand.nextGaussian() / 50;
            double dy = rand.nextGaussian() / 50;
            double dz = rand.nextGaussian() / 50;

            ((ServerWorld) world).spawnParticle(
                    particle,
                    pos.x - dx,
                    pos.y + MathHelper.nextDouble(rand, 0, 1 - dy),
                    pos.z - dz,
                    5,
                    dx,
                    dy,
                    dz,
                    rand.nextGaussian() / 50
            );
        }
    }

    public static double parseChance(JsonObject object, String key) {
        return parseChance(object, key, 0);
    }

    public static double parseChance(JsonObject object, String key, double dv) {
        try {
            return getDouble(object, key, dv);
        } catch (Exception ex) {
            return JSONUtils.getBoolean(object, key, dv == 1) ? 1 : 0;
        }
    }

    public static double getDouble(JsonObject object, String key) {
        if (object.has(key)) {
            JsonElement e = object.get(key);
            if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber()) {
                return e.getAsDouble();
            } else {
                throw new JsonSyntaxException("Could not parse double from " + key + " as it's not a number!");
            }
        } else {
            throw new JsonSyntaxException("Missing " + key + ", expected to find a Double");
        }
    }

    public static double getDouble(JsonObject object, String key, double dv) {
        return object.has(key) ? getDouble(object, key) : dv;
    }

    // shouldn't be needed, but who knows
    public static void ensureClientSide(NetworkEvent.Context context) {
        if (context.getDirection() != NetworkDirection.PLAY_TO_CLIENT) {
            throw new UnsupportedOperationException("Packet should only be handled on client!");
        }
    }
    // endregion network

    public static String translate(String langKey, @Nullable Style style, Object... replacements) {
        return new TranslationTextComponent(langKey, replacements).setStyle(style == null ? new Style() : style).getFormattedText();
    }

    public static ITextComponent formatChance(double chance, TextFormatting... styles) {
        return new StringTextComponent(String.format("%.2f%%", chance * 100.0)).applyTextStyles(styles);
    }

    /**
     * NOTE: This method originally stems from the Botania mod by Vazkii, which is Open Source
     * and distributed under the Botania License (see http://botaniamod.net/license.php)
     * <p>
     * Find the original Botania GitHub repository here: https://github.com/Vazkii/Botania
     * <p>
     * (Original class: vazkii.botania.client.integration.jei.petalapothecary.PetalApothecaryRecipeCategory, created by <williewillus>)
     */
    public static Point rotatePointAbout(Point in, Point about, double degrees) {
        double rad = degrees * Math.PI / 180.0;
        double newX = Math.cos(rad) * (in.x - about.x) - Math.sin(rad) * (in.y - about.y) + about.x;
        double newY = Math.sin(rad) * (in.x - about.x) + Math.cos(rad) * (in.y - about.y) + about.y;
        return new Point((int) Math.round(newX), (int) Math.round(newY));
    }
}
