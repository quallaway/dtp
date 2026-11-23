package dev.dtp.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import dev.dtp.menu.RecyclerMenu;
import dev.dtp.recycling.Recycling;
import dev.dtp.registry.ModBlocks;
import dev.dtp.registry.ModItems;

/**
 * Der Recycler arbeitet wie ein Ofen: Eingabe (oben), Brennstoff (Seiten) und Schrott (unten), dieselben Hopper-Seiten
 * und dieselben Brennwerte. Ein Item braucht {@link #PROCESS_TICKS}; nur recycelbare Items zünden Brennstoff.
 * Nicht Recycelbares, das jemand von Hand einlegt, bleibt einfach liegen — wie Erde im Ofen.
 */
public class RecyclerBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    public static final int INPUT = 0;
    public static final int FUEL = 1;
    public static final int OUTPUT = 2;
    /** Ticks je Item: 20 Sekunden, doppelt so lang wie im Ofen. Eine Kohle reicht für 4 Items. */
    public static final int PROCESS_TICKS = 400;
    /** Wie schnell der Fortschritt ohne Glut zurückgeht (Vanilla: {@code BURN_COOL_SPEED}). */
    private static final int COOL_SPEED = 2;

    public static final int DATA_LIT_TIME = 0;
    public static final int DATA_LIT_DURATION = 1;
    public static final int DATA_PROGRESS = 2;
    public static final int DATA_TOTAL = 3;
    public static final int DATA_COUNT = 4;

    private static final int[] SLOTS_FOR_UP = {INPUT};
    private static final int[] SLOTS_FOR_DOWN = {OUTPUT, FUEL};
    private static final int[] SLOTS_FOR_SIDES = {FUEL};

    private NonNullList<ItemStack> items = NonNullList.withSize(3, ItemStack.EMPTY);
    private int litTimeRemaining;
    private int litTotalTime;
    private int progress;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_LIT_TIME -> litTimeRemaining;
                case DATA_LIT_DURATION -> litTotalTime;
                case DATA_PROGRESS -> progress;
                case DATA_TOTAL -> PROCESS_TICKS;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case DATA_LIT_TIME -> litTimeRemaining = value;
                case DATA_LIT_DURATION -> litTotalTime = value;
                case DATA_PROGRESS -> progress = value;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public RecyclerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.RECYCLER_BE.get(), pos, state);
    }

    /** Nach dem Vorbild von {@code AbstractFurnaceBlockEntity.serverTick}, ohne Rezepte und Erfahrung. */
    public static void serverTick(Level level, BlockPos pos, BlockState state, RecyclerBlockEntity recycler) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        boolean changed = false;
        boolean wasLit = recycler.litTimeRemaining > 0;
        if (wasLit) {
            recycler.litTimeRemaining--;
        }
        boolean isLit = recycler.litTimeRemaining > 0;

        ItemStack input = recycler.items.get(INPUT);
        ItemStack fuel = recycler.items.get(FUEL);
        if (isLit || !fuel.isEmpty() && !input.isEmpty()) {
            if (recycler.canProcess(serverLevel)) {
                if (!isLit) {
                    int burn = recycler.burnDuration(serverLevel, fuel);
                    recycler.litTimeRemaining = burn;
                    recycler.litTotalTime = burn;
                    if (burn > 0) {
                        recycler.consumeFuel(serverLevel, pos, fuel);
                        isLit = true;
                        changed = true;
                    }
                }
                if (isLit) {
                    recycler.progress++;
                    if (recycler.progress >= PROCESS_TICKS) {
                        recycler.progress = 0;
                        recycler.processOne(serverLevel);
                        changed = true;
                    }
                } else {
                    recycler.progress = 0;
                }
            } else {
                recycler.progress = 0;
            }
        } else if (recycler.progress > 0) {
            recycler.progress = Mth.clamp(recycler.progress - COOL_SPEED, 0, PROCESS_TICKS);
        }

        if (wasLit != isLit) {
            changed = true;
            state = state.setValue(RecyclerBlock.LIT, isLit);
            level.setBlockAndUpdate(pos, state);
        }
        if (changed) {
            setChanged(level, pos, state);
        }
    }

    /** Recycelbar und der Schrott passt sicher ins Ausgabefach (höchstens der aufgerundete Wert eines Items)? */
    private boolean canProcess(ServerLevel level) {
        ItemStack one = items.get(INPUT).copyWithCount(1);
        if (one.isEmpty()) {
            return false;
        }
        double value = Recycling.value(level.getServer(), one).orElse(-1.0);
        if (value < 0.0) {
            return false;
        }
        ItemStack output = items.get(OUTPUT);
        int worstCase = (int) Math.ceil(value);
        return output.isEmpty() || output.getCount() + worstCase <= output.getMaxStackSize();
    }

    private void processOne(ServerLevel level) {
        ItemStack input = items.get(INPUT);
        int scrap = Recycling.scrapFor(level.getServer(), input.copyWithCount(1), level.getRandom());
        input.shrink(1);
        if (scrap > 0) {
            ItemStack output = items.get(OUTPUT);
            if (output.isEmpty()) {
                items.set(OUTPUT, new ItemStack(ModItems.SCRAP.get(), scrap));
            } else {
                output.grow(scrap);
            }
        }
        effects(level, worldPosition);
    }

    /** Brenndauer wie im Vanilla-Ofen, aus der Komponente {@code cooking_fuel}. */
    private int burnDuration(ServerLevel level, ItemStack fuel) {
        return ResolvableInt.getFromItem(fuel, DataComponents.COOKING_FUEL, CookingFuel::burnTime, getLootContext(level, fuel), 0);
    }

    /** Wie der Vanilla-Ofen: Der Rest (Lavaeimer → Eimer) bleibt im Brennstofffach oder fällt heraus. */
    private void consumeFuel(ServerLevel level, BlockPos pos, ItemStack fuel) {
        ItemStackTemplate remainder = fuel.getCraftingRemainder();
        ItemStack newFuel = fuel;
        fuel.shrink(1);
        if (remainder != null) {
            if (fuel.isEmpty()) {
                newFuel = remainder.create();
            } else {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), remainder.create());
            }
        }
        items.set(FUEL, newFuel);
    }

    /** Schleifstein und ein Hauch Rauch: das Zeichen, dass etwas zerlegt wurde. */
    static void effects(ServerLevel level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.6f, 0.9f + level.getRandom().nextFloat() * 0.2f);
        level.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, 4, 0.2, 0.05, 0.2, 0.01);
    }

    public boolean lit() {
        return litTimeRemaining > 0;
    }

    public int litTimeRemaining() {
        return litTimeRemaining;
    }

    public int progress() {
        return progress;
    }

    /** Von Hand (Fenster) nimmt die Eingabe alles an, wie der Ofen; Brennstoff und Eimer gehen ins Brennstofffach. */
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case INPUT -> true;
            case FUEL -> isFuel(stack) || stack.is(Items.BUCKET) && !items.get(FUEL).is(Items.BUCKET);
            default -> false;
        };
    }

    public static boolean isFuel(ItemStack stack) {
        return stack.has(DataComponents.COOKING_FUEL);
    }

    @Override
    public int[] getSlotsForFace(Direction direction) {
        return switch (direction) {
            case UP -> SLOTS_FOR_UP;
            case DOWN -> SLOTS_FOR_DOWN;
            default -> SLOTS_FOR_SIDES;
        };
    }

    /** Hopper füllen nur Recycelbares in die Eingabe — sonst läge im Fach, was der Recycler nie zerlegt. */
    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
        if (slot == INPUT) {
            return Recycling.accepts(level == null ? null : level.getServer(), stack);
        }
        return canPlaceItem(slot, stack);
    }

    /** Unten: Schrott, aus dem Brennstofffach nur leere Eimer (Vanilla-Tag {@code furnace_fuel_bottom_takeable}). */
    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return direction != Direction.DOWN || slot != FUEL || stack.is(ItemTags.FURNACE_FUEL_BOTTOM_TAKEABLE);
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    /**
     * Eine neue Eingabe beginnt von vorn, wie im Ofen. Innerhalb einer NeoForge-Transaktion (Hopper) bleibt der
     * Fortschritt stehen; ein Hopper legt ohnehin nur dasselbe Item nach oder ein neues in ein leeres Fach.
     */
    @Override
    public void setItem(int slot, ItemStack stack, boolean insideTransaction) {
        boolean same = !stack.isEmpty() && ItemStack.isSameItemSameComponents(items.get(slot), stack);
        super.setItem(slot, stack, insideTransaction);
        if (slot == INPUT && !same && !insideTransaction) {
            progress = 0;
        }
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.dtp.recycler");
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new RecyclerMenu(containerId, inventory, this, data);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(3, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        litTimeRemaining = input.getIntOr("lit_time_remaining", 0);
        litTotalTime = input.getIntOr("lit_total_time", 0);
        progress = input.getIntOr("progress", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putInt("lit_time_remaining", litTimeRemaining);
        output.putInt("lit_total_time", litTotalTime);
        output.putInt("progress", progress);
    }
}
