<!--
DTP Handbook, English. This file is the whole book: chapters, pages, order and text.
Format and workflow: docs/71-entwicklung-und-test.md, section "Handbuch bearbeiten".

  # Title                          new chapter (shown in the table of contents)
  ## Title {#id grid=name warning}  new page; #id is required and identical in every language
                                   grid= shows a recipe grid: beacon, market_stand, trade_chest,
                                   deposit_chest, bulletin_board, statistics_board, upgrade_table, repair_station, relay_beacon,
                                   chunk_loader, market_scale, blackmarket, slot_machine, recycler, taler, guide
                                   warning highlights the page in red
  blank line                       new paragraph (single line breaks become spaces)
  **bold**  *italic*  - item       formatting and bullet list
  <!- - … - ->                     comment (without the spaces), not shown

Pages that are too long continue on a following page in game automatically.
The German file (de_de.md) must have the same page ids, grids and warning flags in the same order;
the GameTest guide_files checks this.

Rule of thumb for the introduction chapters (Welcome to Workshop, Depths, Rumors): every sentence says what exists,
where to find it or what to do next – never how much or how exactly. No numbers, formulas, costs, ranges or times.

Exception: the reference chapters Beacon Effects, Server Upgrades and Personal Upgrades say what an effect applies to
(blocks, animals, items, exceptions, consequences) and may use lists and numbers for that. The in-game tooltip stays
short (what + how much), the book provides the rest. Contraband and the black market trader stay out.
-->

# Welcome

## The Idea {#welcome.idea}

You produce goods, sell them at the market stand for taler and use them to unlock permanent improvements – together for the whole server and each of you for yourselves.

Variety pays off: whatever you sell in bulk loses value. Whatever sits unsold for a while is soon in demand again.

## It's Still Minecraft {#welcome.minecraft}

There is no fixed order and nothing you have to do. Build, explore and farm however you like – the mod rewards you for it.

This book only covers the start. Much is best discovered yourselves: at the beacon, in the workshop and at the market stand.

## This Book {#welcome.book grid=guide}

Lost your handbook? A book and a copper, iron or gold nugget make a new one.

The most important DTP recipes are already in your recipe book, too.

# First Steps

## 1. The Beacon {#start.beacon grid=beacon}

Pick a starting spot together.

Then craft the beacon and place it. There is only one per server, it cannot be broken again, and your territory begins with it.

## 2. Your First Taler {#start.market}

Without a beacon the market pays next to nothing – so place it first.

Then build a market stand and a deposit chest right next to it. Sell your first goods at the market stand; the taler end up in the deposit chest.

With your first taler, unlock something new at the beacon or build yourselves a workshop.

# Taler

## Taler {#taler grid=taler}

Taler come in copper, silver and gold – as taler, pouch and sack. Nine make the next tier, and it works the other way round too.

You always pay from deposit chests nearby, never from your inventory. Taler drop on death – in the chest they are safe.

# The Blocks

## Beacon {#block.beacon grid=beacon}

The heart of your server. Buy shared improvements here: a larger territory, new unlocks and effects for everyone.

## Market Stand {#block.market_stand grid=market_stand}

This is where you sell – from your hand or from trade chests right next to it. It also shows the current prices.

A redstone signal makes it sell on its own.

## Trade Chest {#block.trade_chest grid=trade_chest}

Storage for the market stand and the bulletin board. Fill it by hand or with a hopper. A comparator reads its fill level like a chest.

## Deposit Chest {#block.deposit_chest grid=deposit_chest}

Your till: earnings land here, and every purchase is paid from here.

Place it near the market stand, workshop and beacon. A comparator reads its fill level like a chest. A redstone signal sorts its Taler, just like the **Sort Taler** button.

## Bulletin Board {#block.bulletin_board grid=bulletin_board}

The notice board for contracts: accept delivery jobs and hand them in.

## Statistics Board {#block.statistics_board grid=statistics_board}

Shows what your server has sold: pieces and Taler per item, for the last 14 market days or all time. Filters by the same groups as the Market Stand; click **Pcs** or **Taler** to sort ascending or descending.

The second tab counts completed contracts: in total and per player by tier.

## Workshop {#block.upgrade_table grid=upgrade_table}

Personal upgrades and modules.

The recipe needs a copper sack – your first savings goal.

## Repair Station {#block.repair_station grid=repair_station}

Repairs tools, weapons and armor for taler – no experience, no materials.

Put the item on the left and taler on the right. The more valuable the coin, the more gets repaired; overpayment comes back as change. Durable gear costs a lot more, and in the long run Mending stays the better way.

## Relay Beacon {#block.relay_beacon grid=relay_beacon}

For outposts, in other dimensions too. It spreads effects you have already unlocked at the beacon. Furnace Speed and Mending Aura come along without a slot once you have bought them.

## Chunk Loader {#block.chunk_loader grid=chunk_loader}

Keeps the area around it loaded so your farms keep running while you are elsewhere — in the Nether too.

You pay with taler from a nearby deposit chest, for a limited time. The time only runs while someone is online; when nobody is around, the chunk loader rests as well.

## Market Scale {#block.market_scale grid=market_scale}

Outputs the market value of a good as a redstone signal.

The recipe only works after a market improvement at the beacon.

With the Contract Clock, sneak + right-click with a clock binds the clock to the scale's item.

## Black Market {#block.blackmarket grid=blackmarket}

Improves enchantments in exchange for taler.

But beware: not every deal there ends well.

## Slot Machine {#block.slot_machine grid=slot_machine}

Right-click with any taler to insert it. The reels spin for everyone to see, and whatever you win drops out of the front.

- Three gold taler: jackpot, two coin tiers up
- Three matching symbols: one coin tier up
- The first two match: consolation prize, one coin tier down

In the long run the machine wins. Only play what you are happy to lose.

## Recycler {#block.recycler grid=recycler}

Breaks worn-out gear down into **scrap**: leather armor, stone tools, saddles and other gear nobody needs any more. Anything a furnace already melts into nuggets is refused.

It works like a furnace: gear on top, fuel below, scrap comes out on the right. It takes much longer than a furnace. Wooden tools make good fuel.

Hoppers feed gear from the top and fuel from the sides, and pull scrap out of the bottom.

Sell scrap at the market stand. It is not worth much.

# The Market

## Prices {#market.prices}

Not everything can be sold. The market stand shows what it accepts – mostly raw materials and produce.

Everyone shares the same prices. Selling a lot of one kind pushes its price down; over time it recovers. So mix it up.

Outside your territory the market pays almost nothing.

## Market Events {#market.events}

Every now and then a group of goods is in high demand: whoever has stock then sells high. Less often, a fast recovery covers the whole market. Depressed prices recover twice as fast and sales push prices down less, so running farms pay off most. The market stand and the chat announce these market events – keep your eyes open.

# Contracts

## Delivery Jobs {#contracts}

Contracts hang on the bulletin board: bring a certain amount of a good and get more for it than at the market. Deliveries come from the trade chests next to it.

You unlock contracts at the beacon. Every finished contract moves the whole server forward.

Speed pays: deliver within the bonus window and you get 20% more. After that the bonus is gone, you still get the full reward for one day, then the contract loses 10% every day, down to half. It never expires. The board shows the current value and when it drops next.

- Tier 1: 120% until day 3, 100% until day 4, 90% until day 5 … from day 8 50%
- The window starts when the contract appears on the board, not when you accept it.

# Beacon and Territory

## Your Territory {#beacon.territory}

The beacon creates your territory: the area inside the world border. The border grows when you buy more territory at the beacon.

Market stand, bulletin board and workshop only work properly inside the territory.

## At the Beacon {#beacon.upgrades}

At the beacon you buy together: more territory, new unlocks, market improvements and effects for everyone.

You pay from deposit chests near the beacon. Just have a look at the tabs – much of it explains itself. What exactly the effects affect is listed at the back of the book.

# Workshop

## Your Workshop {#workshop}

In the workshop everyone buys their own personal upgrades. You keep them even after death.

You equip modules there and can swap them. Some modules are skills you trigger with their own key (default: R).

What exactly the upgrades affect is listed in the reference chapters further back.

<!--
Reference chapters from here on: lists, numbers and exceptions are allowed. Page title = upgrade name from the lang
file (without "Effect:"). Only effects whose tooltip leaves questions open. Numbers are the defaults from datapack and
config — whoever changes them updates the page here as well.
-->

# Beacon Effects

## Range {#fx.range}

A beacon effect only works while it sits in an effect slot. Switching is free at any time.

- Main beacon: Overworld only, in a square of 2,240 blocks around the beacon – no matter how large your territory is right now.
- Relay beacon: 80 blocks around the block, only in its own dimension.
- Height never matters: everything works deep down in the mine, too.
- The same effect from several beacons only counts once.

## Farming {#fx.effect_agriculture}

**Growth:** every growth step has a 30% chance of one extra stage. This applies to:

- Wheat, carrots, potatoes, beetroots
- Torchflowers
- Pumpkin and melon stems until fully grown – the fruit itself appears as fast as usual

Not faster: pitcher plants, nether wart, cocoa, sweet berries, sugar cane, cactus, bamboo, kelp, cave vines.

**No trampling:** farmland stays intact no matter who jumps on it – players, animals or monsters.

**Compost:** composters get a 10% higher chance to fill per item, e.g. seeds 40 instead of 30%. This also works for hoppers and farmers.

## Stablemaster {#fx.effect_stablemaster}

**Breeding:** all animals are ready again after 3¾ instead of 5 minutes, frogs, sniffers and turtles included. Not villagers.

**Foals** inherit better stats: health, jump strength and speed each move 10% closer to the maximum. This applies to foals of horses, donkeys, mules, llamas and trader llamas – the other mounts inherit no stats.

**Mounts** are 20% faster in range while you steer them. They take no fall damage, even without a rider – and neither do you in the saddle. This applies to:

- Horse, donkey, mule, skeleton horse and zombie horse
- Camel and camel husk
- Llama and trader llama (fall protection only, they cannot be steered)
- Pig and strider
- Nautilus and zombie nautilus
- Happy ghast

**Rest:** dismount in range and your mount stays put for 1 minute – enough time to put it on a lead. Once it is leashed or you ride again, the rest is over.

**Paths:** 30% faster on dirt path, gravel, coarse dirt and rooted dirt – on foot and on every mount. Not in boats or minecarts. On paths this adds up with the mount speed.

*Tip: gravel is everywhere – a gravel road to the next outpost is quickly built.*

## Beekeeping {#fx.effect_beekeeping}

Applies to beehives and bee nests in range.

- Honey: every nectar delivery has a 50% chance of an extra honey level.
- Harvesting by hand: shears always give one more honeycomb, a glass bottle gives a second honey bottle. Dispensers get no bonus.
- Pollination: bees carrying nectar grow plants up to 3 blocks beside and 1–3 blocks below them: wheat, carrots, potatoes, beetroots, torchflowers, pitcher plants, pumpkin and melon stems, sweet berries and cave vines.
- Bees never sting players. Harvesting without a campfire leaves the bees inside the hive.

*Tip: flowers on one side of the field, beehives on the other – the bees fertilize the field on their way home.*

## Master Builder {#fx.effect_builder}

**Reach:** +2 blocks for everything you do with blocks: building, mining, opening chests, using levers and buttons. Attacks on creatures do not reach further.

**Undo:** blocks you placed yourself can be broken instantly for 10 seconds. The block comes back to your inventory as if mined with Silk Touch – glass stays glass – and your tool takes no wear.

- Survival mode only, up to 64 blocks at once
- Not for blocks with contents or data: chests, furnaces, barrels, signs, banners, beds …
- Not for blocks that never drop anything

*Tip: tall walls and roofs need far less scaffolding – and helper blocks are collected again instantly.*

## Feather Fall {#fx.effect_feather_fall}

Halves the fall damage of players in range, on top of the Feather Falling enchantment.

Animals and monsters fall as usual. Mounts are protected by the Stablemaster.

## Expedition Prep {#fx.effect_expedition}

- Potions you drink in range last twice as long – even after you leave the territory. With a 25% chance the potion is kept.
- Drinkable potions only, no splash or lingering potions. Instant effects such as Healing still apply once.
- All food gives 50% more saturation. The hunger bar fills as usual.

*Tip: drink your potions and eat your fill in the territory before you set out.*

# Server Upgrades

## Furnace Speed {#server.furnace_speed}

Applies to furnaces, blast furnaces and smokers in the beacon area (the same area as the beacon effects) and around every relay beacon, not campfires. Blast furnaces and smokers stay twice as fast as a furnace; the bonus comes on top.

A fuel burns just as long, but smelts more items in the faster furnace:

- Coal: 10 (level I) or 12 (level II) instead of 8
- Lava bucket: 125 or 150 instead of 100

*Tip: large smelting and smoking setups save noticeable fuel.*

## Endermen Leave Blocks {#server.enderman_griefing}

Endermen no longer pick up blocks and no longer place any – in all dimensions, even outside the territory. Grass, dirt, flowers and pumpkins around your base stay where they are.

## Creeper Protection {#server.creeper_griefing}

Creeper explosions no longer destroy blocks, anywhere and in every dimension. Players, animals and dropped items still take damage.

Still destructive: TNT, ghasts, the Wither, end crystals, beds in the Nether and respawn anchors.

## Master Smith {#server.anvil_cap}

No more “Too Expensive!”: any anvil work that would cost 40 levels or more costs at most 39. Applies to every player and every anvil – repairing, enchanting with books and renaming.

Cheaper work costs as usual.

*Tip: your favorite tools can be repaired and re-enchanted as often as you like.*

## Mending Aura {#server.mending_aura}

Slowly repairs damaged items in your whole inventory – armor and offhand included – anywhere in the beacon area (the same area as the beacon effects) and around every relay beacon, in the Nether and the End too.

- Level I: 1 durability every 10 seconds, level II: 2
- Several beacons at once count only once

*Tip: the aura maintains, Mending repairs. For tools you use a lot, Mending stays the main way.*

# Personal Upgrades

## Area Harvest {#personal.hoe_harvest}

Right-clicking a mature crop with a hoe harvests the area around it at the same height and replants right away. The seeds come from the harvest, and Fortune on the hoe applies.

- Wheat, carrots, potatoes, beetroots
- Nether wart
- Cocoa: the area stands upright along the trunk side, and only cocoa on that same side is harvested. The beans go straight into your inventory.

Not: sweet berries, pumpkins, melons, sugar cane, torchflowers, pitcher plants.

*Tip: a wall of jungle logs, one side covered in cocoa – one click harvests the whole plantation. Nether wart fields on soul sand pay off as well.*

## Rich Harvest {#personal.harvest_bonus}

A chance of double yield when you harvest a mature crop with a hoe – when breaking it normally and with Area Harvest.

- The same crops as Area Harvest: wheat, carrots, potatoes, beetroots, nether wart, cocoa
- Wheat seeds and beetroot seeds are not doubled. Carrots, potatoes, nether wart and cocoa beans are their own seeds and double in full.
- Works together with Fortune.
- Unlike vanilla, Fortune on wheat and beetroots also gives more wheat or beetroots, not just more seeds.

## More Wool {#personal.wool_bonus}

Extra wool with any shears, in the sheep's color. Only when you shear yourself – dispensers with shears get no bonus.

## Fast Growth {#personal.baby_growth}

Feed a baby animal its food and it grows an extra bit – instantly at the last level. Applies to every animal you can feed: cows, pigs, sheep, chickens, horses, wolves, cats, bees …

Not villagers or tadpoles.

*Tip: with Farming at the beacon, a small herd quickly becomes a large one.*

## Tree Felling {#personal.tree_felling}

Sneak while chopping a log with an axe, and every connected log of the same kind comes down with it – diagonal connections too. Each log costs durability.

- All logs and wood blocks, stripped ones too, plus crimson and warped stems
- Exactly the same block only: an oak log doesn't take a birch log with it
- Leaves stay

*Tip: big spruces and jungle trees are really worth it now.*

## Vein Mining {#personal.vein_mining}

Sneak while mining an ore with a pickaxe, and the whole vein comes with it – diagonal connections too. Each block costs durability.

- All ores, including nether ores and ancient debris
- Exactly the same kind only: iron ore doesn't take deepslate iron ore with it
- The pickaxe must be able to mine the ore

## Area Mining 3×3 {#personal.area_mining}

Pickaxe and shovel mine 3×3, across the side you hit: forward gives a wall, downward a floor area. Each block costs durability.

Only natural blocks of the same group as the block you hit come along, the same in both directions:

- Pickaxe: rock – stone, cobblestone, granite, diorite, andesite, tuff, calcite, dripstone block, deepslate, sandstone, terracotta, netherrack, basalt, blackstone, end stone
- Pickaxe: obsidian – only obsidian, a group of its own
- Shovel: ground – grass block, dirt, coarse dirt, podzol, mycelium, dirt path, farmland, mud, clay, gravel, sand, soul sand, soul soil, snow block

Ores, building blocks such as stone bricks, stairs and walls, and everything else stay. If the block you hit is not in the group itself, only that block breaks.

While sneaking at ores or logs, Vein Mining and Tree Felling take priority.

## Durability {#personal.unbreaking_bonus}

Counts as extra levels of Unbreaking, even beyond III, on everything you wear or hold:

- Armor and elytra
- Tools, weapons, shield, fishing rod, bow … in main hand and offhand

Items in your inventory do not count.

Tools and elytra last much longer: a diamond pickaxe with Unbreaking III lasts about 6,200 blocks, with level 2 of this upgrade about 9,400. Armor barely benefits, vanilla calculates Unbreaking more weakly there.

## Protection {#personal.protection_bonus}

Each worn armor piece counts one more level of Protection, even without the enchantment. Vanilla caps the total protection from enchantments at 80%: a set with Protection IV reaches it exactly with this upgrade.

## Fortune+ {#personal.fortune_bonus}

Tools in your hand count extra levels of Fortune, even beyond the maximum: pickaxe, axe, shovel, hoe.

- No effect together with Silk Touch.
- Also works with Area Harvest, Vein Mining and Tree Felling.

## Looting+ {#personal.looting_bonus}

The weapon in your main hand counts extra levels of Looting, even beyond the maximum.

- Swords and spears only
- Not axes, maces, tridents, bows or crossbows

## Fishing Treasure {#personal.fishing_treasure}

A chance per catch of a structure find: pottery sherds, armor trim templates, heart of the sea, trial keys, enchanted books or a silver sack.

Open water only: a 5×5 area of water must surround the bobber, with only air or lily pads above it. A 1×1 hole gives no finds.

## Double Bobbers {#personal.double_bobbers}

Every catch brings two items – anywhere, even from a 1×1 hole. In open water both can be treasure or, with Fishing Treasure, structure finds.

## Rush {#personal.rush}

Triggers when you kill a monster, with arrows or tridents too: zombies, skeletons, creepers, spiders, slimes, ghasts, hoglins, shulkers, phantoms …

Animals, villagers and players do not count.

## Barrier {#personal.barrier}

Triggers on hits from monsters, players, arrows and creeper explosions, and on fire or lava.

Not on fall damage, drowning, poison, hunger or suffocation – so the shield is still there when the fight starts.

## Updraft {#personal.updraft}

Only firework rockets you fire while gliding with elytra. Works everywhere, even outside the territory and in other dimensions.

## Double Jump {#personal.double_jump}

A second jump in mid-air, once until you land. Not in water, while climbing, riding, flying or gliding with elytra.

## Fishing Net {#personal.fishing_net}

Press the skill key to catch every cod, salmon, tropical fish and pufferfish swimming in the water within 3 blocks. The catch goes straight into your inventory.

- Fish from a bucket – in an aquarium, say – are spared.
- With no fish in reach nothing happens, and the cooldown does not start.

## Quick Delivery {#personal.quick_delivery}

More bonus for fast deliverers, less patience in return: 30% instead of 20% time bonus within the same bonus window. After that the bonus is gone, you get the full reward for one day, then the contract loses 20% instead of 10% per day, down to half.

- Tier 1: 130% until day 3, 100% until day 4, 80% until day 5, 60% until day 6, then 50%
- Applies to all your contracts and cannot be switched off.

*Tip: only for players who reliably finish within the bonus window – one day after it ends, a contract without Quick Delivery pays more.*

## Contract Clock {#personal.contract_clock}

Hover over a clock in your inventory: its tooltip lists every accepted contract with delivery progress, current value and the time until the next drop, e.g. "Carrot: 23 / 64, 120%, 0.6 d".

Sneak + right-click a market scale with the clock to bind the clock to the scale's item. It then also shows that item's market price, e.g. "Wheat: 12 T, 85%". Bind again for another item; an empty scale unbinds the clock.

## Remote Delivery {#personal.remote_delivery}

Sneak + right-click with the clock in the air to deliver matching items from your inventory and hotbar to your contracts – anywhere in the territory, no board needed. Armor and off-hand stay untouched.

The reward goes into your inventory. Carried Taler are lost on death, so deposit them soon.

- Cooldown: 5 seconds
- Requires the Contract Clock

# Mind the Depths!

## Darkness Below {#depth warning}

Below the deepslate line, darkness surrounds you. Nothing is forbidden down there – but it isn't pleasant either.

You can unlock the depths at the beacon.

A nether portal also stays dark until you unlock the Nether at the beacon.

# Rumors

## A Rumor {#rumors}

Lots of taler in one place don't go unnoticed for long.

There are tales of a trader who drops by once enough has piled up in your deposit chests – selling things no market has.
