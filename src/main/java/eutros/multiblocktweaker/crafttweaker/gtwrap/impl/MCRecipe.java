package eutros.multiblocktweaker.crafttweaker.gtwrap.impl;

import crafttweaker.api.item.IIngredient;
import crafttweaker.api.item.IItemStack;
import crafttweaker.api.liquid.ILiquidStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import eutros.multiblocktweaker.crafttweaker.gtwrap.interfaces.IRecipe;
import gregtech.api.recipes.CountableIngredient;
import gregtech.api.recipes.Recipe;

import java.util.Arrays;
import java.util.stream.Collectors;

public class MCRecipe implements IRecipe {

    private final Recipe inner;

    public MCRecipe(Recipe inner) {
        this.inner = inner;
    }

    @Override
    public boolean matches(boolean consumeIfSuccessful, IItemStack[] inputs, ILiquidStack[] fluidInputs) {
        return inner.matches(consumeIfSuccessful,
                Arrays.asList(CraftTweakerMC.getItemStacks(inputs)),
                Arrays.asList(CraftTweakerMC.getLiquidStacks(fluidInputs)));
    }

    @Override
    public IIngredient[] getInputs() {
        return CraftTweakerMC.getIIngredients(inner.getInputs().stream()
                .map(CountableIngredient::getIngredient)
                .collect(Collectors.toList()));
    }

    @Override
    public IItemStack[] getOutputs() {
        return CraftTweakerMC.getIItemStacks(inner.getOutputs());
    }

    @Override
    public IItemStack[] getAllItemOutputs(int maxOutputSlots) {
        return CraftTweakerMC.getIItemStacks(inner.getAllItemOutputs(maxOutputSlots));
    }

    @Override
    public ILiquidStack[] getFluidInputs() {
        return inner.getFluidInputs().stream().map(CraftTweakerMC::getILiquidStack).toArray(ILiquidStack[]::new);
    }

    @Override
    public boolean hasInputFluid(ILiquidStack fluid) {
        return inner.hasInputFluid(CraftTweakerMC.getLiquidStack(fluid));
    }

    @Override
    public ILiquidStack[] getFluidOutputs() {
        return inner.getFluidOutputs().stream().map(CraftTweakerMC::getILiquidStack).toArray(ILiquidStack[]::new);
    }

    @Override
    public int getDuration() {
        return inner.getDuration();
    }

    @Override
    public int getEUt() {
        return inner.getEUt();
    }

    @Override
    public boolean isHidden() {
        return inner.isHidden();
    }

    @Override
    public boolean hasValidInputsForDisplay() {
        return inner.hasValidInputsForDisplay();
    }

    @Override
    public String[] getPropertyKeys() {
        return inner.getPropertyKeys().toArray(new String[0]);
    }

    @Override
    public boolean getBooleanProperty(String key) {
        return inner.getBooleanProperty(key);
    }

    @Override
    public int getIntegerProperty(String key) {
        return inner.getIntegerProperty(key);
    }

    @Override
    public String getProperty(String key) {
        return inner.getStringProperty(key);
    }

}
