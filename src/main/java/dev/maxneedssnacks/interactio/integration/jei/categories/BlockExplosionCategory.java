package dev.maxneedssnacks.interactio.integration.jei.categories;

import dev.maxneedssnacks.interactio.Utils;
import dev.maxneedssnacks.interactio.integration.jei.util.TooltipCallbacks;
import dev.maxneedssnacks.interactio.recipe.BlockExplosionRecipe;
import dev.maxneedssnacks.interactio.recipe.ingredient.WeightedOutput;
import dev.maxneedssnacks.interactio.recipe.util.InWorldRecipeType;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IGuiItemStackGroup;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ResourceLocation;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class BlockExplosionCategory implements IRecipeCategory<BlockExplosionRecipe> {

    public static final ResourceLocation UID = InWorldRecipeType.BLOCK_EXPLODE.registryName;

    private final IGuiHelper guiHelper;

    private final IDrawableStatic background;
    //FIXME: Add an overlay. I know. I'm lazy.
    //private final IDrawableStatic overlay;

    private final IDrawable icon;

    private final String localizedName;

    public BlockExplosionCategory(IGuiHelper guiHelper) {
        this.guiHelper = guiHelper;

        background = guiHelper.createBlankDrawable(96, 34);

        icon = guiHelper.createDrawableIngredient(new ItemStack(Items.TNT));

        localizedName = Utils.translate("interactio.jei.block_explode", null);
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public Class<BlockExplosionRecipe> getRecipeClass() {
        return BlockExplosionRecipe.class;
    }

    @Override
    public String getTitle() {
        return localizedName;
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setIngredients(BlockExplosionRecipe recipe, IIngredients ingredients) {
        // display input block as item
        ingredients.setInput(VanillaTypes.ITEM, recipe.getInput().asItem().getDefaultInstance());

        // display resulting block as item, as well
        ingredients.setOutputLists(VanillaTypes.ITEM, Collections.singletonList(recipe.getOutput().stream()
                .map(WeightedOutput.WeightedEntry::getResult)
                .map(Block::asItem)
                .map(Item::getDefaultInstance)
                .collect(Collectors.toList())));

    }

    @Override
    public void setRecipe(IRecipeLayout layout, BlockExplosionRecipe recipe, IIngredients ingredients) {

        List<List<ItemStack>> outputs = ingredients.getOutputs(VanillaTypes.ITEM);

        IGuiItemStackGroup itemStackGroup = layout.getItemStacks();

        WeightedOutput<ItemStack> output = new WeightedOutput<>(recipe.getOutput().emptyWeight);
        recipe.getOutput().forEach(entry -> output.add(entry.getResult().asItem().getDefaultInstance(), entry.getWeight()));

        WeightedOutput.WeightedEntry<ItemStack> empty = new WeightedOutput.WeightedEntry<>(Items.BARRIER.getDefaultInstance(), output.emptyWeight);

        itemStackGroup.init(0, true, 4, 8);
        itemStackGroup.init(1, false, 72, 8);

        if (output.emptyWeight > 0) outputs.get(0).add(empty.getResult());
        itemStackGroup.set(ingredients);

        itemStackGroup.addTooltipCallback((idx, input, stack, tooltip) -> {
            TooltipCallbacks.weightedOutput(input, stack, tooltip, output, empty, false, entry -> entry.getResult().equals(stack, false));
            TooltipCallbacks.recipeID(input, tooltip, recipe);
        });

    }

    @Override
    public void draw(BlockExplosionRecipe recipe, double mouseX, double mouseY) {

        guiHelper.createDrawableIngredient(new ItemStack(Items.TNT)).draw(38, 9);

        guiHelper.getSlotDrawable().draw(4, 8);
        guiHelper.getSlotDrawable().draw(72, 8);

    }

}
